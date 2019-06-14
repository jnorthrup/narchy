package nars.term.util.conj;

import jcog.Util;
import jcog.data.set.LongObjectArraySet;
import nars.NAL;
import nars.subterm.DisposableTermList;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Term;
import nars.term.util.TermException;
import nars.term.util.builder.InterningTermBuilder;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.Set;

import static nars.Op.CONJ;
import static nars.Op.NEG;
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

    public static ConjList subtract(ConjList from, Term conj) {
        return subtract(from, conj, TIMELESS);
    }

    public static ConjList events(Term conj, long occOffset) {
        occOffset = occAuto(conj, occOffset);

        ConjList l = new ConjList();
        conj.eventsAND(l::add,
                occOffset, true, false);
        return l;
    }

    public static ConjList subtract(ConjList from, Term conj, long occOffset) {
        conj.eventsAND((when, what) -> {
            if (when == ETERNAL)
                from.removeAll(what);
            else
                from.remove(when, what);
            return true;
        }, occOffset, true, false);
        return from;
    }

    private static long occAuto(Term conj, long occOffset) {
        return occOffset == TIMELESS ? (Conj.isSeq(conj) ? 0 : ETERNAL) : occOffset;
    }

    /**
     * consistent with ConjBuilder - semantics slightly different than superclass and List.addAt: returns true only if False or Null have been added; a duplicate value returns true
     */
    @Override
    public boolean addEvent(long when, Term t) {

        if (t == False || t == Null)
//            clear(); //fail
            return false;


        boolean result = true;
        //quick chest for absorb or conflict
        int n = size();
        for (int i = 0; i < n; i++) {
            long ww = when(i);
            if (ww == ETERNAL || ww == when) {
                Term ii = get(i);
                if (ii.equals(t))
                    return true; //exists

                if (ii.equalsNeg(t)) {
//                clear();
                    return false; //conflict
                }
            }
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
        int s = size();
        int c = 0;
        for (int i = 0; i < s; i++)
            if (this.when[i] == w)
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

        if (n == 1)
            return get(0);




        if (B instanceof InterningTermBuilder && NAL.CONJ_COMMUTIVE_LOOPBACK) {
            long w0 = when[0];
            boolean parallel = true;
            for (int i = 1, whenLength = when.length; i < whenLength; i++) {
                if (when[i] != w0) {
                    parallel = false;
                    break; //difference
                }
            }
            //all same time
            if (parallel) {
                return B.conj( toArrayRecycled(Term[]::new));
                //return ConjCommutive.the(B, DTERNAL /*(w0 == ETERNAL) ? DTERNAL : 0*/, true, false, toArrayRecycled(Term[]::new));
            }
        }

        if (n == 2) {
            //sequence shortcut
            long wa = when[0], wb = when[1];
            if (wa !=ETERNAL && wb !=ETERNAL && when[0]!=when[1]) {
                Term a = get(0);
                if (!a.hasAny(CONJ)) {
                    Term b = get(1);
                    if (!b.hasAny(CONJ)) {
                        if (!a.equals(b)) {
                            return B.conjAppend(a, Tense.occToDT(wb - wa), b);
                        }
                    }
                }
            }
        }

        sortThis(); //puts eternals first, and organizes contiguously timed items

        //failsafe impl:
        ConjBuilder c = new ConjTree();
        for (int i = 0; i < n; i++) {
            if (!c.add(when[i], this.get(i)))
                break;
        }

        return c.term(B);
    }


    public final Subterms asSubterms(boolean commute) {
        DisposableTermList terms = new DisposableTermList(this.items);
        if (commute)
            terms.sortAndDedup();
        return terms;
    }

    public final boolean removeNeg(long at, Term t) {
        return removeIf((when, what) -> at == when && t.equalsNeg(what));
    }

    /**
     * returns true if something removed
     */
    public int removeAll(Term x, long offset, boolean polarity) {

        if (x.op() == CONJ && x.dt() != XTERNAL) {
            //remove components
            final boolean[] removed = {false};
            if (!x.eventsAND((when, what) -> {
                if (contains(when, what.negIf(polarity)))
                    return false; //contradiction

                removed[0] |= remove(when, what.negIf(!polarity));
                return true;
            }, offset, true, false))
                return -1;
            return removed[0] ? +1 : 0;
        } else {
            if (remove(offset, x.negIf(polarity)))
                return -1;
            if (remove(offset, x.negIf(!polarity)))
                return 1;
            return 0;
        }
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
        Set<Term> uncount = null;
        for (int i = 0; i < n; i++) {
            Term xi = get(i);
            if (count.containsKey(xi.neg())) {
                if (uncount == null) uncount = new UnifiedSet(n);
                uncount.add(xi.unneg());
                continue;
            }
            count.getIfAbsentPut(xi, RoaringBitmap::new).add(Tense.occToDT(when(i)));
        }


        if (count.allSatisfy(t->t.getCardinality()==u)) {
            //completely annihilates everything
            //so also remove any occurring in the parallel events
            T.removeParallel(count.keySet());
        } else {

            if (uncount != null) {
                for (Term uc : uncount) {
                    count.removeKey(uc);
                    count.removeKey(uc.neg());
                }
                if (count.isEmpty())
                    return;
            }

            TermList toDistribute = new TermList(n);
            if (!count.keyValuesView().toSortedList().allSatisfy((xcc) -> {
                Term x = xcc.getOne();
                RoaringBitmap cc = xcc.getTwo();
                int c = cc.getCardinality();
                if (c < u) {
                    if (x.op() != NEG) {
                        if (T.pos != null && T.posRemove(x))
                            toDistribute.add(x);
                    } else {
                        if (T.neg != null && T.negRemove(x.unneg()))
                            toDistribute.add(x);
                    }
                } else {
                    PeekableIntIterator ei = cc.getIntIterator();
                    while (ei.hasNext()) {
                        if (eventCount(ei.next()) == 1)
                            return true; //factoring would erase this event so ignore it
                    }
                    //new factor component
                    if (!T.addParallel(x))
                        return false;
                    removeAll(x);
                }
                return true;
            })) {
                T.terminate(False);
                return;
            }


            int dd = toDistribute.size();
            if (dd > 0) {
//            if(dd > 1)
//                toDistribute.sortAndDedup();

                n = size();

                //distribute partial factors
                for (int i = 0; i < n; i++) {
                    Term xf = get(i);
                    if (dd == 1 && toDistribute.sub(0).equals(xf))
                        continue;

                    Term[] t = new Term[dd + 1];
                    toDistribute.arrayClone(t);
                    t[t.length - 1] = xf;
                    Term xd = CONJ.the(t);
                    if (xd == False || xd == Null) {
                        T.terminate(xd);
                        return;
                    }
                    set(i, xd);
                }
            }
        }

        condense(B);


    }

    /** counts # of unique occurrence times, assuming that the events have already been sorted by them */
    private int eventOccurrences_if_sorted() {
        int c = 1;
        long x = when[0];
        for (int i = 1; i < size; i++) {
            long y = when[i];
            if (y != x) {
                c++;
                x = y;
            }
        }
        return c;
    }

    /** combine events at the same time into parallel conjunctions
     * @param b*/
    public void condense(TermBuilder B) {
        int s = size();
        if (s <= 1)
            return;

//        sortThis();

        int start = 0;
        long last = when[0];
        for (int i = 1; i < s; i++) {
            long wi = when[i];
            if (i < s && last!= wi) {
                last = wi;
                start = i;
            } else {
                if (i > start) {

                    Term x = B.conj(Arrays.copyOfRange(array(), start, i + 1));
                    if (x == True) {
                        //handled below HACK
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


    }


    public int centerByVolume(int startIndex, int endIndex) {
        int n= endIndex - startIndex;
        int midIndex = centerByIndex(startIndex, endIndex);
        if (n <= 2)
            return midIndex;
        int v[] = new int[n];

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
}
