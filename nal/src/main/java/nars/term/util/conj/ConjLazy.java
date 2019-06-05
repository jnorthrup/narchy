package nars.term.util.conj;

import jcog.WTF;
import jcog.data.set.LongObjectArraySet;
import nars.Task;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Term;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * prepares construction of a conjunction target from components,
 * in the most efficient method possible according to them.
 * it is lighter weight than Conj.java in buffering / accumulating
 * events prior to complete construction.
 */
public class ConjLazy extends LongObjectArraySet<Term> implements ConjBuilder {

    public ConjLazy() {
        this(4);
    }

    public ConjLazy(int expectedSize) {
        super(0, new Term[expectedSize]);
    }






    /** TODO add support for supersampling to include task.end() features */
    public static Term sequence(Task[] events, int ditherDT) {
        int eventsSize = events.length;
        switch (eventsSize) {
            case 0:
                return True;
            case 1:
                return sequenceTerm(events[0]);
        }

        ConjBuilder ce =
                new ConjLazy(eventsSize);
                //new ConjTree();

        for (Task o : events) {
            if (!ce.add(Tense.dither(o.start(), ditherDT), sequenceTerm(o))) {
                break;
            }
        }

        return ce.term();
    }

    private static Term sequenceTerm(Task o) {
        return o.term().negIf(o.isNegative());
    }

    /**
     * consistent with ConjBuilder - semantics slightly different than superclass and List.addAt: returns true only if False or Null have been added; a duplicate value returns true
     */
    @Override
    public boolean addEvent(long when, Term t) {
        if (when == TIMELESS)
            throw new WTF();

        boolean result = true;
        if (t == True)
            return true; //ignore

        if (t == False || t == Null) {
            clear(); //fail
            return false;
        }

        //quick chest for conflict
        int n = size();
        for (int i = 0; i < n; i++) {
            if (when(i) == when && get(i).equalsNeg(t)) {
                clear();
                return false; //conflict
            }
        }

        return add(when, t, true);
    }

    @Override
    public final boolean add(long w, Term t) {
        return ConjBuilder.super.add(w,t);
    }

    @Override
    public int eventOccurrences() {
        int s = size();
        if (s == 0) return 0;
        else if (s == 1) return 1;
        else if (s == 2) {
            //quick tests
            if (when[0] == when[1]) return 1;
            else return 2;
        } else if (s == 3) {
            //quick tests
            boolean a01 = when[0] == when[1];
            if (a01) {
                if (when[1] == when[2]) return 1;
                else return 2;
            }
            return when[1] == when[2] ? 2 : 3;
        }

        LongHashSet h = new LongHashSet(s);
        for (int i = 0; i < s; i++) {
            h.add(when[i]);
        }
        return h.size();
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

    public static ConjLazy events(Term conj) {
        return events(conj, TIMELESS);
    }
    public static ConjLazy subtract(ConjLazy from, Term conj) {
        return subtract(from, conj, TIMELESS);
    }

    public static ConjLazy events(Term conj, long occOffset) {
        occOffset = occAuto(conj, occOffset);

        ConjLazy l = new ConjLazy();
        conj.eventsWhile(l::add,
                occOffset, true, false);
        return l;
    }

    public static ConjLazy subtract(ConjLazy from, Term conj, long occOffset) {
        conj.eventsWhile(from::remove,
                occOffset, true, false);
        return from;
    }

    private static long occAuto(Term conj, long occOffset) {
        return occOffset == TIMELESS ? Conj.isSeq(conj) ? 0 : ETERNAL : occOffset;
    }



    @Override
    public void negateEvents() {
        replaceAll(Term::neg);
    }

    @Override
    public Term term(TermBuilder B) {
        int n = size();
        switch (n) {
            case 0:
                return True;
            case 1:
                return get(0);
            case 2: {
                long w0 = when[0], w1 = when[1];

//                if ((w0 == ETERNAL) ^ (w1 == ETERNAL)) {
//                    w0 = w1 = ETERNAL; //auto collapse ot dternal
//                    //w0 = w1 = 0; //auto promote to parallel
//                }

                if (w0 == w1) {
                    //SAME TIME
                    Term a = items[0], b = items[1];
                    //return CONJ.the(B, DTERNAL /*(w0 == ETERNAL) ? DTERNAL : 0*/, a, b);
                    return ConjCommutive.the(B, DTERNAL /*(w0 == ETERNAL) ? DTERNAL : 0*/, true, false, items[0], items[1]);
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
                    return ConjCommutive.the(B, DTERNAL /*(w0 == ETERNAL) ? DTERNAL : 0*/, true, false, toArrayRecycled(Term[]::new));
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
        TermList terms = new TermList(this.items);
        if (commute)
            terms.sortAndDedup();
        return terms;
    }

    public final boolean removeNeg(long at, Term t) {
        return removeIf((when, what) -> at == when && t.equalsNeg(what));
    }
}
