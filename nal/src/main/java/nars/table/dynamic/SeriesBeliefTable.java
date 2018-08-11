package nars.table.dynamic;

import jcog.data.list.FasterList;
import jcog.math.Longerval;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.control.proto.Remember;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.ITask;
import nars.task.TaskProxy;
import nars.task.proxy.SpecialOccurrenceTask;
import nars.task.signal.SignalTask;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.dynamic.DynTruth;
import nars.util.task.series.TaskSeries;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.time.Tense.ETERNAL;

/**
 * adds a TaskSeries additional Task buffer which can be evaluated from, or not depending
 * if a stored task is available or not.
 */
public abstract class SeriesBeliefTable<T extends Task> extends DynamicBeliefTable {

    public final TaskSeries<T> series;

    public SeriesBeliefTable(Term c, boolean beliefOrGoal, EternalTable e, TemporalBeliefTable t, TaskSeries<T> s) {
        super(c, beliefOrGoal, e, t);
        this.series = s;
    }

    @Override
    public int size() {
        return super.size() + series.size();
    }

    protected Truthed eval(boolean taskOrJustTruth, long start, long end, NAR nar) {
        int dur = nar.dur();
        DynTruth d = series.truth(start, end, dur, nar);
        if (d == null || d.isEmpty())
            return null;

        if (taskOrJustTruth) {
            if (d.size() == 1) {
                TaskRegion d0 = d.get(0);
                if (d0 instanceof TaskProxy) {
                    d0 = ((TaskProxy) d0).clone();
                }
                return (Truthed) d0; //only task
            } else {
                //adjust the start, end time to match the tasks found
                long s = d.start();
                if (s != ETERNAL) {
                    long e = d.end();
                    start = s;
                    end = e;
                }
            }
        }

        Truth pp = Param.truth(start, end, dur).add((Collection) d).filter().truth();
        if (pp == null)
            return null;

//        float freqRes = taskOrJustTruth ? Math.max(nar.freqResolution.floatValue(), res.asFloat()) : 0;
//        float confRes = taskOrJustTruth ? nar.confResolution.floatValue() : 0;
//        float eviMin = taskOrJustTruth ? w2cSafe(nar.confMin.floatValue()) : Float.MIN_NORMAL;
        return d.eval(term, (dd, n) -> pp, taskOrJustTruth, beliefOrGoal, nar);
    }

    @Override
    public void clear() {
        super.clear();
        series.clear();
    }

    @Override
    public Stream<Task> streamTasks() {
        return Stream.concat(super.streamTasks(), series.stream());
    }

    @Override
    public void forEachTask(boolean includeEternal, long minT, long maxT, Consumer<? super Task> x) {
        super.forEachTask(includeEternal, minT, maxT, x);
        series.forEach(minT, maxT, true, x);
    }

    @Override
    public void forEachTask(Consumer<? super Task> action) {
        super.forEachTask(action);
        series.forEach(action);
    }

    @Override
    public Task taskDynamic(long start, long end, Term template, NAR nar) {
        return (Task) (eval(true, start, end, nar));
    }

    @Override
    protected @Nullable Truth truthDynamic(long start, long end, Term templateIgnored, NAR nar) {
        return (Truth) (eval(false, start, end, nar));
    }

    @Override
    public void sampleDynamic(long s, long e, Consumer<Task> c, NAR nar) {
        series.forEach(s, e, false, c);
    }


    @Override
    public void add(Remember r, NAR n) {

        Task x = r.input;
        assert(!(x instanceof SeriesTask));

        if (Param.FILTER_SIGNAL_TABLE_TEMPORAL_TASKS) {
            Task y = absorbNonSignal(x);
            if (y == null) {
                r.reject();
                return;
            } else if (y!=x) {
                r.input = y;
            }
        }

        super.add(r, n);
    }

    public void clean(NAR nar) {
        if (!Param.FILTER_SIGNAL_TABLE_TEMPORAL_TASKS)
            return;

        if (!series.isEmpty() && !temporal.isEmpty()) {
            try {
                long sStart = series.start(), sEnd = series.end();

                List<Task> deleteAfter = new FasterList(4);
                temporal.whileEach(sStart, sEnd, t -> {
                    if (t.isDeleted() || absorbNonSignal(t, sStart, sEnd)) {
                        deleteAfter.add(t);
                    } else {
                        //System.out.println(t + " saved");
                    }
                    return true;
                });
                deleteAfter.forEach(temporal::removeTask);
            } catch (NoSuchElementException e) {
                //just in case
            }
        }
    }

    @Nullable Task absorbNonSignal(Task t) {
        if (t.isEternal())
            return t; //no change

        long seriesEnd = series.end();
        if (t.end() > seriesEnd) {
            return new SpecialOccurrenceTask(t, seriesEnd, t.end() );
        }

        //similar for before the beginning

        if (!series.isEmpty() && absorbNonSignal(t, series.start(), seriesEnd))
            return null;

        return t;
    }

    /**
     * used for if you can cache seriesStart,seriesEnd for a batch of calls
     */
    boolean absorbNonSignal(Task t, long seriesStart, long seriesEnd) {

        /*if (!t.isInput())*/ {
            long tEnd = t.end();

                long tStart = t.start();
                if (Longerval.intersectLength(tStart, tEnd, seriesStart, seriesEnd) != -1) {
                    return !series.isEmpty(tStart, t.end());
                }
            //}
        }

        return false;
    }

    public static final class SeriesTask extends SignalTask {

        public SeriesTask(Term term, byte punc, Truth value, long start, long end, long[] stamp) {
            super(term, punc, value, start, start, end, stamp);
        }

//        @Override
//        public ITask inputSubTask(Task ignored, NAR n) {
//            throw new UnsupportedOperationException("use input(concept) for internal storage procedure");
//        }

        /**
         * passive insertion subtask only
         */
        @Deprecated public ITask input(TaskConcept concept) {
            //return new TaskLinkTaskAndEmit(this, priElseZero(), concept);
            return new SeriesRemember(this, concept);
        }

    }

    protected static class SeriesRemember extends Remember {

        public SeriesRemember(SeriesTask task, TaskConcept concept) {
            super(task, concept);
            remember(task);
        }

        @Override
        protected void input(NAR n) {
            //DONT. just go straight to postprocessing
        }
    }

}
