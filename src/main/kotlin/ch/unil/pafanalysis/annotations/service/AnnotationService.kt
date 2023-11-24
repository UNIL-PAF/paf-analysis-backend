package ch.unil.pafanalysis.annotations.service

import ch.unil.pafanalysis.annotations.model.AnnotationInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.time.LocalDateTime


@Service
class AnnotationService {

    @Autowired
    private var annotationRepo: AnnotationRepository? = null

    @Autowired
    private var env: Environment? = null

    private fun getAnnotationPath(): String? {
        return env?.getProperty("result.path.annotations")
    }

    fun delete(annotationId: Int): Int? {
        val annot = annotationRepo?.findById(annotationId) ?: throw Exception("Could not load annotation [$annotationId].")
        val uploadedFile = File(getAnnotationPath() + annot.fileName)
        uploadedFile.delete()
        return annotationRepo?.deleteById(annotationId)
    }

    fun createNewAnnotation(file: MultipartFile, name: String?, description: String?): Int? {
        val creationTime = LocalDateTime.now()
        val origFileName = file.originalFilename
        val currentDateTime: java.util.Date = java.util.Date()
        val currentTimestamp: Long = currentDateTime.time

        val fileName = "${origFileName}_$currentTimestamp"

        val uploadedFile = File(getAnnotationPath() + fileName)
        file.transferTo(uploadedFile)

        val newAnnotation = AnnotationInfo(
            name = name,
            description = description,
            fileName = fileName,
            origFileName = origFileName,
            nrEntries = getNrEntries(uploadedFile),
            creationDate = creationTime
        )

        val inserted = annotationRepo?.saveAndFlush(newAnnotation)
        return inserted?.id
    }

    private fun getNrEntries(file: File): Int{
        val reader = BufferedReader(FileReader(file))
        // ignore header
        reader.readLine()

        return reader.readLines().fold(0){acc, l ->
            if(l.contains("Type")) acc else acc+1
        }
    }
}