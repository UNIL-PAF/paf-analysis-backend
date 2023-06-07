package ch.unil.pafanalysis.pdf

import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File


@Service
class PdfService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var commonStep: CommonStep? = null

    fun createPdf(analysisId: Int): File {
        val analysis = analysisRepo?.findById(analysisId)
        val steps = analysisService?.sortAnalysisSteps(analysis?.analysisSteps)

        val filePath = kotlin.io.path.createTempFile(suffix = ".pdf").toFile()
        val pdf = PdfDocument(PdfWriter(filePath))

        val pageSize: PageSize = PageSize.A4
        val document: Document? = Document(pdf, pageSize)

        val plotWidth: Float = pageSize?.width?.minus(document?.rightMargin?: 0f)?.minus(document?.leftMargin?: 0f)

        steps?.forEachIndexed { i, step ->
            val div = commonStep?.getRunner(step.type)?.createPdf(step, pdf, plotWidth, i + 1)
            div?.isKeepTogether = true
            document?.add(div)
        }

        document?.close()
        pdf.close()

        return filePath
    }

}