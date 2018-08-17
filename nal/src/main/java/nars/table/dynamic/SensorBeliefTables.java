package nars.table.dynamic;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Param;
import nars.concept.TaskConcept;
import nars.concept.util.ConceptBuilder;
import nars.table.BeliefTables;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.util.series.ConcurrentSkiplistTaskSeries;
import nars.task.util.series.TaskSeries;
import nars.term.Term;
import nars.truth.Truth;

import java.util.concurrent.ConcurrentSkipListMap;

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
        this(term, beliefOrGoal, b.newTemporalTable(term));
    }

    public SensorBeliefTables(Term c, boolean beliefOrGoal, TemporalBeliefTable t) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                t, new ConcurrentSkiplistTaskSeries<>(new ConcurrentSkipListMap<>(), /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE) {
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

    SensorBeliefTables(Term term, boolean beliefOrGoal, TemporalBeliefTable t, TaskSeries s) {
        super(t, new SeriesBeliefTable(term, beliefOrGoal, s));
        this.series = tableFirst(SeriesBeliefTable.class);
    }

    public SeriesBeliefTable.SeriesRemember add(Truth value, long start, long end, TaskConcept c, NAR n) {


        if (value==null)
            return null;

        value = value.ditherFreq(Math.max(n.freqResolution.asFloat(), res.asFloat()));

        SeriesBeliefTable.SeriesTask x = series.series.add(value,
                start, end, n.dur(),
                series.term, series.punc(),
                n);

        series.clean(n, tables);

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
