package nars.derive.timing;

import nars.Task;
import nars.term.Term;

import java.util.function.BiFunction;

/** simply matches the task time exactly. */
public class TaskTimeDeriverTiming implements BiFunction<Task, Term, long[]> {

    @Override
    public long[] apply(Task task, Term term) {
        return new long[] { task.start(), task.end() };
    }
}
