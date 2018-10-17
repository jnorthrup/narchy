package nars.table.dynamic;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Param;
import nars.concept.TaskConcept;
import nars.concept.util.ConceptBuilder;
import nars.control.proto.Remember;
import nars.table.BeliefTables;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.util.series.AbstractTaskSeries;
import nars.task.util.series.ConcurrentRingBufferTaskSeries;
import nars.term.Term;
import nars.truth.Truth;

/**
 * special belief tables implementation
 * dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 */
public class SensorBeliefTables extends BeliefTables {

    public final SeriesBeliefTable series;

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
                //new ConcurrentSkiplistTaskSeries( /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE)
                new ConcurrentRingBufferTaskSeries( /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE)
                {

                    @Override
                    protected SeriesBeliefTable.SeriesTask newTask(Term term, byte punc, long nextStart, long nextEnd, Truth next, NAR nar) {
                        nextEnd = Math.max(nextStart, nextEnd); //HACK
                        SeriesBeliefTable.SeriesTask nextT = new SeriesBeliefTable.SeriesTask(
                                term,
                                punc,
                                next,
                                nextStart, nextEnd,
                                nar.evidence());
                        return nextT;
                    }
                });
    }

    SensorBeliefTables(Term term, boolean beliefOrGoal, TemporalBeliefTable t, AbstractTaskSeries s) {
        super(new SeriesBeliefTable(term, beliefOrGoal, s), t);
        this.series = tableFirst(SeriesBeliefTable.class);
    }

    @Override
    public void add(Remember r, NAR n) {

        if (r.input instanceof SeriesBeliefTable.SeriesTask) {
//            if (Param.DEBUG)
//                throw new WTF();
            r.input = null;
            return;
        }

        super.add(r, n);
    }


    public SeriesBeliefTable.SeriesRemember add(Truth value, long start, long end, TaskConcept c, float dur, NAR n) {


        if (value!=null) {
            value = value.ditherFreq(Math.max(n.freqResolution.asFloat(), res.asFloat()));
        }

        SeriesBeliefTable.SeriesTask x = series.series.add(value,
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
