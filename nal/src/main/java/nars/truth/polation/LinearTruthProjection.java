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

    @Override
    @Nullable
    public Truth truth(float eviMin, boolean dither, boolean tCrop, NAR nar) {

        if (size()==0)
            return null;
        else {
            commit(false, tCrop,1);
            if (active()==0)
                return null;
        }


        double eviFactor = 1f;
        if (nar != null) {

            eviFactor *= intermpolateAndCull(nar);
            if (eviFactor < ScalarValue.EPSILON)
                return null;

            if (active() == 0)
                return null;
        }

        double wFreqSum = 0, /*wSum = 0,*/ eSum = 0;
//        double wFreqPos = 0, wFreqNeg = 0;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            TaskComponent x = get(i);

            double e = x.evi;
            if (e!=e)
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


        double F = (float)wFreqSum / eSum;

//        double F2 = (((wFreqPos - wFreqNeg) / (wFreqPos+wFreqNeg)/ wSum + 1)/2 ) ;
//        if (!Util.equals(F,F2))
//            System.out.println(F + " "+ F2);

        return dither ? Truth.theDithered((float)F, (float)eAvg, nar) : PreciseTruth.byEvi(F, eAvg);
    }


//    public long range() {
//        return start == ETERNAL ? 1 : (end - start) + 1;
//    }

}

