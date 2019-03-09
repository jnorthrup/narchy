package nars.truth.polation;

import jcog.pri.ScalarValue;
import nars.NAR;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;

/**
 * result truth:
 * frequency = linear combination of frequency values weighted by evidence;
 * evidence = evidence sum
 * <p>
 * this implememnt aggregates combined evidence via linear inteprolation
 */
public class LinearTruthPolation extends Projection {

    public LinearTruthPolation(long start, long end, int dur) {
        super(start, end, dur);
    }

    public final Truth truth(NAR nar) {
        return truth(Float.MIN_NORMAL, nar);
    }

    @Override
    @Nullable
    public Truth truth(float eviMin, NAR nar) {

        //trim TODO test. may need to invalidate some computed values if the range changes
//        int s0 = size();
//        long S = Long.MAX_VALUE, E = Long.MIN_VALUE;
//        for (int i = 0; i < s0; i++) {
//            TaskComponent x = get(i);
//
//            Task t = x.task();
//            long ts = t.start();
//            if (ts != ETERNAL) {
//                if (ts < S)
//                    S = ts;
//                long te = t.end();
//                if (te > E)
//                    E = te;
//            }
//        }
//        if (S != Long.MAX_VALUE) {
//            //trim
//            //TODO make optional
//            start = Util.clamp(S, start, end);
//            end = Util.clamp(E, start, end);
//        }

        validate();

        float eviFactor = 1f;
        if (nar != null) {

            eviFactor *= intermpolate(nar);
            if (eviFactor < ScalarValue.EPSILON)
                return null;
            /*if (target.volume() > nar.termVolumeMax.intValue())
                return null;*/
        }

        int s = size();
        if (s == 0)
            return null;



        float wFreqSum = 0;
        float wSum = 0;
        float eSum = 0;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            TaskComponent x = get(i);

            float e = x.evi;
            eSum += e;

            float w = e;
            //e * Math.abs(f-0.5f)*2f; /* polarity weighting */
            //e * (0.5f + Math.abs(f-0.5f)); /* polarity partial weighting */
            //e * (1 + (2*Math.abs(f-0.5f))); /* 2:1 compression polarity partial weighting */
            wSum += w;

            float f = x.task().freq(start, end);
            wFreqSum += w * f;
        }

        if (wSum < Float.MIN_NORMAL)
            return null;

        eSum *= eviFactor;
        if (eSum < eviMin)
            return null;

        float eAvg;
        if (start==ETERNAL) {
            eAvg = eSum;
        } else {
            long range = 1 + (end - start);
            eAvg = eSum / range;
        }

        return PreciseTruth.byEvi((wFreqSum / wSum), eAvg);
    }


//    public long range() {
//        return start == ETERNAL ? 1 : (end - start) + 1;
//    }

}

