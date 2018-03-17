package nars.table;

import jcog.Util;
import jcog.decide.Roulette;
import jcog.sort.CachedTopN;
import jcog.sort.Top;
import jcog.sort.TopN;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.Revision;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * holds a set of ranked question/quests tasks
 * top ranking items are stored in the lower indexes so they will be first iterated
 */
public interface TaskTable {

    /**
     * attempt to insert a task.
     *
     * @return: whether the table was possibly modified.  if async or unsure, return
     * true to be safe
     */
    boolean add(Task t, TaskConcept c, NAR n);


    int capacity();


    /**
     * number of items in this collection
     */
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }


    void forEachTask(Consumer<? super Task> x);


    /**
     * returns true if the task was removed
     */
    boolean removeTask(Task x);

    void clear();

    Stream<Task> streamTasks();

    default Task[] toArray() {
        return streamTasks().toArray(Task[]::new);
    }

    default void match(TaskMatch m, Consumer<Task> target) {
        if (isEmpty())
            return;

        //TODO expand here
        Random rng = m.sample();
        if (rng == null) {
            //strongest
            //prefer temporally relevant, and original
            int limit = m.limit();
            assert (limit > 0);
            if (limit == 1) {
                Top<Task> q = new Top<>(m.value());
                forEachTask(q::accept);
                Task the = q.the;
                if (the != null)
                    target.accept(the);
            } else {
                TopN<Task> q = new CachedTopN<>(new Task[limit], m.value());
                forEachTask(q::accept);
                q.forEach(target);
            }
        } else {
            //sampled
            Task[] t = toArray();
            if (t.length == 0)
                return;

            int limit = m.limit();
            if (t.length <= limit) {
                //provide all
                for (Task x : t)
                    target.accept(x);
                return;
            }

            //roulette selection
            FloatFunction<Task> value = m.value();

            float[] w = Util.map(t, value);
            final int[] remain = {limit};
            Roulette.RouletteUnique.run(w, (x)->{
                target.accept(t[x]);
                return --remain[0] >0;
            }, rng);

        }

    }

    default int match(TaskMatch m, Task[] target) {
        final int[] i = {0};
        match(m, (x)->{
            target[i[0]++] = x;
        });
        return i[0];
    }

    @Nullable
    default Task matchThe(TaskMatch m) {
        assert(m.limit()==1);
        Task[] x = new Task[1];
        int r = match(m, x);
        if (r == 0)
            return null;
        else
            return x[0];
    }


    default Task match(long when, Term template, NAR nar) {
        return match(when, when, template, nar);
    }
    default Task match(long start, long end, Term template, NAR nar) {

        TaskMatch m =
                template == null ?
                    TaskMatch.best(start, end) :
                    TaskMatch.best(start, end,
                            t-> 1 / (1 + Revision.dtDiff(template, t.term())));

        return matchThe(m);
    }

    default Task sample(long start, long end, Term template, NAR nar) {
        //TODO include template in value function
        return matchThe(TaskMatch.sampled(start, end, nar.random()));
    }
}
