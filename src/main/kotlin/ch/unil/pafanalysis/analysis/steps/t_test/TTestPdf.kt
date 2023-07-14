package ch.unil.pafanalysis.analysis.steps.t_test

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.DottedBorder
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.Property
import com.itextpdf.layout.renderer.CellRenderer
import org.springframework.stereotype.Service


@Service
class TTestPdf() : PdfCommon() {

    val multiTestCorrText = mapOf(
        "BH" to "Benjamin & Hochberg (FDR)",
        "none" to "None"
    )

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, TTest::class.java)
        val parsedParams = gson.fromJson(step.parameters, TTestParams::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - t-test", plotWidth = plotWidth))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val paramsData: List<Pair<String, String>> = listOf(
            Pair("Significance threshold:", parsedParams.signThres.toString()),
            Pair("Multiple testing correction:", multiTestCorrText[parsedParams.multiTestCorr] ?: "")
        )
        val leftCell = getParamsCell(paramsData, 2 * cellFifth)
        colTable.addCell(leftCell)

        // 2. data
        val middleDiv = Div()
        middleDiv.add(getParagraph("Number of significant results:", bold = true, underline = true))
        val tableData: List<Pair<String, String>> = res.comparisions?.map{ comp ->
            Pair("${comp.firstGroup} - ${comp.secondGroup}:", comp.numberOfSignificant.toString())
        } ?: emptyList()
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 2 * cellFifth))

        // 3. results
        val rightDiv = Div()
        rightDiv.add(getParagraph("${step.nrProteinGroups} protein groups"))
        rightDiv.add(getParagraph("Table ${step.tableNr}"))
        colTable.addCell(getResultCell(rightDiv, cellFifth))

        stepDiv.add(colTable)
        return stepDiv
    }

}
