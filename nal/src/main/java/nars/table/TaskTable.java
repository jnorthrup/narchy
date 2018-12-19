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
import static nars.truth.func.TruthFunctions.c2wSafe;

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

    /** default behavior is similar to match but sample implies an attempt at fair, random sort order of visited components.
     *  see mplementations such as BeliefTables */
    default void sample(Answer m) {
        match(m);
    }

//        if (isEmpty())
//            return;
//
//        forEachTask(m);
//        throw new TODO();
//    }

    @Nullable
    default Task match(long when, Term template, NAR nar) {
        return match(when, when, template, nar);
    }
    @Nullable default Task match(long start, long end, Term template, NAR nar) { return match(start, end, template, null, nar); }

    @Nullable default Task match(long start, long end, @Nullable Term template, Predicate<Task> filter, NAR nar) {
        return !isEmpty() ? matching(start, end, template, filter, nar).task(true, true, false) : null;
    }

    default Answer matching(long start, long end, @Nullable Term template, Predicate<Task> filter, NAR nar) {
        boolean beliefOrQuestion = !(this instanceof QuestionTable);
        return Answer.relevance(beliefOrQuestion,
                beliefOrQuestion ? Answer.TASK_LIMIT_DEFAULT : 1, start, end, template, filter, nar)
                .match(this);
    }


    @Nullable default Task answer(long start, long end, Term template, NAR n) {
        return answer(start, end, template, null, n);
    }
    @Nullable default Task answer(long start, long end, Term template, Predicate<Task> filter, NAR n) {
        return !isEmpty() ? matching(start, end, template, filter, n)
                .task(true, true, true) : null;
    }


    default Task sample(long start, long end, Term template, NAR nar) {
        return sample(start, end, template, null, nar);
    }

    default Task sample(long start, long end, Term template, Predicate<Task> filter, NAR nar) {

        if (isEmpty())
            return null;

        boolean isBeliefOrGoal = !(this instanceof QuestionTable);
        return Answer.relevance(isBeliefOrGoal,
                isBeliefOrGoal ? Answer.BELIEF_SAMPLE_CAPACITY : Answer.QUESTION_SAMPLE_CAPACITY,
                start, end, template, isBeliefOrGoal ? filterConfMin(filter, nar) : filter, nar)
            .ditherTruth(true)
            .sample(this)
            .task(false, false, false);

//        return matching(start, end, template, filter, nar).task(false, false, false);

    }

    static Predicate<Task> filterConfMin(Predicate<Task> filter, NAR nar) {
        float eviMin = c2wSafe(nar.confMin.floatValue());

        return (filter == null) ?
            t -> t.evi() >= eviMin
            :
            t -> t.evi() >= eviMin && filter.test(t)
        ;
    }

    /** clear and fully deallocate if possible */
    default void delete() {
        clear();
    }

}
