package ch.unil.pafanalysis.analysis.steps.order_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*

@Service
class AsyncOrderColumnsRunner() : CommonStep() {

    @Autowired
    private var env: Environment? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, OrderColumnsParams().javaClass)

            val origTable = ReadTableData().getTable(
                env?.getProperty("output.path").plus(newStep?.resultTablePath),
                newStep?.commonResult?.headers
            )

            val intCol = newStep?.columnInfo?.columnMapping?.intCol
            val order1 =  if(params.moveSelIntFirst == true) moveSelIntFirst(origTable, intCol) else origTable
            val order2 = changeOrder(order1, params.move)

            WriteTableData().write(
                env?.getProperty("output.path")?.plus(newStep?.resultTablePath)!!,
                order2!!)

            newStep?.copy(
                results = gson.toJson(
                    OrderColumns(
                    )
                ),
                commonResult = newStep?.commonResult?.copy(headers = order2?.headers)
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun changeOrder(table: Table?, move: List<MoveCol>?): Table? {
        return move?.fold(table){ acc, mov ->
            val newHeaders: List<Header>? = acc?.headers?.swap(mov.from!!, mov.to!!)
            val columns =  acc?.cols?.swap(mov.from!!, mov.to!!)
            Table(newHeaders?.mapIndexed{ i, h -> h.copy(idx = i)}, columns)
        }
    }

    private fun <T> List<T>.swap(from: Int, to: Int): List<T>? {
        val my = this.toMutableList()
        val tmp = my[from]
        my.add(to, tmp)
        my.removeAt(from)
        return my.toList()
    }

    private fun moveSelIntFirst(table: Table?, intCol: String?): Table? {
        val emptyBeforeAndAfter = Pair(Pair(emptyList<Header>(), emptyList<Header>()), Pair(listOf(emptyList<Any>()), listOf(emptyList<Any>())))

        val newRes = table?.headers?.zip(table?.cols!!)?.fold(emptyBeforeAndAfter) { acc, col ->
            if(col.first.experiment != null && col.first.experiment?.field == intCol){
                val newHeaders: Pair<List<Header>, List<Header>>  = Pair(acc.first.first + col.first, acc.first.second)
                val addThis = if(acc.second.first.last().isEmpty()) listOf(col.second) else acc.second.first.plusElement(col.second)
                val newCols: Pair<List<List<Any>>, List<List<Any>>> = Pair(addThis, acc.second.second)
                Pair(newHeaders, newCols)
            }else{
                val newHeaders: Pair<List<Header>, List<Header>> = Pair(acc.first.first, acc.first.second + col.first)
                val addThis = if(acc.second.second.last().isEmpty()) listOf(col.second) else acc.second.second.plusElement(col.second)
                val newCols: Pair<List<List<Any>>, List<List<Any>>> = Pair(acc.second.first, addThis)
                Pair(newHeaders, newCols)
            }
        }


        val newHeaders = newRes?.first?.first?.plus(newRes?.first?.second)
        val corrIdxHeaders = newHeaders?.mapIndexed{ i, h -> h.copy(idx = i)}
        val newCols = newRes?.second?.first?.plus(newRes?.second?.second)

        return Table(corrIdxHeaders, newCols)
    }

}