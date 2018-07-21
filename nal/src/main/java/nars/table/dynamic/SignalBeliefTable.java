package nars.table.dynamic;

import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.concept.ConceptBuilder;
import nars.util.task.series.ConcurrentSkiplistTaskSeries;
import nars.util.task.series.TaskSeries;

import java.util.List;
import java.util.NoSuchElementException;
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

    public FloatRange pri() {
        return pri;
    }

    public FloatRange resolution() {
        return res;
    }

    public SignalBeliefTable(Term term, boolean beliefOrGoal, ConceptBuilder b) {
        this(term, beliefOrGoal, b.newTemporalTable(term));
    }

    public SignalBeliefTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                t, new ConcurrentSkiplistTaskSeries<>(new ConcurrentSkipListMap<>(), /*@Deprecated*/ Param.SIGNAL_BELIEF_TABLE_SERIES_SIZE) {
                    @Override
                    public SeriesTask newTask(Term term, byte punc, long nextStart, long nextEnd, Truth next, NAR nar, boolean removePrev, long[] lastStamp) {
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

    public SignalTask add(Truth value, long start, long end, Concept c, NAR nar) {

        value = value.ditherFreq(Math.max(nar.freqResolution.asFloat(), res.asFloat()));
        SeriesTask x = series.add(term, punc(), start, end,
                value, nar.dur(), nar);

        if (x!=null) {
            x.pri(pri.asFloat());
            x.concept = c;
        }

        clean(nar);

        return x;
    }

    public void setPri(FloatRange pri) {
        this.pri = pri;
    }
    public void resolution(FloatRange res) {
        this.res = res;
    }

    public void clean(NAR nar) {
        if (!series.isEmpty()) {
            try {
                long sstart = series.start();
                long send = series.end();


                List<Task> deleteAfter = new FasterList(4);
                temporal.whileEach(sstart, send, t -> {
                    if (t.end() < send) { 
                        deleteAfter.add(t);
                    }
                    return true;
                });
                deleteAfter.forEach(temporal::removeTask);
            } catch (NoSuchElementException e) {
                
            }
        }
    }

    @Override
    protected boolean dynamicOverrides() {
        return true;
    }

}
