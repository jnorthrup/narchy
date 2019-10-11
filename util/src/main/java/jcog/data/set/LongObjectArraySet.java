package jcog.data.set;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.sort.QuickSort;
import jcog.util.ArrayUtil;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.impl.iterator.ImmutableEmptyLongIterator;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;


/**
 * a set of (long,object) pairs as 2 array lists
 * TODO sort and binary search lookup
 */
public class LongObjectArraySet<X> extends FasterList<X> {

    public long[] when;

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
    public void swap(int a, int b) {
        if (a != b) {
            ArrayUtil.swapObj(items, a, b);
            ArrayUtil.swapLong(when, a, b);
        }
    }

    @Override
    public LongObjectArraySet<X> sortThis() {

        int size = this.size;
        if (size > 1)
            QuickSort.quickSort(0, size, this::whenFirstCompare, this::swap);
        return this;
//
//        long[] when = this.when;
//        X[] ii = this.items;
//
//        int left = 0, right = size - 1;
//        for (int i = left, j = i; i < right; j = ++i) {
//
//            X xi = ii[i + 1];
//            long li = when[i + 1];
//
//            while (li < when[j]) {
//                ii[j+1] = ii[j];
//                when[j + 1] = when[j];
//                if (j-- == left)
//                    break;
//            }
//
//            ii[j+1] = xi;
//            when[j + 1] = li;
//        }
//
//        if (get(0) instanceof Comparable) {
//            //sort the items within each timeslot
//            int a = 0;
//            long x = when[0];
//            for (int i = 1; i <= size; i++) {
//                long y = i<size ? when[i] : TIMELESS;
//                if (y != x) {
//                    if (i - a > 1)
//                        Arrays.sort(ii, a, i);
//
//                    x = y;
//                    a = i;
//                }
//            }
//        }
    }

    public LongObjectArraySet<X> sortThisByValue() {
        int size = this.size;
        if (size > 1)
            QuickSort.quickSort(0, size, this::valueFirstCompare, this::swap);
        return this;

//        long[] when = this.when;
//        X[] ii = this.items;
//
//        int left = 0, right = size - 1;
//        for (int i = left, j = i; i < right; j = ++i) {
//
//            X xi = ii[i + 1];
//            long li = when[i + 1];
//
//            while (valueFirstCompare(i+1, j)<0) {
//                ii[j + 1] = ii[j];
//                when[j + 1] = when[j];
//                if (j-- == left)
//                    break;
//            }
//
//            ii[j + 1] = xi;
//            when[j + 1] = li;
//        }
//        return this;
    }

    private int whenFirstCompare(int ia, int ib) {
        if (ia == ib) return 0;
        long[] ww = this.when;
        int ab = Long.compare(ww[ia], ww[ib]);
        if (ab != 0)
            return ab;

        X[] ii = this.items;
        X a = ii[ia], b = ii[ib];
        return ((Comparable)a).compareTo(b); //TODO non-Comparable compare by obj identity
    }

    private int valueFirstCompare(int ia, int ib) {
        if (ia == ib) return 0;
        X[] ii = this.items;
        X a = ii[ia], b = ii[ib];
        int ab = ((Comparable)a).compareTo(b); //TODO non-Comparable compare by obj identity
        if (ab != 0)
            return ab;

        long[] ww = when;
        return Long.compare(ww[ia], ww[ib]);
    }

    @Override
    public void trimToSize() {
        super.trimToSize();
        if (when.length > size)
            when = Arrays.copyOf(when, size);
    }

    public boolean contains(long w, X what) {
        return contains(w, what, 0, size());
    }

    public final boolean contains(long w, X what, int startIndex, int finalIndexExc) {
        long[] longs = this.when;
        X[] ii = this.items;
        for (int i = startIndex; i < finalIndexExc; i++) {
            if (longs[i] == w && ii[i].equals(what))
                return true;
        }
        return false;
    }

    /** assumes its been sorted */
    protected int _indexOf(long w, X what, int startIndex, int finalIndexExc, BiPredicate<X,X> equal, int dtTolerance) {
        long[] longs = this.when;
        X[] ii = this.items;
        boolean forward = finalIndexExc >= startIndex;
        for (int i = startIndex; ; i+= forward ? 1 : -1) {
            long ll = longs[i];
            if (Util.equals(ll, w, dtTolerance) && equal.test(ii[i],what))
                return i;
            if (i == finalIndexExc || (forward && (ll > w)) || (!forward && ll < w))
                break; //past the target
        }
        return -1;
    }

    public String toItemString() { return super.toString(); }

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

    public boolean add(long w, X t) {
        return add(w, t, false);
    }


    protected  boolean add(long w, X t, boolean valueIfExists) {

        //check for existing
        int n = size();
        for (int i = 0; i < n; i++) {
            if (when(i) == w && get(i).equals(t))
                return valueIfExists; //existing found
        }

        addDirect(w, t);
        return true;
    }

    protected final void addDirect(long w, X t) {
        int s = addAndGetSize(t);

        //match long[] to the Object[] capacity
        long[] ww = this.when;
        if (ww.length < s) {
            this.when = ww = Arrays.copyOf(ww, items.length);
        }

        ww[s - 1] = w;
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
        int s = size();
        if (s == 0)
            return false;
        else {
            removeThe(0, s);
            return true;
        }
    }

    public boolean remove(long at, X t) {
        return removeIf((when, what) -> at == when && what.equals(t));
    }


    /**
     * removes the ith tuple
     */
    public final void removeThe(int i) {
        removeThe(i, size());
    }

    private void removeThe(int i, int s) {
        removeWhen(i, s);
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

    @Override
    public final boolean removeIf(Predicate<? super X> predicate) {
        return removeIf((when,what)->predicate.test(what));
    }

    @Override
    public final <P> boolean removeIfWith(Predicate2<? super X, ? super P> predicate, P parameter) {
        return removeIf((when,what)->predicate.test(what, parameter));
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

    public final boolean removeAll(int... indices) {
        int s = size;
        return removeAll(MetalBitSet.bits(s).set(indices), s);
    }

    private boolean removeAll(MetalBitSet m, int s) {
        int toRemove = Math.min(m.cardinality(),s);
        if (toRemove == 0)
            return false;
        int next = -1, removed = 0;
        while (toRemove > 0) {
             next = m.next(true, next + 1, s);
             removeThe(next - removed);
             removed++;
             toRemove--;
        }
        return true;
    }

    @Override
    public final boolean removeNulls() {
        return removeIf((when,what)->what==null);
    }

    @Override
    public X removeLast() {
        removeThe(size-1);
        return null; //TODO
    }

    @Override
    public void reverse() {
        int s = size;
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

    public final long when(int i) {
        return when[i];
    }

    public final void removeAll(MetalBitSet b) {
        removeAll(b, size());
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
