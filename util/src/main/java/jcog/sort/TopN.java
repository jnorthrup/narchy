package jcog.sort;

import jcog.data.list.FasterList;
import jcog.data.pool.MetalPool;
import jcog.decide.Roulette;
import jcog.pri.Ranked;
import jcog.util.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import static java.lang.Float.NEGATIVE_INFINITY;

/**
 * warning: this keeps duplicate insertions
 */
public class TopN<X> extends SortedArray<X> implements Consumer<X>, FloatFunction<X> {


    public final static TopN Empty = new TopN(ArrayUtils.EMPTY_OBJECT_ARRAY, (x, min) -> Float.NaN) {
        @Override
        public void setCapacity(int capacity) {

        }
    };

    public FloatRank<X> rank;
    private float min;
    private int capacity;

    public TopN(X[] target) {
        this.items = target;
        this.min = NEGATIVE_INFINITY;
    }

    /**
     * try to use the FloatRank if a scoring function can be interrupted
     */
    public TopN(X[] target, FloatFunction<X> rank) {
        this(target, FloatRank.the(rank));
    }

    public TopN(X[] target, FloatRank<X> rank) {
        this(target);
        rank(rank, target.length);
    }

    @Override
    public void clear() {
        super.clear();
        min = NEGATIVE_INFINITY;
    }

    public TopN<X> rank(FloatRank<X> rank, int capacity) {
        this.rank = rank;
        //if (capacity > 0)
            setCapacity(capacity);
//        else
//            this.capacity = 0; //HACK
        return this;
    }

    public void setCapacity(int capacity) {

        if (capacity <= 0)
            throw new ArrayIndexOutOfBoundsException("capacity must be > 0");
        if (!(items.length >= capacity))
            throw new ArrayIndexOutOfBoundsException("insufficient item capcacity");

        this.capacity = capacity;
    }

    @Override
    public final int capacity() {
        return capacity;
    }

    @Override
    protected boolean exhaustiveFind() {
        return false;
    }


    public void clear(int newCapacity, IntFunction<X[]> newArray) {
        min = NEGATIVE_INFINITY;
        if (items == null || items.length != newCapacity) {
            items = newArray.apply(newCapacity);
            size = 0;
        } else {
            super.clear();
        }
    }

    @Override
    public final int add(X element, float negRank, FloatFunction<X> cmp) {

        return size == capacity() && -negRank <= min
                ? -1
                :
                super.add(element, negRank, cmp);
    }


    public final boolean add(/*@NotNull */X e) {
        int r = add(e, this);
        if (r >= 0) {
            commit();
            return true;
        }
        return false;
    }


    @Override
    protected boolean grows() {
        return false;
    }


    @Override
    public final void accept(X e) {
        add(e);
    }

    private void commit() {
        min = _minValueIfFull();
    }

    public X pop() {
        int s = size();
        if (s == 0) return null;
        commit();
        return removeFirst();
    }

    @Override
    public void removeFast(int index) {
        throw new UnsupportedOperationException();
    }

    public List<X> drain(int count) {
        count = Math.min(count, size);
        List<X> x = new FasterList(count);
        for (int i = 0; i < count; i++) {
            x.add(removeFirst());
        }
        commit();
        return x;
    }

    public X[] drain(X[] next) {

        X[] current = this.items;

        this.items = next;
        this.size = 0;
        commit();

        return current;
    }


    public float maxValue() {
        X f = first();
        return f != null ? rank.rank(f) : Float.NaN;
    }

    public float minValue() {
        X f = last();
        return f != null ? rank.rank(f) : Float.NaN;
    }

    private float _minValueIfFull() {
        return size() == capacity() ? minValue() : NEGATIVE_INFINITY;
    }

    public X top() {
        return isEmpty() ? null : get(0);
    }

    /**
     * what % to remain; ex: rate of 25% removes the lower 75%
     */
    public void removePercentage(float below, boolean ofExistingOrCapacity) {
        assert (below >= 0 && below <= 1.0f);
        int belowIndex = (int) Math.floor(ofExistingOrCapacity ? size() : capacity() * below);
        if (belowIndex < size) {
            size = belowIndex;
            Arrays.fill(items, size, items.length - 1, null);
            commit();
        }
    }

//    public Set<X> removePercentageToSet(float below) {
//        assert(below >= 0 && below <= 1.0f);
//        int belowIndex = (int) Math.floor(size() * below);
//        if (belowIndex == size)
//            return Set.of();
//
//        int toRemove = size - belowIndex;
//        Set<X> removed = new HashSet();
//        for (int i = 0; i < toRemove; i++) {
//            removed.addAt(removeLast());
//        }
//        return removed;
//    }

    public boolean isFull() {
        return size() >= capacity();
    }

    /**
     * 0 < thresh <= 1
     */
    private boolean isFull(float thresh) {
        return (size() >= capacity() * thresh);
    }

    @Nullable
    public X getRoulette(Random rng) {
        return getRoulette(rng, rank);
    }

    /**
     * note: this assumes the ranking function operates in a range >= 0 so that by choosing a roulette weight 0 it should be skipped
     * and not surprise the roulette like a value of NEGATIVE_INFINITY or NaN *will*.
     */
    @Nullable
    public X getRoulette(Random rng, Predicate<X> filter) {
        return getRoulette(rng, (X x, float min) -> {
            if (!filter.test(x))
                return 0;
            return rank.rank(x);
        });
    }

    /**
     * roulette select
     */
    @Nullable
    public X getRoulette(Random rng, FloatRank<X> rank) {
        int n = size();
        if (n == 0)
            return null;
        IntToFloatFunction select = i -> rank.rank(get(i));
        return get( //n < 8 ?
                Roulette.selectRouletteCached(n, select, rng) //must be cached for consistency
                // :
                // Roulette.selectRoulette(n, select, rng)
        );

    }

    public final float minValueIfFull() {
        return min;
    }

    @Override
    public final float floatValueOf(X x) {
        return -rank.rank(x, min);
    }

//
//    public static <X> ThreadLocal<MetalPool<TopN<X>>> newPool(IntFunction<X[]> arrayBuilder) {
//        return MetalPool.threadLocal(() -> {
//            int initialCapacity = 32;
//            return new TopN<>(arrayBuilder.apply(initialCapacity), new CachedFloatFunction<>(initialCapacity * 2, x -> Float.NaN));
//        });
//    }

    //    /**
//     * default pool
//     */
//    public final static ThreadLocal<MetalPool<TopN<Object>>> pool = TopN.newPool(Object[]::new);

//    public static TopN pooled(int capacity, FloatFunction rank) {
//        return pooled(capacity, capacity > 1, rank);
//    }
//
//    public static TopN pooled(int capacity, boolean cache, FloatFunction rank) {
//        return pooled(pool, capacity, cache, rank, Object[]::new);
//    }

//    public static <X> TopN<X> pooled(ThreadLocal<MetalPool<TopN<X>>> pool, int capacity, FloatFunction<X> rank, IntFunction<Object[]> arrayBuilder) {
//        return pooled(pool, capacity, capacity > 1, rank, arrayBuilder);
//    }

//    public static <X> TopN<X> pooled(ThreadLocal<MetalPool<TopN<X>>> pool, int capacity, boolean cache, FloatFunction<X> rank, IntFunction<Object[]> arrayBuilder) {
//        if (!cache) {
//            return new TopN(arrayBuilder.apply(1), rank);
//        } else {
//            TopN t = pool.get().get();
//            ((CachedFloatFunction) t.rank).value(rank);
//            if (t.items.length < capacity)
//                t.items = arrayBuilder.apply(capacity);
//            return t;
//        }
//    }

    public static <X> RankedTopN<X> pooled(ThreadLocal<MetalPool<RankedTopN>> pool, int capacity, FloatFunction<X> rank) {
        return pooled(pool, capacity, FloatRank.the(rank));
    }

    public static <X> RankedTopN<X> pooled(ThreadLocal<MetalPool<RankedTopN>> pool, int capacity, FloatRank<X> rank) {
        RankedTopN<X> t = pool.get().get();
        if (t.items.length < capacity)
            t.items = new Ranked[capacity];
        t.ranking(rank, capacity);
        return t;
    }

//    public static void unpool(TopN<Object> t) {
//        unpool(pool, t);
//    }

//    public static <X> void unpool(ThreadLocal<MetalPool<TopN<X>>> pool, TopN<X> t) {
//        if (!(t.rank instanceof CachedFloatFunction))
//            return; //wasnt from the pool
//
//        t.clear();
//        ((CachedFloatFunction) t.rank).value(r -> Float.NaN);
//        pool.get().put(t);
//    }

    public static void unpool(ThreadLocal<MetalPool<RankedTopN>> pool, RankedTopN t) {
        t.clear();
        t.rank = null;
        t.pool = null;
        pool.get().put(t);
    }

    public void compact(float thresh) {
        int s = size();
        if (s == 0) {
            items = null;
            capacity = 0;
        } else {
            if (!isFull(thresh)) {
                items = Arrays.copyOf(items, s);
                capacity = s;
            }
        }
    }


    /**
     * creates a copy of the array, trimmed
     */
    public X[] toArray() {
        return Arrays.copyOf(items, size());
    }

    @Nullable
    public X[] toArrayOrNullIfEmpty() {
        int s = size();
        return s > 0 ? Arrays.copyOf(items, s) : null;
    }

    @Nullable
    public X[] toArrayIfSameSizeOrRecycleIfAtCapacity(@Nullable X[] x) {
        int s = size();
        int xl = x != null ? x.length : 0;
        if (s == 0)
            return null;
        else if (xl == s) {
            System.arraycopy(items, 0, x, 0, s);
            return x;
        } else {
            if (items.length == s)
                return items;
            else
                return Arrays.copyOf(items, s);
        }
    }
}
