package nars.util.term;

import nars.Op;
import nars.subterm.ArrayTermVector;
import nars.subterm.Neg;
import nars.subterm.Subterms;
import nars.subterm.UnitSubterm;
import nars.term.Compound;
import nars.term.Term;
import nars.term.anon.AnonID;
import nars.term.anon.AnonVector;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;
import nars.unify.match.EllipsisMatch;
import nars.unify.match.Ellipsislike;

import java.util.Arrays;
import java.util.Collection;

import static nars.Op.Null;
import static nars.term.Terms.sorted;
import static nars.time.Tense.DTERNAL;

/**
 * interface for term and subterm builders
 * this call tree eventually ends by either:
 * - instance(..)
 * - reduction to another term or True/False/Null
 */
public abstract class TermBuilder {

    public final Term compound(Op o, Term... u) {
        return compound(o, DTERNAL, u);
    }

    public final Term compound(Op o, int dt, Term[] u) {
        if (Op.hasNull(u))
            return Null;
        return newCompound(o, dt, o.commute(dt, u.length) ? sorted(u) : u);
    }

    abstract protected Term newCompound(Op o, int dt, Term[] u);

    abstract public Subterms newSubterms(Term... s);


    public Subterms subterms(Collection<Term> s) {
        return newSubterms(s.toArray(Op.EmptyTermArray));
    }

    public Subterms subtermsInstance(Term... t) {
        final int tLength = t.length;
        if (tLength == 0)
            return Op.EmptySubterms;

        boolean purelyAnon = true;
        for (int i = 0; i < tLength; i++) {
            Term x = t[i];
            if (x instanceof EllipsisMatch)
                throw new RuntimeException("ellipsis match should not be a subterm of ANYTHING");

            if (purelyAnon) {
                if (!(x instanceof AnonID)) {
                    Term ux = x.unneg();
                    if (x != ux && ux instanceof AnonID) {
                        //allow anon here, but not t.length > 1 there is still some problem probably with commutives
                        //purelyAnon = true
                    } else {
                        purelyAnon = false;
                    }
                }
            }
        }

        if (!purelyAnon) {
            switch (t.length) {
                case 0:
                    throw new UnsupportedOperationException();
                case 1:
                    //return new TermVector1(t[0]);
                    return new UnitSubterm(t[0]);
                //case 2:
                //return new TermVector2(t);
                default:
                    return new ArrayTermVector(t);
            }
        } else {
            return new AnonVector(t);
        }

    }



    public Term compoundInstance(Op o, int dt, Term[] u) {
        assert (!o.atomic) : o + " is atomic, with subterms: " + (u);

        boolean hasEllipsis = false;

        for (Term x : u) {
            if (!hasEllipsis && (x instanceof Ellipsislike))
                hasEllipsis = true;
            if (x == Null)
                return Null;
        }

        int s = u.length;
        assert (o.maxSize >= s) :
                "subterm overflow: " + o + ' ' + Arrays.toString(u);
        assert (o.minSize <= s || hasEllipsis) :
                "subterm underflow: " + o + ' ' + Arrays.toString(u);

        if (s == 1) {
            switch (o) {
                case NEG:
                    return Neg.the(u[0]);
                default:
                    return new CachedUnitCompound(o, u[0]);
            }
        } else {
            return theCompound(o, dt, newSubterms(u));
        }
    }


    public Compound theCompound(Op op, Subterms subterms) {
        return theCompound(op, DTERNAL, subterms);
    }

    public Compound theCompound(Op op, int dt, Subterms subterms) {
        //HACK predict if compound will differ from its root
        if (!op.temporal && !subterms.isTemporal()) { //TODO there are more cases
            assert(dt == DTERNAL);
            return new CachedCompound.SimpleCachedCompound(op, subterms);
        } else {
            return new CachedCompound.TemporalCachedCompound(op, dt, subterms);
        }
    }


}
