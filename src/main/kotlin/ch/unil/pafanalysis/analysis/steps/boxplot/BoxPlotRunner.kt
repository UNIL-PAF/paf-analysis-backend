package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.AnalysisStepService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.Crc32HashComputations
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.ceil

@Service
class BoxPlotRunner(): CommonStep() {

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    private val gson = Gson()

    fun run(oldStepId: Int): AnalysisStepStatus {
        val newStep = runCommonStep(AnalysisStepType.BOXPLOT, oldStepId, false)
        val boxplot = createBoxplotObj(newStep?.columnInfo?.columnMapping, newStep?.resultTablePath)
        val updatedStep = newStep?.copy(status = AnalysisStepStatus.DONE.value, results = gson.toJson(boxplot))
        analysisStepRepository?.save(updatedStep!!)

        return AnalysisStepStatus.DONE
    }

    override fun computeAndUpdate(step: AnalysisStep, stepBefore: AnalysisStep, newHash: Long) {
        setPathes(step.analysis)
        val boxplot = createBoxplotObj(step.columnInfo?.columnMapping, step.resultTablePath)

        val newStep = step.copy(resultTableHash = stepBefore?.resultTableHash, results = gson.toJson(boxplot))
        analysisStepRepository?.save(newStep)

        updateNextStep(step)
    }

    private fun createBoxplotObj(columnMapping: ColumnMapping?, resultTablePath: String?): BoxPlot {
        val expDetailsTable = columnMapping?.experimentNames?.map{ name ->
            columnMapping?.experimentDetails?.get(name)
        }?.filter{ it?.isSelected?:false }

        val experimentNames = expDetailsTable?.map{ it?.name!! }
        val groupedExpDetails: Map<String?, List<ExpInfo?>>? = expDetailsTable?.groupBy { it?.group }
        val boxplotGroupData = groupedExpDetails?.mapKeys { createBoxplotGroupData(it.key, it.value, resultTablePath) }

        return BoxPlot(experimentNames = experimentNames, data = boxplotGroupData?.keys?.toList())
    }

    private fun createBoxplotGroupData(group: String?, expInfoList: List<ExpInfo?>?, resultTablePath: String?): BoxPlotGroupData {
        val listOfInts = getListOfInts(expInfoList, resultTablePath)
        val listOfBoxplots = listOfInts.map{ BoxPlotData( it.first, computeBoxplotData(it.second)) }
        return BoxPlotGroupData(group = group, data = listOfBoxplots)
    }

    private fun getListOfInts(expInfoList: List<ExpInfo?>?, resultTablePath: String?): List<Pair<String, List<Double>>>{
        println(outputRoot?.plus(resultTablePath))

        return emptyList<Pair<String, List<Double>>>()
    }

    private fun computeBoxplotData(ints: List<Double>): List<Double>{
        val min = ints.minOrNull()!!
        val Q1 = percentile(ints, 25.0)
        val median = percentile(ints, 50.0)
        val Q3 = percentile(ints, 75.0)
        val max = ints.maxOrNull()!!
        return listOf(min, Q1, median, Q3, max)
    }

    private fun percentile(ints: List<Double>, percentile: Double): Double {
        val index = ceil(percentile / 100.0 * ints.size).toInt()
        return ints[index - 1]
    }

}