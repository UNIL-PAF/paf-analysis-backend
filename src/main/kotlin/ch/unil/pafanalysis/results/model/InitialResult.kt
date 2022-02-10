package ch.unil.pafanalysis.results.model

import java.time.LocalDateTime
import javax.persistence.*

@Entity
class InitialResult (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    val status: String? = null,
    val resFile: String? = null,
    val path: String? = null,
    val fileCreationDate: LocalDateTime? = null,
    val lastModifDate: LocalDateTime? = null
)