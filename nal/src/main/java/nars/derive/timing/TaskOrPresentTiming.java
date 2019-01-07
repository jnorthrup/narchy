package nars.derive.timing;

import nars.NAR;
import nars.Task;
import nars.term.Term;

import java.util.Random;
import java.util.function.BiFunction;

/** chooses at random whether to focus on a radius surrounding the present time, or the task's occurrence (if non eternal) */
public class TaskOrPresentTiming implements BiFunction<Task, Term, long[]> {

    private final Random rng;
    private final NAR nar;

    public TaskOrPresentTiming(NAR n) {
        this.nar = n;
        this.rng = n.random();
    }

    @Override
    public long[] apply(Task task, Term term) {
//        if (task.isEternal())
//            return new long[] { ETERNAL, ETERNAL };

        long[] tt;
        if (task.isEternal() || rng.nextBoolean()) {
            //present
            tt = presentDuration(1);
        } else {
            //task
            tt = taskTime(task);
        }


        return tt;
    }

    private long[] presentDuration(float factor) {
        long now = nar.time.now();
        int dur = Math.round(factor * nar.dur());
        return new long[] { now - dur/2, now + dur/2 };
    }


    public static long[] taskTime(Task t) {
        return new long[]{t.start(), t.end()};
    }

}