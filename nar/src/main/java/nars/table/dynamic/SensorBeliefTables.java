package nars.table.dynamic;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.math.LongInterval;
import jcog.sort.FloatRank;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.op.Remember;
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

//    /**
//     * permanent tasklink "generator" anchored in eternity when inseted to the concept on new tasks, but clones currently-timed tasklinks for propagation
//     */
//    public final AtomicTaskLink tasklink;

    public SensorBeliefTables(Term c, boolean beliefOrGoal) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                //new ConcurrentSkiplistTaskSeries<>(Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE)
                new RingBufferTaskSeries<>(  NAL.belief.signal.SIGNAL_BELIEF_TABLE_SERIES_SIZE));
    }

    private SensorBeliefTables(Term term, boolean beliefOrGoal, AbstractTaskSeries<Task> s) {
        super(new SeriesBeliefTable<>(term, beliefOrGoal, s));

        this.series = tableFirst(SeriesBeliefTable.class); assert(series!=null);

        add(new MyRTreeBeliefTable());

//        tasklink = newTaskLink(term);
    }

//    private AtomicTaskLink newTaskLink(Term term) {
//        return new AtomicTaskLink(term);
//    }

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



    public void add(Truth value, long now, FloatSupplier pri, short[] cause, int dur, What w) {
        NAR n = w.nar;

        if (value!=null) {
            value = value.dither(
                Math.max(w.nar.freqResolution.floatValue(), this.res.asFloat()),
                n.confResolution.floatValue()
            );
        }


        SeriesTask x =
            add(value,
                    now,
                series.term, series.punc(),
                dur,
                n);

        if (x!=null) {
            series.clean(this, n);
            x.cause(cause);
            remember(x, pri, w);
        } else {
            this.prev = null;
        }
    }

//    long[] eviShared = null;

    /** @param dur can be either a perceptual duration which changes, or a 'physical duration' determined by
     *             the interface itself (ex: clock rate) */
    private SeriesTask add(@Nullable Truth next, long now, Term term, byte punc, int dur, NAR nar) {


        SeriesTask nextT = null, last = series.series.last();
        long lastEnd = last!=null ? last.end() : Long.MIN_VALUE;
        long nextStart = Math.max(lastEnd+1, now - dur /* /2 */);
        long nextEnd = Math.max(nextStart+1, now + dur/2);
        if (last != null) {
            long lastStart = last.start();
            if (lastEnd > now)
                return null; //too soon, does this happen?

            long gapCycles = (now - lastEnd);
            if (gapCycles <= series.series.latchDurs() * dur) {

                if (next!=null) {
                    long stretchCycles = (now - lastStart);
                    boolean stretchable = stretchCycles <= series.series.stretchDurs() * dur;
                    if (stretchable) {
                        if (last.truth().equals(next)) {
                            //continue, if not excessively long


                            //Truth lastEnds = last.truth(lastEnd, 0);
                            //if (lastEnds!=null && lastEnds.equals(next)) {
                            //stretch
                            stretch(last, nextEnd);
                            return last;

                        }
                    }
                }

                //form new task either because the value changed, or because the latch duration was exceeded


                /*if (next == null) {
                    //guess that the signal stopped midway between (starting) now and the end of the last
                    long midGap = Math.min(nextStart-1, lastEnd + dur/2);
                    stretch(last, midGap);*/


                //stretch the previous to the current starting point for the new task
                if (lastEnd < nextStart-1)
                    stretch(last, nextStart-1);

            }
        }

        if (next != null) {



//                System.out.println("new " + now + " .. " + nextEnd + " (" + (nextEnd - now) + " cycles)");

            series.add(nextT = newTask(term, punc, nextStart, nextEnd, next, nar));
        }

        return nextT;
    }

    private SeriesTask newTask(Term term, byte punc, long s, long e, Truth next, NAR nar) {
        long[] evi;

//        if (Param.REVISION_ALLOW_OVERLAP_IF_DISJOINT_TIME) {
//            if (eviShared == null)
//                eviShared = nar.evidence();
//            evi = eviShared;
//        } else {
            evi = nar.evidence(); //unique
//        }



        return new SeriesTask(term, punc, next, s, e, evi);
    }

    static private void stretch(SeriesTask t, long e) {
//        System.out.println("stretch " + t.end() + " .. " +  e + " (" + (e - t.end()) + " cycles)");
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
        if (surprise!=surprise || surprise < Float.MIN_NORMAL)
            return;

        next.priMax(surprise); //set the task's pri too


        ///float delta = tasklink.priMergeGetDelta(next.punc(), surprise, PriMerge.max);

//        float delta = tasklink.priMax(next.punc(), p/2);
//        delta += tasklink.priMax(QUESTION, p/4);
//        delta += tasklink.priMax(QUEST, p/4);


        ((TaskLinkWhat)w).links.link(next, surprise, w.nar);


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

            long _ss = series.start();
            if (_ss != TIMELESS) {
                long _se = series.end();
                if (_se!=TIMELESS) {
                    long ss = _ss + margin;
                    long se = _se - margin;
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
