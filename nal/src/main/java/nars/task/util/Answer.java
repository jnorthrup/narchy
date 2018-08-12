package nars.task.util;

import jcog.TODO;
import jcog.sort.FloatRank;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.polation.TruthIntegration;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.time.Tense.ETERNAL;

/** collects and ranks Tasks so that matches or evidence-aware truth values may be computed in various ways */
public class Answer extends TaskRank {

    final static int TASK_LIMIT = Param.STAMP_CAPACITY-1;

    private final NAR nar;

    @Deprecated public static Answer the(long start, long end, @Nullable Term template, @Nullable Predicate<Task> filter, NAR nar) {

        long dur = nar.dur();
        FloatRank<Task> r;
        if (template == null) {
            if (start == ETERNAL) {
                throw new TODO();
            } else {
                r = (t, m) -> {
                    if (!filter.test(t))
                        return Float.NaN;
                    return TruthIntegration.eviInteg(t, start, end, dur);
                };
            }
        } else {
            r = (t, m) -> {
                throw new TODO();
            };
        }
        return the(new TimeRangeFilter(start, end, true), r, nar);
    }

    public static Answer the(@Nullable TimeRangeFilter  timeFilter, FloatRank<Task> rank, NAR nar) {
        return new Answer(TASK_LIMIT, rank, timeFilter, nar);
    }

    private Answer(int limit, FloatRank<Task> rank, @Nullable TimeRangeFilter timeFilter, NAR nar) {
        super(limit, rank, timeFilter);
        this.nar = nar;
    }

    /**
     * matches, and projects to the specified time-range if necessary
     */
    @Nullable public Task task(boolean forceProject) {
        throw new TODO();
//        Task m = match(start, end, template, filter, nar);
//        if (m == null)
//            return null;
//        if (m.containedBy(start, end))
//            return m;
//        Task t = Task.project(false, m, start, end, nar, false);
//        if (t instanceof TaskProxy) {
//            //dither truth
//            @Nullable PreciseTruth tt = t.truth().dither(nar);
//            if (tt != null) {
//                t = Task.clone(t, t.term(), tt, t.punc());
//            } else {
//                t = null;
//            }
//        }
//        return t;
    }

    public Truth truth() {
        throw new TODO();
    }
}
