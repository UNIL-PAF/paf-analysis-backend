package ch.unil.pafanalysis.common

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode


@SpringBootTest
class MultipleTestingCorrectionTests {
    private val multipleTestingCorrection = MultipleTestingCorrection()

    private val roundingPrecision = 7

    private fun roundNumber(n: Double?, precision: Int = roundingPrecision): BigDecimal {
        return if(n == null) BigDecimal(0)
        else BigDecimal(n).setScale(precision, RoundingMode.HALF_UP)
    }

    val pValues = listOf(0.5115971846305029, 0.13342150861942348, 2.1219720015088704E-4, 0.7669253914223869, 0.5386524858076908, 0.13644989387712292, 0.7669253914223869, 1.38969324751674E-8, 1.3367493932621122E-15, 1.814914092331906E-8, 1.3367493932621122E-15, 0.016672324830906114, 0.2470337210492337, 5.542336947317843E-7, 0.006282332304847211, 8.168509395259438E-25, 0.2470337210492337, 0.2755660284124305, 0.016672324830906114, 0.008015938329771034, 2.8951921617610145E-58, 2.5605596456490807E-87, 0.0013271556775395082, 4.0735974736803104E-49, 3.8713934593321652E-87, 5.9783332267038705E-5, 9.98346216121834E-90, 4.6726177634953577E-11, 0.08674141696451743, 0.004084685611214766, 0.004084685611214766, 2.7906081594612658E-19, 1.7851476825095667E-30, 1.4551904387282868E-30, 0.39451443775743555, 1.9758399605715796E-21, 0.4839096390876153, 2.0919382151005303E-11, 0.061072659647187284, 0.39451443775743555, 0.0034589626136756957, 0.0034589626136756957, 0.4657777121357213, 0.9676799508158234, 6.066335323614925E-4, 4.7706891973546046E-17, 4.770786473522798E-5, 0.0031260293195786108, 6.758694165850002E-4, 0.9736161494515179, 5.967599281842035E-4, 3.2342507664456865E-35, 7.104528477780371E-14, 0.4430657380030324, 0.8325141401783932, 1.1847328449659235E-6, 0.053661126594769445, 1.1086831300893402E-26, 1.0947316011686396E-5, 0.004844340694207448, 0.0046373190497113435, 0.01730817191029916, 1.821676046038044E-29, 2.0790119039751433E-6, 0.6254190976801952, 0.20587731663055775, 0.17891843046554157, 0.21829128080988447, 0.003440738013457443, 0.6194273359980235, 0.9407283732289775, 0.04091592012472678, 0.06373314902701196, 0.06373314902701196, 0.2101184210632555, 0.2101184210632555, 0.0015438595375611294, 0.021916139027364147, 0.006967688718530428, 3.5599584964939507E-6, 2.6500508524446117E-17, 0.017682619882513087, 1.24835333366014E-12, 0.8903808333499127, 0.41627709747346986, 0.02168840667991882, 0.0031573322910311502, 0.012568804513423529, 0.0031573322910311502, 0.5190467615543534, 5.56498383494284E-4, 4.020798674032195E-6, 2.8711402742779493E-12, 0.04582245902239023, 0.14409396498993443, 0.05225523258733006, 1.6167774694457628E-4, 0.046111035633984256, 0.6363185909989135, 3.04328912069282E-4)

    @Test
    fun checkFdrCorrection() {
        val qValues = multipleTestingCorrection.fdrCorrection(pValues)
        assert(roundNumber(qValues.first()) == roundNumber(5.813604e-01))
        assert(roundNumber(qValues.minOrNull()) == roundNumber(9.983462e-88))
        assert(roundNumber(qValues.maxOrNull()) == roundNumber(0.9736161))
        assert(roundNumber(qValues.average()) == roundNumber(0.1717161))
    }

}
