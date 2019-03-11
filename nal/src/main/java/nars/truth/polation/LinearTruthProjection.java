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
    public Truth truth(float eviMin, boolean dither, NAR nar) {

        if (active() == 0)
            return null;

        double eviFactor = 1f;
        if (nar != null) {

            eviFactor *= intermpolateAndCull(nar);
            if (eviFactor < ScalarValue.EPSILON)
                return null;

            if (active() == 0)
                return null;
        }

        double wFreqSum = 0, wSum = 0, eSum = 0;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            TaskComponent x = get(i);

            float e = x.evi;
            if (e!=e)
                continue;

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

        double F = (float)wFreqSum / wSum;
        return dither ? Truth.theDithered((float)F, (float)eAvg, nar) : PreciseTruth.byEvi(F, eAvg);
    }


//    public long range() {
//        return start == ETERNAL ? 1 : (end - start) + 1;
//    }

}

