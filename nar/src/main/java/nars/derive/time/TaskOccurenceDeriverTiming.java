package nars.derive.time;

import jcog.func.TriFunction;
import nars.Task;
import nars.attention.What;
import nars.term.Term;

/** simply matches the task time exactly. */
public class TaskOccurenceDeriverTiming implements TriFunction<What, Task, Term, long[]> {

    @Override
    public long[] apply(What what, Task task, Term term) {
        return new long[] { task.start(), task.end() };
    }
}
