package nars.table.dynamic;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.math.LongInterval;
import jcog.sort.FloatRank;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.op.Remember;
import nars.control.op.TaskEvent;
import nars.link.AbstractTaskLink;
import nars.link.AtomicTaskLink;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.SeriesBeliefTable.SeriesTask;
import nars.table.temporal.RTreeBeliefTable;
import nars.task.util.series.AbstractTaskSeries;
import nars.task.util.series.RingBufferTaskSeries;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.lerp;
import static nars.Op.BELIEF;
import static nars.time.Tense.TIMELESS;

/**
 * special belief tables implementation
 * dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 *
 */
public class SensorBeliefTables extends BeliefTables {

    public final SeriesBeliefTable<SeriesTask> series;

    private Task current = null;

    /** priority factor for new tasks which are fully unsurprising */
    private float minSurprise = NAL.signal.SENSOR_SURPRISE_MIN_DEFAULT;


    public SensorBeliefTables(Term c, boolean beliefOrGoal) {
        this(c, beliefOrGoal, NAL.signal.SIGNAL_BELIEF_TABLE_SERIES_SIZE);
    }

    public SensorBeliefTables(Term c, boolean beliefOrGoal, int capacity) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                //new ConcurrentSkiplistTaskSeries<>(capacity)
                new RingBufferTaskSeries<>(  capacity ));
    }

    private SensorBeliefTables(Term term, boolean beliefOrGoal, AbstractTaskSeries<Task> s) {
        super(new SeriesBeliefTable<>(term, beliefOrGoal, s));

        this.series = tableFirst(SeriesBeliefTable.class); assert(series!=null);

        add(new MyRTreeBeliefTable());

    }

    @Override
    public final void remember(Remember r) {
        Task x = r.input;
        if (x instanceof SeriesTask)
            return;

        if (NAL.signal.SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS && !x.isEternal() && !x.isInput()) {
            if (series.absorbNonSignal(x)) {
                r.forget(x);
                return;
            }
        }

        super.remember(r);
    }



    public SeriesTask update(Truth value, long now, short[] cause, float dur, What w) {
        NAR n = w.nar;

        SeriesTask x =
            add(value,
                    now,
                series.term, series.punc(),
                dur,
                n);

        if (x!=null) {
            series.clean(this, n);
            x.cause(cause);
        }
        return x;
    }

    public void input(Truth value, long now, FloatSupplier pri, short[] cause, float dur, What w, @Deprecated boolean link) {
        SeriesTask x = update(value, now, cause, dur, w);

        if(x!=null)
            remember(x, w, pri, link, dur);

        this.current = x;
    }

//    long[] eviShared = null;

    /** @param dur can be either a perceptual duration which changes, or a 'physical duration' determined by
     *             the interface itself (ex: clock rate) */
    private SeriesTask add(@Nullable Truth next, long now, Term term, byte punc, float dur, NAL nar) {


        AbstractTaskSeries<SeriesTask> series = this.series.series;

        SeriesTask nextT = null, last = series.last();
        long lastEnd = last!=null ? last.end() : Long.MIN_VALUE;
        long nextStart = Math.max(lastEnd+1, Math.round(now - dur/2));
        long nextEnd = Math.max(now, nextStart); //Math.max(nextStart+1, Math.round( now + dur/2));
        if (last != null) {
            long lastStart = last.start();
            if (lastEnd > now)
                return null; //too soon, does this happen?

            long gapCycles = (now - lastEnd);
            if (gapCycles <= series.latchDurs() * dur) {

                if (next!=null) {
                    long stretchCycles = (now - lastStart);
                    boolean stretchable = stretchCycles <= series.stretchDurs() * dur;
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
            this.series.add(nextT = newTask(term, punc, nextStart, nextEnd, next, nar));
        }

        return nextT;
    }

    private SeriesTask newTask(Term term, byte punc, long s, long e, Truth truth, NAL nar) {
        long[] evi;
            evi = nar.evidence(); //unique

        return new SeriesTask(term, punc, truth, s, e, evi);
    }

    static private void stretch(SeriesTask t, long e) {
//        System.out.println("stretch " + t.end() + " .. " +  e + " (" + (e - t.end()) + " cycles)");
        t.setEnd(e);
    }


    /** link and emit */
    private void remember(Task next, What w, FloatSupplier pri, boolean link, float dur) {

        Task prev = this.current;

        float p;
        if (prev!=next) {
            float nextPri = pri.asFloat() *
                    lerp((float) SensorBeliefTables.surprise(prev, next, dur),
                            minSurprise, 1);

            next.priSet(nextPri);

            if (link) {
                TaskLinkWhat ww = (TaskLinkWhat) w;
                AbstractTaskLink tl = AtomicTaskLink.link(next.term());
                tl.priSet(BELIEF, nextPri);
//        tl.priSet(BELIEF, surprise*2f/3f);
////        tl.priSet(GOAL, surprise/3);
//        tl.priSet(QUEST, surprise*1f/3f);
                ww.links.link(tl);
            }

            TaskEvent.emit(next, w.nar);
        }

    }

    /**
     * used to determine the priority factor of new signal tasks,
     * with respect to how significantly it changed from a previous value,
     * OR the time duration since last received signal
     */
    static double surprise(final Task prev, final Task next, float dur) {

        final boolean NEW = prev == null;
        if (NEW)
            return 1;

        final boolean stretched = prev == next;
        if (stretched)
            return 0;

        float deltaFreq = prev != next ? Math.abs(prev.freq() - next.freq()) : 0; //TODO use a moving average or other anomaly/surprise detection

        long sepCycles  = stretched ? 0 : Math.abs(next.start() - prev.end());
        double deltaTime = 1 - NAL.evi(1, sepCycles, dur);

        return Util.or(deltaFreq , deltaTime);
    }


    public float surprise() {
        Task n = current;
        return (n!=null) ? n.priElseZero() : 0;
    }

    public final Task current() {
        return current;
    }

    public BeliefTable minSurprise(float s) {
        this.minSurprise = s;
        return this;
    }

    /**
     * adjusted compression task value to exclude regions where the series belief table is defined.
     * allows regions outside of this more importance (ex: future)
     */
    private final class MyRTreeBeliefTable extends RTreeBeliefTable {

        @Override protected FloatRank<Task> taskStrength(boolean beliefOrGoal, long now, float narDur, int tableDur) {
            FloatRank<Task> base = super.taskStrength(beliefOrGoal, now, narDur, tableDur);


            long _ss = series.start();
            if (_ss != TIMELESS) {
                long _se = series.end();
                if (_se!=TIMELESS) {
                    float margin = narDur;
                    long ss = Math.round(_ss + margin);
                    long se = Math.round(_se - margin);
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
