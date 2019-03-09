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
public class LinearTruthProjection extends TruthProjection {

    public LinearTruthProjection(long start, long end, int dur) {
        super(start, end, dur);
    }

    public final Truth truth(NAR nar) {
        return truth(Float.MIN_NORMAL, nar);
    }

    @Override
    @Nullable
    public Truth truth(float eviMin, NAR nar) {

        double eviFactor = 1f;
        if (nar != null) {

            eviFactor *= intermpolate(nar);
            if (eviFactor < ScalarValue.EPSILON)
                return null;
        }

        int s = size();
        if (s == 0)
            return null;

        double wFreqSum = 0, wSum = 0, eSum = 0;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            TaskComponent x = get(i);

            float e = x.evi;
            assert(e==e);
            eSum += e;

            float w = e;
            //e * Math.abs(f-0.5f)*2f; /* polarity weighting */
            //e * (0.5f + Math.abs(f-0.5f)); /* polarity partial weighting */
            //e * (1 + (2*Math.abs(f-0.5f))); /* 2:1 compression polarity partial weighting */
            wSum += w;

            double f = x.task().freq(start, end);
            wFreqSum += w * f;
        }

        if (wSum < Float.MIN_NORMAL)
            return null;

        eSum *= eviFactor;
        if (eSum < eviMin)
            return null;

        double eAvg;
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

