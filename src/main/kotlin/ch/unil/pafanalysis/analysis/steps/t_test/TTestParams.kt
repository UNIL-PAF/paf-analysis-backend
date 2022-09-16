package ch.unil.pafanalysis.analysis.steps.t_test

data class TTestParams (
    val field: String? = null,
    val s0: Double? = null,
    val firstGroup: String? = null,
    val twoSided: Boolean? = null,
    val multiTestCorr: String? = MulitTestCorr.BH.value,
    val signThres: Double? = 0.05,
    val altHypothesis: String? = AltHypothesis.TWO_SIDED.value,
    val valuesAreLog: Boolean = true
)

enum class AltHypothesis(val value: String) {
    TWO_SIDED("two-sided"),
    GREATER("greater"),
    LESS("less")
}

enum class MulitTestCorr(val value: String) {
    BONFERRONI("bonferroni"),
    BH("BH"),
    BY("BY")
}