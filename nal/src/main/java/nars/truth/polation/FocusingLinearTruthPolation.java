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

    public FocusingLinearTruthPolation(long start, long end, int dur) {
        super(start, end, dur);
    }

    @Override
    public TruthPolation add(Task t) {
        super.add(t);

        if (dur > minDur) {
            if (!t.isEternal()) {
                long dd = t.minDistanceTo(start, end);
                if (dd < dur)
                    dur = Math.max(minDur, (int) dd);
            }
        }

        return this;
    }


    @Override
    @Nullable
    public Truth truth() {

        int s = size();
        float e, f;
        switch (s) {
            case 0: return null;
            case 1: {
                //accelerated single case
                TaskComponent x = update(0);
                if (x == null)
                    return null; //could have been pre-filtered
                e = x.evi;
                f = x.freq;
                break;
            }
            default: {
                float eviSum = 0, wFreqSum = 0;
                for (int i = 0; i < s; i++) {
                    TaskComponent x = update(i);
                    if (x == null)
                        continue;  //could have been pre-filtered

                    float ee = x.evi;

                    eviSum += ee;
//                        float ce = w2cSafe(ee);
//                        confSum += ce;
                    //wFreqSum += ce * x.freq;
                    wFreqSum += ee * x.freq;
                }
                e = eviSum;
                if (e < Float.MIN_NORMAL)
                    return null;

                //f = (wFreqSum / confSum);
                f = (wFreqSum / eviSum);
                break;
            }
        }

        return new PreciseTruth(f, e, false);
    }


}
