package nars.truth.proj;

import nars.NAL;
import nars.Task;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * result truth:
 * frequency = linear combination of frequency values weighted by evidence;
 * evidence = evidence sum
 * <p>
 * this implememnt aggregates combined evidence via linear inteprolation
 */
public class LinearTruthProjection extends TruthProjection {

    public LinearTruthProjection(long start, long end) {
        super(start, end);
    }

    @Override
    @Nullable
    public final Truth truth(double eviMin, boolean dither, boolean tShrink, boolean commitFirst, NAL n) {

        if (commitFirst) {
            if (!commit(tShrink, n))
                return null;
        }


        double wFreqSum = 0, /*wSum = 0,*/ eSum = 0;
        double[] evi = this.evi;
        Task[] items = this.items;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {


            double e = evi[i];
            if (e != e || e <= Double.MIN_NORMAL)
                continue;


            //float w = e;
            //e * Math.abs(f-0.5f)*2f; /* polarity weighting */
            //e * (0.5f + Math.abs(f-0.5f)); /* polarity partial weighting */
            //e * (1 + (2*Math.abs(f-0.5f))); /* 2:1 compression polarity partial weighting */
            //wSum += w;

            Task x = items[i];
//            if (x == null)
//                continue;
            eSum += e;
            double f = x.freq(start, end);
            wFreqSum += e * f;
//            if (f >= 0.5f) wFreqPos += w * (1-f)*2; else wFreqNeg += w * (0.5 - f)*2;
        }

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
        return dither ? Truth.theDithered((float)F, eAvg, n) : PreciseTruth.byEvi(F, eAvg);
    }


}

