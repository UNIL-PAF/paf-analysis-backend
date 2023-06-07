package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.UnitValue
import org.springframework.stereotype.Service
import java.util.*


@Service
class FilterPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, Filter::class.java)

        val stepDiv = Div()
        stepDiv.add(horizontalLineDiv())
        stepDiv.add(titleDiv("$stepNr - Filter rows", step.nrProteinGroups, step.tableNr, plotWidth))

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Rows removed" to Paragraph(res.nrRowsRemoved.toString())
        )

        stepDiv.add(addTwoRowTable(tableData))
        if(step.comments != null) stepDiv.add(commentDiv(step.comments))
        return stepDiv
    }

}
