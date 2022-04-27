package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import com.google.common.math.Quantiles
import com.google.gson.Gson
import org.springframework.stereotype.Service
import kotlin.math.log2

@Service
class BoxPlotRunner() : CommonStep() {

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    private val gson = Gson()

    private var columnMapping: ColumnMapping? = null

    private val readTableData = ReadTableData()

    fun run(oldStepId: Int): AnalysisStepStatus {
        val newStep = runCommonStep(AnalysisStepType.BOXPLOT, oldStepId, false)
        columnMapping = newStep?.columnInfo?.columnMapping
        val boxplot = createBoxplotObj(newStep)
        val updatedStep = newStep?.copy(status = AnalysisStepStatus.DONE.value, results = gson.toJson(boxplot))
        analysisStepRepository?.save(updatedStep!!)
        return AnalysisStepStatus.DONE
    }

    fun updateParams(analysisStep: AnalysisStep, params: String): AnalysisStepStatus {
        setPathes(analysisStep.analysis)
        columnMapping = analysisStep?.columnInfo?.columnMapping
        val stepWithParams = analysisStep.copy(parameters = params)

        val oldStep =
            if (analysisStep?.beforeId != null) analysisStepRepository?.findById(analysisStep?.beforeId) else null
        val newHash = computeStepHash(stepWithParams, oldStep)
        val boxplot = createBoxplotObj(stepWithParams)

        val newStep = stepWithParams.copy(
            status = AnalysisStepStatus.DONE.value,
            results = gson.toJson(boxplot),
            stepHash = newHash
        )
        analysisStepRepository?.save(newStep!!)
        return AnalysisStepStatus.DONE
    }

    override fun computeAndUpdate(step: AnalysisStep, stepBefore: AnalysisStep, newHash: Long) {
        setPathes(step.analysis)
        columnMapping = step?.columnInfo?.columnMapping
        val stepWithNewResTable =
            step.copy(resultTableHash = stepBefore?.resultTableHash, resultTablePath = stepBefore?.resultTablePath)
        val boxplot = createBoxplotObj(stepWithNewResTable)
        val stepToSave = stepWithNewResTable.copy(results = gson.toJson(boxplot))
        analysisStepRepository?.save(stepToSave)
    }

    private fun createBoxplotObj(analysisStep: AnalysisStep?): BoxPlot {
        val expDetailsTable = columnMapping?.experimentNames?.map { name ->
            columnMapping?.experimentDetails?.get(name)
        }?.filter { it?.isSelected ?: false }

        val experimentNames = expDetailsTable?.map { it?.name!! }
        val groupedExpDetails: Map<String?, List<ExpInfo?>>? = expDetailsTable?.groupBy { it?.group }
        val boxplotGroupData = groupedExpDetails?.mapKeys { createBoxplotGroupData(it.key, it.value, analysisStep) }

        return BoxPlot(experimentNames = experimentNames, data = boxplotGroupData?.keys?.toList())
    }

    private fun createBoxplotGroupData(
        group: String?,
        expInfoList: List<ExpInfo?>?,
        analysisStep: AnalysisStep?
    ): BoxPlotGroupData {
        val logScale = if (analysisStep?.parameters != null) {
            val boxPlotParams: BoxPlotParams = gson.fromJson(analysisStep.parameters, BoxPlotParams().javaClass)
            boxPlotParams.logScale
        } else false

        val intColumn = if (analysisStep?.parameters != null) {
            val boxPlotParams: BoxPlotParams = gson.fromJson(analysisStep.parameters, BoxPlotParams().javaClass)
            boxPlotParams.column
        } else null

        val listOfInts = readTableData.getListOfInts(expInfoList, analysisStep, outputRoot, intColumn)
        val listOfBoxplots = listOfInts.map { BoxPlotData(it.first, computeBoxplotData(it.second, logScale)) }
        return BoxPlotGroupData(group = group, data = listOfBoxplots)
    }


    private fun computeBoxplotData(intsX: List<Double>, logScale: Boolean?): List<Double>? {
        val ints = intsX.filter { !it.isNaN() }

        val normInts = if (logScale != false) {
            ints.filter { it != 0.0 }.map { log2(it) }
        } else {
            ints
        }

        val min = normInts.minOrNull()!!
        val q25: Double = Quantiles.percentiles().index(25).compute(normInts)
        val q50: Double = Quantiles.percentiles().index(50).compute(normInts)
        val q75: Double = Quantiles.percentiles().index(75).compute(normInts)
        val max = normInts.maxOrNull()!!
        return listOf(min, q25, q50, q75, max)
    }

}