package nars.derive.timing;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.term.Term;

import java.util.function.BiFunction;

import static nars.time.Tense.ETERNAL;

public class NonEternalTaskOccurenceOrPresentDeriverTiming implements BiFunction<Task, Term, long[]> {

    public final FloatRange durRadius = new FloatRange(4, 0, 32);

    private final NAR nar;

    public NonEternalTaskOccurenceOrPresentDeriverTiming(NAR nar) {
        this.nar = nar;
    }

    @Override public long[] apply(Task task, Term term) {
        long ts = task.start();
        if (ts!=ETERNAL) {
            return new long[]{ts, task.end()};
        } else {
            long rad = Math.round(nar.dur() * durRadius.floatValue());
            long now = nar.time();
            return new long[] { now - rad, now + rad };
        }
    }
}