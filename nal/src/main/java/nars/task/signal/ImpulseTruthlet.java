package nars.task.signal;

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
    public long start() {
        return super.start() - Math.max(1,defined.range()/2);
    }
    @Override
    public long end() {
        return super.end() + Math.max(1,defined.range()/2);
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        if (defined.containsTime(when)) {
            super.truth(when, freqEvi);
        } else {
            freqEvi[0] = freqOtherwise;
            freqEvi[1] = eviOtherwise;
        }
    }

}
