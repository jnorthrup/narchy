package nars.term.util.builder;

import jcog.data.byt.DynBytes;
import nars.Op;
import nars.subterm.*;
import nars.term.Compound;
import nars.term.Statement;
import nars.term.Term;
import nars.term.Terms;
import nars.term.anon.AnonID;
import nars.term.atom.Bool;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;
import nars.term.util.Conj;
import nars.term.util.TermException;
import nars.term.util.transform.CompoundNormalization;
import nars.term.util.transform.Retemporalize;
import nars.time.Tense;
import nars.unify.ellipsis.EllipsisMatch;
import nars.unify.ellipsis.Ellipsislike;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.Terms.sorted;
import static nars.time.Tense.*;

/**
 * interface for term and subterm builders
 * this call tree eventually ends by either:
 * - instance(..)
 * - reduction to another term or True/False/Null
 */
public abstract class TermBuilder {

    abstract public Term compound(Op o, int dt, Term... u);

    protected abstract Subterms subterms(@Nullable Op inOp, Term... u);

    public final Term compound(Op o, Term... u) {
        return compound(o, DTERNAL, u);
    }

//    protected Term resolve(Term x){
//        return x;
//    }

    public final Subterms subterms(Term... s) {
        if (s.length == 0)
            return Op.EmptySubterms;
        return subterms(null, s);
    }


    public final Subterms subterms(Collection<Term> s) {
        return subterms(s.toArray(Op.EmptyTermArray));
    }

    public Subterms theSubterms(boolean tryAnon, Term... t) {
        final int tLength = t.length;
        if (tLength == 0)
            return Op.EmptySubterms;

        if (tryAnon && isAnon(t))
            return new AnonVector(t);
        else
            return newSubtermsVector(t);

    }

    static boolean isAnon(Term[] t) {
        for (Term x : t) {
            //assert (!(x instanceof EllipsisMatch)) : "ellipsis match should not be a subterm of ANYTHING";
            if (x instanceof AnonID)
                continue;
            if (x instanceof Neg && x.unneg() instanceof AnonID)
                continue;
            return false;
        }
        return true;
    }

    static Subterms newSubtermsVector(Term[] t) {
        Term t0 = t[0];
        switch (t.length) {
            case 0:
                throw new UnsupportedOperationException();

            case 1: {
                return new UniSubterm(t0);
            }

            case 2: {
                Term t1 = t[1];

                return
//                    return (this instanceof InterningTermBuilder) ?
//                            new BiSubterm.ReversibleBiSubterm(t[0], t[1]) :
                        new BiSubterm(t0, t1);
            }

            default: {
                //TODO Param.SUBTERM_BYTE_KEY_CACHED_BELOW_VOLUME
                return new ArrayTermVector(t);
            }
        }
    }
    public final Term theSortedCompound(Op o, int dt, Collection<Term> u) {
        assert(Tense.dtSpecial(dt));
        Term[] s = sorted(u);
        if (s.length == 1 && o == CONJ)
            return s[0];

        return theCompound(o, dt, s);
    }

    public final Term theSortedCompound(Op o, int dt, Term... u) {
        assert(Tense.dtSpecial(dt));
        Term[] s = sorted(u);
        if (s.length == 1 && o == CONJ)
            return s[0];

        return theCompound(o, dt, s);
    }

    public final Term theCompound(Op o, int dt, Term... u) {
        return theCompound(o, dt, u, null);
    }

    protected Term theCompound(Op o, int dt, Term[] t, @Nullable DynBytes key) {
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

        if (s == 1 && !AnonID.isAnonPosOrNeg(t[0])) {
            Term x = t[0];
            switch (o) {
                case NEG:
                    return NEG.the(x);
                case CONJ:
                    break; //skip below
                default:
                    return new CachedUnitCompound(o, x);
            }
        }

        Subterms subs = subterms(o, t, dt, key);

        return CachedCompound.newCompound(o, dt, subs);
    }

    protected Subterms subterms(Op o, Term[] t, int dt, @Nullable DynBytes key) {
        return subterms(o, t);
    }


    public static Compound newCompound(Op op, Subterms subterms) {
        return CachedCompound.newCompound(op, DTERNAL, subterms);
    }


    public Term normalize(Compound x, byte varOffset) {
        Term y = new CompoundNormalization(x, varOffset).transformCompound(x);

//        LazyCompound yy = new LazyCompound();
//        new nars.util.term.transform.CompoundNormalization(this, varOffset)
//                .transform(this, yy);
//        Term y = yy.get();

        if (varOffset == 0 && y instanceof Compound) {
            y.subterms().setNormalized();
        }

        return y;

    }



    //TODO presorted flag
    public Term conj(final int dt, Term[] u) {

        switch (dt) {
            case 0:
            case DTERNAL:
                u = Terms.sorted(u);
                break;
            case XTERNAL:
                Term[] v = Terms.sorted(u);
                if (v.length != 1) {
                    u = v; //only if not collapsed to one item in case of repeat
                } else  {
                    if (/*!(v[0] instanceof Ellipsislike) || */(u.length > 1 && u[0].equals(u[1])))
                        u = new Term[]{v[0], v[0]};
                    else {
                        u = v;
                    }
                }
                break;
        }
        switch (u.length) {

            case 0:
                return Bool.True;

            case 1:
                Term only = u[0];
                if (only instanceof EllipsisMatch) {

                    return conj(dt, only.arrayShared());
                } else {

                    return only instanceof Ellipsislike ?
                            theCompound(CONJ, dt, only)
                            :
                            only;
                }

        }


        int trues = 0;
        for (Term t : u) {
            if (t == Bool.Null || t == Bool.False)
                return t;
            else if (t == Bool.True)
                trues++;
        }

        if (trues > 0) {

            int sizeAfterTrueRemoved = u.length - trues;
            switch (sizeAfterTrueRemoved) {
                case 0:

                    return Bool.True;
                case 1: {

                    for (Term uu : u) {
                        if (uu != Bool.True) {
                            assert (!(uu instanceof Ellipsislike)) : "if this happens, TODO";
                            return uu;
                        }
                    }
                    throw new RuntimeException("should have found non-True term to return");
                }
                default: {
                    Term[] y = new Term[sizeAfterTrueRemoved];
                    int j = 0;
                    for (int i = 0; j < y.length; i++) {
                        Term uu = u[i];
                        if (uu != Bool.True)
                            y[j++] = uu;
                    }
                    assert (j == y.length);

                    u = y;
                }
            }
        }


        switch (dt) {
            case DTERNAL:
            case 0: {
                if (u.length == 2) {


                    //quick test
                    Term a = u[0], b = u[1];
                    if (Term.commonStructure(a, b)) {
                        if (a.equals(b))
                            return u[0];
                        if (a.equalsNeg(b))
                            return Bool.False;
                    }

                    if (!a.hasAny(Op.CONJ.bit) && !b.hasAny(Op.CONJ.bit)) {
                        //fast construct for simple case, verified above to not contradict itself
                        //return compound(CONJ, dt, sorted(u[0], u[1]));
                        return theCompound(CONJ, dt, /*sorted*/u[0], u[1]);
                    }

                }
                //TODO fast 3-ary case

                assert u.length > 1;
                Conj c = new Conj(u.length);
                long sdt = dt == DTERNAL ? ETERNAL : 0;
                for (Term x : u) {
                    if (!c.add(sdt, x))
                        break;
                }
                return c.term();
            }

            case XTERNAL:
                int ul = u.length;
                switch (ul) {
                    case 0:
                        return Bool.True;

                    case 1:
                        return u[0];

                    default: {
                        if (ul == 2) {
                            //special case: simple arity=2
                            if (!u[0].equals(u[1])) { // && !unfoldableInneralXternalConj(u[0]) && !unfoldableInneralXternalConj(u[1])) {
                                return theCompound(CONJ, XTERNAL, sorted(u));
                            } else
                                return theCompound(CONJ, XTERNAL, u[0], u[0]); //repeat
                        } else {

                            MutableSet<Term> uux = new UnifiedSet(ul, 1f);
                            for (Term uu : u) {
//                            if (unfoldableInneralXternalConj(uu)) {
//                                uu.subterms().forEach(uux::add);
//                            } else {
                                uux.add(uu);
//                            }
                            }


                            if (uux.size() == 1) {
                                Term only = uux.getOnly();
                                return theCompound(CONJ, XTERNAL, only, only); //repeat
                            } else {
                                return theCompound(CONJ, XTERNAL, sorted(uux));
                            }
                        }
                    }


//                    case 2: {
//
//
//                        Term a = u[0];
//                        if (a.op() == CONJ && a.dt() == XTERNAL && a.subs() == 2) {
//                            Term b = u[1];
//
//                            int va = a.volume();
//                            int vb = b.volume();
//
//                            if (va > vb) {
//                                Term[] aa = a.subterms().arrayShared();
//                                int va0 = aa[0].volume();
//                                int va1 = aa[1].volume();
//                                int vamin = Math.min(va0, va1);
//
//
//                                if ((va - vamin) > (vb + vamin)) {
//                                    int min = va0 <= va1 ? 0 : 1;
//
//                                    Term[] xu = {CONJ.the(XTERNAL, new Term[]{b, aa[min]}), aa[1 - min]};
//                                    Arrays.sort(xu);
//                                    return compound(CONJ, XTERNAL, xu);
//                                }
//                            }
//
//                        }
//                        break;
//                    }
//
                }


            default: {
                if (u.length != 2) {
                    //if (Param.DEBUG_EXTRA)
                    throw new TermException("temporal conjunction with n!=2 subterms");
                    //return Null;
                }

                return (dt >= 0) ?
                        Conj.the(u[0], 0, u[1], +dt + u[0].eventRange()) :
                        Conj.the(u[1], 0, u[0], -dt + u[1].eventRange());
            }
        }

    }

//    private static boolean unfoldableInneralXternalConj(Term x) {
//        return x.op() == CONJ && x.dt() == XTERNAL;
//    }

    public Term root(Compound x) {

        return x.temporalize(Retemporalize.root);
    }

    public Term concept(Compound x) {
        Term term = x.unneg().root();

        Op op = term.op();
        assert (op != NEG) : this + " concept() to NEG: " + x.unneg().root();
        if (!op.conceptualizable)
            return Bool.Null;


        return term.normalize();
//        Term term2 = term.normalize();
//        if (term2 != term) {
//            if (term2 == null)
//                return Bool.Null;
//
//            //assert (term2.op() == op): term2 + " not a normal normalization of " + term; //<- allowed to happen when image normalization is involved
//
//            term = term2.unneg();
//        }
//
//
//        return term;
    }

    protected Term statement(Op op, int dt, Term subject, Term predicate) {
        return Statement.statement(op, dt, subject, predicate);
    }

    public final Term statement(Op op, int dt, Term[] u) {
        assert (u.length == 2) : op + " requires 2 arguments, but got: " + Arrays.toString(u);
        return statement(op, dt, u[0], u[1]);
    }

}
