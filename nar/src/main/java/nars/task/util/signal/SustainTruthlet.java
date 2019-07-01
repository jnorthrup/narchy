package nars.task.util.signal;

import jcog.Skill;
import nars.NAL;
import nars.util.Timed;

import static nars.time.Tense.ETERNAL;

/** fades evidence before the beginning and after the end of a defined RangeTruthlet
 *
 *   -  - - --- -----*
 *                    \
 *                    *---- - - -  -   -
 *
 *                  |  |
 * */
@Skill({"Sustain","Audio_feedback"})
public class SustainTruthlet extends ProxyTruthlet<RangeTruthlet> {

    float dur;

    public SustainTruthlet(RangeTruthlet r, Timed timed) {
        this(r, timed.dur());
    }

    public SustainTruthlet(RangeTruthlet r, float dur) {
        super(r);
        this.dur = dur;
    }

    @Override
    public void truth(long when, float[] freqEvi) {

        if (when == ETERNAL)
            when = mid();

        long dist;
        long start, end;

        long w;
        
        if (when < (start=start())) {
            dist = Math.abs((w = start) - when);
        } else if (when > (end=end())) {
            dist = Math.abs(when - (w = end));
        } else {
            dist = 0; 
            w = when;
        }

        super.truth(w, freqEvi);
        if (dist > 0) {
            float f = freqEvi[0];
            if (f == f)
                freqEvi[1] = (float) NAL.evi(freqEvi[1], dist, /* dur */ dur());
        }

    }

    public final float dur() {
        return dur;
    }
}
