package nars.term.util.conj;

import jcog.WTF;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.builder.TermBuilder;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;

import static nars.Op.CONJ;
import static nars.time.Tense.*;

public interface ConjBuilder {
    /**
     * returns false if contradiction occurred, in which case this
     * ConjEvents instance is
     * now corrupt and its result via .target() should be considered final
     */
    default boolean add(long at, Term x) {
        if (at == DTERNAL || at == XTERNAL)//HACK TEMPORARY
            throw new WTF("probably meant ETERNAL or TIMELESS");
        if (at == TIMELESS)
            throw new WTF("invalid time");

        return
                (x instanceof Compound && x.op() == CONJ) ?
                        addConjEvent(at, x)
                        :
                        addEvent(at, x)
        ;
    }
    default boolean addAll(Iterable<LongObjectPair<Term>> x) {
        for (LongObjectPair<Term> xx : x) {
            if (!add(xx))
                return false;
        }
        return true;
    }

    default boolean add(LongObjectPair<Term> whenWhat) {
        return add(whenWhat.getOne(), whenWhat.getTwo());
    }


    /** for internal use only */
    boolean addEvent(long at, Term x);

    int eventOccurrences();

    default boolean remove(LongObjectPair<Term> e) { return remove(e.getOne(), e.getTwo()); }
    boolean remove(long at, Term t);
    boolean removeAll(Term term);

    int eventCount(long when);

    void negateEvents();

    default Term term() {
        return term(Op.terms);
    }

    Term term(TermBuilder b);


    LongIterator eventOccIterator();

    /** private use only */
    default boolean addConjEvent(long at, Term x) {

        int xdt = x.dt();
//        if (xdt == DTERNAL) {
//            if (at == ETERNAL) {
//
//                Subterms tt = x.subterms();
//                if (tt.hasAny(CONJ)) {
//                    //add any contained sequences first
//                    return tt.AND(ttt ->
//                            (ttt.op() != CONJ || Tense.dtSpecial(ttt.dt())) || add(0, ttt)
//                    ) && tt.AND(ttt ->
//                            (ttt.op() == CONJ && !Tense.dtSpecial(ttt.dt())) || add(ETERNAL, ttt)
//                    );
//                }
//
//            }
//        }


        if (xdt!=XTERNAL && (at != ETERNAL || (xdt == 0) || (xdt == DTERNAL))) {

            if (at == ETERNAL && Conj.isSeq(x))
                at = 0;

            return x.eventsWhile(this::addEvent, at,
                    at != ETERNAL, //unpack parallel except in DTERNAL root, allowing: ((a&|b) && (c&|d))
                    true,
                    false);
        } else {
            return addEvent(at, x);
        }
    }

    default boolean addAuto(Term t) {
        return add(t.dt() == DTERNAL ? ETERNAL : 0, t);
    }

    default long shiftOrZero() {
        long s = shift();
        if (s == ETERNAL)
            return 0;
        else {
            assert(s!=TIMELESS);
            return s;
        }
    }

    default long shift() {
        long min = TIMELESS;
        LongIterator ii = eventOccIterator();
        while (ii.hasNext()) {
            long t = ii.next();
            if (t != ETERNAL) {
                if (t < min)
                    min = t;
            }
        }
        return min == Long.MAX_VALUE ? ETERNAL : min;
    }
}
