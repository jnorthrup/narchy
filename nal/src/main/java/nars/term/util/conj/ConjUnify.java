package nars.term.util.conj;

import nars.$;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.unify.Unify;

import java.util.Arrays;
import java.util.SortedSet;

import static nars.Op.CONJ;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

public class ConjUnify {

    /** conjunction unfiication when # subterms differ */
    public static boolean unifyConj(Term x, Term y, Subterms xx, Subterms yy, Unify u) {

        if (!Subterms.possiblyUnifiableAssumingNotEqual(xx, yy, u.varBits))
            return false;

        int xdt = x.dt(), ydt = y.dt();

        boolean var = u.varIn(xx) || u.varIn(yy);

        if ((xdt == XTERNAL || ydt == XTERNAL)) {

            if (((xdt==XTERNAL && !xx.hasAny(CONJ)) || (ydt==XTERNAL && !yy.hasAny(CONJ)))) {
                //one is a simple XTERNAL (no conj subterms) so will match if equal root
                if (x.equalsRoot(y))
                    return true;
            }

            SortedSet<Term> xe = x.eventSet();
            SortedSet<Term> ye = y.eventSet();
            if (xe.equals(ye))
                return true;

            if (!var)
                return false;
            if (xe.size()!=ye.size())
                return false;

//            if (xe.size()!=ye.size() && Ellipsis.firstEllipsis(xx)==null && Ellipsis.firstEllipsis(yy)==null)
//                return false; //differing # of events

            try {
                return Subterms.unifyCommute($.vFast(xe), $.vFast(ye), u);
            } catch (Throwable t) {
                System.err.println("unify stack overflow?: " + xe + " " + ye);
                throw t;
            }

        } else {
            if (!var)
                return false; //TODO allow slightly different constants

            boolean xSeq = Conj.isSeq(x);
            boolean ySeq = Conj.isSeq(y);
            if (xSeq!=ySeq)
                return false;

            if (!xSeq) {
                //ordinary conjunctive unification
                return xx.subs()==yy.subs() && Subterms.unifyCommute(xx, yy, u);
            } else {

                if (x.eventRange() != y.eventRange())
                    return false; //TODO allow time splicing/shifting

                //TODO test sequences containing vars
                ConjList xl = ConjList.events(x);
                ConjList yl = ConjList.events(y);
                if (xl.size() != yl.size())
                    return false; //TODO this can possibly work if a raw variable matches a range
                if (!Arrays.equals(xl.when, yl.when))
                    return false; //TODO allow time splicing/shifting

                xl.removeIf((long when, Term z) -> {
                    if (!u.varIn(z)) {
                        yl.remove(when, z);
                        return true;
                    }
                    return false;
                });
                int xls = xl.size();
                if (xls != yl.size())
                    return false;
                if (xls > 1)
                    return Subterms.unifyLinear($.vFast(xl), $.vFast(yl), u);
                else
                    return xl.get(0).unify(yl.get(0), u);
            }
        }
    }

    /** xx smaller */
    public static boolean eventsCommon(Term x, Term y) {
        if (x.op()!=CONJ || y.op()!=CONJ || !Term.commonStructure(x.subterms(), y.subterms()))
            return false;

        if (x.volume() > y.volume()) {
            //swap
            Term z = y;
            y = x;
            x = z;
        }

        Term Y = y;
        return x.eventsOR((when, xx) -> Conj.eventOf(Y, xx, ETERNAL, 1), ETERNAL, true, true);


//        if (y.subterms().containsAny(x.subterms()))
//        if (!Conj.isSeq(x) && !Conj.seq(y)) {
//            return ;
//        } else {
//
//            ConjList xe = ConjList.events(x);
//
//            //return !scan(yy, (when, what) -> !xe.remove(when, what));
//            return !y.eventsAND((when, what)->!xe.remove(when,what),
//                    Conj.isSeq(y) ? ETERNAL : 0, true, false);
//        }

    }
}
