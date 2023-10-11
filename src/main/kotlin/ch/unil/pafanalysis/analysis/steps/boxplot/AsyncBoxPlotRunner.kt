package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import com.google.common.math.Quantiles
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.math.log2

@Service
class AsyncBoxPlotRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val hMap = HeaderTypeMapping()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val boxplot = createBoxplotObj(newStep)
            newStep?.copy(
                results = gson.toJson(boxplot),
            )
        }
        tryToRun(funToRun, newStep)
    }

    private fun createBoxplotObj(analysisStep: AnalysisStep?): BoxPlot {
        val expDetailsTable = analysisStep?.columnInfo?.columnMapping?.experimentNames?.map { name ->
            analysisStep?.columnInfo?.columnMapping?.experimentDetails?.get(name)
        }?.filter { it?.isSelected ?: false }

        val experimentNames = expDetailsTable?.map { it?.name!! }
        val groupedExpDetails: Map<String?, List<ExpInfo?>>? = expDetailsTable?.groupBy { it?.group }
        val params = gson.fromJson(analysisStep?.parameters, BoxPlotParams().javaClass)

        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.commonResult?.headers
        )
        val intCol = params?.column ?: analysisStep?.columnInfo?.columnMapping?.intCol

        val groupNames = analysisStep?.columnInfo?.columnMapping?.groupsOrdered ?: groupedExpDetails?.keys

        val boxplotGroupData = groupNames?.map { createBoxplotGroupData(it, table, intCol, analysisStep?.columnInfo?.columnMapping?.experimentDetails) }
        val selProtData = getSelProtData(table, intCol, params, analysisStep?.analysis?.result?.type, analysisStep?.columnInfo?.columnMapping?.experimentNames, analysisStep?.columnInfo?.columnMapping?.experimentDetails)

        return BoxPlot(
            experimentNames = experimentNames,
            boxPlotData = boxplotGroupData,
            selProtData = selProtData
        )
    }

    private fun getSelProtData(table: Table?, intCol: String?, params: BoxPlotParams?, resType: String?, expNames: List<String>?, expDetails: Map<String, ExpInfo>?): List<SelProtData>? {
        if (params?.selProts == null) return null
        val (headers, intMatrix) = readTableData.getDoubleMatrix(table, intCol, expDetails)

        val colOrder = expNames?.map{ n -> headers.indexOf(headers.find{it.experiment?.name == n}) }
        val orderById = colOrder?.withIndex()?.associate { (index, it) -> it to index }
        val sortedIntMatrix = intMatrix.withIndex().sortedBy { (index, _) -> orderById?.get(index) }.map{it.value}

        val protGroup = readTableData.getStringColumn(table, hMap.getCol("proteinIds", resType))?.map { it.split(";")?.get(0) }
        val genes = readTableData.getStringColumn(table, hMap.getCol("geneNames", resType))?.map { it.split(";")?.get(0) }

        val selProts = params?.selProts.map { p ->
            val i = protGroup?.indexOf(p)
            if(i != null && i >= 0){
                val ints = sortedIntMatrix.map { if (i == null) null else it[i] }
                val logInts = ints.map { if (it != null && !it.isNaN() && it > 0.0) log2(it) else null }

                val gene = if (i == null) "" else genes?.get(i)
                SelProtData(prot = p, ints = ints, logInts = logInts, gene = gene)
            }else{
                null
            }
        }

        return selProts.filterNotNull().ifEmpty { null }
    }

    private fun createBoxplotGroupData(
        group: String?,
        table: Table?,
        intCol: String?,
        expDetails: Map<String, ExpInfo>?
    ): BoxPlotGroupData {
        val (headers, ints) = readTableData.getDoubleMatrix(table, intCol, expDetails, group)

        val listOfBoxplots =
            headers.mapIndexed { i, h ->
                BoxPlotData(
                    h.experiment?.name,
                    computeBoxplotData(ints[i], false),
                    computeBoxplotData(ints[i], true)
                )
            }

        return BoxPlotGroupData(group = group, groupData = listOfBoxplots)
    }


    private fun computeBoxplotData(ints: List<Double>, logScale: Boolean?): List<Double>? {
        val normInts = if (logScale != false) {
            ints.filter { it != 0.0 && !it.isNaN() }.map { log2(it) }
        } else {
            ints
        }

        val intsFlt = normInts.filter { !it.isNaN() }

        val min = intsFlt.minOrNull()!!
        val q25: Double = Quantiles.percentiles().index(25).compute(intsFlt)
        val q50: Double = Quantiles.percentiles().index(50).compute(intsFlt)
        val q75: Double = Quantiles.percentiles().index(75).compute(intsFlt)
        val max = intsFlt.maxOrNull()!!
        return listOf(min, q25, q50, q75, max)
    }

}