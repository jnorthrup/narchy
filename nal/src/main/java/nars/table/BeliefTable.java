package nars.table;

import nars.NAR;
import nars.NAL;
import nars.Task;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * A model storing, ranking, and projecting beliefs or goals (tasks with TruthValue).
 * It should iterate in top-down order (highest ranking first)
 *
 * TODO make an abstract class not interface
 */
public interface BeliefTable extends TaskTable {

    BeliefTable Empty = new EmptyBeliefTable();
    BeliefTable[] EmptyArray = new BeliefTable[0];

    static double eternalTaskValue(Task eternal) {
        return eternal.evi();
    }

    static double eternalTaskValueWithOriginality(Task eternal) {
        return eternalTaskValue(eternal) * eternal.originality();
    }


    default Truth truth(long when, NAR nar) {
        return truth(when, when, null, null, nar);
    }

    default Truth truth(long start, long end, NAR nar) {
        return truth(start, end, null, null, nar);
    }


    default Truth truth(long start, long end, @Nullable Term template, Predicate<Task> filter, NAR n) {
        return truth(start, end, template, filter, Answer.BELIEF_MATCH_CAPACITY, n.dur(), n);
    }

    /** precision = max # of tasks to include in the sample */
    default Truth truth(long start, long end, @Nullable Term template, Predicate<Task> filter, int precision, int dur, NAR n) {
        assert(precision < NAL.STAMP_CAPACITY);
        if (isEmpty())
            return null;
        return Answer.relevant(true, precision, start, end, template, filter, n)
                .dur(dur)
                .match(this).truth();
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










































