package ch.unil.pafanalysis.analysis.steps.normalization

data class Normalization(
    val min: Double?,
    val max: Double?,
    val mean: Double?,
    val median: Double?,
    val nrNans: Int?,
    val sum: Double?
)