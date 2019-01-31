package nars.term.util.conj;

import jcog.WTF;
import nars.term.Compound;
import nars.term.Term;
import nars.time.Tense;
import org.eclipse.collections.api.iterator.LongIterator;

import static nars.Op.CONJ;
import static nars.time.Tense.*;

public interface ConjBuilder {
    /**
     * returns false if contradiction occurred, in which case this
     * ConjEvents instance is
     * now corrupt and its result via .target() should be considered final
     */
    default boolean add(long at, Term x) {
        if (at == DTERNAL || at == XTERNAL)//TEMPORARY
            throw new WTF("probably meant ETERNAL or TIMELESS");

        return
                (x instanceof Compound && x.op() == CONJ) ?
                        addConjEvent(at, x)
                        :
                        addEvent(at, x)
        ;
    }

    /** for internal use only */
    boolean addEvent(long at, Term x);

    int eventOccurrences();

    boolean remove(long at, Term t);
    boolean removeAll(Term term);

    int eventCount(long when);

    void negateEvents();

    Term term();


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

    default int shiftOrDTERNAL() {
        return eventOccurrences() == 1 && eventCount(ETERNAL) > 0 ? DTERNAL : Tense.occToDT(shift());
    }

    default long shift() {
        long min = Long.MAX_VALUE;
        LongIterator ii = eventOccIterator();
        while (ii.hasNext()) {
            long t = ii.next();
            if (t != ETERNAL) {
                if (t < min)
                    min = t;
            }
        }
        return min == Long.MAX_VALUE ? 0 : min;
    }
}
