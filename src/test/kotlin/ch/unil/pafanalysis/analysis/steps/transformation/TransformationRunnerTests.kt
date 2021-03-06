package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.BufferedWriter
import java.io.FileWriter
import java.math.BigDecimal
import java.math.RoundingMode


@SpringBootTest
class TransformationRunnerTests {

    @Autowired
    private val imputation: ImputationRunner? = null

    @Autowired
    private val normalization: NormalizationRunner? = null

    @Autowired
    private val transformation: LogTransformationRunner? = null

    @Autowired
    val colParser: ColumnMappingParser? = null

    private val readTableData = ReadTableData()

    private var ints: List<List<Double>>? = null

    @BeforeEach
    fun init() {
        val resPath = "./src/test/resources/results/maxquant/Grepper-13695-710/"
        val filePath = resPath + "proteinGroups.txt"
        val mqMapping = colParser!!.parse(filePath, resPath, ResultType.MaxQuant).first
        val table = readTableData.getTable(filePath, mqMapping)
        ints = readTableData.getDoubleMatrix(table, "LFQ.intensity").second
    }

    @Test
    fun defaultTransformationChain() {
        val params = TransformationParams(
            transformationType = TransformationType.LOG2.value,
            normalizationType = NormalizationType.MEDIAN.value,
            imputationType = ImputationType.NORMAL.value,
            imputationParams = ImputationParams()
        )
        val res1 = transformation?.runTransformation(ints!!, params)
        val res2 = normalization?.runNormalization(res1!!, params)
        val res3 = imputation?.runImputation(res2!!, params)
        val oneRes = BigDecimal(res3!![0][22]).setScale(5, RoundingMode.HALF_EVEN).toDouble()

        assert(ints!![0][22] == 0.0)
        assert(oneRes == -4.55469)
    }

}
