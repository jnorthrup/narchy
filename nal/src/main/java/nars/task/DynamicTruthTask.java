package nars.task;

import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.Timed;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Function;

import static nars.Op.*;

public class DynamicTruthTask extends UnevaluatedTask /*NALTask*/{

    public DynamicTruthTask(Term c, boolean beliefOrGoal, Truth tr, Timed n, long start, long end, long[] stamp) throws TaskException {
        super(c, beliefOrGoal ? Op.BELIEF : Op.GOAL, tr, n.time(), start, end, stamp);

        assert(c.op() != NEG): c + " is invalid task content op (NEG)";
    }

    public static NALTask Task(Term content, Truth t, Function<Random, long[]> stamp, boolean beliefOrGoal, long start, long end, NAR nar) {
        if (content.op() == NEG) {
            content = content.unneg();
            t = t.neg();
        }

        @Nullable ObjectBooleanPair<Term> r = Task.tryContent(
                content,
                beliefOrGoal ? BELIEF : GOAL, !Param.DEBUG_EXTRA);
        if (r == null)
            return null;

        return new DynamicTruthTask(
                r.getOne(), beliefOrGoal,
                t.negIf(r.getTwo()),
                nar, start, end,
                stamp.apply(nar.random()));
    }

    @Override
    public ITask next(NAR n) {
        return Remember.the(this, Param.DYNAMIC_TRUTH_TASK_STORE, true, true, n);
    }

    @Override
    public boolean isInput() {
        return false;
    }

}
