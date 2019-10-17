package nars.task.util.series;

import com.google.common.collect.Iterators;
import jcog.math.LongInterval;
import nars.table.dynamic.SeriesBeliefTable;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.math.LongInterval.ETERNAL;
import static jcog.math.LongInterval.TIMELESS;

public class ConcurrentSkiplistTaskSeries<T extends SeriesBeliefTable.SeriesTask> extends AbstractTaskSeries<T> {

    /**
     * tasks are indexed by their midpoint. since the series
     * is guaranteed to be ordered and non-overlapping, two entries
     * should not have the same mid-point even if they overlap
     * slightly.
     */
    final NavigableMap<Long, T> q;

    public ConcurrentSkiplistTaskSeries(int cap) {
        this(new ConcurrentSkipListMap<>(), cap);
    }

    public ConcurrentSkiplistTaskSeries(NavigableMap<Long, T> q, int cap) {
        super(cap);
        this.q = q;
    }

    @Override
    public final boolean isEmpty() {
        return q.isEmpty();
    }

    @Override
    public final boolean isEmpty(LongInterval l) {
        return !q.isEmpty() && super.isEmpty(l);
    }
    @Override
    public long start() {
        Map.Entry<Long, T> e = q.firstEntry();
        if (e == null)
            return TIMELESS;
        return e.getValue().start();
    }

    @Override
    public long end() {
        Map.Entry<Long, T> e = q.lastEntry();
        if (e == null)
            return TIMELESS;
        return e.getValue().end();
    }

    @Override
    public final int size() {
        return q.size();
    }

    @Override
    public Stream<T> stream() {
        return q.values().stream();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        q.values().forEach(action);
    }


    @Override
    public void clear() {
        q.clear();
    }

    @Override
    public boolean whileEach(final long minT, final long maxT, boolean exactRange, Predicate<? super T> x) {
        assert(minT!=ETERNAL);


        Long low = q.floorKey(minT);
        if (low == null)
            low = minT;

        if (!exactRange) {
            Long lower = q.lowerKey(low);
            if (lower != null)
                low = lower;
        }

        Long high = q.ceilingKey(maxT);
        if (high == null)
            high = maxT;

        if (!exactRange) {
            Long higher = q.higherKey(high);
            if (higher != null)
                high = higher;
        }


        Iterator<T> ii;
        if (low != high) {
            ii = q.subMap(low, true, high, true).values().iterator();
        } else {
            T the = q.get(low);
            if (the == null)
                return true; //nothing
            ii = Iterators.singletonIterator(the);
        }

        while (ii.hasNext()) {
            T xx = ii.next();
            if (exactRange && !xx.intersectsRaw(minT, maxT))
                continue;
            if (!x.test(xx))
                return false;
        }

        return true;
    }

    @Override
    public boolean isEmpty(long start, long end) {
        try {
            return q.subMap(start, true, end, true).isEmpty();
        } catch (NoSuchElementException e) {
            return true;
        }
    }


    @Override
    public @Nullable T last() {
        Map.Entry<Long, T> x = q.lastEntry();
        return x!=null ? x.getValue() : null;
    }
    @Override
    public @Nullable T first() {
        Map.Entry<Long, T> x = q.firstEntry();
        return x!=null ? x.getValue() : null;
    }




    @Override
    protected @Nullable T pop() {
        return q.remove(q.firstKey());
    }

    @Override
    public void push(T t) {
        q.put(t.start(), t);
    }
}
