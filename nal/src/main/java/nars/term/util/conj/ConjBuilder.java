package nars.term.util.conj;

import jcog.WTF;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Bool;
import nars.term.util.TermException;
import nars.term.util.builder.TermBuilder;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;

import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.Null_Array;
import static nars.term.atom.Bool.True;
import static nars.time.Tense.*;

public interface ConjBuilder {

    Predicate<Term> isTemporalComponent = Conj::isSeq;
    Predicate<Term> isEternalComponent = isTemporalComponent.negate();

    static Term[] preSort(int dt, Term[] u) {

        switch (dt) {
            case 0:
            case DTERNAL:
                return ConjBuilder.preSorted(u);

            case XTERNAL:
                Term[] v = ConjBuilder.preSorted(u);
                if (v.length == 1 && !(v[0] instanceof Bool)) {
                    if (/*!(v[0] instanceof Ellipsislike) || */(u.length > 1 && u[0].equals(u[1])))
                        return new Term[]{v[0], v[0]};
                }
                return v;

            default:
                return u;
        }
    }

    static Term[] preSorted(Term[] u) {

        for (Term t : u) {
            if (t == Bool.Null)
                return Bool.Null_Array;
            if (t == null)
                throw new NullPointerException();
        }

        int trues = 0;
        for (Term t : u) {

            if (t == Bool.False)
                return Bool.False_Array;
            if (t == Bool.True)
                trues++;
            else if (!t.op().eventable)
                return Null_Array;
        }
        if (trues > 0) {


            int sizeAfterTrueRemoved = u.length - trues;
            switch (sizeAfterTrueRemoved) {
                case 0:
                    return Op.EmptyTermArray;
                case 1: {

                    for (Term uu : u) {
                        if (uu != Bool.True) {
                            //assert (!(uu instanceof Ellipsislike)) : "if this happens, TODO";
                            return new Term[]{uu};
                        }
                    }
                    throw new RuntimeException("should have found non-True target to return");
                }
                default: {
                    Term[] y = new Term[sizeAfterTrueRemoved];
                    int j = 0;
                    for (int i = 0; j < y.length; i++) {
                        Term uu = u[i];
                        if (uu != Bool.True)
                            y[j++] = uu;
                    }
                    u = y;
                }
            }
        }
        return Terms.commute(u);
    }

    /**
     * different semantics than .add() -- returns true even if existing present.  returns false on conflict
     * returns false only if contradiction occurred, in which case this
     * ConjEvents instance is
     * now corrupt and its result via .target() should be considered final
     */
    default boolean add(long at, Term x) {
        if (at == DTERNAL || at == XTERNAL)//HACK TEMPORARY
            throw new WTF("probably meant ETERNAL or TIMELESS");
        if (at == TIMELESS)
            throw new WTF("invalid time");

        if (x == True)
            return true; //ignore

        if (!(x instanceof Bool) && !x.op().eventable)
            throw new TermException("invalid Conj event", x);

        if (x instanceof Compound) {

            if (x.op() == CONJ)
                return addConjEvent(at, x);

        }

        return addEvent(at, x);

    }

    default boolean add(LongObjectPair<Term> whenWhat) {
        return add(whenWhat.getOne(), whenWhat.getTwo());
    }

    /**
     * for internal use only
     */
    boolean addEvent(long at, Term x);

    int eventOccurrences();

    default boolean remove(LongObjectPair<Term> e) {
        return remove(e.getOne(), e.getTwo());
    }

    boolean remove(long at, Term t);

    boolean removeAll(Term term);

    int eventCount(long when);

    void negateEvents();

    default Term term() {
        return term(Op.terms);
    }

    Term term(TermBuilder b);

    LongIterator eventOccIterator();

    /**
     * private use only
     */
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


        if (xdt != XTERNAL) {

            if (at == ETERNAL && Conj.isSeq(x))
                at = 0;

            if (xdt == DTERNAL || xdt == 0 || at != ETERNAL) {
                return x.eventsWhile(this::add, at,
                        true,
                        false);
            }
        }

        return addEvent(at, x);
    }

    default boolean addAuto(Term t) {
        return add(Conj.isSeq(t) ? 0 : ETERNAL, t);
    }

    default long shiftOrZero() {
        long s = shift();
        if (s == ETERNAL)
            return 0;
        else {
            assert (s != TIMELESS);
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
        return min == TIMELESS ? ETERNAL : min;
    }
}
