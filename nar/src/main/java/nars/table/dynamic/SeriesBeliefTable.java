package nars.table.dynamic;

import jcog.Util;
import jcog.WTF;
import nars.NAL;
import nars.Task;
import nars.table.BeliefTable;
import nars.table.TaskTable;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
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

import static nars.Op.CONJ;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;

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
    public final void match(Answer a) {
        long s = a.start, e;

        double dur = a.dur;
        if (s == ETERNAL) {
            //choose now as the default focus time
            long now = a.nar.time();
            s = (long)Math.floor(now - dur/2);
            e = (long)Math.ceil(now + dur/2);
        } else {
            e = a.end;
        }

        int aTTL = a.ttl; //save

        //use at most a specific fraction of the TTL
        int seriesTTL = Math.min(aTTL, (int) (NAL.signal.SERIES_MATCH_MIN + Math.ceil(
            NAL.signal.SERIES_MATCH_ADDITIONAL_RATE_PER_DUR / Math.max(1, dur) * (e - s))));
        a.ttl = seriesTTL;
        series.whileEach(s, e, false, a);
        int ttlUsed = seriesTTL - a.ttl;

        a.ttl = aTTL - ttlUsed; //restore
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

    /** TODO only remove tasks which are weaker than the sensor */
    void clean(List<BeliefTable> tables, int marginCycles) {
        if (!NAL.signal.SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS)
            return;

        long sStart = series.start(), sEnd;
        if (sStart != TIMELESS && (sEnd = series.end()) != TIMELESS) {

            long finalEnd = sEnd - marginCycles, finalStart = sStart + marginCycles;
            if (finalStart < finalEnd) {

                Predicate<Task> cleaner = t -> absorbNonSignal(t, finalStart, finalEnd);

                for (int i = 0, tablesSize = tables.size(); i < tablesSize; i++) {
                    TaskTable b = tables.get(i);
                    if (b!=this && !(b instanceof DynamicTaskTable) && !(b instanceof EternalTable)) {
                        ((TemporalBeliefTable)b).removeIf(cleaner, finalStart, finalEnd);
                    }
                }
            }

        }
    }


    /**
     * used for if you can cache seriesStart,seriesEnd for a batch of calls
     * TODO only remove tasks which are weaker than the sensor
     */
    boolean absorbNonSignal(Task t, long seriesStart, long seriesEnd) {

        if (t.isGoal())
            throw new WTF();
        //assert(!t.isGoal());

        if (t.isDeleted())
            return true;

        long tStart = t.start();
        if (tStart != ETERNAL && seriesStart <= tStart) {
//            if (seriesStart != TIMELESS && seriesEnd != TIMELESS /* allow prediction 'suffix' */) {
            long tEnd = t.end();
            if (seriesEnd >= tEnd) {

                //if (LongInterval.intersectLength(tStart, tEnd, seriesStart, seriesEnd) != -1) {
                    //TODO actually absorb (transfer) the non-series task priority in proportion to the amount predicted, gradually until complete absorption
                    return !series.isEmpty(tStart, tEnd);
                //}
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



}
