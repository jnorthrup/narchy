package nars.table;

import nars.NAL;
import nars.NAR;
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

    static double eternalTaskValue(Task eternal) {
        return eternal.evi();
    }

    static double eternalTaskValueWithOriginality(Task eternal) {
        return eternalTaskValue(eternal) * eternal.originality();
    }


    default Truth truth(long when, NAR nar) {
        return truth(when, when, null, null, nar);
    }
    default Truth truth(long when, float dur, NAR nar) {
        return truth(when, when, dur, nar);
    }
    default Truth truth(long start, long end, float dur, NAR nar) {
        return truth(start,end, null, null, dur, nar);
    }

    default Truth truth(long start, long end, NAR nar) {
        return truth(start, end, null, null, nar);
    }


    default Truth truth(long start, long end, @Nullable Term template, Predicate<Task> filter, NAR n) {
        return truth(start, end, template, filter, n.dur(), n);
    }
    default Truth truth(long start, long end, @Nullable Term template, Predicate<Task> filter, float dur, NAR n) {
        return truth(start, end, template, filter, NAL.ANSWER_BELIEF_MATCH_CAPACITY, dur, n);
    }

    /** precision = max # of tasks to include in the sample */
    default Truth truth(long start, long end, @Nullable Term template, Predicate<Task> filter, int precision, float dur, NAR n) {
        assert(precision <= NAL.STAMP_CAPACITY);
        if (isEmpty())
            return null;
        return Answer.taskStrength(true, precision, start, end, template, filter, n)
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










































