package nars.table;

import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.table.question.QuestionTable;
import nars.task.util.Answer;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
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

    void match(Answer m);
//        if (isEmpty())
//            return;
//
//        forEachTask(m);
//        throw new TODO();
//    }

    @Nullable default Task match(long when, Term template, NAR nar) {
        return match(when, when, template, nar);
    }
    @Nullable default Task match(long start, long end, Term template, NAR nar) { return match(start, end, template, null, nar); }

    @Nullable default Task match(long start, long end, @Nullable Term template, Predicate<Task> filter, NAR nar) {
        return !isEmpty() ? matching(start, end, template, filter, nar).task(true, true, false) : null;
    }

    default Answer matching(long start, long end, @Nullable Term template, Predicate<Task> filter, NAR nar) {
        return !isEmpty() ? Answer.relevance(!(this instanceof QuestionTable),
                start, end, template, filter, nar)
                .match(this) : null;
    }


    @Nullable default Task answer(long start, long end, Term template, NAR n) {
        return answer(start, end, template, null, n);
    }
    @Nullable default Task answer(long start, long end, Term template, Predicate<Task> filter, NAR n) {
        return !isEmpty() ? matching(start, end, template, filter, n).task(true, true, true) : null;
    }


    @Deprecated
    default Task sample(long start, long end, Term template, NAR nar) {

        if (isEmpty())
            return null;

        return matching(start, end, template, null, nar).task(false, false, false);
    }
}
