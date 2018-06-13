package nars.table;

import jcog.sort.CachedTopN;
import jcog.sort.Top;
import jcog.sort.Top2;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.term.Term;
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

    default void match(TaskMatch m, NAR nar, Consumer<Task> target) {
        if (isEmpty())
            return;

        
        int limit = m.limit();
        Random rng = m.random();
        if (rng == null) {
            
            
            assert (limit > 0);
            if (limit == 1) {
                Top<Task> q = new Top<>(m.value());
                forEachTask(q);
                Task the = q.the;
                if (the != null)
                    target.accept(the);
            } else {
                CachedTopN<Task> q = new CachedTopN<>(limit, m.value());
                forEachTask(q::accept);
                q.forEachItem(target);
            }
        } else {

            int s = size();
            if (s == 1) {
                target.accept(streamTasks().findFirst().get());
            } else {

                Top2<Task> t = new Top2<>(m.value());
                forEachTask(t::add);

                if (t.size() <= limit) {
                    t.forEach(target); 
                } else {
                    t.sample(target, m.value(), rng);
                }

            }
        }

    }

    default int match(TaskMatch m, NAR nar, Task[] target) {
        final int[] i = {0};
        match(m, nar, x->{
            //if (x!=null)
            assert(x!=null);
            target[i[0]++] = x;
        });
        return i[0];
    }

    @Nullable
    default Task matchThe(TaskMatch m, NAR nar) {
        assert(m.limit()==1);
        Task[] x = new Task[1];
        int r = match(m, nar, x);
        return r == 0 ? null : x[0];
    }


    default Task match(long when, Term template, NAR nar) {
        return match(when, when, template, nar);
    }

    default Task match(long start, long end, Term template, NAR nar) {

        if (isEmpty())
            return null;


        return matchThe(TaskMatch.sampled(start, end, template, nar.random()), nar);
    }

    default Task sample(long start, long end, Term template, NAR nar) {

        if (isEmpty())
            return null;

        
        return matchThe(TaskMatch.sampled(start, end, template, nar.random()), nar);
    }
}
