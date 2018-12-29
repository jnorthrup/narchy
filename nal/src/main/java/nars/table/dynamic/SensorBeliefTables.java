package nars.table.dynamic;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.concept.util.ConceptBuilder;
import nars.control.proto.Remember;
import nars.link.TaskLink;
import nars.table.BeliefTables;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.task.util.Answer;
import nars.task.util.series.AbstractTaskSeries;
import nars.task.util.series.RingBufferTaskSeries;
import nars.term.Term;
import nars.time.Tense;
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


        float fRes = Math.max(n.freqResolution.asFloat(), res.asFloat());
        if (value!=null) {
            value = value.ditherFreq(fRes);
        }

        SeriesBeliefTable.SeriesTask x = add(value,
                start, end,
                series.term, series.punc(),
                n);

        series.clean(tables);

        if (x!=null) {
            SeriesBeliefTable.SeriesRemember y = x.input(c);
            y.remember(new MyTaskLink(c));
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

        SeriesBeliefTable.SeriesTask nextT = null;
        SeriesBeliefTable.SeriesTask last = series.series.last();
        boolean stretchPrev = false;
        if (last != null) {
            long lastStart = last.start(), lastEnd = last.end();
            if (lastEnd > nextStart)
                return null;

            long dur = nextEnd - nextStart;

            double gapDurs = ((double)(nextStart - lastEnd)) / dur;
            if (gapDurs <= series.series.latchDurs()) {

                if (next!=null) {
                    double stretchDurs = ((double) (nextEnd - lastStart)) / dur;
                    boolean stretchable = (stretchDurs <= series.series.stretchDurs());
                    if (stretchable) {
                        Truth lastEnds = last.truth(lastEnd, 0);
                        if (lastEnds.equals(next)) {
                            //stretch
                            last.setEnd(nextEnd);
                            return last;
                        }
                    }
                }

                //form new task either because the value changed, or because the latch duration was exceeded
                long midGap = Tense.dither(Math.max(lastEnd, (lastEnd + nextStart)/2L), nar);
                //assert(midGap >= lastEnd): lastEnd + " " + midGap + ' ' + nextStart;
                last.setEnd(Math.max(last.end(), midGap-1));

                if (next == null) {
                    return last;
                }

                nextStart = Math.min(last.end() + 1, nextStart);
                nextEnd = Math.max(nextStart, nextEnd);
                stretchPrev = false;

            } else {

                stretchPrev = false;
                nextStart = Math.max(nextStart, lastEnd/* +1 */);
                nextEnd = Math.max(nextEnd, nextStart);

                //form new task at the specified interval, regardless of the previous task since it was excessively long ago
                //TODO maybe grow the previous task half a gap duration
            }

        }

        //assert(nextStart <= nextEnd);

        if (!stretchPrev && next != null) {
            nextT = newTask(term, punc, nextStart, nextEnd, next, nar);
            if (nextT != null)
                series.add(nextT);
        }

        return nextT;

    }



    @Override
    public void match(Answer a) {
        if (series.series.contains(a.time)) {
            //try to allow the series to be the only authority in reporting
            series.match(a);
            if (a.tasks.isEmpty()) {
                //if nothing was found, then search other tables
                tables.each(t -> {
                    if (t!=series)
                        t.match(a);
                });
            }
        } else {
            super.match(a);
        }
    }


    public FloatRange resolution() {
        return res;
    }

    public void resolution(FloatRange res) {
        this.res = res;
    }

    private class MyTaskLink extends AbstractTask {
        private final TaskConcept c;

        Task lastActivated = null;
        public MyTaskLink(TaskConcept c) {
            this.c = c;
        }

        @Override
        public ITask next(NAR n) {
            Task lastTask = series.series.last();
            if (lastTask!=null) {
                float p = lastTask.pri();
                if (p == p) {
                    series.tasklink.pri(p);

                    TaskLink.link(series.tasklink, c);

                    if (lastActivated!=lastTask) {
                        n.activate(c, p);
                        lastActivated = lastTask;
                    } //else: reactivate?
                }
            }
            return null;
        }
    }
}
