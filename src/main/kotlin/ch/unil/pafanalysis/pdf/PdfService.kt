package ch.unil.pafanalysis.pdf

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.zip.ZipDataSelection
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.colors.WebColors
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.VerticalAlignment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Service
class PdfService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var commonStep: CommonStep? = null

    private val fontSizeConst = 8f
    val myFont = StandardFonts.HELVETICA
    val myBoldFont = StandardFonts.HELVETICA_BOLD
    val antCyan = DeviceRgb(244, 240, 236)

    fun createPdf(analysisId: Int, zipSelection: ZipDataSelection? = null): File {
        val analysis = analysisRepo?.findById(analysisId)
        val steps = analysisService?.sortAnalysisSteps(analysis?.analysisSteps)

        val filePath = kotlin.io.path.createTempFile(suffix = ".pdf").toFile()
        val pdf = PdfDocument(PdfWriter(filePath))

        val pageSize: PageSize = PageSize.A4
        val document: Document? = Document(pdf, pageSize, false)

        val plotWidth: Float = pageSize?.width?.minus(document?.rightMargin?: 0f)?.minus(document?.leftMargin?: 0f)

        addLogo(document, pdf)
        addResultInfo(analysis, document)
        addSteps(steps, document, pdf, plotWidth, zipSelection)
        addHeaderAndFooter(document, pdf, pageSize, plotWidth)

        document?.close()
        pdf.close()

        return filePath
    }

    private fun addResultInfo(analysis: Analysis?, document: Document?){
        val div = Div()

        val result: Result? = analysis?.result

        val title = Paragraph(result?.name).setFont(PdfFontFactory.createFont(myBoldFont))
            .setFontSize(14f)
            .setBackgroundColor(antCyan)
            .setPaddingLeft(5f)
            .setFontColor(ColorConstants.BLACK)
        title.setMarginTop(0f)
        div.add(title)
        div.setPaddingTop(0f)

        val infoTable = Table(2).setPaddingLeft(5f)

        addInfoCell("Description", result?.description ?: "", infoTable)

        val analysisName = if(analysis?.name != null) analysis.name else if(analysis?.idx != null) "#${analysis.idx + 1}" else ""
        addInfoCell("Analysis", analysisName, infoTable)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        addInfoCell("Report creation date", LocalDateTime.now().format(formatter), infoTable)

        div.setBorder(SolidBorder(antCyan, 1f))
        div.setMarginTop(10f)
        div.setMarginBottom(20f)
        div.add(infoTable)

        document?.add(div)
    }

    private fun addInfoCell(name: String, cont: String, table: Table) {
        val cell1 = Cell().setBorder(Border.NO_BORDER)
        val p1 = Paragraph(name).setFont(PdfFontFactory.createFont(myBoldFont)).setFontSize(fontSizeConst)
        cell1.add(p1)
        val cell2 = Cell().setBorder(Border.NO_BORDER)
        val p2 = Paragraph(cont).setFont(PdfFontFactory.createFont(myFont)).setFontSize(fontSizeConst)
        cell2.add(p2)
        table.addCell(cell1)
        table.addCell(cell2)
    }

    private fun addLogo(document: Document?, pdf: PdfDocument){
        val imagePath = "/resources/images/lo_unil06_bleu.pdf"
        val unilLogoServer = File(ClassPathResource(imagePath).path)
        val unilLogo = if(unilLogoServer.exists()) unilLogoServer else File(ClassPathResource("/src/main$imagePath").path)
        val sourcePdf = PdfDocument(PdfReader(unilLogo))
        val pdfPlot = sourcePdf.getPage(1)
        val pdfPlotCopy: PdfFormXObject = pdfPlot.copyAsFormXObject(pdf)
        sourcePdf.close()
        val img = Image(pdfPlotCopy)
        img.scaleToFit(156f/2, 58f/2)
        val p = Paragraph()
        p.add(img)
        document?.add(p)
    }

    private fun commentDiv(comment: String): Div {
        val p1 = Paragraph(comment).setFont(PdfFontFactory.createFont(myFont))
        p1.setFontColor(DeviceRgb(0, 109, 117))
        p1.setFontSize(fontSizeConst)
        p1.setPaddingLeft(5f)
        val div = Div()
        div.add(p1)
        return div
    }

    private fun addSteps(steps: List<AnalysisStep>?, document: Document?, pdf: PdfDocument, plotWidth: Float,  zipSelection: ZipDataSelection? = null){
        var removedSteps = 0
        steps?.forEachIndexed { i, step ->
            val div = commonStep?.getRunner(step.type)?.createPdf(step, pdf, plotWidth, i + 1 - removedSteps)
            if(step.comments != null) div?.add(commentDiv(step.comments))
            div?.setMarginBottom(15f)
            div?.isKeepTogether = true
            // only add the step if it is in the zipSelection
            if(zipSelection?.steps === null || zipSelection?.steps.contains(step.id!!)){
                document?.add(div)
            }else removedSteps++
        }
    }

    private fun addHeaderAndFooter(document: Document?, pdf: PdfDocument, pageSize: PageSize, plotWidth: Float){
        val headerFontSize = 8f

        // paging position
        val xPosNum = plotWidth/2 + 30f
        val yPosNum = 25f

        // left header pos
        val xPosLeft = 55f
        val yPosTop = pageSize?.height.minus(10f)
        val leftHeader = Paragraph("UNIL - PAF").setFont(PdfFontFactory.createFont(myFont)).setFontSize(headerFontSize)

        // right header
        val xPosRight= pageSize?.width.minus(15f)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val rightHeader = Paragraph(LocalDateTime.now().format(formatter)).setFont(PdfFontFactory.createFont(myFont)).setFontSize(headerFontSize)

        val numberOfPages: Int = pdf.getNumberOfPages()
        for (i in 1..numberOfPages) {
            // Write aligned text to the specified by parameters point
            document?.showTextAligned(
                Paragraph(String.format("page %s of %s", i, numberOfPages)).setFontSize(headerFontSize),
                xPosNum, yPosNum, i, TextAlignment.CENTER, VerticalAlignment.TOP, 0f
            )

            document?.showTextAligned(
                leftHeader, xPosLeft, yPosTop, i, TextAlignment.RIGHT, VerticalAlignment.TOP, 0f
            )

            document?.showTextAligned(
                rightHeader, xPosRight, yPosTop, i, TextAlignment.RIGHT, VerticalAlignment.TOP, 0f
            )
        }
    }

}