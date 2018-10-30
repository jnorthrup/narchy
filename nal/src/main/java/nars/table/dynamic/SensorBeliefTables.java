package nars.table.dynamic;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Param;
import nars.concept.TaskConcept;
import nars.concept.util.ConceptBuilder;
import nars.control.proto.Remember;
import nars.table.BeliefTables;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.util.Answer;
import nars.task.util.series.AbstractTaskSeries;
import nars.task.util.series.ConcurrentRingBufferTaskSeries;
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

    /**
     * prioritizes generated tasks
     */
    private FloatRange pri;

    public FloatRange res;

    public SensorBeliefTables(Term term, boolean beliefOrGoal, ConceptBuilder b) {
        this(term, beliefOrGoal, b.newTemporalTable(term, beliefOrGoal));
    }

    public SensorBeliefTables(Term c, boolean beliefOrGoal, TemporalBeliefTable t) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                t,
                //new ConcurrentSkiplistTaskSeries<>( /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE)
                new ConcurrentRingBufferTaskSeries<>( /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE));
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


    public SeriesBeliefTable.SeriesRemember add(Truth value, long start, long end, TaskConcept c, float dur, NAR n) {


        if (value!=null) {
            value = value.ditherFreq(Math.max(n.freqResolution.asFloat(), res.asFloat()));
        }

        SeriesBeliefTable.SeriesTask x = add(value,
                start, end, dur,
                series.term, series.punc(),
                n);

        series.clean(tables);

        if (x!=null) {
            x.pri(pri.asFloat());
            return x.input(c);
        }

        return null;
    }



    long[] eviShared = null;

    protected SeriesBeliefTable.SeriesTask newTask(Term term, byte punc, long nextStart, long nextEnd, Truth next, NAR nar) {

        if (eviShared == null)
            eviShared = nar.evidence();
        long[] evi = eviShared;

        //long[] evi = nar.evidence();

        return new SeriesBeliefTable.SeriesTask(
                term,
                punc,
                next,
                nextStart, nextEnd,
                evi);
    }


    public SeriesBeliefTable.SeriesTask add(@Nullable Truth next, long nextStart, long nextEnd, float dur, Term term, byte punc, NAR nar) {

        SeriesBeliefTable.SeriesTask nextT = null;
        SeriesBeliefTable.SeriesTask last = series.series.last();
        boolean stretchPrev = false;
        if (last != null) {
            long lastStart = last.start(), lastEnd = last.end();
            if (lastEnd > nextStart)
                return null;


            double gapDurs = ((double)(nextStart - lastEnd)) / dur;
            if (gapDurs <= series.series.stretchDurs()) {

                if (next!=null) {
                    double stretchDurs = ((double) (nextEnd - lastStart)) / dur;
                    if (stretchDurs <= series.series.latchDur()) {
                        Truth lastEnds = last.truth(lastEnd, 0);
                        if (lastEnds.equals(next)) {
                            //stretch
                            last.setEnd(nextEnd);
                            return last;
                        }
                    }
                }

                //form new task either because the value changed, or because the latch duration was exceeded
                long midGap = Math.max(lastEnd, (lastEnd + nextStart)/2L);
                assert(midGap >= lastEnd): lastEnd + " " + midGap + ' ' + nextStart;
                last.setEnd(midGap);
                if (next == null) {
                    return last; //TODO check right time
                }

                nextStart = midGap+1; //Tense.dither(midGap, nar);
                //midGap+1; //start the new task directly after the midpoint between its start and the end of the last task
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
            if (nextT == null)
                return null;

            synchronized (this) {

                series.series.compress();

                series.series.push(nextT);

            }
        }

        return nextT;

    }



    @Override
    public void match(Answer r) {
        if (series.series.contains(r.time)) {
            //try to allow the series to be the only authority in reporting
            series.match(r);
            if (r.tasks.isEmpty()) {
                //if nothing was found, then search other tables
                tables.each(t -> {
                    if (t!=series)
                        t.match(r);
                });
            }
        } else {
            super.match(r);
        }
    }

//    @Override
//    public void sample(Answer a) {
//        match(a); //same preference as match
//    }

    public FloatRange pri() {
        return pri;
    }

    public FloatRange resolution() {
        return res;
    }

    public void setPri(FloatRange pri) {
        this.pri = pri;
    }
    public void resolution(FloatRange res) {
        this.res = res;
    }

}
