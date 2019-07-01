package nars.truth.proj;

import jcog.pri.ScalarValue;
import nars.NAL;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.assertFinite;
import static nars.time.Tense.ETERNAL;

/**
 * result truth:
 * frequency = linear combination of frequency values weighted by evidence;
 * evidence = evidence sum
 * <p>
 * this implememnt aggregates combined evidence via linear inteprolation
 */
public class LinearTruthProjection extends TruthProjection {

    public LinearTruthProjection(long start, long end, float dur) {
        super(start, end, dur);
    }

    @Override
    @Nullable
    public Truth truth(double eviMin, boolean dither, boolean shrink, NAL nar) {

        if (size()==0)
            return null;
        else {
            commit(shrink, 1, false);
            removeNulls();
            if (active()==0)
                return null;
        }


        double eviFactor = 1f;
        if (nar != null) {

            float c = intermpolateAndCull(nar); assertFinite(c);
            eviFactor *= c;
            if (eviFactor < ScalarValue.EPSILON)
                return null;

            if (active() == 0)
                return null;
        }

        double wFreqSum = 0, /*wSum = 0,*/ eSum = 0;
//        double wFreqPos = 0, wFreqNeg = 0;
        for (TaskComponent x : this) {
            double e = x.evi;
            if (e != e)
                continue;

            e *= eviFactor;
            eSum += e;

            //float w = e;
            //e * Math.abs(f-0.5f)*2f; /* polarity weighting */
            //e * (0.5f + Math.abs(f-0.5f)); /* polarity partial weighting */
            //e * (1 + (2*Math.abs(f-0.5f))); /* 2:1 compression polarity partial weighting */
            //wSum += w;

            double f = x.task().freq(start, end);
            wFreqSum += e * f;
//            if (f >= 0.5f) wFreqPos += w * (1-f)*2; else wFreqNeg += w * (0.5 - f)*2;
        }

//        if (wSum < Float.MIN_NORMAL)
//            return null;

        if (eSum < eviMin)
            return null;

        double eAvg;
        if (start==ETERNAL) {
            eAvg = eSum;
        } else {
            long range = 1 + (end - start);
            eAvg = eSum / range;

            if (eAvg < eviMin)
                return null;
        }


        double F = wFreqSum / eSum;

//        double F2 = (((wFreqPos - wFreqNeg) / (wFreqPos+wFreqNeg)/ wSum + 1)/2 ) ;
//        if (!Util.equals(F,F2))
//            System.out.println(F + " "+ F2);

        return dither ? Truth.theDithered((float)F, eAvg, nar) : PreciseTruth.byEvi(F, eAvg);
    }


//    public long range() {
//        return start == ETERNAL ? 1 : (end - start) + 1;
//    }

}

