package nars.term.util.builder;

import jcog.data.byt.DynBytes;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;
import nars.term.util.Statement;
import nars.term.util.TermException;
import nars.term.util.conj.ConjBuilder;
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
import static nars.term.Terms.commute;
import static nars.time.Tense.*;

/**
 * interface for target and subterm builders
 * this call tree eventually ends by either:
 * - instance(..)
 * - reduction to another target or True/False/Null
 */
public abstract class TermBuilder implements TermConstructor {


    public final Term newSortedCompound(Op o, int dt, Collection<Term> u) {
        assert (Tense.dtSpecial(dt));
        Term[] s = commute(u);
        if (s.length == 1 && o == CONJ)
            return s[0];

        return newCompound(o, dt, s);
    }

    public final Term newCompound(Op o, int dt, Term... u) {
        return newCompound(o, dt, u, null);
    }

    final Term newCompound(Op o, int dt, Term[] t, @Nullable DynBytes key) {

        int s = t.length;
        if (o.maxSubs < s)
                throw new TermException("subterm overflow", o, dt, t);

        boolean hasEllipsis = false;
        for (Term x : t) {
            if (x == Bool.Null)
                return Bool.Null;
            if (x instanceof Ellipsislike)
                hasEllipsis = true;
        }

        assert (o.minSubs <= s || hasEllipsis) :
                "subterm underflow: " + o + ' ' + Arrays.toString(t);

        if (o == NEG) {
            assert(t.length == 1);
            return neg(t[0]);
        } else if (s == 1 && o!= CONJ /* HACK */ ) {
            return newCompound1(o, t[0]);
        } else {
            return newCompoundN(o, dt, t, key);
        }
    }

    private Compound newCompoundN(Op o, int dt, Term[] t, @Nullable DynBytes key) {
        return newCompound(o, dt, subterms(o, t, dt, key));
    }


    protected Term newCompound1(Op o, Term x) {
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

    public final Term conj(boolean preSorted, int dt, Term... u) {

        if (!preSorted && u.length > 1)
            u = ConjBuilder.preSort(dt, u);

        switch (u.length) {
            case 0:
                return Bool.True;

            case 1:
                Term only = u[0];
//                if (only instanceof EllipsisMatch)
//                    return conj(dt, only.arrayShared());
//                else
                //special case
                if (only instanceof Ellipsislike){
                    assert(dt==DTERNAL);
                    return newCompound(CONJ, dt, only);
                } else
                    return only;
        }

        Term y;
        switch (dt) {
            case DTERNAL:
            case 0:
                y = ConjCommutive.the(Op.terms, dt, false, u); break;

            case XTERNAL: y = ConjCommutive.theXternal(this, u); break;

            default: y = ConjSeq.sequence(this, dt, u); break;
        }

//        {
//            //TEMPORARY FOR DEBUG
//            Term ay = y.anon();
//            if (y.volume()!=ay.volume())
//                throw new WTF();
//        }
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
//        try {
            return Statement.statement(this, op, dt, subject, predicate);
//        } catch (StackOverflowError e) {
//            throw new TermException("statement stack overflow", op, dt, subject, predicate);  //HACK
//        }

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

    public Term neg(Term x) {
        return Neg.neg(x);
    }

    public abstract Atom atom(String id);
}
