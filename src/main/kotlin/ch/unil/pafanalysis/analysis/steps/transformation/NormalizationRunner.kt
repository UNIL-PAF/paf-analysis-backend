package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.steps.StepException
import com.google.common.math.Quantiles
import org.springframework.stereotype.Service

@Service
class NormalizationRunner() {

    fun runNormalization(
        ints: List<List<Double>>,
        transformationParams: TransformationParams
    ): List<List<Double>> {
        if(transformationParams.normalizationType == NormalizationType.NONE.value) return ints

        val subtract = when (transformationParams.normalizationType) {
            NormalizationType.MEDIAN.value -> fun(orig: List<Double>): Double {
                return Quantiles.median().compute(orig)
            }
            NormalizationType.MEAN.value -> fun(orig: List<Double>): Double { return orig.average() }
            else -> {
                throw StepException("${transformationParams.normalizationType} is not implemented.")
            }
        }

        return ints.map { orig: List<Double> ->
            val noNaNs = orig.filter { !it.isNaN() }
            orig.map { it - subtract(noNaNs) }
        }
    }


}