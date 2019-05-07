package nars.term.util.builder;

import jcog.data.byt.DynBytes;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.anon.Intrin;
import nars.term.atom.Bool;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;
import nars.term.util.Statement;
import nars.term.util.TermException;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjCommutive;
import nars.term.util.conj.ConjSeq;
import nars.term.util.transform.CompoundNormalization;
import nars.term.var.ellipsis.Ellipsislike;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.Terms.sorted;
import static nars.time.Tense.*;

/**
 * interface for target and subterm builders
 * this call tree eventually ends by either:
 * - instance(..)
 * - reduction to another target or True/False/Null
 */
public abstract class TermBuilder implements TermConstructor {


    public final Term theSortedCompound(Op o, int dt, Collection<Term> u) {
        assert (Tense.dtSpecial(dt));
        Term[] s = sorted(u);
        if (s.length == 1 && o == CONJ)
            return s[0];

        return theCompound(o, dt, s);
    }

    public final Term theCompound(Op o, int dt, Term... u) {
        return theCompound(o, dt, u, null);
    }

    final Term theCompound(Op o, int dt, Term[] t, @Nullable DynBytes key) {
        assert (!o.atomic) : o + " is atomic, yet given subterms: " + Arrays.toString(t);


        boolean hasEllipsis = false;

        for (Term x : t) {
            if (x == Bool.Null)
                return Bool.Null;
            if (!hasEllipsis && (x instanceof Ellipsislike))
                hasEllipsis = true;
        }

        int s = t.length;
        assert (o.maxSubs >= s) :
                "subterm overflow: " + o + ' ' + Arrays.toString(t);
        assert (o.minSubs <= s || hasEllipsis) :
                "subterm underflow: " + o + ' ' + Arrays.toString(t);

        if (s == 1 && !Intrin.intrinsic(t[0])) {
            Term x = t[0];
            switch (o) {
                case NEG:
                    return NEG.the(x);
                case CONJ:
                    break; //skip below
                default:
                    return newCompound1(o, x);
            }
        }

        return newCompoundN(o, dt, t, key);
    }

    private Compound newCompoundN(Op o, int dt, Term[] t, @Nullable DynBytes key) {
        return newCompound(o, dt, subterms(o, t, dt, key));
    }

    protected Compound newCompound1(Op o, Term x) {
        return new CachedUnitCompound(o, x);
    }

    protected Subterms subterms(Op o, Term[] t, int dt, @Nullable DynBytes key) {
        return subterms(o, t);
    }



    public static Compound newCompound(Op op, Subterms subterms) {
        return newCompound(op, DTERNAL, subterms);
    }

    public static Compound newCompound(Op op, int dt, Subterms subterms) {
        return CachedCompound.newCompound(op, dt, subterms);
    }
    public Term normalize(Compound x, byte varOffset) {
        Term y = new CompoundNormalization(x, varOffset).applyCompound(x);

//        LazyCompound yy = new LazyCompound();
//        new nars.util.target.transform.CompoundNormalization(this, varOffset)
//                .transform(this, yy);
//        Term y = yy.get();

        if (varOffset == 0 && y instanceof Compound) {
            y.subterms().setNormalized();
        }

        return y;

    }

    public Term conj(final int dt, Term... u) {
        return conj(false, dt, u);
    }

    protected final Term conj(boolean preSorted, int dt, Term... u) {

        if (!preSorted && u.length > 1)
            u = Conj.preSort(dt, u);

        switch (u.length) {
            case 0:
                return Bool.True;

            case 1:
                Term only = u[0];
//                if (only instanceof EllipsisMatch)
//                    return conj(dt, only.arrayShared());
//                else
                    return only instanceof Ellipsislike ?
                            theCompound(CONJ, dt, only) //special case
                            :
                            only;
        }

        Term y;
        switch (dt) {
            case DTERNAL:
            case 0:  y = ConjCommutive.theSorted(this, dt, u); break;

            case XTERNAL: y = ConjCommutive.theXternal(this, u); break;

            default: y = ConjSeq.sequence(this, dt, u); break;
        }

        return y;
    }


    public Term root(Compound x) {
        if (!x.hasAny(Op.Temporal))
            return x;
        return x.temporalize(
                NAL.conceptualization
        );
    }

    public Term concept(Compound x) {
        Term term = x.unneg().root();

        Op op = term.op();
        if (op==NEG)
            throw new TermException("TermBuilder.concept(): x.unneg().root() is NEG", x);
        if (!op.conceptualizable)
            return Bool.Null;


        return term.normalize();
//        Term term2 = target.normalize();
//        if (term2 != target) {
//            if (term2 == null)
//                return Bool.Null;
//
//            //assert (term2.op() == op): term2 + " not a normal normalization of " + target; //<- allowed to happen when image normalization is involved
//
//            target = term2.unneg();
//        }
//
//
//        return target;
    }

    protected Term statement(Op op, int dt, Term subject, Term predicate) {
        return Statement.statement(this, op, dt, subject, predicate);
    }

    public final Term statement(Op op, int dt, Term[] u) {
        if (u.length != 2)
            throw new TermException(op + " requires 2 arguments, but got: " + Arrays.toString(u), op, dt, u);

        return statement(op, dt, u[0], u[1]);
    }

    /**
     * TODO option for instantiating CompoundLight base's in the bottom part of this
     */
    public Term dt(Compound x, int nextDT) {

        int baseDT = x.dt();
        if (nextDT == baseDT)
            return x; //no change

        Op op = x.op();
        assert (op.temporal);

        Subterms xs = x.subterms();
        if (baseDT != XTERNAL && nextDT != XTERNAL && dtSpecial(baseDT) == dtSpecial(nextDT)) {
            if (!xs.hasAny(CONJ.bit | NEG.bit)
                    //|| (!xs.hasAny(CONJ) && xs.hasAny(NEG) && xs.count(NEG) <= 1) //exception: one or less negs
            ) {
                /* simple case - fast transform non-concurrent -> non-concurrent */
                return compound(op, nextDT, xs);
                //return CachedCompound.newCompound(op, nextDT, xs);
            }
        }

//        if (op == CONJ) {
//            boolean baseConcurrent = Conj.concurrentInternal(baseDT);
//            if (!Conj.concurrentInternal(nextDT)) {
//
//                boolean repeating = xx.length == 2 && xx[0].equals(xx[1]);
//
//                if (Param.DEBUG_EXTRA) {
//                    if (baseConcurrent) {
//                        if (!repeating)
//                            throw new TermException(CONJ, baseDT, xx, "ambiguous DT change from concurrent to non-concurrent and non-repeating");
//                    }
//                }
//
//                if (repeating) {
//                    nextDT = Math.abs(nextDT);
//                    if (nextDT == baseDT) {
//                        //can this be detected earlier, if it happens
//                        return x;
//                    }
//                }
//
////                if (!baseConcurrent) {
////                    //fast transform non-concurrent -> non-concurrent
////                    return Op.compound(CONJ, nextDT, xx);
////                }
//            } else {
//
//                if (baseConcurrent) {
//                    if (baseDT == XTERNAL) {
//                        //changing to non-XTERNAL, check for repeats
//                        if (xx.length < 2) {
//
//                        } else if (xx.length == 2) {
//                            if (xx[0].equals(xx[1]))
//                                return xx[0]; //collapse
//                            else if (xx[0].equalsNeg(xx[1]))
//                                return Bool.False; //contradict
//                            else if (xx[0].hasAny(CONJ.bit | NEG.bit) || xx[1].hasAny(CONJ.bit | NEG.bit)) {
//                                //need to thoroughly construct
//                                return CONJ.the(nextDT, xx);
//                            }
//                        } else {
//                            //need to thoroughly check for co-negations
//                            return CONJ.the(nextDT, xx);
//                        }
//                    }
//                    //fast transform concurrent -> concurrent, subs wont change
//                    return Op.compound(CONJ, nextDT, xx);
//                }
//
//            }
//        }


        return op.the(this, nextDT, xs);


    }
}
