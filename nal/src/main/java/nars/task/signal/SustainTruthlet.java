package nars.task.signal;

import jcog.Skill;
import nars.Param;

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

    public SustainTruthlet(RangeTruthlet r) {
        super(r);
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        super.truth(when, freqEvi);
        if (!containsTime(when) && freqEvi[0] != freqEvi[0]) {

            long dist;
            long start = start();

            long w;
            //nearest endpoint
            if (when < start) {
                w = start;
                dist = Math.abs(start - when);
            } else {
                dist = Math.abs(when - (w = end()));
            }

            float[] x = super.truth(w);
            freqEvi[0] = x[0];
            freqEvi[1] = (float) Param.evi(x[1], dist, /* dur */ 1 + range() / 2); //dist is relative to the event's range

        }
    }
}
