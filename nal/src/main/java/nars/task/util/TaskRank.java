package nars.task.util;

import jcog.math.Longerval;
import jcog.sort.TopN;
import nars.Task;
import nars.truth.Truth;
import nars.truth.polation.FocusingLinearTruthPolation;
import nars.truth.polation.TruthPolation;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.time.Tense.ETERNAL;

/** query object used for selecting tasks.  can be applied to multiple belief tables or other sources of task. */
public class TaskRank implements Consumer<Task> {

    final TopN<Task> tasks;

    @Nullable final LongLongPredicate timeFilter;

    static LongLongPredicate time(long start, long end, boolean intersectOrContain, boolean allowEternal) {
        assert(start!=ETERNAL);

        LongLongPredicate ii =
                intersectOrContain ?
                    (s, e) -> Longerval.intersectLength(s, e, start, end) != -1
                        :
                    (s, e) -> (s >= start && e <= end);

        if (allowEternal) {
            return (s,e)->{
                if (s == ETERNAL) return true; //allow eternal
                else return ii.accept(s, e);
            };
        } else {
            return ii;
        }
    }

    public TaskRank(int limit, FloatFunction<Task> rank, @Nullable LongLongPredicate timeFilter) {
        this.tasks = new TopN<>(new Task[limit], rank);
        this.timeFilter = timeFilter;
    }


    @Override
    public final void accept(Task task) {
        if (timeFilter == null || timeFilter.accept(task.start(), task.end())) {
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
