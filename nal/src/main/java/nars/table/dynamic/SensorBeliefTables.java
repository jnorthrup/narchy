package nars.table.dynamic;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.math.Longerval;
import jcog.pri.ScalarValue;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.op.Remember;
import nars.index.concept.AbstractConceptIndex;
import nars.link.TaskLink;
import nars.table.BeliefTables;
import nars.table.temporal.RTreeBeliefTable;
import nars.task.util.series.AbstractTaskSeries;
import nars.task.util.series.RingBufferTaskSeries;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import static java.lang.Float.NaN;
import static nars.time.Tense.TIMELESS;

/**
 * special belief tables implementation
 * dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 *
 */
public class SensorBeliefTables extends BeliefTables {

    public final MySensorBeliefTable series;

    public FloatRange res;
    private FloatSupplier pri;

    public SensorBeliefTables(Term c, boolean beliefOrGoal) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                //new ConcurrentSkiplistTaskSeries<>( /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE)
                new RingBufferTaskSeries<>( /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE));
    }

    SensorBeliefTables(Term term, boolean beliefOrGoal, AbstractTaskSeries<SeriesBeliefTable.SeriesTask> s) {
        super(new MySensorBeliefTable(term, beliefOrGoal, s));

        this.series = tableFirst(MySensorBeliefTable.class);

        tables.add(new MyRTreeBeliefTable());
    }

    @Override
    public void add(Remember r, NAR n) {

        if (r.input instanceof SeriesBeliefTable.SeriesTask) {
            r.input = null;
            r.done = true;
            return;
        }

        super.add(r, n);
    }



    public void add(Truth value, long start, long end, FloatSupplier pri, short cause, NAR n) {


        if (value!=null) {
            value = value.dither(
                    Math.max(n.freqResolution.asFloat(), res.asFloat()),
                    n.confResolution.floatValue()
            );
        }

        SeriesBeliefTable.SeriesTask x = add(value,
                start, end,
                series.term, series.punc(),
                n);


        if (x!=null) {
            series.clean(tables);
            x.cause(new short[] { cause });
            remember(x, pri, n);
        } else {
            this.prev = null;
        }
    }




    long[] eviShared = null;



    private SeriesBeliefTable.SeriesTask add(@Nullable Truth next, long nextStart, long nextEnd, Term term, byte punc, NAR nar) {

        SeriesBeliefTable.SeriesTask nextT = null, last = series.series.last();
        if (last != null) {
            long lastStart = last.start(), lastEnd = last.end();
            if (lastEnd >= nextStart)
                return null; //too soon, does this happen?

            int dur = nar.dur();

            long gapCycles = (nextStart - lastEnd);
            if (gapCycles <= series.series.latchDurs() * dur) {

                if (next!=null) {
                    long stretchCycles = (nextEnd - lastStart);
                    if (stretchCycles <= series.series.stretchDurs() * dur) {
                        Truth lastEnds = last.truth(lastEnd, 0);
                        if (lastEnds!=null && lastEnds.equals(next)) {
                            //stretch
                            end(last, nextEnd, nar);
                            return last;
                        }
                    }
                }

                //form new task either because the value changed, or because the latch duration was exceeded

                if (next == null) {
                    //guess that the signal stopped midway between (starting) now and the end of the last
                    long midGap = Math.min(nextStart-1, lastEnd + dur/2);
                    end(last, midGap, nar);
                } else {
                    //stretch the previous to the current starting point for the new task
                    end(last, nextStart-1, nar);
                }

            }
        }

        if (next != null) {
            nextT = newTask(term, punc, nextStart, nextEnd, next, nar);
            if (nextT != null)
                series.add(nextT);

        }

        return nextT;

    }

    protected SeriesBeliefTable.SeriesTask newTask(Term term, byte punc, long s, long e, Truth next, NAR nar) {
        long[] evi;

        if (Param.ALLOW_REVISION_OVERLAP_IF_DISJOINT_TIME) {
            if (eviShared == null)
                eviShared = nar.evidence();
            evi = eviShared;
        } else {
            evi = nar.evidence(); //unique
        }

        if (Param.SIGNAL_TASK_OCC_DITHER) {
            int dither = nar.dtDither();
            s = Tense.dither(s, dither);
            e = Tense.dither(e, dither);
        }

        return new SeriesBeliefTable.SeriesTask(term, punc, next, s, e, evi);
    }

    static private void end(SeriesBeliefTable.SeriesTask s, long e, NAR nar) {
        if (Param.SIGNAL_TASK_OCC_DITHER) {
            e = Tense.dither(e, nar);
        }
        s.setEnd(e);
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


    /** priority of tasklink applied to a new or stretched existing sensor task */
    private float surprise(Task prev, Task next, NAR n) {




        float p = pri.asFloat();
        if (p != p)
            return NaN;

        boolean NEW = prev==null;

        boolean stretched = !NEW && prev==next;

        boolean latched = !NEW && !stretched &&
                Math.abs(next.start() - prev.end()) < Param.SIGNAL_LATCH_LIMIT_DURS * n.dur();

        //decrease tasklink priority if the same task or if similar truth

        if (prev==null || (!stretched && !latched)) {
            return p; //assume novel
        } else {

            float deltaFreq = prev!=next? Math.abs(prev.freq() - next.freq()) : 0; //TODO use a moving average or other anomaly/surprise detection

            return p * Util.lerp(deltaFreq, Param.SIGNAL_UNSURPRISING_FACTOR, 1);
        }


    }

    private void remember(Task next, FloatSupplier pri, NAR n) {
        //if (y==prev)

        Task prev = this.prev;
        this.prev = next;
        this.pri = pri;

        if (next == null)
            return; //?

        //float before = series.tasklink.priElseZero();
        float p = surprise(prev, next, n);
        if (p!=p)
            return;

        next.priMax(p); //set the task's pri too

        float delta = series.tasklink.priMax(next.punc(), p);

        TaskLink.link(series.tasklink, n);

        if (delta > ScalarValue.EPSILON)
            ((AbstractConceptIndex)n.concepts).active.bag.pressurize(delta); //HACK

        if (prev!=next)
            n.eventTask.emit(next);

    }

    private static final class MySensorBeliefTable extends SeriesBeliefTable<SeriesBeliefTable.SeriesTask> {

        private MySensorBeliefTable(Term c, boolean beliefOrGoal, AbstractTaskSeries<SeriesBeliefTable.SeriesTask> s) {
            super(c, beliefOrGoal, s);
        }

        @Override
        public void add(Remember r, NAR n) {

            Task x = r.input;

            if (x.isEternal() || x instanceof SeriesTask)
                return; //already owned, or was owned

            if (Param.SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS) {
                if (absorbNonSignal(x, start(), end())) {
                    r.reject();
                }
            }

        }
    }

    /**
     * adjusted compression task value to exclude regions where the series belief table is defined.
     * allows regions outside of this more importance (ex: future)
     */
    private final class MyRTreeBeliefTable extends RTreeBeliefTable {

        @Override protected FloatFunction<Task> taskSurviveValue(boolean beliefOrGoal, int dur, long now) {
            FloatFunction<Task> base = super.taskSurviveValue(beliefOrGoal, dur, now);
            return (t) -> {
                float v = base.floatValueOf(t);
                if (v == v) {
                    long ss = series.start(), se = series.end();
                    if (ss!=TIMELESS && se!=TIMELESS) {
                        long l = Longerval.intersectLength(t.start(), t.end(), ss, se);
                        if (l > 0) {
                            //discount the rank in proportion to how much of the task overlaps with the series
                            float overlap = Util.unitizeSafe(l / ((float) t.range()));
                            v *= (1-overlap);
                        }
                    }
                }
                return v;
            };
        }
    }
}
