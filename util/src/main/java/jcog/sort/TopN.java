package jcog.sort;

import jcog.list.FasterList;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.List;
import java.util.function.Consumer;

public class TopN<E> extends SortedArray<E> implements Consumer<E> {

    private final FloatFunction<E> rank;

    E min = null;
    float admitThresh = Float.POSITIVE_INFINITY;

    public TopN(E[] target, FloatFunction<E> rank) {
        this.list = target;
        this.rank = (x) -> -rank.floatValueOf(x); //descending
    }


    //    /**
//     * resets the best values, effectively setting a the minimum entry requirement
//     * untested
//     */
    public TopN min(float min) {
        this.admitThresh = min;
        return this;
    }

    @Override
    public void clear() {
        this.admitThresh = Float.POSITIVE_INFINITY;
        super.clear();
    }

    @Override
    public int add(E element, float elementRank, FloatFunction<E> cmp) {
        //TODO TEST THIS
        int s = this.size;
        if (s == list.length) {
//            assert (last() == min):
//                    last() + "=last but min=" + min;

            if (elementRank >= admitThresh) {
                reject(element);
                return -1; //insufficient
            }
        }


        //identity test
        for (int i = 0; i < s; i++) {
            if (list[i] == element) {
                return -1; //duplicate
            }
        }

        int r = super.add(element, elementRank, cmp);
        if (r >= 0) {
            update();
        }
        return r;
    }

    @Override
    public boolean add(E e) {
        int r = add(e, rank);
        return r >= 0;
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
    public E remove(int index) {
        E e = super.remove(index);
        update();
        return e;
    }


    private void update() {
        E nextMin = last();
        if (min != nextMin) {
            this.min = nextMin;
            admitThresh = ((size < capacity()) || nextMin == null) ? Float.POSITIVE_INFINITY :
                    rank.floatValueOf(last());
        }
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
        this.admitThresh = Float.POSITIVE_INFINITY;
        this.size = 0;

        return current;
    }

    public float minAdmission() {
        if (size == capacity())
            return -admitThresh;
        else
            return Float.NEGATIVE_INFINITY;
    }

    public float maxValue() {
        E f = first();
        if (f != null)
            return rank.floatValueOf(f);
        else
            return Float.NaN;
    }

    public float minValue() {
        E f = last();
        if (f != null)
            return rank.floatValueOf(f);
        else
            return Float.NaN;
    }
}
