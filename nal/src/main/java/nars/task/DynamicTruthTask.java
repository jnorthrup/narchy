package nars.task;

import jcog.Util;
import nars.Op;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.Timed;

public class DynamicTruthTask extends TemporalTask implements UnevaluatedTask  {

    public DynamicTruthTask(Term c, boolean beliefOrGoal, Truth tr, Timed n, long start, long end, long[] stamp) throws TaskException {
        super(c, beliefOrGoal ? Op.BELIEF : Op.GOAL, tr, n.time(), start, end, stamp);
    }

    @Override
    public boolean isInput() {
        return false;
    }
}
