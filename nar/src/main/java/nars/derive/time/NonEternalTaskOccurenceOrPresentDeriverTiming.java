package nars.derive.time;

import jcog.math.FloatRange;
import nars.Task;
import nars.attention.What;
import nars.derive.util.TimeFocus;
import nars.term.Term;

import static nars.time.Tense.ETERNAL;

public class NonEternalTaskOccurenceOrPresentDeriverTiming implements TimeFocus {

    public final FloatRange durRadius = new FloatRange(1f, (float) 0, 32.0F);


    public NonEternalTaskOccurenceOrPresentDeriverTiming() {
    }

    @Override public long[] premise(What what, Task task, Term beliefTerm) {
        long ts = task.start();
        if (ts!=ETERNAL) {
            return new long[] {ts, task.end()};
        } else {
            float rad = what.dur() * durRadius.floatValue()/ 2.0F;
            long now = what.time();
            return new long[] {(long) Math.round((float) now - rad), (long) Math.round((float) now + rad)};
        }
    }
}