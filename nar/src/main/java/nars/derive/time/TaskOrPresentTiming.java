package nars.derive.time;

import jcog.func.TriFunction;
import nars.Task;
import nars.attention.What;
import nars.term.Term;

/** chooses at random whether to focus on a radius surrounding the present time, or the task's occurrence (if non eternal) */
public class TaskOrPresentTiming implements TriFunction<What, Task, Term, long[]> {


    public TaskOrPresentTiming() {
    }

    @Override
    public long[] apply(What what, Task task, Term term) {
//        if (task.isEternal())
//            return new long[] { ETERNAL, ETERNAL };

        long[] tt;
        if (task.isEternal() || what.random().nextBoolean()) {
            //present
            long now = what.time();
            int dur = Math.round((float) 1 * what.dur());
            tt = new long[]{now - (long) (dur / 2), now + (long) (dur / 2)};
        } else {
            //task
            tt = taskTime(task);
        }


        return tt;
    }


    static long[] taskTime(Task t) {
        return new long[]{t.start(), t.end()};
    }

}