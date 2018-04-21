package nars.concept.dynamic;

import jcog.TODO;
import jcog.list.FasterList;
import jcog.math.FloatSupplier;
import jcog.sort.Top2;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.concept.util.ConceptBuilder;
import nars.control.proto.TaskLinkTask;
import nars.link.TaskLink;
import nars.table.TaskMatch;
import nars.table.TemporalBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.task.util.PredictionFeedback;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
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


    /**
     * TODO implement TaskTable
     */
    public interface TimeSeries {

        /** the provided truth value should already be dithered */
        SignalTask add(Term term, byte punc, long start, long end, Truth nextValue, int dur, NAR nar);

        @Nullable DynTruth truth(long start, long end, long dur, NAR nar);

        int size();

        void forEach(long minT, long maxT, boolean exactRange, Consumer<? super Task> x);

        void clear();

        Stream<Task> stream();

        int forEach(long start, long end, int limit, Consumer<Task> target);

        default FasterList<Task> toList(long start, long end, int limit) {
            int size = size();
            if (size == 0)
                return new FasterList(0);

            FasterList<Task> l = new FasterList<>(Math.min(size, limit));
            forEach(start, end, limit, l::add);
            l.compact();
            return l;
        }

        void forEach(Consumer<? super Task> action);

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
        public void forEach(Consumer<? super Task> action) {
            at.values().forEach(action);
        }

        @Override
        public void clear() {
            at.clear();
        }

        @Override
        public void forEach(final long minT, final long maxT, boolean exactRange, Consumer<? super Task> x) {
            if (at.isEmpty())
                return;
            Long low, high;
            low = at.lowerKey(minT);
            if (low == null)
                low = at.floorKey(minT); //TODO is this necessary?
            if (low == null)
                low = minT;

            high = at.higherKey(maxT);
            if (high == null)
                high = at.ceilingKey(maxT); //TODO is this necessary?
            if (high == null)
                high = maxT;

            at.subMap(low, high).values().forEach(exactRange ? xx -> {
                if (xx.intersects(minT, maxT))
                    x.accept(xx);
            } : x);
        }

        @Override
        public SignalTask add(Term term, byte punc, long nextStart, long nextEnd, Truth next, int dur, NAR nar) {

            SignalTask nextTask = null;

            synchronized (this) { //TODO try to make this synch free but for now this works

                Map.Entry<Long, Task> lastEntry = at.lastEntry();
                boolean removePrev = false;
                long lastStamp = Long.MIN_VALUE;
                long lastEntryKey;
                if (next!=null && lastEntry!=null) {
                    Task last = lastEntry.getValue();
                    lastEntryKey = lastEntry.getKey();
                    long lastStart = last.start();
                    if (lastStart > nextStart)
                        return null; //too late

                    long lastEnd = last.end();
                    if (nextStart - lastEnd <= dur) {
                        Truth lastEnds = last.truth(lastEnd, dur);
                        if (lastEnds.equals(next)) {
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

                if (next!=null) {
                    nextTask = new ScalarSignalTask(
                            term,
                            punc,
                            next,
                            nextStart, nextEnd,
                            removePrev ? lastStamp : nar.time.nextStamp());
                }

                if (removePrev) {
                    Task p = at.remove(lastEntryKey);
                    if (p!=null)
                        p.delete();
                }

                if (nextTask!=null)
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

//        final static int SELECT_ALL_THRESHOLD = 3;
        final static int MAX_TASKS_TRUTHPOLATED = Param.STAMP_CAPACITY - 1;

        @Override
        public DynTruth truth(long start, long end, long dur, NAR nar) {

            int size = size();
            if (size == 0)
                return null;

            DynTruth d = new DynTruth(MAX_TASKS_TRUTHPOLATED);
//            if (size <= SELECT_ALL_THRESHOLD) {
//                at.values().forEach(d::add);
//            } else {
                forEach(start, end, MAX_TASKS_TRUTHPOLATED, d::add);
//            }

            return d;
        }

        @Override public int forEach(long start, long end, int limit, Consumer<Task> target) {
            FasterList<Task> inner = new FasterList(limit);
            forEach(start, end, false, inner::add);

            int inners = inner.size();
            int n = 0;
            if (inners > 0) {
                if (inners <= limit) {
                    for (Task x : inner) {
                        target.accept(x);
                        n++;
                    }
                } else {
                    long mid = (start+end)/2L;
                    inner.sortThisByLong(x -> x.midDistanceTo(mid));
                    for (int i = 0; i < limit; i++) {
                        target.accept(inner.get(i));
                        n++;
                    }
                }
            }

//            if (n < 1 /*MIN_TASKS_TRUTHPOLATED */) {
//                Map.Entry<Long, Task> above = at.higherEntry(end);
//                if (above != null) { target.accept(above.getValue()); n++; }
//
//                if (n < limit) {
//                    Map.Entry<Long, Task> below = at.lowerEntry(start);
//                    if (below != null) { target.accept(below.getValue()); n++; }
//                }
//            }
            return n;
        }
    }

    public final TimeSeries series;

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
    public Task sample(long start, long end, Term template, NAR nar) {
        return matchThe(TaskMatch.sampled(start, end, nar.random()));
    }

    @Override
    public int size() {
        return super.size() + series.size();
    }

    @Override
    public void match(TaskMatch m, Consumer<Task> target) {
        long s = m.start();
        long e = m.end();
        FloatFunction<Task> value = m.value();
        Top2<Task> ss = new Top2<>(value);
        series.forEach(s, e, false, ss::add);
        if (ss.isEmpty()) {
            temporal.match(m, target);
        } else {
            temporal.match(m, ss::add);

            //combine results from sensor series and from the temporal table
            if (ss.size()==1) {
                target.accept(ss.a); //simple case
            } else {
                if (m.limit() > 1)
                    throw new TODO();
                ss.sample(target, m.value(), m.random());
            }
        }
    }

    protected Truthed eval(boolean taskOrJustTruth, long start, long end, NAR nar) {
        int dur = nar.dur();
        DynTruth d = series.truth(start, end, dur, nar);
        if (d == null)
            return null;

        Truth pp = Param.truth(start, end, dur).add((Collection)d).filter().truth();
        if (pp == null)
            return null;

        float freqRes = taskOrJustTruth ? Math.max(nar.freqResolution.floatValue(), res.asFloat()) : 0;
        float confRes =
                0; //nar.confResolution.floatValue();
        float eviMin = Float.MIN_NORMAL;
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

    public void pri(FloatSupplier pri) {
        this.pri = pri;
    }
    public void res(FloatSupplier res) {
        this.res = res;
    }

    public SignalTask add(Truth value, long start, long end, int dur, NAR nar) {

        value = value.ditherFreq(Math.max(nar.freqResolution.asFloat(), res.asFloat()));
        SignalTask x = series.add(term, punc(), start, end, value, dur, nar);

        if (x!=null)
            x.pri(pri.asFloat());

        PredictionFeedback.feedbackSignal(x, this, nar);

        return x;
    }

    @Override
    public boolean add(Task x, TaskConcept concept, NAR nar) {

        if (Param.FILTER_DYNAMIC_MATCHES) {
            if (!(x instanceof SignalTask) &&
                !x.isEternal() &&
                //input.punc() == punc() &&
                !x.isInput()) {

                PredictionFeedback.feedbackNonSignal(x, this, nar);
                if (x.isDeleted())
                    return false;

            }
        }

        return super.add(x, concept, nar);
    }

    static class ScalarSignalTask extends SignalTask {

        /** the tasklink, so it can be removed if this task is stretched (replaced by another and its tasklink) */
        private TaskLink.GeneralTaskLink link;

        public ScalarSignalTask(Term term, byte punc, Truth value, long start, long end, long stamp) {
            super(term, punc, value, start, end, stamp);
        }

        @Override
        public boolean delete() {
            if (super.delete()) {
                TaskLink.GeneralTaskLink l = link;
                if (l!=null) {
                    l.delete();
                    link = null;
                }
                return true;
            }

            return false;
        }

        @Override
        public ITask next(NAR n) {
            float pri = this.pri();
            if (pri!=pri)
                return null; //deleted before it could be processed

            n.emotion.onInput(this, n);

            //only activate
            return new TaskLinkTask(this, pri);
        }

    }
}
