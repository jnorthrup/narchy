package nars.derive.timing;

import nars.NAR;
import nars.Task;
import nars.term.Term;

import java.util.function.BiFunction;

public class NonEternalTaskOccurenceOrPresentDeriverTiming implements BiFunction<Task, Term, long[]> {

    private final NAR nar;

    public NonEternalTaskOccurenceOrPresentDeriverTiming(NAR nar) {
        this.nar = nar;
    }

    @Override public long[] apply(Task task, Term term) {
        if (!task.isEternal()) {
            return new long[]{task.start(), task.end()};
        } else {
            int dur = nar.dur();
            long now = nar.time();
            return new long[] { now - dur/2, now + dur/2 };
        }
    }
}