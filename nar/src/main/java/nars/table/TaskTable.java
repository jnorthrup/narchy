package nars.table;

import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.control.op.Remember;
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
        if (minT == ETERNAL) {
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

    void match(Answer a);

    default Answer matching(long start, long end, @Nullable Term template, Predicate<Task> filter, float dur, NAR nar) {
        var beliefOrQuestion = !(this instanceof QuestionTable);

        var a = Answer.taskStrength(beliefOrQuestion, NAL.ANSWER_BELIEF_MATCH_CAPACITY, start, end, template, filter, nar).dur(dur);
        match(a);
        return a;
    }

    default @Nullable Task match(long start, long end, boolean forceProject, @Nullable Term template, Predicate<Task> filter, float dur, NAR nar, boolean ditherTruth) {
        return !isEmpty() ? matching(start, end, template, filter, dur, nar)
                .task(true, forceProject, ditherTruth) : null;
    }

    default @Nullable   Task match(When<What> w, @Nullable Term template, Predicate<Task> filter, float dur, boolean ditherTruth) {
        return match(w.start, w.end, false, template, filter, dur, w.x.nar, ditherTruth); }

    default @Nullable   Task match(long start, long end, Term template, float dur, NAR nar) {
        return match(start, end, template, null, dur, nar); }

    default @Nullable   Task match(long start, long end, @Nullable Term template, Predicate<Task> filter, float dur, NAR nar) {
        return match(start, end, false, template, filter, dur,nar, false);
    }
    default @Nullable   Task matchExact(long start, long end, Term template, Predicate<Task> filter, float dur, NAR n) {
        return match(start, end, true, template, filter, dur, n, false);
    }

    private @Nullable Answer sampleAnswer(When<What> when, @Nullable Term template, @Nullable Predicate<Task> filter) {
        return sampleAnswer(when.start, when.end, template, filter, when.dur, when.x.nar);
    }

    @Deprecated
    default @Nullable Answer sampleAnswer(long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, float dur, NAR nar) {

        if (isEmpty())
            return null;

        var isBeliefOrGoal = !(this instanceof QuestionTable);

        var a = Answer.taskStrength(isBeliefOrGoal,
            isBeliefOrGoal ? NAL.ANSWER_BELIEF_SAMPLE_CAPACITY : NAL.ANSWER_QUESTION_SAMPLE_CAPACITY,
            start, end, template, filter, nar);
        a.dur(dur);

        match(a);
        return a.isEmpty() ? null : a;
    }

//    @Nullable default Answer sampleSome(When<NAR> when, @Nullable Term template, @Nullable Predicate<Task> filter) {
//        return sampleAnswer(when, template, filter);
//    }

    default @Nullable Task sample(When<What> when, @Nullable Term template, @Nullable Predicate<Task> filter) {
        var a = sampleAnswer(when, template, filter);
        return a==null ? null : a.sample();
    }

    /** clear and fully deallocate if possible */
    default void delete() {
        clear();
    }


}
