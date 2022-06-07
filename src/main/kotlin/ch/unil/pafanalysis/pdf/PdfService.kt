package ch.unil.pafanalysis.pdf

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.svg.converter.SvgConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.FileInputStream


@Service
class PdfService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var commonStep: CommonStep? = null


    fun createPdf(analysisId: Int): String {
        val analysis = analysisRepo?.findById(analysisId)
        val steps = analysisService?.sortAnalysisSteps(analysis?.analysisSteps)

        val filePath = "/tmp/a.pdf"
        val pdf = PdfDocument(PdfWriter(filePath))
        val document = Document(pdf)

        steps?.forEach{
            val title = it.type
            document.add(Paragraph(title))

            

            if(it.type == AnalysisStepType.BOXPLOT.value){
                val image: Image = SvgConverter.convertToImage(FileInputStream("/tmp/bar.svg"), pdf)
                image.scaleToFit(300f, 300f)
                document.add(image)
            }

        }

        val outputRoot = commonStep?.getOutputRoot(commonStep?.getResultType(analysis?.result?.type))
        document.close()

        return filePath
    }

    fun createEcharts(step: AnalysisStep): String? {
        return step.resultPath + "/echarts.svg"
    }

}