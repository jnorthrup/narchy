package nars.truth.polation;

import nars.NAR;
import nars.Param;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;

/**
 * result truth:
 *      frequency = linear combination of frequency values weighted by evidence;
 *      evidence = evidence sum
 *
 * this implememnt aggregates combined evidence via linear inteprolation
 */
public class LinearTruthPolation extends TruthPolation {

    public LinearTruthPolation(long start, long end, int dur) {
        super(start, end, dur);
    }



    @Override
    @Nullable
    public Truth truth(NAR nar) {

        float eviFactor = 1f;
        if (nar != null) {
            eviFactor *= intermpolate(nar);
            if (eviFactor < Param.TRUTH_MIN_EVI)
                return null;
        }


        int s = size();
        float eAvg, f;
        switch (s) {
            case 0:
                return null;
            case 1: {

                TaskComponent x = update(0);
                if (x == null)
                    return null;
                eAvg = x.evi;
                if (eAvg < Param.TRUTH_MIN_EVI)
                    return null;

                f = x.freq;
                break;
            }
            default: {
                float wFreqSum = 0;
                eAvg = 0;
                for (int i = 0; i < s; i++) {
                    TaskComponent x = update(i);
                    if (x == null)
                        continue;

                    float ee = x.evi;

                    eAvg += ee;

                    wFreqSum += ee * x.freq;
                }
                if (eAvg < Param.TRUTH_MIN_EVI)
                    return null;


                f = (wFreqSum / eAvg);
                break;
            }
        }


        eAvg *= eviFactor;
        if (eAvg >= Param.TRUTH_MIN_EVI)
            return PreciseTruth.byEvi(f, eAvg);
        else
            return null;
    }

    public long range() {
        return start==ETERNAL ? 1 : (end-start)+1;
    }

}

