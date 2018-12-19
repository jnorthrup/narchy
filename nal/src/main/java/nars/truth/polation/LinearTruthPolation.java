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
public class LinearTruthPolation extends TruthPolation {

    public LinearTruthPolation(long start, long end, int dur) {
        super(start, end, dur);
    }

    public final Truth truth(NAR nar) {
        return truth(nar, Float.MIN_NORMAL);
    }

    @Override
    @Nullable
    public Truth truth(NAR nar, float eviMin) {

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
        for (int i = 0; i < s; i++) {
            TaskComponent x = update(i, eviMin);
            if (x == null)
                continue;

            float ee = x.evi;

            eSum += ee;

            wFreqSum += ee * x.freq;

        }

        if (eSum < eviMin)
            return null;


        f = (wFreqSum / eSum);

        eSum *= eviFactor;
        float eAvg;
        if (start==ETERNAL) {
            eAvg = eSum;
        } else {
            long range = 1 + (end - start);
            eAvg = eSum / range;
        }

        return PreciseTruth.byEvi(f, eAvg);
    }

//    public long range() {
//        return start == ETERNAL ? 1 : (end - start) + 1;
//    }

}

