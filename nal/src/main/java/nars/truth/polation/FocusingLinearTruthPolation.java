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

    static final boolean durShrink = true;

    private final static int minDur =
            //0;
            1;
            

    public FocusingLinearTruthPolation(long start, long end, int dur) {
        super(start, end, dur);
    }

    @Override
    public TruthPolation add(Task t) {
        super.add(t);

        if (durShrink) {
            if (dur > minDur) {
                if (!t.isEternal()) {
                    long dd = t.minTimeTo(start, end);
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
                return null; 
        } 


        int s = size();
        float eAvg, f;
        switch (s) {
            case 0: return null;
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
        if (eAvg > Param.TRUTH_MIN_EVI)
            return new PreciseTruth(f, eAvg, false);
        else
            return null;
    }


}
