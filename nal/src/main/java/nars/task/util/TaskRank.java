package nars.task.util;

import jcog.sort.FloatRank;
import jcog.sort.TopN;
import nars.Task;
import nars.truth.Truth;
import nars.truth.polation.FocusingLinearTruthPolation;
import nars.truth.polation.TruthPolation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** query object used for selecting tasks.  can be applied to multiple belief tables or other sources of task. */
public class TaskRank implements Consumer<Task> {



    final TopN<Task> tasks;

    @Nullable
    public final TimeRangeFilter time;


    public TaskRank(int limit, FloatRank<Task> rank, @Nullable TimeRangeFilter time) {
        this.tasks = new TopN<>(new Task[limit], rank);
        this.time = time;
    }


    @Override
    public final void accept(Task task) {
        if (time == null || time.accept(task.start(), task.end())) {
            tasks.accept(task);
        }
    }

    public boolean isEmpty() { return tasks.isEmpty(); }

    @Nullable public Truth truth(long s, long e, int dur) {
        return isEmpty() ? null : truth(new FocusingLinearTruthPolation(s, e, dur));
    }

    @Nullable public Truth truth(TruthPolation p) {
        p.add(tasks);
        p.filterCyclic();
        return p.truth();
    }

}
