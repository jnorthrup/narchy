package nars.truth.polation;

import nars.NAR;
import nars.Param;
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

    static final boolean durShrink = false;

    private final static int minDur =
            0; //<- anything besides what matches the specified interval is ignored
            //1; //<- allows some temporal bleed-through during interpolation when an exact match is present

    public FocusingLinearTruthPolation(long start, long end, int dur) {
        super(start, end, dur);
    }

    @Override
    public TruthPolation add(Task t) {
        super.add(t);

        if (durShrink) {
            if (dur > minDur) {
                if (!t.isEternal()) {
                    long dd = t.minDistanceTo(start, end);
                    if (dd < dur)
                        dur = Math.max(minDur, (int) dd);
                }
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
            if (eviFactor < Param.TRUTH_MIN_EVI)
                return null; //intermpolate failure
        } //ELSE: maybe fail if all the tasks dont share the exact same term


        int s = size();
        float eAvg, f;
        switch (s) {
            case 0: return null;
            case 1: {
                //accelerated single case
                TaskComponent x = update(0);
                if (x == null)
                    return null; //could have been pre-filtered
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
                        continue;  //could have been pre-filtered

                    float ee = x.evi;

                    eAvg += ee;
//                        float ce = w2cSafe(ee);
//                        confSum += ce;
                    //wFreqSum += ce * x.freq;
                    wFreqSum += ee * x.freq;
                }
                if (eAvg < Param.TRUTH_MIN_EVI)
                    return null;

                //f = (wFreqSum / confSum);
                f = (wFreqSum / eAvg);
                break;
            }
        }


        eAvg *= eviFactor;
        if (eAvg > Param.TRUTH_MIN_EVI)
            return new PreciseTruth(f, eAvg, false);
        else
            return null;
    }


}
