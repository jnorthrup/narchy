package nars.derive.time;

import nars.Task;
import nars.attention.What;
import nars.derive.util.TimeFocus;
import nars.term.Term;

/** simply matches the task time exactly. */
public class TaskOccurenceTiming implements TimeFocus {

    @Override
    public long[] premise(What what, Task task, Term beliefTerm) {
        return new long[] { task.start(), task.end() };
    }
}
