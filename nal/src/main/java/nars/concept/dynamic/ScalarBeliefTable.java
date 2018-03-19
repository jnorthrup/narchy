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
import java.util.function.Supplier;
import java.util.stream.Stream;

import static nars.time.Tense.ETERNAL;

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


    /**
     * TODO implement TaskTable
     */
    interface TimeSeries {

        SignalTask add(Term term, byte punc, long start, long end, Truth nextValue, FloatSupplier res, NAR nar);

        @Nullable DynTruth truth(long start, long end, long dur, NAR nar);

        int size();

        void forEach(long minT, long maxT, Consumer<? super Task> x);

        void clear();

        Stream<Task> stream();

        int forEach(long start, long end, Supplier<Random> rng, int limit, Consumer<Task> target);

        default FasterList<Task> toList(long start, long end, Supplier<Random> rng, int limit) {
            FasterList<Task> l = new FasterList<>(limit);
            forEach(start, end, rng, limit, l::addWithoutResizeCheck);
            l.compact();
            return l;
        }
    }

    /**
     * naive implementation using a NavigableMap of indxed time points. not too smart since it cant represent mergeable flat ranges
     *
     */

    final static int SAMPLE_BATCH_SIZE = 4;

    @Override
    public Task sample(long start, long end, Term template, NAR nar) {
        Task tmp = super.sample(start, end, template, nar);
        if (start != ETERNAL) {
            Random rng = nar.random();
            return Task.eviMax(
                        series.toList(start, end, ()->rng, SAMPLE_BATCH_SIZE).get(rng),
                        tmp,
                        start, end);
        }
        return tmp;
    }

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
        public SignalTask add(Term term, byte punc, long nextStart, long nextEnd, Truth next, FloatSupplier res, NAR nar) {

            int dur = nar.dur();

            SignalTask nextTask = null;

            synchronized (this) { //TODO try to make this synch free but for now this works

                Map.Entry<Long, Task> lastEntry = at.lastEntry();
                boolean removePrev = false;
                long lastStamp = Long.MIN_VALUE;
                long lastEntryKey;
                if (lastEntry!=null) {
                    Task last = lastEntry.getValue();
                    lastEntryKey = lastEntry.getKey();
                    long lastStart = last.start();
                    if (lastStart > nextStart)
                        return null; //too late

                    if (nextStart - last.end() < dur) {
                        Truth lastEnds = last.truth(nextStart, dur);
                        if (lastEnds.equals(next) ||
                                (Math.abs(lastEnds.freq() - next.freq()) < Math.max(nar.freqResolution.floatValue(), res.asFloat())
                                    &&
                                Math.abs(lastEnds.conf() - next.conf()) < Math.max(nar.confResolution.floatValue(), res.asFloat()))
                        ) {
                            //stretch previous task
                            nextStart = lastStart;
                            lastStamp = last.stamp()[0];
                            removePrev = true;
                        }
                    }

                } else {
                    lastEntryKey = Long.MIN_VALUE;
                }

                assert(nextStart <= nextEnd);

                nextTask = new ScalarSignalTask(
                        term,
                        punc,
                        next,
                        nextStart, nextEnd,
                        removePrev ? lastStamp : nar.time.nextStamp());

                if (removePrev) {
                    at.remove(lastEntryKey);
                }

                at.put((nextStart + nextEnd) / 2L, nextTask);

            }

            compress();

            return nextTask;

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

            DynTruth d = new DynTruth(MAX_TASKS_TRUTHPOLATED);
            if (size <= SELECT_ALL_THRESHOLD) {
                at.values().forEach(d::add);
            } else {
                forEach(start, end, nar::random, MAX_TASKS_TRUTHPOLATED, d::add);
            }

            return d;
        }

        @Override public int forEach(long start, long end, Supplier<Random> rng, int limit, Consumer<Task> target) {
            SortedMap<Long, Task> range = at.subMap(start, end);

            Collection<Task> inner = range.values();
            int inners = inner.size();
            int n = 0;
            if (inners > 0) {
                if (inners < limit) {
                    for (Task x : inner) {
                        target.accept(x);
                        n++;
                    }
                } else {
                    //HACK sample random subset
                    FasterList<Task> all = new FasterList(inner);
                    Random r = rng.get();
                    for (int i = 0; i < limit; i++) {
                        target.accept(all.remove(r.nextInt(all.size())));
                        n++;
                    }
                }
            }

            if (n < 1 /*MIN_TASKS_TRUTHPOLATED */) {
                Map.Entry<Long, Task> above = at.higherEntry(end);
                if (above != null) { target.accept(above.getValue()); n++; }

                if (n < limit) {
                    Map.Entry<Long, Task> below = at.lowerEntry(start);
                    if (below != null) { target.accept(below.getValue()); n++; }
                }
            }
            return n;
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
        float eviMin = Truth.EVIMIN;
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

    public SignalTask add(Truth value, long start, long end, NAR nar) {

        SignalTask t = series.add(term, punc(), start, end, value, res, nar);

        if (t!=null)
            t.pri(pri.asFloat());

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
