package nars.table;

import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.Answer;
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




    default Truth truth(long start, long end, @Nullable Term template, Predicate<Task> filter, NAR n) {
        if (isEmpty())
            return null;
        return Answer.relevance(true, Answer.BELIEF_MATCH_CAPACITY, start, end, template, filter, n).
                match(this).truth();
    }

    default Truth truth(long start, long end, @Nullable Term template, NAR n) {
        return truth(start, end, template, null, n);
    }



    @Deprecated
    default Truth truth(long when, NAR nar) {
        return truth(when, when, null, nar);
    }

    @Deprecated
    default Truth truth(long start, long end, NAR nar) {
        return truth(start, end, null, nar);
    }

    default void print(/*@NotNull*/ PrintStream out) {
        this.forEachTask(t -> out.println(t + " " + Arrays.toString(t.stamp())));
    }

    default void print() {
        print(System.out);
    }


}










































    /* when does projecting to now not play a role? I guess there is no case,
    
    
    
    Ranker BeliefConfidenceOrOriginality = (belief, bestToBeat) -> {
        final float confidence = belief.getTruth().getConfidence();
        final float originality = belief.getOriginality();
        return or(confidence, originality);
    };*/










































