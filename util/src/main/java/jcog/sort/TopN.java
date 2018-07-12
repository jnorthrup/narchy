package jcog.sort;

import jcog.list.FasterList;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/** warning: this keeps duplicate insertions */
public class TopN<X> extends SortedArray<X> implements Consumer<X> {

    protected final FloatFunction<X> rank;

    public TopN(X[] target, FloatFunction<X> rank) {
        this.items = target;
        this.rank = rank; 
    }

    public final float rank(X x) {
        return rank.floatValueOf(x); 
    }

    public final float rankNeg(X x) {
        return -rank.floatValueOf(x); 
    }

    public void clear(int newCapacity, IntFunction<X[]> newArray) {
        if (items == null || items.length != newCapacity) {
            items = newArray.apply(newCapacity);
            size = 0;
        } else {
            super.clear();
        }
    }

    @Override
    public int add(X element, float elementRank, FloatFunction<X> cmp) {

        if (this.size == items.length) {
            if (elementRank >= minValueIfFull()) {
                rejectOnEntry(element);
                return -1; 
            }
        }

        return super.add(element, elementRank, cmp);
    }

    protected void rejectOnEntry(X e) {

    }

    @Override
    public boolean add(X e) {
        int r = add(e, this::rankNeg);
        return r >= 0;
    }
    
    protected boolean add(X e, float elementRank) {
        return add(e, elementRank, this::rankNeg)!=-1;
    }

    @Override
    protected boolean grows() {
        return false;
    }

    @Override
    protected X[] newArray(int oldSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void accept(X e) {
        add(e);
    }

    public X pop() {
        int s = size();
        if (s == 0) return null;
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
            x.add(pop());
        }
        return x;
    }

    public X[] drain(X[] next) {

        X[] current = this.items;

        this.items = next;
        this.size = 0;

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
        return f != null ? rank(f) : Float.NaN;
    }

    public float minValueIfFull() {
        return size() == capacity() ? minValue() : Float.NEGATIVE_INFINITY;
    }

    public X top() { return isEmpty() ? null : get(0); }

    /** what % to remain; ex: rate of 25% removes the lower 75% */
    public void removePercentage(float below, boolean ofExistingOrCapacity) {
        assert(below >= 0 && below <= 1.0f);
        int belowIndex = (int) Math.floor(ofExistingOrCapacity ? size(): capacity() * below);
        if (belowIndex < size) {
            size = belowIndex;
            Arrays.fill(items, size, items.length-1, null);
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
}
