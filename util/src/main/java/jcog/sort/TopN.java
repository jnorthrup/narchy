package jcog.sort;

import jcog.list.FasterList;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class TopN<E> extends SortedArray<E> implements Consumer<E> {

    private final FloatFunction<E> rank;

    public TopN(E[] target, FloatFunction<E> rank) {
        this.list = target;
        this.rank = rank; 
    }

    public final float rank(E x) {
        return rank.floatValueOf(x); 
    }

    public final float rankNeg(E x) {
        return -rank.floatValueOf(x); 
    }

    public void clear(int newCapacity, IntFunction<E[]> newArray) {
        if (list == null || list.length != newCapacity) {
            list = newArray.apply(newCapacity);
            size = 0;
        } else {
            super.clear();
        }
    }

    @Override
    public int add(E element, float elementRank, FloatFunction<E> cmp) {

        if (this.size == list.length) {
            if (elementRank >= minValueIfFull()) {
                rejectOnEntry(element);
                return -1; 
            }
        }

        return super.add(element, elementRank, cmp);
    }

    protected void rejectOnEntry(E e) {

    }

    @Override
    public boolean add(E e) {
        int r = add(e, this::rankNeg);
        return r >= 0;
    }
    
    protected boolean add(E e, float elementRank) {
        return add(e, elementRank, this::rankNeg)!=-1;
    }

    @Override
    protected boolean grows() {
        return false;
    }

    @Override
    protected E[] newArray(int oldSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void accept(E e) {
        add(e);
    }

    public E pop() {
        int s = size();
        if (s == 0) return null;
        return removeFirst();
    }

    @Override
    public void removeFast(int index) {
        throw new UnsupportedOperationException();
    }

    public List<E> drain(int count) {
        count = Math.min(count, size);
        List<E> x = new FasterList(count);
        for (int i = 0; i < count; i++) {
            x.add(pop());
        }
        return x;
    }

    public E[] drain(E[] next) {

        E[] current = this.list;

        this.list = next;
        this.size = 0;

        return current;
    }


    public float maxValue() {
        E f = first();
        if (f != null)
            return rankNeg(f);
        else
            return Float.NaN;
    }

    public float minValue() {
        E f = last();
        return f != null ? rank(f) : Float.NaN;
    }

    public float minValueIfFull() {
        return size() == capacity() ? minValue() : Float.NEGATIVE_INFINITY;
    }

}
