package nars.truth.dynamic;

import nars.NAR;
import nars.Op;
import nars.control.op.Remember;
import nars.task.ITask;
import nars.task.UnevaluatedTask;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.Timed;

import static nars.Op.NEG;

public class DynamicTruthTask extends UnevaluatedTask /*NALTask*/{

    DynamicTruthTask(Term c, boolean beliefOrGoal, Truth tr, Timed n, long start, long end, long[] stamp) throws TaskException {
        super(c, beliefOrGoal ? Op.BELIEF : Op.GOAL, tr, n.time(), start, end, stamp);

        assert(c.op() != NEG): c + " is invalid task content op (NEG)";
    }

    @Override
    public ITask next(NAR n) {
        return Remember.the(this, false, true, true, n);
    }

    @Override
    public boolean isInput() {
        return false;
    }

}
