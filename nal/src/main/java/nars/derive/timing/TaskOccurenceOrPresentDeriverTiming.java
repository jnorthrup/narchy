package nars.derive.timing;

import nars.NAR;
import nars.Task;
import nars.term.Term;

import java.util.function.BiFunction;

public class TaskOccurenceOrPresentDeriverTiming implements BiFunction<Task, Term, long[]> {

    private final NAR nar;

    public TaskOccurenceOrPresentDeriverTiming(NAR nar) {
        this.nar = nar;
    }

    @Override public long[] apply(Task task, Term term) {
        if (nar.random().nextBoolean()) {
            return new long[]{task.start(), task.end()};
        } else {
            int dur = nar.dur();
            long now = nar.time();
            return new long[] { now - dur/2, now + dur/2 };
        }
    }
}