package nars.derive.timing;

import jcog.func.TriFunction;
import jcog.math.FloatRange;
import nars.Task;
import nars.attention.What;
import nars.term.Term;

import static nars.time.Tense.ETERNAL;

public class NonEternalTaskOccurenceOrPresentDeriverTiming implements TriFunction<What, Task, Term, long[]> {

    public final FloatRange durRadius = new FloatRange(1f, 0, 32);


    public NonEternalTaskOccurenceOrPresentDeriverTiming() {
    }

    @Override public long[] apply(What what, Task task, Term term) {
        long ts = task.start();
        if (ts!=ETERNAL) {
            return new long[]{ts, task.end()};
        } else {
            float rad = what.dur() * durRadius.floatValue()/2;
            long now = what.time();
            return new long[] { Math.round(now - rad), Math.round(now + rad) };
        }
    }
}