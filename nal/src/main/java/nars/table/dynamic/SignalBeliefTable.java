package nars.table.dynamic;

import com.google.common.collect.Iterators;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.control.proto.Remember;
import nars.control.proto.TaskLinkTask;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.task.util.PredictionFeedback;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.dynamic.DynTruth;
import nars.util.TimeAware;
import nars.util.concept.ConceptBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.truth.TruthFunctions.w2cSafe;

/**
 * dynamically computes matching truths and tasks according to
 * a lossy 1-D wave updated directly by a signal input
 */
public class SignalBeliefTable extends DynamicBeliefTable {


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


    /**
     * TODO implement TaskTable
     */
    public interface TimeSeries {

        /** the provided truth value should already be dithered */
        ScalarSignalTask add(Term term, byte punc, long start, long end, Truth nextValue, int dur, NAR nar);

        @Nullable DynTruth truth(long start, long end, long dur, TimeAware timeAware);

        int size();

        default void forEach(long minT, long maxT, boolean exactRange, Consumer<? super Task> x) {
            if (!isEmpty())
                whileEach(minT, maxT, exactRange, (t) -> { x.accept(t); return true; } );
        }

        /** returns false if the predicate ever returns false; otherwise returns true even if empty.  this allows it to be chained recursively to other such iterators */
        boolean whileEach(long minT, long maxT, boolean exactRange, Predicate<? super Task> x);

        void clear();

        Stream<Task> stream();

        int forEach(long start, long end, int limit, Consumer<Task> target);

//        default FasterList<Task> toList(long start, long end, int limit) {
//            int size = size();
//            if (size == 0)
//                return new FasterList(0);
//
//            FasterList<Task> l = new FasterList<>(Math.min(size, limit));
//            forEach(start, end, limit, l::add);
//            l.compact();
//            return l;
//        }

        void forEach(Consumer<? super Task> action);

        long start();
        long end();

        default boolean isEmpty() {
            return size()==0;
        }

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
        public long start() {
            return at.firstKey();
        }

        @Override
        public long end() {
            return at.lastKey();
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
        public boolean whileEach(final long minT, final long maxT, boolean exactRange, Predicate<? super Task> x) {



            Long low = at.floorKey(minT);
            if (low == null)
                low = minT;

            if (!exactRange) {
                Long lower = at.lowerKey(low);
                if (lower != null)
                    low = lower;
            }

            Long high = at.ceilingKey(maxT);
            if (high == null)
                high = maxT;

            if (!exactRange) {
                Long higher = at.higherKey(high);
                if (higher != null)
                    high = higher;
            }




            Iterator<Task> ii;
            if (low!=high) {
                ii = at.subMap(low, high).values().iterator();
            } else {
                Task the = at.get(low);
                if (the == null)
                    return true; //nothing
                ii = Iterators.singletonIterator(the);
            }

            while (ii.hasNext()) {
                Task xx = ii.next();
                if (exactRange && !xx.intersects(minT, maxT))
                    continue;
                if (!x.test(xx))
                    return false;
            }

            return true;
        }

        @Override
        public ScalarSignalTask add(Term term, byte punc, long nextStart, long nextEnd, Truth next, int dur, NAR nar) {

            ScalarSignalTask nextTask = null;

            synchronized (this) { 

                Map.Entry<Long, Task> lastEntry = at.lastEntry();
                boolean removePrev = false;
                long lastStamp = Long.MIN_VALUE;
                long lastEntryKey;
                if (next!=null && lastEntry!=null) {
                    Task last = lastEntry.getValue();
                    lastEntryKey = lastEntry.getKey();
                    long lastStart = last.start();
                    if (lastStart > nextStart)
                        return null; 

                    long lastEnd = last.end();
                    if (nextStart - lastEnd <= dur) {
                        Truth lastEnds = last.truth(lastEnd, dur);
                        if (lastEnds.equals(next)) {
                            
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


                }

                if (nextTask!=null)
                    at.put((nextStart + nextEnd) / 2L, nextTask);

            }

            compress();

            return nextTask;

        }

        void compress() {
            if (!compressing.weakCompareAndSetAcquire(false, true))
                return;

            try {

                
                while (at.size() > cap) {
                    at.remove(at.firstKey()).delete();
                }

            } finally {
                compressing.setRelease(false);
            }
        }


        final static int MAX_TASKS_TRUTHPOLATED = Param.STAMP_CAPACITY - 1;

        @Override
        public DynTruth truth(long start, long end, long dur, TimeAware timeAware) {

            int size = size();
            if (size == 0)
                return null;

            DynTruth d = new DynTruth(Math.min(size,MAX_TASKS_TRUTHPOLATED));



                forEach(start, end, MAX_TASKS_TRUTHPOLATED, d::add);


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
                    inner.sortThisByLong(x -> x.midTimeTo(mid));
                    for (int i = 0; i < limit; i++) {
                        target.accept(inner.get(i));
                        n++;
                    }
                }
            }










            return n;
        }
    }

    public final TimeSeries series;

    public SignalBeliefTable(Term term, boolean beliefOrGoal, ConceptBuilder b) {
        this(term, beliefOrGoal, b.newTemporalTable(term));
    }

    public SignalBeliefTable(Term c, boolean beliefOrGoal, TemporalBeliefTable t) {
        this(c, beliefOrGoal,
                //TODO impl time series with concurrent ring buffer from gluegen
                new DefaultTimeSeries(new ConcurrentSkipListMap<>()
                        , /*@Deprecated*/ 256),
                t);
    }

    SignalBeliefTable(Term c, boolean beliefOrGoal, TimeSeries series, TemporalBeliefTable t) {
        super(c, beliefOrGoal, EternalTable.EMPTY, t);
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

        Truth pp = Param.truth(start, end, dur).add((Collection)d).filter().truth();
        if (pp == null)
            return null;

        float freqRes = taskOrJustTruth ? Math.max(nar.freqResolution.floatValue(), res.asFloat()) : 0;
        float confRes = taskOrJustTruth ? nar.confResolution.floatValue() : 0;
        float eviMin = taskOrJustTruth ? w2cSafe(nar.confMin.floatValue()) : Float.MIN_NORMAL;
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

    @Override
    public void sampleDynamic(long s, long e, Consumer<Task> c, NAR nar) {
        series.forEach(s, e, false, c);
    }

    public void setPri(FloatRange pri) {
        this.pri = pri;
    }
    public void resolution(FloatRange res) {
        this.res = res;
    }

    public SignalTask add(Truth value, long start, long end, Concept c, NAR nar) {

        value = value.ditherFreq(Math.max(nar.freqResolution.asFloat(), res.asFloat()));
        ScalarSignalTask x = series.add(term, punc(), start, end,
                value, nar.dur(), nar);

        if (x!=null) {
            x.pri(pri.asFloat());
            x.concept = c;
        }

        clean(nar);

        return x;
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
    public void add(Remember r, NAR n) {

        Task x = r.input;
        if (Param.FILTER_DYNAMIC_MATCHES) {
            if (!(x instanceof SignalTask) &&
                !x.isEternal() &&
                !x.isInput()) {

                if (!series.isEmpty()) {
                    if (PredictionFeedback.absorbNonSignal(x, series.start(), series.end(), n)) {
                        r.reject();
                        return;
                    }
                }

            }
        }

        super.add(r, n);
    }

    static final class ScalarSignalTask extends SignalTask {

        public Concept concept;

        ScalarSignalTask(Term term, byte punc, Truth value, long start, long end, long stamp) {
            super(term, punc, value, start, end, stamp);
        }

        @Override
        public ITask inputStrategy(Task result, NAR n) {
            return new TaskLinkTask(this, pri(), concept); //just link
        }

    }

    @Override
    protected boolean dynamicOverrides() {
        return true;
    }
}
