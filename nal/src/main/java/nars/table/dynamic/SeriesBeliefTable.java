package nars.table.dynamic;

import jcog.data.list.FasterList;
import jcog.math.Longerval;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.control.proto.Remember;
import nars.table.BeliefTable;
import nars.table.TaskTable;
import nars.table.eternal.EternalTable;
import nars.task.TaskProxy;
import nars.task.proxy.SpecialOccurrenceTask;
import nars.task.signal.SignalTask;
import nars.task.util.Answer;
import nars.task.util.TaskRegion;
import nars.task.util.series.TaskSeries;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.dynamic.DynTruth;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.time.Tense.ETERNAL;

/**
 * adds a TaskSeries additional Task buffer which can be evaluated from, or not depending
 * if a stored task is available or not.
 */
public class SeriesBeliefTable extends DynamicTaskTable {

    public final TaskSeries<SeriesTask> series;

    public SeriesBeliefTable(Term c, boolean beliefOrGoal, TaskSeries<SeriesTask> s) {
        super(c, beliefOrGoal);
        this.series = s;
    }

//    @Override
//    public void match(Answer t) {
//        series.forEach(t.time.start, t.time.end, false, t);
//    }

    @Override
    protected Task taskDynamic(Answer a) {
        return (Task) (eval(true, a.time.start, a.time.end, a.filter, a.nar));
    }

    @Override
    public int size() {
        return series.size();
    }

    protected Truthed eval(boolean taskOrJustTruth, long start, long end, @Nullable Predicate<Task> filter, NAR nar) {
        int dur = nar.dur();
        DynTruth d = series.truth(start, end, dur, filter, nar);
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
                    start = s;
                    end = d.end();
                } else {
                    start = end = ETERNAL;
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
        series.clear();
    }

    @Override
    public Stream<? extends Task> streamTasks() {
        return series.stream();
    }

    @Override
    public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
        series.forEach(minT, maxT, true, x);
    }

    @Override
    public void forEachTask(Consumer<? super Task> action) {
        series.forEach(action);
    }



    @Override
    protected Truth truthDynamic(long start, long end, Term templateIgnored, Predicate filter, NAR nar) {
        return (Truth) (eval(false, start, end, filter, nar));
    }


    @Override
    public void add(Remember r, NAR n) {

        Task x = r.input;

        if (x.isEternal() || x instanceof SeriesTask)
            return; //already owned, or was owned

        if (Param.FILTER_SIGNAL_TABLE_TEMPORAL_TASKS) {
            Task y = absorbNonSignal(x, cleanMargin(n));
            if (y == null) {
                r.reject();
                return;
            } else if (y!=x) {
                r.input = y;
            }
        }

    }

    /** time margin to shrink the series end length allowing tasks to survive in the "present" before being cleaned */
    protected int cleanMargin(NAR n) {
        return n.dur();
    }

    public void clean(NAR nar, List<BeliefTable> tables) {
        if (!Param.FILTER_SIGNAL_TABLE_TEMPORAL_TASKS)
            return;

        if (!series.isEmpty()) {
                long sStart = series.start(), sEnd = series.end() - cleanMargin(nar);

                List<Task> deleteAfter = new FasterList(4);
                for (TaskTable b : tables) {
                    if (!(b instanceof DynamicTaskTable) && !(b instanceof EternalTable)) {
                        b.forEachTask(sStart, sEnd, t -> {
                            if (t.isDeleted() || absorbNonSignal(t, sStart, sEnd)) {
                                deleteAfter.add(t);
                            } else {
                                //System.out.println(t + " saved");
                            }
                        });
                    }
                    if (!deleteAfter.isEmpty()) {
                        deleteAfter.forEach(b::removeTask);
                        deleteAfter.clear();
                    }
                }

        }
    }

    @Nullable Task absorbNonSignal(Task t, int dur) {
        if (t.isEternal())
            return t; //no change

        long seriesEnd = series.end();
        if (t.end() > seriesEnd) {
            return new SpecialOccurrenceTask(t, seriesEnd, t.end() );
        }

        //similar for before the beginning

        if (!series.isEmpty() && absorbNonSignal(t, series.start(), seriesEnd - dur))
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

        /** current endpoint */
        long e;

        public SeriesTask(Term term, byte punc, Truth value, long start, long end, long[] stamp) {
            super(term, punc, value, start, start, end, stamp);
            if (stamp.length != 1)
                throw new UnsupportedOperationException("requires stamp of length 1 so it can be considered an Input Task and thus have consistent hashing even while its occurrrence time is stretched");
            this.e = end;
        }

        public void setEnd(long e) {
            this.e = e;
        }

        @Override
        public long end() {
            return e;
        }

        //        @Override
//        public ITask inputSubTask(Task ignored, NAR n) {
//            throw new UnsupportedOperationException("use input(concept) for internal storage procedure");
//        }

        /**
         * passive insertion subtask only
         */
        @Deprecated public SeriesRemember input(TaskConcept concept) {
            //return new TaskLinkTaskAndEmit(this, priElseZero(), concept);
            return new SeriesRemember(this, concept);
        }

    }

    public static class SeriesRemember extends Remember {

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
