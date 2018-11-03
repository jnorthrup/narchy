package nars.truth.polation;

import jcog.pri.ScalarValue;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.time.Tense;
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
            if (eviFactor < ScalarValue.EPSILON)
                return null;
            /*if (term.volume() > nar.termVolumeMax.intValue())
                return null;*/
        }

        int s = size();
        if (s == 0)
            return null;

        float eSum, f;

        float wFreqSum = 0;
        eSum = 0;
        long S = Long.MAX_VALUE, E = Long.MIN_VALUE;
        for (int i = 0; i < s; i++) {
            TaskComponent x = update(i);
            if (x == null)
                continue;

            float ee = x.evi;

            eSum += ee;

            wFreqSum += ee * x.freq;

            Task t = x.task();
            long ts = t.start();
            if (ts != ETERNAL) {
                if (ts < S)
                    S = ts;
                long te = t.end();
                if (te > E)
                    E = te;
            }
        }

        if (eSum < Param.TRUTH_MIN_EVI)
            return null;

        if (S == Long.MAX_VALUE) {
            S = E = Tense.ETERNAL;
        }
        //trim
        //TODO make optional
        start = S;
        end = E;

        f = (wFreqSum / eSum);

        eSum *= eviFactor;
        float eAvg;
        if (start==ETERNAL) {
            eAvg = eSum;
        } else {
            long range = 1 + (end - start);
            eAvg = eSum / range;
        }
        if (eAvg >= Param.TRUTH_MIN_EVI)
            return PreciseTruth.byEvi(f, eAvg);
        else
            return null;
    }

    public long range() {
        return start == ETERNAL ? 1 : (end - start) + 1;
    }

}

