package nars.table;

import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.table.eternal.EternalTable;
import nars.table.question.QuestionTable;
import nars.task.util.Answer;
import nars.term.Term;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.math.LongInterval.TIMELESS;
import static nars.time.Tense.ETERNAL;

/**
 * holds a set of ranked question/quests tasks
 * top ranking items are stored in the lower indexes so they will be first iterated
 */
public interface TaskTable {

    /**
     * attempt to insert a task.
     */
    void remember(Remember r);


    /**
     * number of items in this collection
     * warning: size()==0 does not necessarily mean that isEmpty(), although this is true for the default implementation
     */
    int taskCount();

    default boolean isEmpty() {
        return taskCount() == 0;
    }


    default void forEachTask(Consumer<? super Task> x) {
        taskStream().forEach(x);
    }

    /**
     * TODO add 'intersects or contains' option
     */
    default void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
        if (minT == ETERNAL || (this instanceof EternalTable)) {
            forEachTask(x);
        } else {
            assert(minT!=TIMELESS);
            forEachTask(t -> {
                if (t.intersectsRaw(minT, maxT))
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
    Stream<? extends Task> taskStream();

    default Task[] taskArray() {
        return taskStream().toArray(Task[]::new);
    }

    void match(Answer m);

    default Answer matching(long start, long end, @Nullable Term template, Predicate<Task> filter, int dur, NAR nar) {
        boolean beliefOrQuestion = !(this instanceof QuestionTable);
        assert(beliefOrQuestion);

        return Answer.relevant(beliefOrQuestion,
                Answer.BELIEF_MATCH_CAPACITY, start, end, template, filter, nar)
                .dur(dur)
                .match(this);
    }

    @Nullable default Task match(long start, long end, boolean forceProject, @Nullable Term template, Predicate<Task> filter, int dur, NAR nar) {
        return !isEmpty() ? matching(start, end, template, filter, dur, nar)
                .task(true, forceProject, false) : null;
    }

    @Nullable default /* final */ Task match(When<NAR> w, @Nullable Term template, Predicate<Task> filter) {
        return match(w.start, w.end, template, filter, w.dur, w.x); }

    @Nullable default /* final */ Task match(long start, long end, Term template, int dur, NAR nar) {
        return match(start, end, template, null, dur, nar); }

    @Nullable default /* final */ Task match(long start, long end, @Nullable Term template, Predicate<Task> filter, int dur, NAR nar) {
        return match(start, end, false, template, filter, dur,nar);
    }
    @Nullable default /* final */ Task matchExact(long start, long end, Term template, Predicate<Task> filter, int dur, NAR n) {
        return match(start, end, true, template, filter, dur, n);
    }

    default Task sample(When<NAR> when, @Nullable Term template, @Nullable Predicate<Task> filter) {

        if (isEmpty())
            return null;

        boolean isBeliefOrGoal = !(this instanceof QuestionTable);

        Answer answer = Answer.relevant(isBeliefOrGoal,
                isBeliefOrGoal ? Answer.BELIEF_SAMPLE_CAPACITY : Answer.QUESTION_SAMPLE_CAPACITY,
                when.start, when.end, template, filter, when.x);

        return answer.match(this).tasks.getRoulette(answer.random());
    }

    /** clear and fully deallocate if possible */
    default void delete() {
        clear();
    }
}
