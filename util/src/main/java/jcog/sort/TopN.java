package jcog.sort;

import jcog.data.list.FasterList;
import jcog.decide.Roulette;
import jcog.math.FloatSupplier;
import jcog.util.ArrayUtil;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;

import static java.lang.Float.NEGATIVE_INFINITY;

/**
 * warning: this keeps duplicate insertions
 */
public class TopN<X> extends SortedArray<X> implements FloatFunction<X>, TopFilter<X> {


    public final static TopFilter Empty = new TopN(ArrayUtil.EMPTY_OBJECT_ARRAY, (x, min) -> Float.NaN) {
        @Override
        public void setCapacity(int capacity) {

        }
    };

    public FloatRank<X> rank;
    float min= NEGATIVE_INFINITY;
    private int capacity;

    public TopN(X[] target) {
        this.items = target;
        setCapacity(target.length);
    }


    /**
     * try to use the FloatRank if a scoring function can be interrupted
     */
    public TopN(X[] target, FloatFunction<X> rank) {
        this(target, FloatRank.the(rank));
    }

    public TopN(X[] target, FloatRank<X> rank) {
        this(target);
        rank(rank);
    }

    @Override
    public void clear() {
        super.clear();
        min = NEGATIVE_INFINITY;
    }
    @Override
    public void clearWeak() {
        super.clearWeak();
        min = NEGATIVE_INFINITY;
    }

    public TopFilter<X> rank(FloatRank<X> rank) {
        this.rank = rank;
        return this;
    }

    @Deprecated public final TopFilter<X> rank(FloatRank<X> rank, int capacity) {
        this.rank = rank;
        //if (capacity > 0)
            setCapacity(capacity);
            return rank(rank);
//        else
//            this.capacity = 0; //HACK
    }

    public void setCapacity(int capacity) {

        //assert(capacity > 0);
//        if (capacity <= 0)
//            throw new ArrayIndexOutOfBoundsException("capacity must be > 0");

        this.capacity = capacity;
        if (this.items.length != capacity)
            this.items = Arrays.copyOf(items, capacity);
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


    public final boolean add(/*@NotNull*/ X e) {
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
        X x = removeFirst();
        if (x!=null)
            commit();
        return x;
    }


    public List<X> drain(int count) {
        count = Math.min(count, size);
        List<X> x = new FasterList<>(count);
        for (int i = 0; i < count; i++)
            x.add(removeFirst());
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
        return size == capacity ? minValue() : NEGATIVE_INFINITY;
    }

    public final X top() {
        return isEmpty() ? null : items[0];
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

    public final boolean isFull() {
        return size >= capacity;
    }

    /**
     * 0 < thresh <= 1
     */
    private boolean isFull(float thresh) {
        return (size >= capacity * thresh);
    }

    @Nullable
    public final X getRoulette(Random rng) {
        return size > 0 ? getRoulette(rng::nextFloat) : null;
    }

    @Nullable
    public X getRoulette(FloatSupplier rng) {
        return getRoulette(rng, rank);
    }


    /**
     * roulette select
     */
    @Nullable public X getRoulette(FloatSupplier rng, FloatFunction<X> anyRank) {
        int n = size;
        if (n == 0)
            return null;
        else if (n == 1)
            return get(0);
        else {
            IntToFloatFunction select = i -> anyRank.floatValueOf(get(i));
            return get(
                    this instanceof RankedN ?
                            Roulette.selectRoulette(n, select, rng) : //RankedTopN acts as the cache
                            Roulette.selectRouletteCached(n, select, rng) //must be cached for consistency
            );
        }

    }

    public final float minValueIfFull() {
        return min;
    }

    @Override
    public final float floatValueOf(X x) {
        return -rank.rank(x, min);
    }


    public void compact(float thresh) {
        int s = size;
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
        return Arrays.copyOf(items, size);
    }


//    @Nullable
//    public X[] toArrayIfSameSizeOrRecycleIfAtCapacity(@Nullable X[] x) {
//        int s = size();
//        if (s == 0)
//            return null;
//
//        int xl = x != null ? x.length : 0;
//        if (xl == s) {
//            System.arraycopy(items, 0, x, 0, s);
//            return x;
//        } else {
//            return items.length == s ? items : Arrays.copyOf(items, s);
//        }
//    }
}
