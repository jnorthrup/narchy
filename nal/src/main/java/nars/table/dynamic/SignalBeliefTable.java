package nars.table.dynamic;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.control.proto.TaskLinkTask;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.concept.ConceptBuilder;
import nars.util.task.series.ConcurrentSkiplistTaskSeries;
import nars.util.task.series.TaskSeries;

import java.util.concurrent.ConcurrentSkipListMap;

/**
 * dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 */
public class SignalBeliefTable extends SeriesBeliefTable<SeriesBeliefTable.SeriesTask> {

    /**
     * prioritizes generated tasks
     */
    private FloatRange pri;

    private FloatRange res;

    public SignalBeliefTable(Term term, boolean beliefOrGoal, ConceptBuilder b) {
        this(term, beliefOrGoal, b.newTemporalTable(term));
    }

    public SignalBeliefTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                t, new ConcurrentSkiplistTaskSeries<>(new ConcurrentSkipListMap<>(), /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE) {
                    @Override
                    protected SeriesTask newTask(Term term, byte punc, long nextStart, long nextEnd, Truth next, NAR nar, boolean removePrev, long[] lastStamp) {
                        nextEnd = Math.max(nextStart, nextEnd); //HACK
                        SeriesTask nextT = new SeriesTask(
                                term,
                                punc,
                                next,
                                nextStart, nextEnd,
                                removePrev ? lastStamp : nar.evidence());
                        return nextT;
                    }
                });
    }

    SignalBeliefTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t, TaskSeries<SeriesTask> series) {
        super(c, beliefOrGoal, EternalTable.EMPTY, t, series);
    }

    public TaskLinkTask add(Truth value, long start, long end, Concept c, NAR nar) {

        clean(nar);
        if (value == null)
            return null;

        value = value.ditherFreq(Math.max(nar.freqResolution.asFloat(), res.asFloat()));
        if (value==null)
            return null;

        SeriesTask x = series.add(term, punc(), start, end,
                value, nar.dur(), nar);

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


    @Override
    protected boolean dynamicOverrides() {
        return true;
    }

}
