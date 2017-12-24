package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * holds a set of ranked question/quests tasks
 * top ranking items are stored in the lower indexes so they will be first iterated
 */
public interface TaskTable  {

    /**
     * attempt to insert a task.
     *
     * @return: the input task itself, it it was added to the table
     * an existing equivalent task if this was a duplicate
     */
    void add(Task t, TaskConcept c, NAR n);


    int capacity();


    /**
     * number of items in this collection
     */
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }



    void forEachTask(Consumer<? super Task> x);


    /** returns true if the task was removed */
    boolean removeTask(Task x);

    void clear();

    Stream<Task> streamTasks();


}
