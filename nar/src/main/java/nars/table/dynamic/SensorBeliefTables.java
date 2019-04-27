package nars.table.dynamic;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.math.LongInterval;
import jcog.pri.op.PriMerge;
import jcog.sort.FloatRank;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.control.op.Remember;
import nars.link.AtomicTaskLink;
import nars.table.BeliefTables;
import nars.table.dynamic.SeriesBeliefTable.SeriesTask;
import nars.table.temporal.RTreeBeliefTable;
import nars.task.util.series.AbstractTaskSeries;
import nars.task.util.series.RingBufferTaskSeries;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.TIMELESS;

/**
 * special belief tables implementation
 * dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 *
 */
public class SensorBeliefTables extends BeliefTables {

    public final SeriesBeliefTable<SeriesTask> series;

    @Deprecated public FloatRange res;

    /**
     * permanent tasklink "generator" anchored in eternity when inseted to the concept on new tasks, but clones currently-timed tasklinks for propagation
     */
    public final AtomicTaskLink tasklink;

    public SensorBeliefTables(Term c, boolean beliefOrGoal) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                //new ConcurrentSkiplistTaskSeries<>(Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE)
                new RingBufferTaskSeries<>(  NAL.belief.signal.SIGNAL_BELIEF_TABLE_SERIES_SIZE));
    }

    SensorBeliefTables(Term term, boolean beliefOrGoal, AbstractTaskSeries<Task> s) {
        super(new SeriesBeliefTable<>(term, beliefOrGoal, s));

        this.series = tableFirst(SeriesBeliefTable.class); assert(series!=null);

        add(new MyRTreeBeliefTable());

        tasklink = newTaskLink(term);
    }

    protected AtomicTaskLink newTaskLink(Term term) {
        return new AtomicTaskLink(term);
    }

    @Override
    public final void remember(Remember r) {

        if (NAL.belief.signal.SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS) {
            Task x = r.input;
            if (!(x instanceof SeriesTask)) { //shouldnt happen anyway
                if (!x.isEternal() && !x.isInput() /* explicit overrides from user */) {
                    long seriesStart = series.start();
                    if (seriesStart != TIMELESS) {
                        if (series.absorbNonSignal(x, seriesStart, series.end())) {
                            r.forget(x);
                            return;
                        }
                    }
                }
            }
        }

        super.remember(r);
    }



    public void add(Truth value, long start, long end, FloatSupplier pri, short cause, int dur,What w) {
        NAL<NAL<NAR>> n = w.nar;

        if (value!=null) {
            value = value.dither(
                    Math.max(n.freqResolution.asFloat(), res.asFloat()),
                    n.confResolution.floatValue()
            );
        }

        SeriesTask x = add(value,
                start, end,
                series.term, series.punc(),
                dur,
                n);

        if (x!=null) {
            series.clean(this, n);
            x.cause(new short[] { cause });
            remember(x, pri, w);
        } else {
            this.prev = null;
        }
    }

//    long[] eviShared = null;

    private SeriesTask add(@Nullable Truth next, long nextStart, long nextEnd, Term term, byte punc, int dur, NAL<NAL<NAR>> NAL) {

        SeriesTask nextT = null, last = series.series.last();
        if (last != null) {
            long lastStart = last.start(), lastEnd = last.end();
            if (lastEnd > nextStart)
                return null; //too soon, does this happen?

            long gapCycles = (nextStart - lastEnd);
            if (gapCycles <= series.series.latchDurs() * dur) {

                if (next!=null) {
                    if (last.truth().equals(next)) {
                        //continue, if not excessively long
                        long stretchCycles = (nextEnd - lastStart);
                        if (stretchCycles <= series.series.stretchDurs() * dur) {
                            //Truth lastEnds = last.truth(lastEnd, 0);
                            //if (lastEnds!=null && lastEnds.equals(next)) {
                            //stretch
                            stretch(last, nextEnd);
                            return last;
                        }
                    }
                }

                //form new task either because the value changed, or because the latch duration was exceeded


                if (next == null) {
                    //guess that the signal stopped midway between (starting) now and the end of the last
                    long midGap = Math.min(nextStart-1, lastEnd + dur/2);
                    stretch(last, midGap);
                } else {
                    //stretch the previous to the current starting point for the new task
                    if (lastEnd < nextStart-1)
                        stretch(last, nextStart-1);
                }

            }
        }

        if (next != null) {
            series.add(nextT = newTask(term, punc, nextStart, nextEnd, next, NAL));
        }

        return nextT;
    }

    private SeriesTask newTask(Term term, byte punc, long s, long e, Truth next, NAL<NAL<NAR>> NAL) {
        long[] evi;

//        if (Param.REVISION_ALLOW_OVERLAP_IF_DISJOINT_TIME) {
//            if (eviShared == null)
//                eviShared = nar.evidence();
//            evi = eviShared;
//        } else {
            evi = NAL.evidence(); //unique
//        }

//        if (Param.SIGNAL_TASK_OCC_DITHER) {
//            int dither = nar.dtDither();
//            s = Tense.dither(s, dither);
//            e = Tense.dither(e, dither);
//        }

        return new SeriesTask(term, punc, next, s, e, evi);
    }

    static private void stretch(SeriesTask t, long e) {
        t.setEnd(e);
    }


//    @Override
//    public void match(Answer a) {
//        if (series.series.contains(a.time)) {
//            //try to allow the series to be the only authority in reporting
//            series.match(a);
//            if (a.tasks.isEmpty()) {
//                //if nothing was found, then search other tables
//                tables.each(t -> {
//                    if (t!=series)
//                        t.match(a);
//                });
//            }
//        } else {
//            super.match(a);
//        }
//    }


    public FloatRange resolution() {
        return res;
    }

    public void resolution(FloatRange res) {
        this.res = res;
    }



    private Task prev = null;


    /** link and emit */
    private void remember(Task next, FloatSupplier pri, What w) {
        //if (y==prev)

        Task prev = this.prev;
        this.prev = next;

        if (next == null)
            return; //?

        float surprise = NAL.signalSurprise(prev, next, pri, w.dur());
        if (surprise!=surprise)
            return;

        next.pri(surprise); //set the task's pri too

        float delta = tasklink.priMergeGetDelta(next.punc(), surprise, PriMerge.max);

//        float delta = tasklink.priMax(next.punc(), p/2);
//        delta += tasklink.priMax(QUESTION, p/4);
//        delta += tasklink.priMax(QUEST, p/4);

        ((What.TaskLinkWhat)w).links.link(tasklink);

        //if (prev!=next)
            w.nar.eventTask.emit(next);

    }



    /**
     * adjusted compression task value to exclude regions where the series belief table is defined.
     * allows regions outside of this more importance (ex: future)
     */
    private final class MyRTreeBeliefTable extends RTreeBeliefTable {

        @Override protected FloatRank<Task> taskStrength(boolean beliefOrGoal, long now, int narDur, int tableDur) {
            FloatRank<Task> base = super.taskStrength(beliefOrGoal, now, narDur, tableDur);

            int margin = narDur;

            long ss = series.start() + margin;
            if (ss != TIMELESS) {
                long se = series.end() - margin;
                if (se!=TIMELESS) {
                    return (t, min) -> {
                        float v = base.rank(t, min);
                        if (v == v && v > min) {
                            long ts = t.start();
                            long te = t.end();
                            long l = LongInterval.intersectLength(ts, te, ss, se);
                            if (l > 0) {
                                //discount the rank in proportion to how much of the task overlaps with the series
                                double range = (te-ts)+1;
                                float overlap = (float) Util.unitizeSafe(l / range);
                                float keep =
                                        //1 - overlap;
                                        1 - (overlap*overlap); //less intense but still in effect
                                v *= keep;
                            }
                            return v;
                        }
                        return Float.NaN;
                    };
                }
            }

            return base;

        }
    }
}
