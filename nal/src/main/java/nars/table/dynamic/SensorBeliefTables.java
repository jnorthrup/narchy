package nars.table.dynamic;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.concept.util.ConceptBuilder;
import nars.control.op.Remember;
import nars.link.TaskLink;
import nars.table.BeliefTables;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.task.util.series.AbstractTaskSeries;
import nars.task.util.series.RingBufferTaskSeries;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * special belief tables implementation
 * dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 */
public class SensorBeliefTables extends BeliefTables {

    public final SensorBeliefTable series;

    public FloatRange res;

    public SensorBeliefTables(Term term, boolean beliefOrGoal, ConceptBuilder b) {
        this(term.normalize(), beliefOrGoal, b.newTemporalTable(term.normalize(), beliefOrGoal));
    }

    SensorBeliefTables(Term c, boolean beliefOrGoal, TemporalBeliefTable t) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                t,
                //new ConcurrentSkiplistTaskSeries<>( /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE)
                new RingBufferTaskSeries<>( /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE));
    }

    SensorBeliefTables(Term term, boolean beliefOrGoal, TemporalBeliefTable t, AbstractTaskSeries<SeriesBeliefTable.SeriesTask> s) {
        super(new SensorBeliefTable(term, beliefOrGoal, s), t);
        this.series = tableFirst(SensorBeliefTable.class);
    }

    @Override
    public void add(Remember r, NAR n) {

        if (r.input instanceof SeriesBeliefTable.SeriesTask) {
            r.input = null;
            return;
        }

        super.add(r, n);
    }



    public SeriesBeliefTable.SeriesRemember add(Truth value, long start, long end, TaskConcept c, NAR n) {


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

        series.clean(tables);

        if (x!=null) {
            SeriesBeliefTable.SeriesRemember y = x.input(c);
            y.remember(remember(y));
            return y;
        }

        return null;
    }




    long[] eviShared = null;

    protected SeriesBeliefTable.SeriesTask newTask(Term term, byte punc, long nextStart, long nextEnd, Truth next, NAR nar) {
        long[] evi;

        if (Param.ALLOW_REVISION_OVERLAP_IF_DISJOINT_TIME) {
            if (eviShared == null)
                eviShared = nar.evidence();
            evi = eviShared;
        } else {
            evi = nar.evidence(); //unique
        }


        return new SeriesBeliefTable.SeriesTask(
                term,
                punc,
                next,
                nextStart, nextEnd,
                evi);
    }


    private SeriesBeliefTable.SeriesTask add(@Nullable Truth next, long nextStart, long nextEnd, Term term, byte punc, NAR nar) {

        SeriesBeliefTable.SeriesTask nextT = null, last = series.series.last();
        if (last != null) {
            long lastStart = last.start(), lastEnd = last.end();
            if (lastEnd >= nextStart)
                return null; //too soon, does this happen?

            int dur = nar.dur();

            double gapDurs = ((double)(nextStart - lastEnd)) / dur;
            if (gapDurs <= series.series.latchDurs()) {

                if (next!=null) {
                    double stretchDurs = ((double) (nextEnd - lastStart)) / dur;
                    if ((stretchDurs <= series.series.stretchDurs())) {
                        Truth lastEnds = last.truth(lastEnd, 0);
                        if (lastEnds!=null && lastEnds.equals(next)) {
                            //stretch
                            last.setEnd(nextEnd);
                            return last;
                        }
                    }
                }

                //form new task either because the value changed, or because the latch duration was exceeded

                if (next == null) {
                    //guess that the signal stopped midway between (starting) now and the end of the last
                    long midGap = Math.min(nextStart-1, lastEnd + dur/2);
                    last.setEnd(midGap);
                } else {
                    //stretch the previous to the current starting point for the new task
                    last.setEnd(nextStart-1);
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



    private Task prev = null, next = null;

    private final AbstractTask myTaskLink = new AbstractTask() {

        final static float SIGNAL_PRI_FACTOR_SAME_TASK_BEING_STRETCHED = 0.5f;

        @Override
        public ITask next(NAR n) {
            Task next = SensorBeliefTables.this.next, prev = SensorBeliefTables.this.prev;
            if (next == null)
                return null; //?

            //decrease tasklink priority if the same task or if similar truth

            float p = next.priElseZero();
            if (p == p) {
                if (prev!=null && (prev==next || Math.abs(next.start()-prev.end()) < Param.SIGNAL_LATCH_DUR * n.dur())) {

                    if (prev == next || prev.truth().equalsIn(next.truth(), n))
                        p *= SIGNAL_PRI_FACTOR_SAME_TASK_BEING_STRETCHED;
                    //TODO else { ... //difference in truth: surprisingness
                }
                series.tasklink.pri(p);
                TaskLink.link(series.tasklink, n);
            }
            return null;
        }
    };
    private ITask remember(SeriesBeliefTable.SeriesRemember y) {
        //if (y==prev)
        prev = next;
        next = y.input;

        return myTaskLink;
    }
}
