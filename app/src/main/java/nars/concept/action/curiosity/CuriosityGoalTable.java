package nars.concept.action.curiosity;

import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.util.series.RingBufferTaskSeries;
import nars.term.Term;

/** disables revision merge so that revisions, not being CuriosityTask and thus intercepted, cant directly
 *  contaminate the normal derived goal table
 *  and compete (when curiosity confidence is stronger) with authentic derived
 *  goals which we are trying to learn to form and remember. */
public final class CuriosityGoalTable extends SeriesBeliefTable {

    public CuriosityGoalTable(Term c, int capacity) {
        super(c, false, new RingBufferTaskSeries<>(capacity));
    }

    @Override
    public void add(Remember r, NAR n) {
        Task t = r.input;

        if (!(t instanceof CuriosityTask))
            return;
        assert(!(t.isEternal()));

        series.push(t);
        r.remember(t);
    }

//    @Override
//    protected @Nullable Task revise(@Nullable Task x, Task y, NAR nar) {
//        return null;
//    }
}
