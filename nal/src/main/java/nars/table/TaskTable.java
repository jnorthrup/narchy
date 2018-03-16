package nars.table;

import jcog.sort.Top;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.term.Term;

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

    default Task match(long when, Term t, NAR n) {
        if (isEmpty())
            return null;

        //prefer temporally relevant, and original
        Top<Task> q = new Top<>((x -> -x.minDistanceTo(when) / x.originality()));
        forEachTask(q::accept);
        return q.the;
    }

}
