package nars.concept.dynamic;

import jcog.list.FasterList;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.util.ConceptBuilder;
import nars.link.Tasklinks;
import nars.table.TemporalBeliefTable;
import nars.task.ITask;
import nars.task.TruthPolation;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 */
public class ScalarBeliefTable extends DynamicBeliefTable {

    /**
     * prioritizes generated tasks
     */
    private FloatSupplier pri;

    private FloatSupplier res;



    interface TimeSeries {

        void add(long time, Task value);

        DynTruth truth(long start, long end, long dur, NAR nar);

        int size();

        void forEach(long minT, long maxT, Consumer<? super Task> x);

        void clear();

        Stream<Task> stream();
    }

    /**
     * naive implementation using a NavigableMap of indxed time points. not too smart since it cant represent mergeable flat ranges
     */
    static class DefaultTimeSeries implements TimeSeries {

        /**
         * tasks are indexed by their midpoint. since the series
         * is guaranteed to be ordered and non-overlapping, two entries
         * should not have the same mid-point even if they overlap
         * slightly.
         */
        final NavigableMap<Long, Task> at;
        private final int cap;
        final AtomicBoolean compressing = new AtomicBoolean();

        DefaultTimeSeries(NavigableMap<Long, Task> at, int cap) {
            this.at = at;
            this.cap = cap;
        }

        @Override
        public int size() {
            return at.size();
        }

        @Override
        public Stream<Task> stream() {
            return at.values().stream();
        }

        @Override
        public void clear() {
            at.clear();
        }

        @Override
        public void forEach(long minT, long maxT, Consumer<? super Task> x) {
            at.subMap(minT, maxT).values().forEach(x);
        }

        @Override
        public void add(long time, Task value) {

            at.put(time, value);

            compress();

        }

        void compress() {
            if (!compressing.compareAndSet(false, true))
                return;

            try {

                //TODO add better lossy merging etc
                while (at.size() > cap) {
                    at.remove(at.firstKey()).delete();
                }

            } finally {
                compressing.set(false);
            }
        }

        final static int SELECT_ALL_THRESHOLD = 3;
        final static int MAX_TASKS_TRUTHPOLATED = Param.STAMP_CAPACITY - 1;

        @Override
        public DynTruth truth(long start, long end, long dur, NAR nar) {

            int size = size();
            if (size == 0)
                return null;

            DynTruth d = new DynTruth(Math.min(size + 1 /* just in case extra appears while processing */, MAX_TASKS_TRUTHPOLATED));
            if (size <= SELECT_ALL_THRESHOLD) {
                at.values().forEach(d::add);
            } else {

                SortedMap<Long, Task> range = at.subMap(start, end);

                Collection<Task> inner = range.values();
                if (inner.size() < MAX_TASKS_TRUTHPOLATED) {
                    d.addAll(inner);
                } else {
                    //HACK sample random subset
                    FasterList<Task> all = new FasterList(inner);
                    int toRemove = all.size() - MAX_TASKS_TRUTHPOLATED;
                    Random rng = nar.random();
                    for (int i = 0; i < toRemove; i++) {
                        all.removeFast(rng.nextInt(all.size()));
                    }
                    d.addAll(all);
                }

                if (d.size() < MAX_TASKS_TRUTHPOLATED) {
                    Map.Entry<Long, Task> above = at.higherEntry(end);
                    if (above != null) d.add(above.getValue());

                    if (d.size() < MAX_TASKS_TRUTHPOLATED) {
                        Map.Entry<Long, Task> below = at.lowerEntry(start);
                        if (below != null) d.add(below.getValue());
                    }
                }
            }

            return d;
        }
    }

    final TimeSeries series;

    public ScalarBeliefTable(Term term, boolean beliefOrGoal, ConceptBuilder conceptBuilder) {
        this(term, beliefOrGoal, conceptBuilder.newTemporalTable(term));
    }

    public ScalarBeliefTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t) {
        this(c, beliefOrGoal,
                new DefaultTimeSeries(new ConcurrentSkipListMap<>()
                        , /*@Deprecated*/ 512),
                t);
    }

    ScalarBeliefTable(Term c, boolean beliefOrGoal, TimeSeries series, TemporalBeliefTable t) {
        super(c, beliefOrGoal, t);
        this.series = series;
    }

    @Override
    public int size() {
        return super.size() + series.size();
    }


    protected Truthed eval(boolean taskOrJustTruth, long start, long end, NAR nar) {
        int dur = nar.dur();
        DynTruth d = series.truth(start, end, dur, nar);
        if (d == null)
            return null;

        TruthPolation p = new TruthPolation(start, end, dur, d);
        Truth pp = p.truth(false);
        if (pp == null)
            return null;

        float freqRes = taskOrJustTruth ? Math.max(nar.freqResolution.floatValue(), res.asFloat()) : 0;
        float confRes =
                0; //nar.confResolution.floatValue();
        float eviMin = 0;
        return d.eval(term, (dd, n) -> pp, taskOrJustTruth, beliefOrGoal, freqRes, confRes, eviMin, nar);
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
        series.forEach(minT, maxT, x);
    }

    @Override
    public Task taskDynamic(long start, long end, Term template, NAR nar) {
        return (Task) (eval(true, start, end, nar));
    }

    @Override
    protected @Nullable Truth truthDynamic(long start, long end, NAR nar) {
        return (Truth) (eval(false, start, end, nar));
    }

    @Override
    protected @Nullable Term template(long start, long end, Term template, NAR nar) {
        return term;
    }



    public void pri(FloatSupplier pri) {
        this.pri = pri;
    }
    public void res(FloatSupplier res) {
        this.res = res;
    }

    public SignalTask add(Truth value, long start, long end, long stamp) {
        SignalTask t = new ScalarSignalTask(
                term,
                punc(),
                value,
                start, end,
                stamp);

        float p = pri.asFloat();
        t.pri(p);

        series.add((start + end) / 2L, t);

        return t;
    }


    static class ScalarSignalTask extends SignalTask {

        public ScalarSignalTask(Term term, byte punc, Truth value, long start, long end, long stamp) {
            super(term, punc, value, start, end, stamp);
        }

        @Override
        public ITask run(NAR n) {

            n.emotion.onInput(this, n);

            //just activate
            Concept c = n.concept(term);
            if (c!=null) //shouldnt be null, ever
                Tasklinks.linkTask(this, pri, c, n);

            return null;
        }

    }
}
