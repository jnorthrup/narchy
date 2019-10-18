package nars.table.dynamic;

import jcog.Util;
import nars.NAL;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.op.Remember;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.SeriesBeliefTable.SeriesTask;
import nars.table.temporal.RTreeBeliefTable;
import nars.task.util.series.AbstractTaskSeries;
import nars.task.util.series.RingBufferTaskSeries;
import nars.term.Term;
import nars.time.Tense;
import nars.time.When;
import nars.truth.Truth;

import static jcog.Util.lerp;

/**
 * special belief tables implementation
 * dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 *
 */
public class SensorBeliefTables extends BeliefTables {

    public final SeriesBeliefTable series;

    private SeriesTask prev = null, current = null;

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

    private SensorBeliefTables(Term term, boolean beliefOrGoal, AbstractTaskSeries<SeriesTask> s) {
        super(new SeriesBeliefTable(term, beliefOrGoal, s));

        this.series = tableFirst(SeriesBeliefTable.class); assert(series!=null);

        add(new MyRTreeBeliefTable());

        /* 0=series will override the r-tree table */
        //matchMode = 0;
    }

    @Deprecated public static int cleanMarginCycles(float dur) {
        return Math.round(NAL.signal.CLEAN_MARGIN_DURS * dur);
    }

    @Override
    public final void remember(Remember r) {
        Task x = r.input;
        if (x instanceof SeriesTask)
            return; //already inserted when constructed, dont allow any other way

        if (NAL.signal.SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS && !x.isEternal() /*&& !x.isInput()*/) {
            if (absorbNonSignal(r)) {
                r.forget(x);
                return;
            }
        }

        super.remember(r);
    }

    private boolean absorbNonSignal(Remember r) {
        long seriesStart = series.start();
        if (seriesStart == Tense.TIMELESS)
            return false;
        long seriesEnd = series.end();
        if (seriesEnd == Tense.TIMELESS)
            seriesEnd = seriesStart;

        int cleanMargin = cleanMarginCycles(r.what.dur());
        long ss = seriesStart + cleanMargin;
        long ee = seriesEnd - cleanMargin;
        return ss < ee && series.absorbNonSignal(r.input, ss, ee);
    }



    /** pre-commit */
    public boolean input(Truth value, When<What> when, Term why) {
        SeriesTask next = series.add(value, when, why);
        SeriesTask prev = this.current;
        boolean novel = next!=null && next!=prev;
        this.prev = prev;
        this.current = next;
        return novel;
    }


    /** link and emit */
    public void remember(What w, float pri, boolean link, float dur) {

        Task prev = this.prev;
        Task next = this.current;
        //assert(prev!=next);

            float nextPri = pri *
                    lerp((float) SensorBeliefTables.surprise(prev, next, dur),
                            minSurprise, 1);

            next.pri(nextPri);

            if (link) {
                TaskLinkWhat ww = (TaskLinkWhat) w;
                ww.link(next);
//                AbstractTaskLink tl = AtomicTaskLink.link(next.term());
//                tl.priSet(BELIEF, nextPri);
////        tl.priSet(BELIEF, surprise*2f/3f);
//////        tl.priSet(GOAL, surprise/3);
////        tl.priSet(QUEST, surprise*1f/3f);
//                ww.links.link(tl);
            }

            w.emit(next);



    }

    /**
     * used to determine the priority factor of new signal tasks,
     * with respect to how significantly it changed from a previous value,
     * OR the time duration since last received signal
     */
    static double surprise(Task prev, Task next, float dur) {

        boolean NEW = prev == null;
        if (NEW)
            return 1;

        boolean stretched = prev == next;
        if (stretched)
            return 0;

        float deltaFreq = Math.abs(prev.freq() - next.freq()); //TODO use a moving average or other anomaly/surprise detection

        long sepCycles  = Math.abs(next.start() - prev.end());
        double deltaTime = 1 - NAL.evi(1, sepCycles, dur);

        return Util.or(deltaFreq , deltaTime);
    }


    /** surPRIse */
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
    private static final class MyRTreeBeliefTable extends RTreeBeliefTable {

//        @Override protected FloatRank<Task> taskStrength(boolean beliefOrGoal, long now, float narDur, int tableDur) {
//            FloatRank<Task> base = super.taskStrength(beliefOrGoal, now, narDur, tableDur);
//
//
//            long _ss = series.start();
//            if (_ss != TIMELESS) {
//                long _se = series.end();
//                if (_se!=TIMELESS) {
//                    float margin = narDur;
//                    long ss = Math.round(_ss + margin);
//                    long se = Math.round(_se - margin);
//                    return (t, min) -> {
//                        float v = base.rank(t, min);
//                        if (v == v && v > min) {
//                            long ts = t.start();
//                            long te = t.end();
//                            long l = LongInterval.intersectLength(ts, te, ss, se);
//                            if (l > 0) {
//                                //discount the rank in proportion to how much of the task overlaps with the series
//                                double range = (te-ts)+1;
//                                float overlap = (float) Util.unitizeSafe(l / range);
//                                float keep =
//                                        //1 - overlap;
//                                        1 - (overlap*overlap); //less intense but still in effect
//                                v *= keep;
//                            }
//                            return v;
//                        }
//                        return Float.NaN;
//                    };
//                }
//            }
//
//            return base;
//
//        }
    }
}
