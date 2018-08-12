package nars.table;

import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.task.util.Answer;
import nars.task.util.TaskRank;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A model storing, ranking, and projecting beliefs or goals (tasks with TruthValue).
 * It should iterate in top-down order (highest ranking first)
 */
public interface BeliefTable extends TaskTable {

    BeliefTable Empty = new EmptyBeliefTable();

    static float eternalTaskValue(Task eternal) {
        return eternal.evi();
    }

    static float eternalTaskValueWithOriginality(Task eternal) {
        return eternalTaskValue(eternal) * eternal.originality();
    }

    /**
     * minT and maxT inclusive
     * TODO add Predicate<> form of this for early exit
     */
    void forEachTask(long minT, long maxT, Consumer<? super Task> x);

    /**
     * attempt to insert a task; returns what was input or null if nothing changed (rejected)
     */
    @Override
    void add(/*@NotNull*/ Remember r,  /*@NotNull*/ NAR nar);

    void match(TaskRank t);

    @Nullable default Task match(long start, long end, @Nullable Term template, Predicate<Task> filter, NAR nar) {
        return Answer.the(start, end, template, filter,nar).task(false);
    }


    default Truth truth(long start, long end, @Nullable Term template, NAR nar) {
        return Answer.the(start, end, template, null, nar).truth();
    }


    @Deprecated @Override default Task match(long start, long end, Term template, NAR nar) {
        return match(start, end, template, null, nar);
    }

    @Deprecated default Truth truth(long when, NAR nar) {
        return truth(when, when, null, nar);
    }

    @Deprecated default Truth truth(long start, long end, NAR nar) {
        return truth(start, end, null, nar);
    }

    default void print(/*@NotNull*/ PrintStream out) {
        this.forEachTask(t -> out.println(t + " " + Arrays.toString(t.stamp())));
    }

    default void print() {
        print(System.out);
    }

    default Task answer(long start, long end, Term template, Predicate<Task> filter, NAR n) {
        if (isEmpty())
            return null;
        Answer r = Answer.the(start, end, template, filter, n);
        match(r);
        return r.task(true);
    }










































































































































































    /*public float rank(final Task s, final long now) {
        return rankBeliefConfidenceTime(s, now);
    }*/


}










































    /* when does projecting to now not play a role? I guess there is no case,
    
    
    
    Ranker BeliefConfidenceOrOriginality = (belief, bestToBeat) -> {
        final float confidence = belief.getTruth().getConfidence();
        final float originality = belief.getOriginality();
        return or(confidence, originality);
    };*/










































