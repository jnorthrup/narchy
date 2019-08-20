package nars.term.util.conj;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.set.LongObjectArraySet;
import nars.subterm.DisposableTermList;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.util.TermException;
import nars.term.util.builder.InterningTermBuilder;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static jcog.util.ArrayUtil.EMPTY_INT_ARRAY;
import static nars.Op.CONJ;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * prepares construction of a conjunction target from components,
 * in the most efficient method possible according to them.
 * it is lighter weight than Conj.java in buffering / accumulating
 * events prior to complete construction.
 */
public class ConjList extends LongObjectArraySet<Term> implements ConjBuilder {

    public ConjList() {
        this(4);
    }

    public ConjList(int expectedSize) {
        super(0, new Term[expectedSize]);
    }

    public static ConjList events(Term conj) {
        return events(conj, TIMELESS);
    }

//    public static ConjList subtract(ConjList from, Term conj) {
//        return subtract(from, conj, TIMELESS);
//    }

    public static ConjList events(Term conj, long occOffset) {
        occOffset = occOffset == TIMELESS ? 0 : occOffset;

        ConjList l = new ConjList();
        conj.eventsAND(l::add, occOffset, true, false);
        return l;
    }

    @Override
    public boolean removeAll(Term x) {
        return removeIf((Predicate)x::equals);
    }

    public static ConjList subtract(ConjList from, Term conj, long occOffset) {
        conj.eventsAND(
            occOffset == ETERNAL ?
                (when,what)->{ from.removeAll(what); return true; } :
                (when,what)->{ from.remove(when,what); return true; }
        , occOffset, true, false);
        return from;
    }

    /** assumes its sorted */
    private long _start() {
        return when[0];
    }
    /** assumes its sorted */
    private long _end() {
        return when[size-1];
    }

    public boolean contains(ConjList other) {
        return contains(other, Term::equals);
    }

    public boolean contains(ConjList other, BiPredicate<Term,Term> equal) {
        return contains(other, 1, equal).length > 0;
    }
    public int[] contains(ConjList other, int maxResults, BiPredicate<Term,Term> equal) {
        return contains(other, equal, maxResults, true, null, 1);
    }

    /** assumes they are both sorted and/or expanded/condensed in the same way
     * warning: if not forward, both arrays will be reversed.  a restoration by re-reversing is not performed TODO
     *
     * */
    public int[] contains(ConjList other, BiPredicate<Term,Term> equal, int maxLocations, boolean forward /* or reverse */, @Nullable MetalBitSet hit, int dtTolerance) {
        if (this == other) return new int[] { 0 };

        int s = size();
        int os = other.size();
        if (os > s) return EMPTY_INT_ARRAY;
        if (s == 0) return new int[] { 0 };

        other.sortThis();
        sortThis();
        if (other._start() < _start() || other._end() > _end())
            return EMPTY_INT_ARRAY;

        Term otherFirst = other.get(forward ? 0 : os-1);
        IntArrayList locations = null;
        long[] when = this.when;

        int testSpan = s - os;
        for (int j = 0; j <= testSpan; j++) {

            int from = forward ? j : (s - 1 - j);
            if (equal.test(otherFirst, get(from))) {
                if (containsRemainder(other, from, forward, equal, hit, dtTolerance)) {
                    if (locations == null)
                        locations = new IntArrayList(maxLocations!=Integer.MAX_VALUE ? maxLocations : 1);
                    locations.add(Tense.occToDT(when[from]));
                    if (locations.size() >= maxLocations)
                        break;
                }
            }

            if (equal instanceof EventUnifier)
                ((EventUnifier)equal).clear();
            if (hit!=null) hit.clear();
        }
        return locations==null ? EMPTY_INT_ARRAY : locations.toArray();
    }

    private boolean containsRemainder(ConjList other, int start, boolean forward, BiPredicate<Term, Term> equal, @Nullable MetalBitSet hit, int dtTolerance) {
        if (hit!=null) hit.set(start);

        int os = other.size();
        if (os == 1)
            return true; //already matched the first term

        long[] oWhens = other.when;
        Term[] oItems = other.items;
        int oo = forward ? 0 : os - 1;
        long shift = when[start] - oWhens[oo];
        int next = start;
        int s = this.size;
        int end = forward ? s -1 : 0;
        for (int i = 1; i < os; i++) {
            oo += forward ? 1 : -1;
            next += forward ? 1 : -1;
            if (next < 0 || next >= s)
                return false;
            next = _indexOf(oWhens[oo]+shift, oItems[oo], next, end, equal, dtTolerance);
            if (next == -1)
                return false;
            if (hit!=null)
                hit.set(next);
        }
        return true;
    }


    /**
     * consistent with ConjBuilder - semantics slightly different than superclass and List.addAt: returns true only if False or Null have been added; a duplicate value returns true
     */
    @Override
    public boolean addEvent(long when, Term t) {

        if (t == False || t == Null) {
            //throw new UnsupportedOperationException();
            clear(); //fail
            return false;
        }

        //quick chest for absorb or conflict
        int n = size();
        if (n > 0) {
            long[] W = this.when;
            Term[] X = this.items;
            boolean exists = false;
            for (int i = 0; i < n; i++) {
                long ww = W[i];
                if (ww == ETERNAL || ww == when) {
                    Term ii = X[i];
                    if (ii.equals(t)) {
                        exists = true;
                    } else {
                        if (ii.equalsNeg(t)) {
                            clear();
                            return false; //conflict
                        }
                    }
                }
            }
            if (exists)
                return true;
        }

        addDirect(when, t);
        return true;
    }

    @Override
    public final boolean add(long w, Term t) {
        return ConjBuilder.super.add(w, t);
    }



    @Override
    public int eventOccurrences() {
        int s = size();
        long[] when = this.when;
        switch (s) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                //quick tests
                return when[0] == when[1] ? 1 : 2;
            case 3:
                //quick tests
                if (when[0] == when[1])
                    return when[1] == when[2] ? 1 : 2;
                return when[1] == when[2] ? 2 : 3;
            default: {
                LongHashSet h = null;
                long first = when[0];
                for (int i = 1; i < s; i++) {
                    if (h == null) {
                        if (when[i] != first) {
                            h = new LongHashSet(s - i + 1);
                            h.add(first);
                        }
                    }
                    if (h!=null)
                        h.add(when[i]);
                }
                return h!=null ? h.size() : 1;
            }
        }
    }

    @Override
    public int eventCount(long w) {
        int s = size;
        int c = 0;
        long[] when = this.when;
        for (int i = 0; i < s; i++)
            if (when[i] == w)
                c++;
        return c;
    }

    @Override
    public final LongIterator eventOccIterator() {
        return longIterator();
    }

    @Override
    public void negateEvents() {
        replaceAll(Term::neg);
    }

    @Override
    public Term term(TermBuilder B) {
        int n = size();
        if (n == 0)
            return True;

        Term[] items = this.items;

        if (n == 1)
            return items[0];

        long[] when = this.when;


        if (B instanceof InterningTermBuilder) {
            long w0 = when[0];
            boolean allParallel = true;
            for (int i = 1, whenLength = when.length; i < whenLength; i++) {
                if (when[i] != w0) {
                    allParallel = false;
                    break; //difference
                }
            }
            //all same time
            if (allParallel) {
                return B.conj( toArrayRecycled(Term[]::new));
                //return ConjCommutive.the(B, DTERNAL /*(w0 == ETERNAL) ? DTERNAL : 0*/, true, false, toArrayRecycled(Term[]::new));
            }
        }


        if (n == 2) {
            //sequence shortcut
            long wa = when[0], wb = when[1];
            if (wa !=ETERNAL && wb !=ETERNAL && when[0]!=when[1]) {
                Term a = items[0];
                if (!a.hasAny(CONJ)) {
                    Term b = items[1];
                    if (!b.hasAny(CONJ)) {
                        if (!a.equals(b)) {
                            return B.conjAppend(a, Tense.occToDT(wb - wa), b);
                        }
                    }
                }
            }
        }

//        sortThis(); //puts eternals first, and organizes contiguously timed items
//        condense(B);
//        n = size();




        //failsafe impl:
        ConjBuilder c = new ConjTree();
        for (int i = 0; i < n; i++) {
            if (!c.add(when[i], items[i]))
                break;
        }

        return c.term(B);

    }


    public final Subterms asSubterms(boolean commute) {
        DisposableTermList terms = new DisposableTermList(size, this.items);
        if (commute)
            terms.sortAndDedup();
        return terms;
    }

//    public final boolean removeNeg(long at, Term t) {
//        return removeIf((when, what) -> at == when && t.equalsNeg(what));
//    }

    /**
     * returns true if something removed
     */
    public int removeAll(Term x, long offset, boolean polarity) {

//        if (x.op() == CONJ && x.dt() != XTERNAL) {
            //remove components
            final boolean[] removed = {false};
            if (x.eventsOR((when, what) -> {
                if (contains(when, what.negIf(polarity)))
                    return true; //contradiction

                removed[0] |= remove(when, what.negIf(!polarity));
                return false;
            }, offset, true, x.dt()==XTERNAL))
                return -1;

            return removed[0] ? +1 : 0;
//        } else {
//            if (remove(offset, x.negIf(polarity)))
//                return -1;
//            if (remove(offset, x.negIf(!polarity)))
//                return 1;
//            return 0;
//        }
    }


    void factor(ConjTree T, TermBuilder B) {

        int n = size();
        if (n < 2)
            return;

        sortThis();
        int u = eventOccurrences_if_sorted();
//        if (u == n) {
//            condense(B);
//            return;
//        }

        UnifiedMap<Term, RoaringBitmap> count = new UnifiedMap(n);
//        Set<Term> uncount = null;
        for (int i = 0; i < n; i++) {
            Term xi = get(i);
//            if (count.containsKey(xi.neg())) {
//                if (uncount == null) uncount = new UnifiedSet(n);
//                uncount.add(xi.unneg());
//                continue;
//            }
            count.getIfAbsentPut(xi, RoaringBitmap::new).add(Tense.occToDT(when(i)));
        }


        if (count.allSatisfy(t->t.getCardinality()==u)) {
            //completely annihilates everything
            //so also remove any occurring in the parallel events
            //T.removeParallel(count.keySet());
        } else {

//            if (uncount != null) {
//                for (Term uc : uncount) {
//                    count.removeKey(uc);
//                    count.removeKey(uc.neg());
//                }
//                if (count.isEmpty())
//                    return;
//            }

//            TermList toDistribute = new TermList(n);
            if (!count.keyValuesView().toSortedList().allSatisfy((xcc) -> {
                RoaringBitmap cc = xcc.getTwo();
                int c = cc.getCardinality();
                if (c < u) {
//                    if (x.op() != NEG) {
//                        if (T.pos != null && T.posRemove(x))
//                            toDistribute.add(x);
//                    } else {
//                        if (T.neg != null && T.negRemove(x.unneg()))
//                            toDistribute.add(x);
//                    }
                } else {
                    PeekableIntIterator ei = cc.getIntIterator();
                    while (ei.hasNext()) {
                        if (eventCount(ei.next()) == 1)
                            return true; //factoring would erase this event so ignore it
                    }
                    Term x = xcc.getOne();
                    //new factor component
                    if (!T.addParallel(x))
                        return false;
                    removeAll(x);
                }
                return true;
            })) {
                T.terminate(False);
            }



//            int dd = toDistribute.size();
//            if (dd > 0) {
////            if(dd > 1)
////                toDistribute.sortAndDedup();
//
//                n = size();
//
//                //distribute partial factors
//                for (int i = 0; i < n; i++) {
//                    Term xf = get(i);
//                    if (dd == 1 && toDistribute.sub(0).equals(xf))
//                        continue;
//
//                    Term[] t = new Term[dd + 1];
//                    toDistribute.arrayClone(t);
//                    t[t.length - 1] = xf;
//                    Term xd = CONJ.the(t);
//                    if (xd == False || xd == Null) {
//                        T.terminate(xd);
//                        return;
//                    }
//                    set(i, xd);
//                }
//            }
        }



    }

    /** counts # of unique occurrence times, assuming that the events have already been sorted by them */
    private int eventOccurrences_if_sorted() {
        int c = 1;
        long[] when = this.when;
        long x = when[0];
        int s = this.size;
        for (int i = 1; i < s; i++) {
            long y = when[i];
            if (y != x) {
                assert(y > x);
                c++;
                x = y;
            }
        }
        return c;
    }

    /** combine events at the same time into parallel conjunctions
     * assumes the list is already sorted
     * @param b*/
    public boolean condense(TermBuilder B) {
        int s = size();
        if (s <= 1)
            return true;

        //sortThis();
        long[] when = this.when;
        int start = 0;
        long last = when[0];
        for (int i = 1; i < s; i++) {
            long wi = when[i];
            if (last != wi) {
                last = wi;
                start = i;
            } else {
                if (i > start) {

                    Term x = B.conj(Arrays.copyOfRange(array(), start, i + 1));
                    if (x == True) {
                        //handled below HACK
                    } else if (x == False || x == Null) {
                        return false;
                    } else if (!x.op().eventable) {
                        throw new TermException("conj collapse during condense", CONJ, x);
                    } else {
                        set(start, x);
                    }

                    for (int r = 0; r < (i - start); r++) {
                        removeThe(start + 1);
                        s--;
                        i--;
                    }

                    if (x == True) {
                        removeThe(start); s--; i--;
                    }

                }

            }
        }

        return true;

    }


    public int centerByVolume(int startIndex, int endIndex) {
        int n= endIndex - startIndex;
        int midIndex = centerByIndex(startIndex, endIndex);
        if (n <= 2)
            return midIndex;
        int[] v = new int[n];

        for (int i = 0; i < n; i++) {
            v[i] = get(startIndex + i).volume();
        }
        int bestSplit = 1, bestSplitDiff = Integer.MAX_VALUE;
        for (int i = 1; i < n-1; i++) {
            int pd = Math.abs(Util.sum(v, 0, i) - Util.sum(v, i, n));
            if (pd <= bestSplitDiff) {
                bestSplit = i;
                bestSplitDiff = pd;
            }
        }

        return bestSplit + startIndex;

    }

    public int centerByIndex(int startIndex, int endIndex) {
        return startIndex + (endIndex - 1 - startIndex) / 2;
    }

    /** shifts everything so that the initial when is zero. assumes it is non-empty & sorted already */
    public void shift(long shiftFrom) {
        long currentShift = shift();
        long delta = shiftFrom - currentShift;
        if (delta == 0)
            return;
        long[] when = this.when;
        int s = this.size;
        for (int k = 0; k < s; k++)
            when[k] += delta;
    }

    boolean removeAllAt(int f, ConjList x) {
        int xn = x.size();
        boolean removed = false;
        for (int i = 0; i < xn; i++) {
            removed |= remove(x.when[i] + f, x.get(i));
        }
        return removed;
    }
}
