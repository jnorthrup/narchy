package jcog.data.set;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtils;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.impl.iterator.ImmutableEmptyLongIterator;

import java.util.Arrays;
import java.util.NoSuchElementException;


/** a set of (long,object) pairs as 2 array lists
 *  TODO sort and binary search lookup
 * */
public abstract class LongObjectArraySet<X> extends FasterList<X> {

    protected long[] when;

    public LongObjectArraySet(int initialCapacity) {
        super(initialCapacity);
        when = ArrayUtils.EMPTY_LONG_ARRAY;
    }

    /**
     * List semantics are changed in this overridden method. this is just an alias for the addAt(long, X) method
     */
    @Override
    public void add(int when, X t) {
        this.add((long) when, t);
    }

    /** returns true if duplicate found; returns */
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

    public boolean remove(long at, X t) {
        return removeIf((when, what) -> at == when && what.equals(t));
    }

    /** removes the ith tuple */
    public void removeThe(int i) {
        removeWhen(i, size());

        super.removeFast(i);
    }
    private void removeWhen(int i, int s) {
        if (i < s - 1)
            System.arraycopy(when, i + 1, when, i, s - i - 1);
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
            int toRemove = m.cardinality();
            int firstRemoved = m.first(true);
            switch (toRemove) {
                case 0:
                    return false;
                case 1:
                    removeThe(firstRemoved);
                    return true;
                default:

                    for (int i = firstRemoved; i < s;  /* TODO iterate bitset better */) {
                        if (m.get(i)) {
                            removeThe(i);
                            s--;
                        } else {
                            i++;
                        }
                    }
                    return true;
            }
        }

    }


    @Override
    public void reverse() {
        int s = size();
        if (s > 1) {
            super.reverse();
            ArrayUtils.reverse(when, 0, s);
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
            case 0:  return ImmutableEmptyLongIterator.INSTANCE;
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
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int index;
        private int size;

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
