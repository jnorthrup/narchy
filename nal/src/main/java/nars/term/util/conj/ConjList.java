package nars.term.util.conj;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.set.LongObjectArraySet;
import nars.subterm.DisposableTermList;
import nars.subterm.Subterms;
import nars.term.Compound;
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
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static jcog.util.ArrayUtil.EMPTY_INT_ARRAY;
import static nars.Op.CONJ;
import static nars.term.atom.theBool.*;
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
        var l = new ConjList();
        conj.eventsAND(l::add,
            occOffset == TIMELESS ? 0 : occOffset,
            true, false);
        return l;
    }

    /** adds directly in case there is a co-negation conflict that would otherwise be caught by ordinary ConjList .add() */
    public static ConjList eventsXternal(Compound conj, long start) {
        var ee = conj.subterms();
        var l = new ConjList(ee.subs());
        for (var e : ee)
            l.addDirect(start, e);
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

        var s = size;
        var os = other.size;
        if (os > s) return EMPTY_INT_ARRAY;
        if (s == 0) return new int[] { 0 };

        other.sortThis();
        sortThis();
        if (other._start() < _start() || other._end() > _end())
            return EMPTY_INT_ARRAY;

        var otherFirst = other.get(forward ? 0 : os-1);
        IntArrayList locations = null;
        var when = this.when;

        var testSpan = s - os;
        for (var j = 0; j <= testSpan; j++) {

            var from = forward ? j : (s - 1 - j);
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

        var os = other.size;
        if (os == 1)
            return true; //already matched the first term

        var oWhens = other.when;
        var oItems = other.items;
        var oo = forward ? 0 : os - 1;
        var shift = when[start] - oWhens[oo];
        var next = start;
        var s = this.size;
        var end = forward ? s -1 : 0;
        for (var i = 1; i < os; i++) {
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
        var n = size;
        if (n > 0) {
            var W = this.when;
            var X = this.items;
            var exists = false;
            for (var i = 0; i < n; i++) {
                var ww = W[i];
                if (ww == ETERNAL || ww == when) {
                    var ii = X[i];
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
        var s = size();
        var when = this.when;
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
                var first = when[0];
                for (var i = 1; i < s; i++) {
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
        var s = size;
        var when = this.when;
        var count = IntStream.range(0, s).filter(i -> when[i] == w).count();
        return (int) count;
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
        var n = size;
        if (n == 0)
            return True;

        var items = this.items;

        if (n == 1)
            return items[0];

        var when = this.when;


        if (B instanceof InterningTermBuilder) {
            var w0 = when[0];
            var allParallel = IntStream.range(1, n).noneMatch(i -> when[i] != w0);
            //difference
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
                var a = items[0];
                if (!a.hasAny(CONJ)) {
                    var b = items[1];
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
        for (var i = 0; i < n; i++) {
            if (!c.add(when[i], items[i]))
                break;
        }

        return c.term(B);

    }


    public final Subterms asSubterms(boolean commute) {
        var terms = new DisposableTermList(size, this.items);
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
            boolean[] removed = {false};
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


    void factor(ConjTree T) {

        var n = size();
        if (n < 2)
            return;

        sortThis();
        var u = eventOccurrences_if_sorted();
//        if (u == n) {
//            condense(B);
//            return;
//        }

        UnifiedMap<Term, RoaringBitmap> count = new UnifiedMap(n);
//        Set<Term> uncount = null;
        for (var i = 0; i < n; i++) {
            var xi = get(i);
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
                var cc = xcc.getTwo();
                var c = cc.getCardinality();
                if (c < u) {
//                    if (x.op() != NEG) {
//                        if (T.pos != null && T.posRemove(x))
//                            toDistribute.add(x);
//                    } else {
//                        if (T.neg != null && T.negRemove(x.unneg()))
//                            toDistribute.add(x);
//                    }
                } else {
                    var ei = cc.getIntIterator();
                    while (ei.hasNext()) {
                        if (eventCount(ei.next()) == 1)
                            return true; //factoring would erase this event so ignore it
                    }
                    var x = xcc.getOne();
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
        var c = 1;
        var when = this.when;
        var x = when[0];
        var s = this.size;
        for (var i = 1; i < s; i++) {
            var y = when[i];
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
        var s = size();
        if (s <= 1)
            return true;

        //sortThis();
        var when = this.when;
        var start = 0;
        var last = when[0];
        for (var i = 1; i < s; i++) {
            var wi = when[i];
            if (last != wi) {
                last = wi;
                start = i;
            } else {
                if (i > start) {

                    var x = B.conj(Arrays.copyOfRange(array(), start, i + 1));
                    if (x == True) {
                        //handled below HACK
                    } else if (x == False || x == Null) {
                        return false;
                    } else if (!x.op().eventable) {
                        throw new TermException("conj collapse during condense", CONJ, x);
                    } else {
                        set(start, x);
                    }

                    for (var r = 0; r < (i - start); r++) {
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
        var n= endIndex - startIndex;
        var midIndex = centerByIndex(startIndex, endIndex);
        if (n <= 2)
            return midIndex;
        var v = new int[10];
        var count = 0;
        for (var i1 = 0; i1 < n; i1++) {
            var volume = get(startIndex + i1).volume();
            if (v.length == count) v = Arrays.copyOf(v, count * 2);
            v[count++] = volume;
        }
        v = Arrays.copyOfRange(v, 0, count);

        int bestSplit = 1, bestSplitDiff = Integer.MAX_VALUE;
        for (var i = 1; i < n-1; i++) {
            var pd = Math.abs(Util.sum(v, 0, i) - Util.sum(v, i, n));
            if (pd <= bestSplitDiff) {
                bestSplit = i;
                bestSplitDiff = pd;
            }
        }

        return bestSplit + startIndex;

    }

    public static int centerByIndex(int startIndex, int endIndex) {
        return startIndex + (endIndex - 1 - startIndex) / 2;
    }

    /** shifts everything so that the initial when is zero. assumes it is non-empty & sorted already */
    public void shift(long shiftFrom) {
        var currentShift = shift();
        var delta = shiftFrom - currentShift;
        if (delta == 0)
            return;
        var when = this.when;
        var s = this.size;
        for (var k = 0; k < s; k++)
            when[k] += delta;
    }

    boolean removeAllAt(int f, ConjList x) {
        var xn = x.size;
        var ww = x.when;
        var ii = x.items;
        var removed = false;
        for (var i = 0; i < xn; i++) {
            var remove = remove(ww[i] + f, ii[i]);
            removed = removed || remove;
        }
        return removed;
    }

    public long eventRange() {
        var n = size;
        if (n <= 1)
            return 0;
        return when[n-1] - when[0];
    }
}
