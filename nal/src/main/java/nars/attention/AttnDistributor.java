package nars.attention;

import jcog.Paper;
import jcog.Util;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.ScalarValue;
import jcog.util.FloatConsumer;
import nars.NAR;
import nars.control.DurService;
import nars.term.Termed;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;

/**
 * regulates prioritization of a group of concepts to what is
 * necessary to produce a minimum threshold anisotropic priority distribution
 * see: https://en.wikipedia.org/wiki/Entropy_and_life
 * https://en.wikipedia.org/wiki/Average_absolute_deviation
 */
@Paper
public class AttnDistributor {

    private final Iterable<? extends Termed> concepts;

    //final MiniPID control = new MiniPID(0.5f, 0.5f, 0.5f);

    //v0: bang bang
    float decay = 0.01f, grow = 0.01f/2, decaySlow = decay/2;

    float lastGain = Float.NaN;

    private final FloatConsumer gain;

    final RecycledSummaryStatistics pris = new RecycledSummaryStatistics();
    final FloatArrayList pp = new FloatArrayList();


    public AttnDistributor(Iterable<? extends Termed> concepts, FloatConsumer gain, NAR n) {
        super();
        this.concepts = concepts;
        this.gain = gain;


        DurService.on(n, this::update);
    }

    private void update(NAR n) {


        pris.clear();
        pp.clear();

        final float[] dev = {0};

        concepts.forEach(c -> {
            pp.add(n.concepts.pri(c, 0));
        });
        if (pp.isEmpty())
            return;

        float mean = (float) pp.average();

        pp.forEach(p -> {
            dev[0] += Math.abs(p - mean);
            pris.accept(p);
        });

        long N = pris.getN();

        dev[0] /= N;

        double max = pris.getMax();
        float range = (float) (max - pris.getMin());
        if (range < ScalarValue.EPSILON)
            range = 1;

        /** normalized absolute mean deviation */
        float deviation = dev[0];

        /** target minimum deviation for anisotropy threshold "priority bandwidth" */
        float devMin =
                //(range * 0.5f)/N[0];
                //(float) (max * 0.5f)/N[0];
                //(float) ((max * 0.5f)/Math.sqrt(N[0]));
                (float) (0.5f * (range) / Math.sqrt(N));
                //(float) (1 / Math.sqrt(N[0]));
                //0.5f;



        float idealPri =
                //0.5f;
                //1f/ N[0];
                (float) (1f / Math.sqrt(N));
        float minPri =
            2 * ScalarValue.EPSILONsqrt;
            //ScalarValue.EPSILON
            //1f  / (Util.cube(N*2));


        float g = lastGain;

        if (g != g)
            g = idealPri;

        if (deviation < devMin) {
            if (g < idealPri)
                g += grow;
            else
                g -= decay;
        } else {
//            if (g < idealPri)
                g -= decaySlow;
//            else
//                g += grow;
        }


        /*
        System.out.println(
                "var=" + deviation + " > " + devMin + " ? -> " + ((g - lastGain)));
        */

        g = Util.clamp(g, minPri, 1f);
        //control.out(variance, )

        gain.accept(lastGain = g);
    }


}
