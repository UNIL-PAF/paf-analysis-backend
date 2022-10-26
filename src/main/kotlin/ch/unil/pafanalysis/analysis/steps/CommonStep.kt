package ch.unil.pafanalysis.analysis.steps

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.service.ColumnInfoRepository
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlot
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlotRunner
import ch.unil.pafanalysis.analysis.steps.filter.FilterParams
import ch.unil.pafanalysis.analysis.steps.filter.FilterRunner
import ch.unil.pafanalysis.analysis.steps.group_filter.GroupFilterRunner
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.analysis.steps.t_test.TTestRunner
import ch.unil.pafanalysis.analysis.steps.transformation.TransformationRunner
import ch.unil.pafanalysis.analysis.steps.volcano.VolcanoPlotRunner
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.results.model.ResultType
import com.google.gson.Gson
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.layout.element.Image
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDateTime

@Service
open class CommonStep {

    @Autowired
    var analysisStepRepository: AnalysisStepRepository? = null

    @Autowired
    var analysisRepository: AnalysisRepository? = null

    @Autowired
    var columnInfoRepository: ColumnInfoRepository? = null

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var boxPlotRunner: BoxPlotRunner? = null

    @Autowired
    private var transformationRunner: TransformationRunner? = null

    @Autowired
    private var initialResultRunner: InitialResultRunner? = null

    @Autowired
    private var filterRunner: FilterRunner? = null

    @Autowired
    private var groupFilterRunner: GroupFilterRunner? = null

    @Autowired
    private var tTestRunner: TTestRunner? = null

    @Autowired
    private var volcanoPlotRunner: VolcanoPlotRunner? = null

    val gson = Gson()

    val hashComp: Crc32HashComputations = Crc32HashComputations()

    var type: AnalysisStepType? = null

    fun runCommonStep(
        type: AnalysisStepType,
        oldStepId: Int? = null,
        modifiesResult: Boolean? = null,
        step: AnalysisStep? = null,
        params: String? = null,
    ): AnalysisStep? {
        val oldStep: AnalysisStep? = if (oldStepId != null) {
            analysisStepRepository?.findById(oldStepId)
        } else {
            null
        }

        val currentStep = step ?: createEmptyAnalysisStep(oldStep, type, modifiesResult)

        try {
            val stepWithParams =
                if (params != null) currentStep?.copy(parameters = params) else currentStep

            val stepPath = setMainPaths(oldStep?.analysis, stepWithParams)
            val resultType = getResultType(stepWithParams?.analysis?.result?.type)
            val resultTablePathAndHash =
                getResultTablePath(modifiesResult, oldStep, stepPath, stepWithParams?.resultTablePath, resultType)

            val newStep =
                stepWithParams?.copy(
                    resultPath = stepPath,
                    resultTablePath = resultTablePathAndHash.first,
                    resultTableHash = resultTablePathAndHash.second,
                    status = AnalysisStepStatus.RUNNING.value,
                    commonResult = oldStep?.commonResult,
                    tableNr = if(stepWithParams?.modifiesResult == true){ oldStep?.tableNr?.plus(1)} else oldStep?.tableNr
                )
            return analysisStepRepository?.saveAndFlush(newStep!!)
        } catch(e: Exception){
            println("Error in runCommonStep ${currentStep?.id}")
            e.printStackTrace()
            return analysisStepRepository?.saveAndFlush(
                currentStep!!.copy(
                    status = AnalysisStepStatus.ERROR.value,
                    error = e.message,
                    stepHash = Crc32HashComputations().getRandomHash()
                )
            )
        }
    }

    fun addStep(stepId: Int, stepParams: AnalysisStepParams): AnalysisStep? {
        return getRunner(stepParams.type)?.run(stepId, params = stepParams.params)
    }

    fun getRunner(type: String?): CommonRunner? {
        return when (type) {
            AnalysisStepType.INITIAL_RESULT.value -> initialResultRunner
            AnalysisStepType.BOXPLOT.value -> boxPlotRunner
            AnalysisStepType.TRANSFORMATION.value -> transformationRunner
            AnalysisStepType.FILTER.value -> filterRunner
            AnalysisStepType.GROUP_FILTER.value -> groupFilterRunner
            AnalysisStepType.T_TEST.value -> tTestRunner
            AnalysisStepType.VOLCANO_PLOT.value -> volcanoPlotRunner
            else -> throw StepException("Analysis step [$type] not found.")
        }
    }

    fun getResultPath(analysis: Analysis?): String? {
        val resultType = getResultType(analysis?.result?.type)
        return env?.getProperty(if (resultType == ResultType.MaxQuant) "result.path.maxquant" else "result.path.spectronaut") + analysis?.result?.path
    }

    fun getResultType(type: String?): ResultType? {
        return if (type == ResultType.MaxQuant.value) ResultType.MaxQuant else ResultType.Spectronaut
    }

    fun getOutputRoot(): String? {
        return env?.getProperty("output.path")
    }

    fun setMainPaths(analysis: Analysis?, emptyStep: AnalysisStep?): String {
        val outputPath: String = analysis?.id.toString()
        val outputRoot = getOutputRoot()
        createResultDir(outputRoot?.plus(outputPath))

        val stepPath = "$outputPath/${emptyStep?.id}"
        createResultDir(outputRoot?.plus(stepPath))
        return stepPath
    }

    fun getCopyDifference(step: AnalysisStep?): String? {
        return if (step?.parametersHash != null && step?.copyFromId != null) {
            val origStep = analysisStepRepository?.findById(step.copyFromId)
            if (step.parametersHash != origStep?.parametersHash) {
                getRunner(step.type)?.getCopyDifference(step, origStep)
            } else null
        } else null
    }

    fun createEmptyAnalysisStep(
        oldStep: AnalysisStep?,
        type: AnalysisStepType,
        modifiesResult: Boolean? = null
    ): AnalysisStep? {
        val newStep = AnalysisStep(
            status = AnalysisStepStatus.RUNNING.value,
            type = type.value,
            analysis = oldStep?.analysis,
            commonResult = oldStep?.commonResult,
            lastModifDate = LocalDateTime.now(),
            beforeId = oldStep?.id,
            nextId = oldStep?.nextId,
            columnInfo = oldStep?.columnInfo,
            modifiesResult = modifiesResult
        )
        val insertedStep = analysisStepRepository?.saveAndFlush(newStep)
        if (oldStep?.nextId != null) setNextStepBeforeId(oldStep?.nextId, insertedStep?.id)
        updateOldStep(oldStep, insertedStep?.id)
        return insertedStep
    }

    fun updateNextStep(step: AnalysisStep) {
        if (step.nextId != null) {
            val nextStep = analysisStepRepository?.findById(step.nextId!!)
            update(nextStep!!, step)
        }
    }

    fun makeEchartsPlot(step: AnalysisStep, pdf: PdfDocument): Image? {

        val results = gson.fromJson(step.results, BoxPlot::class.java)
        val echartsPlot = results.plot?.copy(outputPath = step.resultPath)

        val echartsServerUrl = env?.getProperty("echarts.server.url").plus("/pdf")

        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        val request = HttpRequest.newBuilder()
            .uri(URI.create(echartsServerUrl))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(echartsPlot)))
            .build();
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val outputRoot = getOutputRoot()

        val pdfPath: String = outputRoot + response.body()
        val sourcePdf = PdfDocument(PdfReader(pdfPath))
        val pdfPlot = sourcePdf.getPage(1)
        val pdfPlotCopy: PdfFormXObject = pdfPlot.copyAsFormXObject(pdf)
        return Image(pdfPlotCopy)
    }

    fun getSelProts(step: AnalysisStep?): List<String>? {
        return when (step?.type) {
            AnalysisStepType.BOXPLOT.value -> boxPlotRunner?.getParameters(step)?.selProts
            AnalysisStepType.VOLCANO_PLOT.value -> volcanoPlotRunner?.getParameters(step)?.selProteins
            else -> null
        }
    }

    private fun paramToHash(step: AnalysisStep?): Long? {
        val filterParams = when (step?.type) {
            AnalysisStepType.BOXPLOT.value -> boxPlotRunner?.getParameters(step).toString()
            AnalysisStepType.FILTER.value -> filterRunner?.getParameters(step).toString()
            AnalysisStepType.GROUP_FILTER.value -> groupFilterRunner?.getParameters(step).toString()
            AnalysisStepType.T_TEST.value -> tTestRunner?.getParameters(step).toString()
            AnalysisStepType.TRANSFORMATION.value -> transformationRunner?.getParameters(step).toString()
            AnalysisStepType.VOLCANO_PLOT.value -> volcanoPlotRunner?.getParameters(step).toString()
            else -> throw RuntimeException("Cannot parse parameters for type [${step?.type}]")
        }
        return hashComp.computeStringHash(filterParams)
    }

    fun tryToRun(runFun: () -> AnalysisStep?, step: AnalysisStep?) {
        try {
            val step = runFun()

            val stepBefore = analysisStepRepository?.findById(step!!.beforeId!!)

            val stepWithFileHash = step?.copy(
                resultTableHash = hashComp.computeFileHash(File(getOutputRoot() + step.resultTablePath)),
                parametersHash =  paramToHash(step)
            )

            val newHash = computeStepHash(stepWithFileHash, stepBefore)

            val updatedStep =
                stepWithFileHash?.copy(
                    status = AnalysisStepStatus.DONE.value,
                    stepHash = newHash,
                    copyDifference = getCopyDifference(stepWithFileHash)
                )
            analysisStepRepository?.saveAndFlush(updatedStep!!)!!
            updateNextStep(updatedStep!!)
        } catch (e: Exception) {
            println("Error in transformation asyncRun ${step?.id}")
            e.printStackTrace()
            analysisStepRepository?.saveAndFlush(
                step!!.copy(
                    status = AnalysisStepStatus.ERROR.value,
                    error = e.message,
                    stepHash = Crc32HashComputations().getRandomHash()
                )
            )
        }
    }

    fun update(step: AnalysisStep, stepBefore: AnalysisStep) {
        val newHash = computeStepHash(step, stepBefore, stepBefore.resultTableHash)

        if (newHash != step.stepHash) {
            val runningStep = analysisStepRepository?.saveAndFlush(step.copy(status = AnalysisStepStatus.RUNNING.value))
            try {
                getRunner(runningStep!!.type)?.run(stepBefore.id!!, runningStep)
            } catch (e: Exception) {
                println("Error in update ${runningStep?.id}")
                e.printStackTrace()
                analysisStepRepository?.saveAndFlush(
                    runningStep!!.copy(
                        status = AnalysisStepStatus.ERROR.value,
                        error = e.message,
                        stepHash = Crc32HashComputations().getRandomHash()
                    )
                )
            }
        } else {
            analysisStepRepository?.saveAndFlush(step.copy(status = AnalysisStepStatus.DONE.value))
            updateNextStep(step!!)
        }
    }

    fun computeStepHash(
        step: AnalysisStep?,
        stepBefore: AnalysisStep? = null,
        resultTableHash: Long? = null
    ): Long? {
        val paramsHash = (step?.parametersHash ?: 0).toString()
        val columnsMappingHash = step?.columnInfo?.columnMappingHash.toString()
        val resultTableHash = (resultTableHash ?: stepBefore?.resultTableHash).toString()
        val commonHash = (step?.commonResult ?: 0).toString()
        val combinedHashString = "$paramsHash:$columnsMappingHash:$resultTableHash:$commonHash"
        return hashComp.computeStringHash(combinedHashString)
    }

    private fun updateOldStep(oldStep: AnalysisStep?, newNextId: Int?) {
        val updatedOldStep = oldStep?.copy(nextId = newNextId)
        analysisStepRepository?.saveAndFlush(updatedOldStep!!)
    }

    private fun setNextStepBeforeId(nextStepId: Int, currentStepId: Int?) {
        val nextStep = analysisStepRepository?.findById(nextStepId)
        val newNextStep = nextStep?.copy(beforeId = currentStepId)
        analysisStepRepository?.saveAndFlush(newNextStep!!)
    }

    fun getResultTablePath(
        modifiesResult: Boolean?,
        oldStep: AnalysisStep?,
        stepPath: String?,
        oldTablePath: String?,
        resultType: ResultType?
    ): Pair<String?, Long?> {
        val pathAndHash = if (modifiesResult != null && modifiesResult) {
            val oldTab = getOutputRoot()?.plus("/") + oldStep?.resultTablePath
            val tabName = if (resultType == ResultType.MaxQuant) {
                "/proteinGroups_"
            } else {
                "/Report_"
            }
            val newTab = stepPath + tabName + Timestamp(System.currentTimeMillis()).time + ".txt"
            val newFile = File(getOutputRoot()?.plus(newTab))
            File(oldTab).copyTo(newFile)

            // remove old if exists
            if (oldTablePath != null) File(getOutputRoot()?.plus("/")?.plus(oldTablePath)).delete()

            Pair(newTab, Crc32HashComputations().computeFileHash(newFile))
        } else {
            Pair(oldStep?.resultTablePath, oldStep?.resultTableHash)
        }

        return pathAndHash
    }

    private fun createResultDir(outputPath: String?): String {
        if (outputPath == null) throw RuntimeException("There is no output path defined.")
        val dir = File(outputPath)
        if (!dir.exists()) dir.mkdir()
        return outputPath
    }
}