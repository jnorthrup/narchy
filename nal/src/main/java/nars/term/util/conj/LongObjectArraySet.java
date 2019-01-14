package nars.term.util.conj;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtils;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;

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
     * List semantics are changed in this overridden method. this is just an alias for the add(long, X) method
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
    public final boolean add(X newItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final X remove(int index) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(long at, X t) {
        return removeIf((when, what) -> at == when && what.equals(t));
    }

    protected void removeEventFast(int i) {
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
                    removeWhen(firstRemoved, s);
                    super.removeFast(firstRemoved);
                    return true;
                default:

                    for (int i = firstRemoved; i < s;  /* TODO iterate bitset better */) {
                        if (m.get(i)) {
                            removeWhen(i, s);
                            super.removeFast(i);
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
                    removeEventFast(0);
                    return true;
                }
            default:
                return removeIf((when, what) -> what.equals(X));
        }

    }

    public LongIterator longIterator() {
        return new InternalLongIterator(when, size());
    }

    public long when(int i) {
        return when[i];
    }

    static class InternalLongIterator implements MutableLongIterator {

        private final long[] data;
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;
        private int lastIndex;

        public InternalLongIterator(long[] data, int size) {
            this.data = data;
            this.lastIndex = size;
        }

        @Override
        public boolean hasNext() {
            return this.currentIndex != lastIndex;
        }

        @Override
        public long next() {
            if (!this.hasNext())
                throw new NoSuchElementException();

            long next = data[this.currentIndex];
            this.lastIndex = this.currentIndex++;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
