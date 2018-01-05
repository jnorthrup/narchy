package nars.task.signal;

import jcog.Util;

import static nars.time.Tense.ETERNAL;

/**
 * linear (1st order) interpolated between two endpoints, ie. trapezoidal.
 * (same evidence for both ends.)
 */
public class LinearTruthlet extends RangeTruthlet {

    public float freqStart;
    public float freqEnd;
    public float evi;

    public LinearTruthlet(long start, float freqStart, long end, float freqEnd, float evi) {
        super(start, end);
        this.freqStart = freqStart;
        this.freqEnd = freqEnd;
        this.evi = evi;
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        if (when == ETERNAL) {
            when = mid();
        }

        if (during(when)) {
            freqEvi[0] = Util.lerp(Util.normalize(when, start, end), freqStart, freqEnd);
            freqEvi[1] = evi;
        } else {
            unknown(freqEvi);
        }
    }
}
