package nars.table;

import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.table.question.QuestionTable;
import nars.task.util.Answer;
import nars.term.Term;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.time.Tense.ETERNAL;

/**
 * holds a set of ranked question/quests tasks
 * top ranking items are stored in the lower indexes so they will be first iterated
 */
public interface TaskTable {

    /**
     * attempt to insert a task.
     */
    void add(Remember r, NAR n);


    /**
     * number of items in this collection
     */
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }


    void forEachTask(Consumer<? super Task> x);

    /**
     * TODO add 'intersects or contains' option
     */
    default void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
        if (minT == ETERNAL) {
            forEachTask(x);
        } else {
            forEachTask(t -> {
                if (t.intersects(minT, maxT))
                    x.accept(t);
            });
        }
    }


    /**
     * returns true if the task was removed
     */
    boolean removeTask(Task x);

    void clear();

    Stream<? extends Task> streamTasks();

    default Task[] toArray() {
        return streamTasks().toArray(Task[]::new);
    }

    default void match(Answer m) {
        if (isEmpty())
            return;

        forEachTask(m);
    }

    @Deprecated
    default Task match(long start, long end, Term template, NAR nar) {

        if (isEmpty())
            return null;

        boolean belief = !(this instanceof QuestionTable);
        return Answer.relevance(belief, belief ? Answer.TASK_LIMIT : 1, start, end, template, null, nar)
                .match(this).task();
    }


}
