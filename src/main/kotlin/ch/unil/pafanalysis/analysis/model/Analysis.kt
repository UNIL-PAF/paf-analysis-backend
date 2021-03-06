package ch.unil.pafanalysis.analysis.model

import ch.unil.pafanalysis.results.model.Result
import com.fasterxml.jackson.annotation.JsonManagedReference
import java.time.LocalDateTime
import javax.persistence.*


@Entity
data class Analysis (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int? = null,
    val idx: Int? = null,
    val name: String? = null,
    val status: String? = null,
    val lastModifDate: LocalDateTime? = null,

    //val resultId: Int? = null,

    @OneToMany(mappedBy="analysis", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JsonManagedReference
    val analysisSteps: List<AnalysisStep>? = null,

    @ManyToOne
    @JoinColumn(name="result_id", nullable=false)
    val result: Result? = null
)