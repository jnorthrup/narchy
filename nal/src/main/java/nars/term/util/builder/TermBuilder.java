package nars.term.util.builder;

import jcog.data.byt.DynBytes;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Interval;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;
import nars.term.compound.Sequence;
import nars.term.util.Statement;
import nars.term.util.TermException;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjPar;
import nars.term.util.conj.ConjSeq;
import nars.term.util.transform.CompoundNormalization;
import nars.term.var.ellipsis.Ellipsislike;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * interface for target and subterm builders
 * this call tree eventually ends by either:
 * - instance(..)
 * - reduction to another target or True/False/Null
 */
public abstract class TermBuilder implements TermConstructor {

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
                return Bool.Null; //try to detect earlier
            if (x instanceof Ellipsislike)
                hasEllipsis = true;
        }

        assert (o.minSubs <= s || hasEllipsis) :
                "subterm underflow: " + o + ' ' + Arrays.toString(t);

        if (s == 1 && o!= CONJ /* HACK */ ) {
            return newCompound1(o, t[0]);
        } else {
            return newCompoundN(o, dt, t, key);
        }
    }

    public Compound newCompoundN(Op o, int dt, Term[] t, @Nullable DynBytes key) {
        return newCompound(o, dt, subterms(o, t, key));
    }


    protected Term newCompound1(Op o, Term x) {
        return o == NEG ? neg(x) : new CachedUnitCompound(o, x);
    }

    protected Subterms subterms(Op o, Term[] t, @Nullable DynBytes key) {
        return subterms(o, t);
    }

    public static Compound newCompound(Op op, Subterms subterms) {
        return newCompound(op, DTERNAL, subterms);
    }

    public static Compound newCompound(Op op, int dt, Subterms subterms) {
        return CachedCompound.newCompound(op, dt, subterms);
    }

    public Term normalize(Compound x, byte varOffset) {

        CompoundNormalization c = new CompoundNormalization(x, varOffset);
        Term y = c.apply(x); //x.transform();

        if (varOffset == 0 && y instanceof Compound)
            ((Compound)y).subtermsDirect().setNormalized();

        return y;
    }

    public final Term conj(Term[] u) {
        return conj(DTERNAL, u);
    }

    public Term conj(final int dt, Term... u) {
        return conj(false, dt, u);
    }

    protected Term conj(boolean preSorted, int dt, Term[] u) {

        if (u.length>0 && u[u.length-1] instanceof Interval)
            return new Sequence(subterms(u));

        if (!preSorted || (dt == XTERNAL /* HACK */))
            u = ConjBuilder.preSort(dt, u);

        switch (u.length) {
            case 0:
                return Bool.True;

            case 1:
                Term only = u[0];
                return only instanceof Ellipsislike ? newCompound(CONJ, dt, only) : only;
        }

        switch (dt) {
            case DTERNAL:
            case 0:
                return ConjPar.the(this, dt,false, u);

            case XTERNAL:
                return newCompound(CONJ, XTERNAL, u);

            default: {
                if (u.length != 2)
                    throw new TermException("temporal conjunction with n!=2 subterms", CONJ, dt, u);
                return conjAppend(u[0], dt, u[1]);
            }
        }

    }

    /** attaches two events together with dt separation */
    public Term conjAppend(Term a, int dt, Term b) {
        if (dt == XTERNAL)
            return conj(XTERNAL, a, b);

        int aRange = a.eventRange(), bRange = b.eventRange();

        if (dt == 0 && aRange == 0 && bRange == 0)
            return conj(dt, a, b);
        else
            return (dt >= 0) ?
                    ConjSeq.sequence(a, 0, b, +dt + aRange, this) :
                    ConjSeq.sequence(b, 0, a, -dt + bRange, this);
    }

    /** merges two events with a dt offset applied to 'b' relative to a */
    @Deprecated public Term conjMerge(Term a, int dt, Term b) {
        if (dt == XTERNAL)
            return conj(XTERNAL, a, b);

        int aRange = a.eventRange(), bRange = b.eventRange();

        if (dt == 0 && aRange == 0 && bRange == 0)
            return conj(dt, a, b);
        else
            return ConjSeq.sequence(a, 0, b, dt, this);
    }

    public Term root(Compound x) {
        return !x.hasAny(Op.Temporal) ? x : _root(x);
    }

    protected Term _root(Compound x) {
        return NAL.conceptualization.apply(x);
    }

    public static Term concept(Compound x) {
        Term term = x.unneg().root().normalize();

        if (term instanceof Neg || !term.op().conceptualizable)
            throw new TermException("not conceptualizable", x);

        return term;
    }

    public Term statement(Op op, int dt, Term subject, Term predicate) {
//        try {
            return Statement.statement(this, op, dt, subject, predicate);
//        } catch (StackOverflowError e) {
//            throw new TermException("statement stack overflow", op, dt, subject, predicate);  //HACK
//        }

    }


    /**
     * TODO option for instantiating CompoundLight base's in the bottom part of this
     */
    public Term dt(Compound x, int nextDT) {

        if (nextDT == 0)
            nextDT = DTERNAL; //HACK

        int baseDT = x.dt();
        if (nextDT == baseDT)
            return x; //no change

        Op op = x.op(); assert (op.temporal);

        Subterms xs = x.subterms();
//        if (baseDT != XTERNAL && nextDT != XTERNAL && dtSpecial(baseDT) == dtSpecial(nextDT)) {
//            if (!xs.hasAny(CONJ.bit | NEG.bit)
//                    //|| (!xs.hasAny(CONJ) && xs.hasAny(NEG) && xs.count(NEG) <= 1) //exception: one or less negs
//            ) {
//                /* simple case - fast transform non-concurrent -> non-concurrent */
//                return compound(op, nextDT, xs);
//            }
//        }


        return op.the(this, nextDT, xs);


    }

    public Term neg(Term x) {
        return Neg.neg(x);
    }

    public abstract Atomic atom(String id);
}
