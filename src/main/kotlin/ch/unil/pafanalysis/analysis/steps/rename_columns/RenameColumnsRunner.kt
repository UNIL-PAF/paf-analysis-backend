package ch.unil.pafanalysis.analysis.steps.rename_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class RenameColumnsRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    @Lazy
    @Autowired
    var asyncRunner: AsyncRenameColumnsRunner? = null

    @Autowired
    var renameColumnsPdf: RenameColumnsPdf? = null

    override var type: AnalysisStepType? = AnalysisStepType.RENAME_COLUMNS

    fun getParameters(step: AnalysisStep?): RenameColumnsParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            RenameColumnsParams().javaClass
        ) else RenameColumnsParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        return renameColumnsPdf?.createPdf(step, pdf, plotWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, version, oldStepId, true, step, params)
        asyncRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(
                if (params.rename?.size != origParams?.rename?.size) " [Number of renames: ${params.rename?.size}]" else ""
            ).plus(
                if (params.addConditionNames != origParams?.addConditionNames) " [Add condition names to headers: ${params.addConditionNames}]" else ""
            )
    }

}