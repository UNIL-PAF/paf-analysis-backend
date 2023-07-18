package ch.unil.pafanalysis.analysis.steps.pca

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.common.EchartsServer
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class PcaRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    override var type: AnalysisStepType? = AnalysisStepType.PCA

    @Autowired
    private var echartsServer: EchartsServer? = null

    @Autowired
    var asyncBoxplotRunner: AsyncPcaRunner? = null

    @Autowired
    var pcaPdf: PcaPdf? = null

    fun getParameters(step: AnalysisStep?): PcaParams {
        return if(step?.parameters != null) gson.fromJson(step?.parameters, PcaParams().javaClass) else PcaParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        return pcaPdf?.createPdf(step, pdf, plotWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, version, oldStepId, false, step, params)
        asyncBoxplotRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        val newResults = gson.fromJson(step.results, PcaRes().javaClass).copy(plot = echartsPlot)
        val newStep = step.copy(results = gson.toJson(newResults))
        analysisStepRepository?.saveAndFlush(newStep)
        return echartsPlot.echartsHash.toString()
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        // there might be differences in selected proteins, which we ignore
        val message = (if (params.column != origParams?.column) " [Column: ${params.column}]" else "")
            .plus(if (params.scale != origParams?.scale) " [Scale: ${params.scale}]" else "")

        return if(message != "") "Parameter(s) changed:".plus(message) else null
    }

}