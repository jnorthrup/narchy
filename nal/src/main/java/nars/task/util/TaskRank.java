package nars.task.util;

import jcog.math.CachedFloatFunction;
import jcog.sort.FloatRank;
import jcog.sort.TopN;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.polation.FocusingLinearTruthPolation;
import nars.truth.polation.TruthPolation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** query object used for selecting tasks.  can be applied to multiple belief tables or other sources of task. */
public class TaskRank implements Consumer<Task> {



    final TopN<Task> tasks;

    public final TimeRangeFilter time;
    private final CachedFloatFunction<Task> cache;


    @Deprecated public Term template = null;


    public TaskRank(int limit, FloatRank<Task> rank, TimeRangeFilter time) {
        this.cache = new CachedFloatFunction<>(64, rank);
        this.tasks = new TopN<>(new Task[limit], cache);
        this.time = time;

    }


    @Override
    public final void accept(Task task) {
        if (task != null) {
            if (!cache.containsKey(task)) {
                //if (time == null || time.accept(task.start(), task.end())) {
                tasks.accept(task);
            }
        }
        //}
    }


    public boolean isEmpty() { return tasks.isEmpty(); }

    @Nullable public Truth truth(long s, long e, int dur) {
        return isEmpty() ? null : truth(new FocusingLinearTruthPolation(s, e, dur));
    }

    @Nullable public Truth truth(TruthPolation p) {
        p.ensureCapacity(tasks.size());
        p.add(tasks);
        p.filterCyclic(false);
        return p.truth();
    }

}
