package nars.table;

import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.table.question.QuestionTable;
import nars.task.util.Answer;
import nars.term.Term;
import nars.time.When;
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
     * warning: size()==0 does not necessarily mean that isEmpty(), although this is true for the default implementation
     */
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }


    default void forEachTask(Consumer<? super Task> x) {
        streamTasks().forEach(x);
    }

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

    void setTaskCapacity(int newCapacity);

    /**
     * returns true if the task was removed.
     * implementations should delete the task only when delete is true
     */
    boolean removeTask(Task x, boolean delete);



    void clear();

    /** in dynamic implementations, this will be an empty stream */
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


    @Nullable default Task match(When w, @Nullable Term template) { return match(w, template, null); }

    @Nullable default Task match(When w, @Nullable Term template, Predicate<Task> filter) { return match(w.start, w.end, template, filter, w.dur, w.nar); }

    @Nullable default Task match(long start, long end, Term template, int dur, NAR nar) { return match(start, end, template, null, dur, nar); }

    @Deprecated @Nullable default Task match(long start, long end, @Nullable Term template, Predicate<Task> filter, int dur, NAR nar) {
        return !isEmpty() ? matching(start, end, template, filter, dur, nar)
                .task(true, false, false) : null;
    }

    default Answer matching(long start, long end, @Nullable Term template, Predicate<Task> filter, int dur, NAR nar) {
        boolean beliefOrQuestion = !(this instanceof QuestionTable);
        assert(beliefOrQuestion);

        return Answer.relevant(beliefOrQuestion,
                Answer.BELIEF_MATCH_CAPACITY, start, end, template, filter, nar)
                .dur(dur)
                .match(this);
    }


    @Nullable default Task answer(long start, long end, Term template, Predicate<Task> filter, int dur, NAR n) {
        return !isEmpty() ? matching(start, end, template, filter, dur, n)
                .task(true, true, false) : null;
    }



    default Task sample(When when, @Nullable Term template, @Nullable Predicate<Task> filter) {

        if (isEmpty())
            return null;

        boolean isBeliefOrGoal = !(this instanceof QuestionTable);

        return Answer.relevant(isBeliefOrGoal,
                isBeliefOrGoal ? Answer.BELIEF_SAMPLE_CAPACITY : Answer.QUESTION_SAMPLE_CAPACITY,
                when.start, when.end, template, filter, when.nar)
            .sample(this);

    }

    //    static Predicate<Task> filterConfMin(Predicate<Task> filter, NAR nar) {
//        float eviMin = c2wSafe(nar.confMin.floatValue());
//
//        return (filter == null) ?
//            t -> t.evi() >= eviMin
//            :
//            t -> t.evi() >= eviMin && filter.test(t)
//        ;
//    }

    /** clear and fully deallocate if possible */
    default void delete() {
        clear();
    }

}
