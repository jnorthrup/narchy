package nars.attention;

import com.google.common.collect.Iterables;
import jcog.Paper;
import jcog.Util;
import jcog.pri.ScalarValue;
import jcog.util.FloatConsumer;
import nars.NAR;
import nars.control.DurService;
import nars.term.Termed;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/** regulates prioritization of a group of concepts to what is
 *  necessary to produce a minimum threshold anisotropic priority distribution
 *  see: https://en.wikipedia.org/wiki/Entropy_and_life
 *  https://en.wikipedia.org/wiki/Average_absolute_deviation
 */
@Paper
public class AttnDistributor {

    private final Iterable<? extends Termed> concepts;

    //final MiniPID control = new MiniPID(0.5f, 0.5f, 0.5f);

    //v0: bang bang
    float decay = 0.01f, grow = 0.01f/2f;

    float lastGain = Float.NaN;

    private final FloatConsumer gain;

//    final RecycledSummaryStatistics pris = new RecycledSummaryStatistics();
    final DescriptiveStatistics pris;

    public AttnDistributor(Iterable<? extends Termed> concepts, FloatConsumer gain, NAR n) {
        super();
        this.concepts = concepts;
        this.gain = gain;

        int windowIterations = 2; //>=1
        int N = Iterables.size(concepts); //only an estimate, if this Iterable changes
        pris = new DescriptiveStatistics(N*windowIterations);

        DurService.on(n, this::update);
    }
    private void update(NAR n) {




//        pris.clear();

        final float[] dev = {0};
        final int[] N = {0};

        float mean = (float) pris.getMean();
        concepts.forEach(c -> {
            float p = n.concepts.pri(c, 0);
            pris.addValue(p);
            dev[0] += Math.abs(p - mean);
            N[0]++;
            //pris.accept(p)
        });

        dev[0] /= N[0];

        double max = pris.getMax();
        float range =  (float) (max -pris.getMin());
        if (range < ScalarValue.EPSILON)
            range = 1;

        /** normalized absolute mean deviation */
        float deviation = dev[0];

        /** target minimum deviation for anisotropy threshold "priority bandwidth" */
        float devMin =
                //(range * 0.5f)/N[0];
                //(float) (max * 0.5f)/N[0];
                //(float) ((max * 0.5f)/Math.sqrt(N[0]));
                (float) ((range )/Math.sqrt(N[0]));
                //(float) (1f/Math.sqrt(N[0]));


        float idealPri =
                //0.5f;
                //1f/ N[0];
                (float) (1f/ Math.sqrt(N[0]));

        float g = lastGain;

        if (g!=g)
            g = idealPri;

        if (deviation < devMin) {
            if (g < idealPri)
                g += grow;
            else
                g -= decay;
        } else {
            if (g < idealPri)
                g -= decay;
            else
                g += grow;
        }

        /*
        System.out.println(
                "var=" + deviation + " > " + devMin + " ? -> " + ((g - lastGain)));
        */

        g = Util.clamp(g, ScalarValue.EPSILON, 1f);
        //control.out(variance, )

        gain.accept(lastGain = g);
    }


}
