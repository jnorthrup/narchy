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

public enum ConjUnify {
    ;

    /** conjunction unfiication when # subterms differ */
    public static boolean unifyConj(Term x, Term y, Subterms xx, Subterms yy, Unify u) {
        boolean result = false;
        boolean finished = false;

        if (Subterms.possiblyUnifiableAssumingNotEqual(xx, yy, u.varBits | (x.hasXternal() || y.hasXternal() ? CONJ.bit : 0))) {
            int xdt = x.dt(), ydt = y.dt();
            boolean var = u.varIn(xx) || u.varIn(yy);
            if ((xdt == XTERNAL || ydt == XTERNAL)) {

                if (((xdt == XTERNAL && !xx.hasAny(CONJ)) || (ydt == XTERNAL && !yy.hasAny(CONJ)))) {
                    //one is a simple XTERNAL (no conj subterms) so will match if equal root
                    if (x.equalsRoot(y)) {
                        result = true;
                        finished = true;
                    }
                }
                if (!finished) {
                    SortedSet<Term> xe = x.eventSet();
                    SortedSet<Term> ye = y.eventSet();
                    if (xe.equals(ye)) {
                        result = true;
                    } else if (var) {
                        if (xe.size() == ye.size()) {//            if (xe.size()!=ye.size() && Ellipsis.firstEllipsis(xx)==null && Ellipsis.firstEllipsis(yy)==null)
//                return false; //differing # of events
                            try {
                                result = Subterms.unifyCommute($.vFast(xe), $.vFast(ye), u);
                            } catch (Throwable t) {
                                System.err.println("unify stack overflow?: " + xe + " " + ye);
                                throw t;
                            }
                        }
                    }

                }

            } else if (var) {
                boolean xSeq = Conj.isSeq(x), ySeq = Conj.isSeq(y);
                if (xSeq == ySeq) {
                    if (!xSeq) {
                        //ordinary conjunctive unification
                        result = xx.subs() == yy.subs() && Subterms.unifyCommute(xx, yy, u);
                    } else if (x.eventRange() == y.eventRange()) {//TODO test sequences containing vars
                        ConjList xl = ConjList.events(x);
                        ConjList yl = ConjList.events(y);//TODO this can possibly work if a raw variable matches a range
                        if (xl.size() == yl.size()) {//TODO allow time splicing/shifting
                            if (Arrays.equals(xl.when, yl.when)) {
                                xl.removeIf((when, z) -> {
                                    if (!u.varIn(z)) {
                                        yl.remove(when, z);
                                        return true;
                                    }
                                    return false;
                                });
                                int xls = xl.size();
                                if (xls == yl.size()) {
                                    result = xls > 1 ? Subterms.unifyLinear($.vFast(xl), $.vFast(yl), u) : xl.get(0).unify(yl.get(0), u);
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
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
