package nars.task.signal;

import nars.Param;

/** an impulse function which provides contrasting truth wave
 * before, during, and after the specified duration (pulse).
 */
public class ImpulseTruthlet extends ProxyTruthlet {

    public float freqOtherwise;
    public float eviOtherwise;

    public ImpulseTruthlet(Truthlet defined, float freqOtherwise, float eviOtherwise) {
        super(defined);
        this.freqOtherwise = freqOtherwise;
        this.eviOtherwise = eviOtherwise;
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        if (defined.containsTime(when)) {
            super.truth(when, freqEvi);
        } else {
            freqEvi[0] = freqOtherwise;
            long dist = Math.min(Math.abs(start()-when), Math.abs(end()-when));
            freqEvi[1] = (float) Param.evi(eviOtherwise, dist, range());
        }
    }

}
