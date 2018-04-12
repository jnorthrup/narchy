package nars.truth.polation;

import nars.Task;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * result frequency = linear combination of frequency values weighted by evidence;
 * result evidence = evidence sum
 * <p>
 * duration is adjusted to the minimum distance of all components, thus
 * 'focusing' or 'narrowing' the duration if components contribute
 * truth within or nearer than it.
 */
public class FocusingLinearTruthPolation extends TruthPolation {

    private final static int minDur =
            0; //<- anything besides what matches the specified interval is ignored
            //1; //<- allows some temporal bleed-through during interpolation when an exact match is present

    /*float eviFactor, float eviMin*/
    public FocusingLinearTruthPolation(long start, long end, int dur) {
        super(start, end, dur);
    }

    @Override
    public void add(Task t) {
        super.add(t);

        long dd =
                t.minDistanceTo(start, end);
        //t.meanDistanceTo((start + end)/2L);

        if (dur > 0) {
            if (dd < dur)
                dur = Math.max(minDur, (int) dd);

//            if (computeDensity) {
//                long ts = Util.clamp(t.start(), start, end);
//                long te = Util.clamp(t.end(), start, end);
//                spanStart = Math.min(ts, spanStart);
//                spanEnd = Math.max(te, spanEnd);
//                rangeSum += Math.max(1, te - ts);
//            }
        }

    }


    @Override
    @Nullable
    public Truth truth() {

        int s = size();
        if (s > 0) {

            //float eviSum = 0, confSum = 0, wFreqSum = 0;
            float eviSum = 0, wFreqSum = 0;
            for (int i = 0; i < s; i++) {
                TaskComponent x = update(i);
                if (x==null)
                    continue;  //could have been pre-filtered

                float ee = x.evi;

                eviSum += ee;
//                        float ce = w2cSafe(ee);
//                        confSum += ce;
                //wFreqSum += ce * x.freq;
                wFreqSum += ee * x.freq;
            }
            assert (Float.isFinite(eviSum) && eviSum > Float.MIN_NORMAL);

            //float f = (wFreqSum / confSum);
            float f = (wFreqSum / eviSum);
            return new PreciseTruth(f, eviSum, false);
        }

        return null;
    }


}
