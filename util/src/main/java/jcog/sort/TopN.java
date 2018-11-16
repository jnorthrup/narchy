package jcog.sort;

import jcog.data.list.FasterList;
import jcog.data.pool.MetalPool;
import jcog.decide.Roulette;
import jcog.math.CachedFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static java.lang.Float.NEGATIVE_INFINITY;

/** warning: this keeps duplicate insertions */
public class TopN<X> extends SortedArray<X> implements Consumer<X>, FloatFunction<X> {


    public FloatRank<X> rank;
    private float min;

    /** try to use the FloatRank if a scoring function can be interrupted */
    @Deprecated public TopN(X[] target, FloatFunction<X> rank) {
        this(target, FloatRank.the(rank));
    }

    public TopN(X[] target, FloatRank<X> rank) {
        this.items = target;
        this.min = NEGATIVE_INFINITY;
        rank(rank);
    }

    @Override
    public void clear() {
        super.clear();
        min = NEGATIVE_INFINITY;
    }

    public TopN<X> rank(FloatRank<X> rank) {
        this.rank = rank;
        return this;
    }

    @Override protected boolean exhaustiveFind() {
        return false;
    }


    /** invert the SortedArray's order so this isnt necessary */
    @Deprecated private float rankNeg(X x) {
        return -rank.rank(x, min);
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


    @Override
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
        if (f != null)
            return rankNeg(f);
        else
            return Float.NaN;
    }

    public float minValue() {
        X f = last();
        return f != null ? rank.rank(f, Float.NEGATIVE_INFINITY) : Float.NaN;
    }

    private float _minValueIfFull() {
        return size() == capacity() ? minValue() : NEGATIVE_INFINITY;
    }

    public X top() { return isEmpty() ? null : get(0); }

    /** what % to remain; ex: rate of 25% removes the lower 75% */
    public void removePercentage(float below, boolean ofExistingOrCapacity) {
        assert(below >= 0 && below <= 1.0f);
        int belowIndex = (int) Math.floor(ofExistingOrCapacity ? size(): capacity() * below);
        if (belowIndex < size) {
            size = belowIndex;
            Arrays.fill(items, size, items.length-1, null);
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
//            removed.add(removeLast());
//        }
//        return removed;
//    }

    public boolean isFull() {
        return size()>=capacity();
    }

    /** roulette select */
    @Nullable
    public X get(Random rng){
        int n = size();
        switch (n) {
            case 0:
                return null;
            case 1:
                return get(0);
            default:
                return get(Roulette.selectRoulette(n, i -> rank.rank(get(i), NEGATIVE_INFINITY), rng));
        }
    }

    public final float minValueIfFull() {
        return min;
    }

    @Override
    public final float floatValueOf(X x) {
        return rankNeg(x);
    }


    public static <X> ThreadLocal<MetalPool<TopN<X>>> newPool(IntFunction<X[]> arrayBuilder) {
        return MetalPool.threadLocal(() -> {
            int initialCapacity = 32;
            return new TopN<>(arrayBuilder.apply(initialCapacity), new CachedFloatFunction<>(initialCapacity*2, x->Float.NaN));
        });
    }

    /** default pool */
    public final static ThreadLocal<MetalPool<TopN<Object>>> pool = TopN.newPool(Object[]::new);

    public static TopN pooled(int capacity, FloatFunction rank) {
        return pooled(capacity, capacity>1, rank);
    }

    public static TopN pooled(int capacity, boolean cache, FloatFunction rank) {
        return pooled(pool, capacity, cache, rank, Object[]::new);
    }

    public static <X> TopN<X> pooled(ThreadLocal<MetalPool<TopN<X>>> pool, int capacity, FloatFunction<X> rank,IntFunction<Object[]> arrayBuilder) {
        return pooled(pool, capacity, capacity>1, rank, arrayBuilder);
    }

    public static <X> TopN<X> pooled(ThreadLocal<MetalPool<TopN<X>>> pool, int capacity, boolean cache, FloatFunction<X> rank,IntFunction<Object[]> arrayBuilder) {
        if (!cache) {
            return new TopN(arrayBuilder.apply(1), rank);
        } else {
            TopN t = pool.get().get();
            ((CachedFloatFunction) t.rank).value(rank);
            if (t.items.length < capacity)
                t.items = arrayBuilder.apply(capacity);
            return t;
        }
    }

    public static void unpool(TopN<Object> t) {
        unpool(pool, t);
    }

    public static <X> void unpool(ThreadLocal<MetalPool<TopN<X>>> pool,TopN<X> t) {
        if (!(t.rank instanceof CachedFloatFunction))
            return; //wasnt from the pool

        t.clear();
        ((CachedFloatFunction)t.rank).value(r -> Float.NaN);
        pool.get().put(t);
    }
}
