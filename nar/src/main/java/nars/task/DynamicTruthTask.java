package nars.task;

import nars.NAL;
import nars.Op;
import nars.Task;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.Timed;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static nars.Op.*;

public class DynamicTruthTask extends UnevaluatedTask /*NALTask*/ {

    public DynamicTruthTask(Term c, boolean beliefOrGoal, Truth tr, Timed n, long start, long end, long[] stamp) throws TaskException {
        super(c, beliefOrGoal ? Op.BELIEF : Op.GOAL, tr, n.time(), start, end, stamp);

        assert (c.op() != NEG) : c + " is invalid task content op (NEG)";
    }

    @Nullable
    public static NALTask task(Term content, Truth t, Supplier<long[]> stamp, boolean beliefOrGoal, long start, long end, Timed time) {
        boolean neg = content.op() == NEG;
        if (neg) {
            content = content.unneg();
        }

        ObjectBooleanPair<Term> r = Task.tryContent(
                content,
                beliefOrGoal ? BELIEF : GOAL, !NAL.test.DEBUG_EXTRA);

        return r!=null ? new DynamicTruthTask(
                r.getOne(), beliefOrGoal,
                t.negIf(neg ^ r.getTwo()),
                time, start, end,
                stamp.get()) : null;
    }

    @Override
    public Task next(Object n) {
        //return Remember.the(this, NAL.belief.DYNAMIC_TRUTH_TASK_STORE, true, true, (What)n);
        return null;
    }

    @Override
    public boolean isInput() {
        return false;
    }

}
