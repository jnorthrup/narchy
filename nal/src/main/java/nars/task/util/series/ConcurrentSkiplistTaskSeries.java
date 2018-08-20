package nars.task.util.series;

import com.google.common.collect.Iterators;
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

abstract public class ConcurrentSkiplistTaskSeries<T extends SeriesBeliefTable.SeriesTask> extends AbstractTaskSeries<T> {

    /**
     * tasks are indexed by their midpoint. since the series
     * is guaranteed to be ordered and non-overlapping, two entries
     * should not have the same mid-point even if they overlap
     * slightly.
     */
    final NavigableMap<Long, T> at;

    public ConcurrentSkiplistTaskSeries(int cap) {
        this(new ConcurrentSkipListMap<Long,T>(), cap);
    }

    public ConcurrentSkiplistTaskSeries(NavigableMap<Long, T> at, int cap) {
        super(cap);
        this.at = at;
    }

    @Override
    public long start() {
        Map.Entry<Long, T> e = at.firstEntry();
        if (e == null)
            return TIMELESS;
        return e.getValue().start();
    }

    @Override
    public long end() {
        Map.Entry<Long, T> e = at.lastEntry();
        if (e == null)
            return TIMELESS;
        return e.getValue().end();
    }

    @Override
    public int size() {
        return at.size();
    }

    @Override
    public Stream<T> stream() {
        return at.values().stream();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        at.values().forEach(action);
    }


    @Override
    public void clear() {
        at.clear();
    }

    @Override
    public boolean whileEach(final long minT, final long maxT, boolean exactRange, Predicate<? super T> x) {
        assert(minT!=ETERNAL);


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


        Iterator<T> ii;
        if (low != high) {
            ii = at.subMap(low, true, high, true).values().iterator();
        } else {
            T the = at.get(low);
            if (the == null)
                return true; //nothing
            ii = Iterators.singletonIterator(the);
        }

        while (ii.hasNext()) {
            T xx = ii.next();
            if (exactRange && !xx.intersects(minT, maxT))
                continue;
            if (!x.test(xx))
                return false;
        }

        return true;
    }

    @Override
    public boolean isEmpty(long start, long end) {
        try {
            return at.subMap(start, true, end, true).isEmpty();
        } catch (NoSuchElementException e) {
            return true;
        }
    }


    @Nullable
    @Override public T last() {
        Map.Entry<Long, T> x = at.lastEntry();
        return x!=null ? x.getValue() : null;
    }
    @Nullable
    @Override public T first() {
        Map.Entry<Long, T> x = at.firstEntry();
        return x!=null ? x.getValue() : null;
    }




    @Nullable @Override protected T pop() {
        return at.remove(at.firstKey());
    }

    @Override
    protected void push(T t) {
        at.put(t.start(), t);
    }
}
