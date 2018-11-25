package nars.attention;

import jcog.Paper;
import jcog.Util;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.ScalarValue;
import jcog.util.FloatConsumer;
import nars.NAR;
import nars.control.DurService;
import nars.term.Termed;

/** regulates prioritization of a group of concepts to what is
 *  necessary to produce a minimum threshold anisotropic priority distribution
 *  see: https://en.wikipedia.org/wiki/Entropy_and_life
 */
@Paper
public class AttnDistributor {

    private final Iterable<? extends Termed> concepts;

    //final MiniPID control = new MiniPID(0.5f, 0.5f, 0.5f);

    //v0: bang bang
    float decay = 0.1f, grow = 0.1f;

    float lastGain = Float.NaN;

    private final FloatConsumer gain;

    final RecycledSummaryStatistics pris = new RecycledSummaryStatistics();

    public AttnDistributor(Iterable<? extends Termed> concepts, FloatConsumer gain, NAR n) {
        super();
        this.concepts = concepts;
        this.gain = gain;

        DurService.on(n, this::update);
    }
    private void update(NAR n) {




        pris.clear();
        concepts.forEach(c -> pris.accept(n.concepts.pri(c, 0)));
        long N = pris.getN();
        float range = Math.max(N * ScalarValue.EPSILON, (float) (pris.getMax()-pris.getMin()));
        float variance = (float) pris.getVariance()/range;
        float threshVariance = 1f/(range * N);


        float idealPri =
                //0.5f;
                1f/ N;

        float g = lastGain;

        if (g!=g)
            g = idealPri;

        if (variance < threshVariance) {
            if (g < idealPri)
                g += grow/N;
            else
                g -= decay/N;
        } else {
            if (g < idealPri)
                g -= decay/N;
            else
                g += grow/N;
        }

        System.out.println(/*pris + " " +*/
                "var=" + variance + " > " + threshVariance + " ? -> " + ((g - lastGain)));
        g = Util.clamp(g, ScalarValue.EPSILON, 1f);
        //control.out(variance, )

        gain.accept(lastGain = g);
    }


}
