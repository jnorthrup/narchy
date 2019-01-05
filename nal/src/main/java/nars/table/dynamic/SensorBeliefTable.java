package nars.table.dynamic;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.series.AbstractTaskSeries;
import nars.term.Term;

@Deprecated public class SensorBeliefTable extends SeriesBeliefTable<SeriesBeliefTable.SeriesTask> {

    public SensorBeliefTable(Term c, boolean beliefOrGoal, AbstractTaskSeries s) {
        super(c, beliefOrGoal, s);
    }

    @Override
    public void add(Remember r, NAR n) {

        Task x = r.input;

        if (x.isEternal() || x instanceof SeriesTask)
            return; //already owned, or was owned

        if (Param.FILTER_SIGNAL_TABLE_TEMPORAL_TASKS) {
            Task y = absorbNonSignal(x, series.start(), series.end()) ? null : x;
            if (y == null) {
                r.reject();
            } else if (y != x) {
                r.input = y; //assume same concept
            }
        }

    }
}
