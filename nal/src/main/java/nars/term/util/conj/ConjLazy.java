package nars.term.util.conj;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtils;
import nars.term.Term;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.util.Arrays;
import java.util.NoSuchElementException;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * prepares construction of a conjunction term from components,
 * in the most efficient method possible according to them.
 * it is lighter weight than Conj.java in buffering / accumulating
 * events prior to complete construction.
 */
public class ConjLazy extends FasterList<Term> implements ConjBuilder {
    private long[] when;

    public ConjLazy(int expectedSize) {
        super(expectedSize);
        when = new long[expectedSize];
    }

    public ConjLazy() {
        this(3);
    }

    public static ConjLazy events(Term conj) {
        return events(conj, 0);
    }

    public static ConjLazy events(Term conj, long occOffset) {
        ConjLazy l = new ConjLazy(conj.op() == CONJ ? 4 : 1);
        conj.eventsWhile(l::add,
                occOffset, true, true, false, 0);
        return l;
    }

//        private Object[] copyItemsWithNewCapacity(int newCapacity) {
////        Object[] newItems = newArray(newCapacity);
////        System.arraycopy(this.items, 0, newItems, 0, Math.min(this.size, newCapacity));
////        return newItems;
//            return items != null ? Arrays.copyOf(items, newCapacity) : newArray(newCapacity);
//        }

    /**
     * List semantics are changed in this overridden method. this is just an alias for the add(long, Term) method
     */
    @Override
    public void add(int when, Term t) {
        this.add((long) when, t);
    }

    @Override
    public boolean add(long when, Term t) {
        if (t == True)
            return true; //ignore

        boolean result = true;
        if (t == False || t == Null) {
            clear(); //fail
            result = false;
        } else {
            //quick check for existing
            int n = size();
            for (int i = 0; i < n; i++) {
                if (when(i) == when && get(i).equals(t))
                    return true; //existing found
            }
        }

        int s = addAndGetSize(t);
        if (this.when.length < s)
            this.when = Arrays.copyOf(this.when, s);
        this.when[s - 1] = when;

        return result;
    }

    @Override
    public final boolean add(Term newItem) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Term remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(long at, Term t) {
        return removeIf((when, what) -> at == when && what.equals(t));
    }

    public void removeEventFast(int i) {
        long w = when[i];

        removeWhen(i, size());

        super.removeFast(i);
    }

    public LongObjectPair<Term> removeEvent(int i) {
        long w = when[i];

        removeWhen(i, size());

        Term x = super.remove(i);

        return PrimitiveTuples.pair(w, x);
    }

    private void removeWhen(int i, int s) {
        if (i < s - 1)
            System.arraycopy(when, i + 1, when, i, s - i - 1);
    }

    public boolean removeIf(LongObjectPredicate<Term> iff) {
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
            switch (toRemove) {
                case 0:
                    return false;
                case 1:
                    removeWhen(0, s);
                    super.removeFast(m.first(true));
                    return true;
                default:

                    for (int i = m.first(true); i < s;  /* TODO iterate bitset better */) {
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

    @Override
    public boolean removeAll(Term term) {
        switch (size()) {
            case 0:
                return false;
            case 1:
                if (get(0).equals(term)) {
                    removeEventFast(0);
                    return true;
                }
            default:
                return removeIf((when, what) -> what.equals(term));
        }

    }

    @Override
    public void negateEvents() {
        replaceAll(Term::neg);
    }

    @Override
    protected final Object[] newArray(int newCapacity) {
        return new Term[newCapacity];
    }

    @Override
    public Term term() {
        int n = size();
        switch (n) {
            case 0:
                return True;
            case 1:
                return get(0);
            case 2: {
                long w0 = when[0], w1 = when[1];

                if (w0 == ETERNAL && w1 != ETERNAL)
                    w0 = w1 = 0; //quick promote to parallel
                else if (w1 == ETERNAL && w0 != ETERNAL)
                    w0 = w1 = 0; //quick promote to parallel

                if (w0 == w1) {
                    Term a = items[0], b = items[1];
                    if (a.equals(b))
                        return a; //quick test
                    else
                        return ConjCommutive.the((w0 == ETERNAL) ? DTERNAL : 0, a, b);
                }
                break;
            }
            default: {
                long w0 = when[0];
                boolean parallel = true;
                for (int i = 1, whenLength = when.length; i < whenLength; i++) {
                    if (when[i] != w0) {
                        parallel = false;
                        break; //difference
                    }
                }
                //all same time
                if (parallel)
                    return ConjCommutive.the((w0 == ETERNAL) ? DTERNAL : 0, this);
            }
        }

        //failsafe impl:
        Conj c = new Conj(n);
        for (int i = 0; i < n; i++) {
            Term t = this.get(i);
            if (!c.add(when[i], t))
                break;
        }
        return c.term();
    }

    @Override
    public LongIterator eventOccIterator() {
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
