package nars.table.dynamic;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.LongInterval;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.table.BeliefTable;
import nars.table.TaskTable;
import nars.table.eternal.EternalTable;
import nars.task.util.Answer;
import nars.task.util.series.AbstractTaskSeries;
import nars.task.util.signal.SignalTask;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.math.LongInterval.TIMELESS;
import static nars.Op.CONJ;
import static nars.time.Tense.ETERNAL;

/**
 * adds a TaskSeries additional Task buffer which can be evaluated from, or not depending
 * if a stored task is available or not.
 */
public class SeriesBeliefTable<T extends Task> extends DynamicTaskTable {

    public final AbstractTaskSeries<T> series;


    public SeriesBeliefTable(Term c, boolean beliefOrGoal, AbstractTaskSeries<T> s) {
        super(c, beliefOrGoal);
        this.series = s;
    }

    @Override
    public int taskCount() {
        return series.size();
    }

    @Override
    public final void match(Answer t) {
        long s = t.time.start, e;
        Predicate<Task> each;
        float dur = Math.max(1, t.dur);
        if (t.time.start == ETERNAL) {
            //choose now as the default focus time
            long now = t.nar.time();
            s = Math.round(now - dur/2);
            e = Math.round(now + dur/2);
        } else {
            e = t.time.end;
        }

        int seriesTTL = (int) (NAL.belief.signal.SERIES_MATCH_MIN + Math.ceil(NAL.belief.signal.SERIES_MATCH_ADDITIONAL_RATE_PER_DUR / dur * (e-s)));
        if (seriesTTL < t.ttl)
            each = Util.limit(t::test, seriesTTL);
        else
            each = t::test;

        series.whileEach(s, e, false, each);
    }


    @Override
    public void clear() {
        series.clear();
    }

    @Override
    public Stream<? extends Task> taskStream() {
        return series.stream();
    }

    @Override
    public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
        series.forEach(minT, maxT, true, x);
    }

    @Override
    public void forEachTask(Consumer<? super Task> action) {
        series.forEach(action);
    }

    void clean(List<BeliefTable> tables, NAR n) {
        if (!NAL.belief.signal.SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS)
            return;

        long sStart = series.start(), sEnd;
        if (sStart != TIMELESS && (sEnd = series.end()) != TIMELESS) {

            TaskFEMABox deleteAfter = new TaskFEMABox(sStart, sEnd);
            Consumer<Task> cleaner = deleteAfter::add;
            for (TaskTable b : tables) {
                if (!(b instanceof DynamicTaskTable) && !(b instanceof EternalTable))
                    b.forEachTask(sStart, sEnd, cleaner);
                deleteAfter.flush(b);
            }

        }
    }

    /**
     * used for if you can cache seriesStart,seriesEnd for a batch of calls
     */
    boolean absorbNonSignal(Task t, long seriesStart, long seriesEnd) {

        if (t.isDeleted())
            return true;

        long tStart = t.start();
        if (tStart != ETERNAL) {
//            if (seriesStart != TIMELESS && seriesEnd != TIMELESS /* allow prediction 'suffix' */) {
            long tEnd = t.end();
            if (seriesEnd >= tEnd) {
                if (LongInterval.intersectLength(tStart, tEnd, seriesStart, seriesEnd) != -1) {
                    //TODO actually absorb (transfer) the non-series task priority in proportion to the amount predicted, gradually until complete absorption
                    return !series.isEmpty(t);
                }
            }
        }
        return false;

    }

    /**
     * adjust CONJ concepts for series task generation
     */
    protected static Term taskTerm(Term x) {
        if (x.op() == CONJ)
            return ((Compound)x).dt(0);
        else
            return x;
    }

    public final void add(T nextT) {
        series.compress();

        series.push(nextT);
    }

    public final long start() {
        return series.start();
    }

    public final long end() {
        return series.end();
    }


    /**
     * has special equality and hashcode convention allowing the end to stretch;
     * otherwise it would be seen as unique when tested after stretch
     */
    public static final class SeriesTask extends SignalTask {

        /**
         * current endpoint
         */
        protected long e;

        SeriesTask(Term term, byte punc, Truth value, long start, long end, long[] stamp) {
            super(SeriesBeliefTable.taskTerm(term), punc, value, start, start, end, stamp);
            if (stamp.length != 1)
                throw new UnsupportedOperationException("requires stamp of length 1 so it can be considered an Input Task and thus have consistent hashing even while its occurrrence time is stretched");
            this.e = end;
        }

        @Override
        protected int hashCalculate(long start, long end, long[] stamp) {
            //TODO also involve Term?
            return Util.hashCombine(term().hashCode(), Util.hashCombine(stamp[0], start));
        }


        /** series tasks can be assumed to be universally unique */
        @Override
        public boolean equals(Object x) {
            return this == x;
//            if (x instanceof SeriesTask) {
//                //TODO also involve Term?
//                Task xx = (Task) x;
//                if (hashCode() != x.hashCode())
//                    return false;
//                return stamp()[0] == xx.stamp()[0] && start() == xx.start() && term().equals(xx.term());
//            }
        }

        public void setEnd(long e) {
            this.e = e;
        }

        @Override
        public long end() {
            return e;
        }

    }


    private final class TaskFEMABox extends FasterList<Task> {
        private final long sStart, sEnd;

        TaskFEMABox(long sStart, long sEnd) {
            super(0);
            this.sStart = sStart;
            this.sEnd = sEnd;
        }

        public void flush(TaskTable b) {
            if (!isEmpty()) {
                forEachWith((t,B) -> B.removeTask(t, true), b);
                clear();
            }
        }

        @Override
        public boolean add(Task t) {
            return absorbNonSignal(t, sStart, sEnd) && super.add(t);
        }
    }
}
