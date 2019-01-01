package nars.attention;

import com.google.common.collect.Streams;
import jcog.Paper;
import jcog.Util;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Termed;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * attention distribution node
 *
 * regulates prioritization of a group of concepts to what is
 * necessary to produce a minimum threshold anisotropic priority distribution
 * see: https://en.wikipedia.org/wiki/Entropy_and_life
 * https://en.wikipedia.org/wiki/Average_absolute_deviation
 */
@Paper
public class AttBranch extends AttNode {

    private final Iterable<? extends Termed> components;


    //final MiniPID control = new MiniPID(0.5f, 0.5f, 0.5f);

//    //v0: bang bang
//    float decay = 0.01f, grow = 0.01f/2, decaySlow = decay/2;


//    final RecycledSummaryStatistics pris = new RecycledSummaryStatistics();
//    final FloatArrayList pp = new FloatArrayList();

//    float priMin = 0.01f;

    public AttBranch(Object id, Iterable<? extends Termed> components) {
        super(id);
        this.components = components;
    }

    @Override
    public Stream<Concept> concepts(NAR nar) {
        return Streams.stream(components).map(nar::concept).filter(Objects::nonNull);
    }




//    @Override protected float myDemand(NAR n) {
//
//
////        pris.clear();
////        pp.clear();
////
////        final float[] dev = {0};
////
////        concepts(n).forEach(c -> {
////            pp.add(n.concepts.pri(c, 0));
////        });
////        if (pp.isEmpty())
////            return 0;
////
////        float mean = (float) pp.average();
////
////        pp.forEach(p -> {
////            dev[0] += Math.abs(p - mean);
////            pris.accept(p);
////        });
////        long N = pris.getN();
//
//        long N = Iterables.size(this.components);
//
//        //return Math.max(priMin, elementIdeal((int) N) - mean) * N;
//        return elementIdeal((int) N, n) * N;
//
////
////        dev[0] /= N;
////
////        double max = pris.getMax();
////        float range = (float) (max - pris.getMin());
////        if (range < ScalarValue.EPSILON)
////            range = 1;
////
////        /** normalized absolute mean deviation */
////        float deviation = dev[0];
////
////        /** target minimum deviation for anisotropy threshold "priority bandwidth" */
////        float devMin =
////                //(range * 0.5f)/N[0];
////                //(float) (max * 0.5f)/N[0];
////                //(float) ((max * 0.5f)/Math.sqrt(N[0]));
////                (float) (0.5f * (range) / Math.sqrt(N));
////                //(float) (1 / Math.sqrt(N[0]));
////                //0.5f;
////
////
////
////        float idealPri =
////                //0.5f;
////                //1f/ N[0];
////                (float) (1f / Math.sqrt(N));
////        float minPri =
////            2 * ScalarValue.EPSILONsqrt;
////            //ScalarValue.EPSILON
////            //1f  / (Util.cube(N*2));
////
////
////        float g = lastGain;
////
////        if (g != g)
////            g = idealPri;
////
////        if (deviation < devMin) {
////            if (g < idealPri)
////                g += grow;
////            else
////                g -= decay;
////        } else {
//////            if (g < idealPri)
////                g -= decaySlow;
//////            else
//////                g += grow;
////        }
////
////
////        /*
////        System.out.println(
////                "var=" + deviation + " > " + devMin + " ? -> " + ((g - lastGain)));
////        */
////
////        g = Util.clamp(g, minPri, 1f);
////        //control.out(variance, )
////
////        return (lastGain = g)*N;
//    }



}
