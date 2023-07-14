package ch.unil.pafanalysis.analysis.steps.remove_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import org.springframework.stereotype.Service


@Service
class RemoveColumnsPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, RemoveColumns::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Remove columns", plotWidth = plotWidth))

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Nr of columns" to Paragraph(res.nrOfColumns.toString()),
            "Nr of columns removed" to Paragraph(res.nrOfColumnsRemoved.toString())
        )

        stepDiv.add(addTwoRowTable(tableData))
        return stepDiv
    }

}
