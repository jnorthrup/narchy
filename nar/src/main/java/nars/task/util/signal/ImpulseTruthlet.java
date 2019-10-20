package nars.task.util.signal;

import nars.NAL;
import nars.time.Tense;

/**
 * an impulse function which provides contrasting truth wave
 * before, during, and after the specified duration (pulse).
 */
public class ImpulseTruthlet extends ProxyTruthlet {

    float freqInactive;
    float eviInactive;

    public ImpulseTruthlet(Truthlet active, float freqInactive, float eviInactive) {
        super(active);
        this.freqInactive = freqInactive;
        this.eviInactive = eviInactive;
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        long s = start();
        long e = end();
        if (when >= s && when <= e) {
            
            super.truth(when, freqEvi);
        } else {
            
            freqEvi[0] = freqInactive;
            long dist = Math.min(Math.abs(s - when), Math.abs(e - when));
            freqEvi[1] = (float) NAL.evi((double) eviInactive, dist, (float) Math.max(1, Tense.occToDT((e - s) / 2L)));
        }
    }

}
