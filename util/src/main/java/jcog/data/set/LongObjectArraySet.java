package jcog.data.set;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.impl.iterator.ImmutableEmptyLongIterator;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Arrays;
import java.util.NoSuchElementException;


/**
 * a set of (long,object) pairs as 2 array lists
 * TODO sort and binary search lookup
 */
public class LongObjectArraySet<X> extends FasterList<X> {

    protected long[] when;

    public LongObjectArraySet() {
        this(0);
    }

    public LongObjectArraySet(int initialSize, X[] array) {
        super(initialSize, array);
        when = ArrayUtil.EMPTY_LONG_ARRAY;
    }

    public LongObjectArraySet(int initialCapacity) {
        super(initialCapacity);
        when = ArrayUtil.EMPTY_LONG_ARRAY;
    }


    @Override
    public FastList<X> sortThis() {
        if (size <= 1)
            return this;

        long[] when = this.when;

        int left = 0, right = size - 1;
        for (int i = left, j = i; i < right; j = ++i) {
            X xi = get(i + 1);

            long li = when[i + 1];
            while (li < when[j]) {
                setFast(j + 1, get(j));
                when[j + 1] = when[j];
                if (j-- == left)
                    break;
            }

            setFast(j + 1, xi);
            when[j + 1] = li;


        }
        return this;
    }

    @Override
    public String toString() {
        //HACK this could be better
        final int[] i = {0};
        return Joiner.on(',').join(Iterables.transform(this, n -> when[i[0]++] + ":" + n));
    }

    /**
     * List semantics are changed in this overridden method. this is just an alias for the addAt(long, X) method
     */
    @Override
    public final void add(int when, X t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeInstance(X term) {
        throw new UnsupportedOperationException();
    }

    /**
     * returns true if duplicate found; returns
     */
    public boolean add(long w, X t) {

        //quick check for existing
        int n = size();
        for (int i = 0; i < n; i++) {
            if (when(i) == w && get(i).equals(t))
                return false; //existing found
        }

        int s = addAndGetSize(t);

        //match long[] to the Object[] capacity
        if (this.when.length < s) {
            this.when = Arrays.copyOf(this.when, items.length);
        }

        this.when[s - 1] = w;

        return true;
    }

    @Override
    public final boolean add(X x) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final X remove(int index) {
        throw new UnsupportedOperationException("use removeThe(index)");
    }

    public boolean removeFirst() {
        if (isEmpty())
            return false;
        removeThe(0);
        return true;
    }

    public boolean remove(long at, X t) {
        return removeIf((when, what) -> at == when && what.equals(t));
    }

    /**
     * removes the ith tuple
     */
    public void removeThe(int i) {
        removeWhen(i, size());

        super.removeFast(i);
    }

    private void removeWhen(int i, int s) {
        if (i < s - 1)
            System.arraycopy(when, i + 1, when, i, s - i - 1);
    }


    public boolean removeIf(long theLong, LongObjectPredicate<X> iff) {
        int s = size();
        MetalBitSet m = MetalBitSet.bits(s);
        for (int i = 0; i < s; i++) {
            long w = when[i];
            if (w == theLong) {
                if (iff.accept(w, get(i)))
                    m.set(i);
            }
        }
        return removeAll(m, s);
    }

    @Override
    public boolean removeAbove(int index) {
        throw new TODO();
    }

    public void forEachEvent(LongObjectProcedure<X> each) {
        removeIf((when,what)->{
            each.value(when,what);
            return false;
        });
    }

    public boolean removeIf(LongObjectPredicate<X> iff) {
        int s = size();
        if (s == 0)
            return false;
        else if (s == 1) {
            if (iff.accept(when[0], get(0))) {
                clear();
                return true;
            } else
                return false;
        } else {

            MetalBitSet m = MetalBitSet.bits(s);
            for (int i = 0; i < s; i++) {
                if (iff.accept(when[i], get(i)))
                    m.set(i);
            }
            return removeAll(m, s);
        }

    }

    public boolean removeAll(MetalBitSet m, int s) {
        int toRemove = m.cardinality();
        if (toRemove == 0)
            return false;

        int firstRemoved = m.first(true);
        if (toRemove == 1) {
            removeThe(firstRemoved);
        } else {
            for (int i = firstRemoved; i < s;  /* TODO iterate bitset better */) {
                if (m.get(i)) {
                    removeThe(i);
                    s--;
                } else {
                    i++;
                }
            }
        }
        return true;
    }

    @Override
    public X removeLast() {
        removeThe(size()-1);
        return null; //TODO
    }

    @Override
    public void reverse() {
        int s = size();
        if (s > 1) {
            super.reverse();
            ArrayUtil.reverse(when, 0, s);
        }
    }
//    public LongObjectPair<X> removeEvent(int i) {
//        long w = when[i];
//
//        removeWhen(i, size());
//
//        X x = super.remove(i);
//
//        return PrimitiveTuples.pair(w, x);
//    }


    public boolean removeAll(X X) {
        switch (size()) {
            case 0:
                return false;
            case 1:
                if (get(0).equals(X)) {
                    removeThe(0);
                    return true;
                }
            default:
                return removeIf((when, what) -> what.equals(X));
        }

    }

    public LongIterator longIterator() {
        int s = size();
        switch (s) {
            case 0:
                return ImmutableEmptyLongIterator.INSTANCE;
            //case 1:  //TODO return LongIterator
            default:
                return new InternalLongIterator(when, s);
        }
    }

    public long when(int i) {
        return when[i];
    }

    static final class InternalLongIterator implements MutableLongIterator {

        private final long[] data;
        private final int size;
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int index;

        public InternalLongIterator(long[] data, int size) {
            this.data = data;
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return this.index < size;
        }

        @Override
        public long next() {
            if (!this.hasNext())
                throw new NoSuchElementException();

            return data[this.index++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
