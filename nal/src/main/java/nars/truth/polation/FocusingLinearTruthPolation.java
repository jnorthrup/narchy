package nars.truth.polation;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;

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
    public Truth truth(NAR nar) {

        float eviFactor = 1f;
        if (nar!=null) {
            eviFactor *= intermpolate(nar);
            if (eviFactor < Float.MIN_NORMAL)
                return null; //intermpolate failure
        }


        int s = size();
        float eInteg, f;
        switch (s) {
            case 0: return null;
            case 1: {
                //accelerated single case
                TaskComponent x = update(0);
                if (x == null)
                    return null; //could have been pre-filtered
                eInteg = x.eviInteg;
                if (eInteg < Float.MIN_NORMAL)
                    return null;

                f = x.freq;
                break;
            }
            default: {
                float wFreqSum = 0;
                eInteg = 0;
                for (int i = 0; i < s; i++) {
                    TaskComponent x = update(i);
                    if (x == null)
                        continue;  //could have been pre-filtered

                    float ee = x.eviInteg;

                    eInteg += ee;
//                        float ce = w2cSafe(ee);
//                        confSum += ce;
                    //wFreqSum += ce * x.freq;
                    wFreqSum += ee * x.freq;
                }
                if (eInteg < Float.MIN_NORMAL)
                    return null;

                //f = (wFreqSum / confSum);
                f = (wFreqSum / eInteg);
                break;
            }
        }

        long rangeDivisor = start==ETERNAL ? 1 : (end-start+1);
        float eAvg = eviFactor * eInteg / rangeDivisor;
        if (eAvg > Param.TRUTH_MIN_EVI)
            return new PreciseTruth(f, eAvg, false);
        else
            return null;
    }


}
