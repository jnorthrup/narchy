package nars.util.term;

import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import nars.Op;
import nars.Param;
import nars.op.mental.AliasConcept;
import nars.subterm.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.anon.AnonID;
import nars.term.anon.AnonVector;
import nars.term.atom.Bool;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;
import nars.term.compound.util.Conj;
import nars.unify.match.EllipsisMatch;
import nars.unify.match.Ellipsislike;
import nars.util.term.transform.CompoundNormalization;
import nars.util.term.transform.Retemporalize;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * interface for term and subterm builders
 * this call tree eventually ends by either:
 * - instance(..)
 * - reduction to another term or True/False/Null
 */
public abstract class TermBuilder {

    abstract public Term compound(Op o, int dt, Term... u);

    protected abstract Subterms subterms(@Nullable Op inOp, Term... s);

    public final Term compound(Op o, Term... u) {
        return compound(o, DTERNAL, u);
    }

//    protected Term resolve(Term x){
//        return x;
//    }

    public final Subterms subterms(Term... s) {
        return subterms(null, s);
    }



    public Subterms subterms(Collection<Term> s) {
        return subterms(s.toArray(Op.EmptyTermArray));
    }

    public Subterms theSubterms(Term... t) {
        final int tLength = t.length;
        if (tLength == 0)
            return Op.EmptySubterms;

        boolean purelyAnon = true;
        for (Term x: t) {
            if (x instanceof EllipsisMatch)
                throw new RuntimeException("ellipsis match should not be a subterm of ANYTHING");

            if (purelyAnon) {
                if (!(x instanceof AnonID)) {
                    Term ux = x.unneg();
                    if (x == ux || !(ux instanceof AnonID)) {
                        purelyAnon = false;
                    }
                }
            }
        }

        if (!purelyAnon) {
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
        } else {
            return new AnonVector(t);
        }

    }


    protected final Term theCompound(Op o, int dt, Term[] u) {
        return theCompound(o, dt, u, null);
    }

    protected Term theCompound(Op o, int dt, Term[] u, @Nullable byte[] key) {
        assert (!o.atomic) : o + " is atomic, with subterms: " + Arrays.toString(u);

        boolean hasEllipsis = false;

        for (Term x : u) {
            if (x == Null)
                return Null;
            if (!hasEllipsis && (x instanceof Ellipsislike))
                hasEllipsis = true;
        }

        int s = u.length;
        assert (o.maxSize >= s) :
                "subterm overflow: " + o + ' ' + Arrays.toString(u);
        assert (o.minSize <= s || hasEllipsis) :
                "subterm underflow: " + o + ' ' + Arrays.toString(u);

        if (s == 1 && !AnonID.isAnonPosOrNeg(u[0])) {
            Term x = /*resolve*/(u[0]);
            switch (o) {
                case NEG:
                    return Neg.the(x);
                default:
                    return new CachedUnitCompound(o, x);
            }
        } else {
            return theCompound(o, dt, subterms(o, u), key);
        }
    }


    public final Compound theCompound(Op op, Subterms subterms) {
        return theCompound(op, DTERNAL, subterms);
    }

    public final Compound theCompound(Op op, int dt, Subterms subterms) {
        return theCompound(op, dt, subterms, null);
    }

    public Compound theCompound(Op op, int dt, Subterms subterms, @Nullable byte[] key) {
//        if (subterms instanceof DisposableTermList)
//            throw new WTF();
        if (!op.temporal && !subterms.hasAny(Op.Temporal)) {
            assert(dt == DTERNAL);
            if (subterms.volume() < Param.TERM_BYTE_KEY_CACHED_BELOW_VOLUME) {
                return new CachedCompound.SimpleCachedCompoundWithBytes(op, subterms, key);
            } else {
                return new CachedCompound.SimpleCachedCompound(op, subterms);
            }
        } else {
            return new CachedCompound.TemporalCachedCompound(op, dt, subterms);
        }
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

    public Term statement(Op op, int dt, Term subject, Term predicate) {
        if (subject == Null || predicate == Null)
            return Null;

        boolean dtConcurrent = concurrent(dt) && dt!=XTERNAL;
        if (dtConcurrent) {
            if (subject.equals(predicate))
                return True;
            if ((op == INH || op == SIM) && subject.equalsRoot(predicate))
                return Null; //dont support non-temporal statements where the root is equal because they cant be conceptualized
        }

        if (op == IMPL) {


            if (subject == True)
                return predicate;
            if (subject == False)
                return Null;

            if (predicate instanceof Bool)
                return Null;


            if (predicate.op() == NEG) {

                return IMPL.the(dt, subject, predicate.unneg()).neg();
            }


            if (subject.hasAny(InvalidImplicationSubj))
                return Null;


            switch (predicate.op()) {
                case IMPL: {
                    return IMPL.the(predicate.dt(), CONJ.the(dt, new Term[]{subject, predicate.sub(0)}), predicate.sub(1));
                }


            }


            if (dt != XTERNAL && subject.dt() != XTERNAL && predicate.dt() != XTERNAL) {

                ArrayHashSet<LongObjectPair<Term>> se = new ArrayHashSet<>(4);
                subject.eventsWhile((w, t) -> {
                    se.add(PrimitiveTuples.pair(w, t));
                    return true;
                }, 0, true, true, false, 0);

                FasterList<LongObjectPair<Term>> pe = new FasterList(4);
                int pre = subject.dtRange();
                boolean dtNotDternal = dt != DTERNAL;
                int edt = pre + (dtNotDternal ? dt : 0);

                final boolean[] peChange = {false};


                boolean contradiction = !predicate.eventsWhile((w, t) -> {
                    LongObjectPair<Term> x = PrimitiveTuples.pair(w, t);
                    if (se.contains(x)) {

                        peChange[0] = true;
                    } else if (se.contains(pair(w, t.neg()))) {
                        return false;
                    } else {
                        pe.add(x);
                    }
                    return true;
                }, edt, true, true, false, 0);

                if (contradiction)
                    return False;


                if ((dt == DTERNAL || dt == 0)) {
                    for (ListIterator<LongObjectPair<Term>> pi = pe.listIterator(); pi.hasNext(); ) {
                        LongObjectPair<Term> pex = pi.next();
                        Term pext = pex.getTwo();
                        if (pext.op() == CONJ) {
                            int pdt = pext.dt();
                            if (pdt == DTERNAL || pdt == 0) {
                                long at = pex.getOne();

                                RoaringBitmap pextRemovals = null;
                                Subterms subPexts = pext.subterms();
                                int subPextsN = subPexts.subs();

                                for (LongObjectPair<Term> sse: se) {
                                    if (sse.getOne() == at) {


                                        Term sset = sse.getTwo();

                                        for (int i = 0; i < subPextsN; i++) {
                                            Term subPext = subPexts.sub(i);
                                            Term merge = CONJ.the(dt, new Term[]{sset, subPext});
                                            if (merge == Null) return Null;
                                            else if (merge == False) {

                                                return False;
                                            } else if (merge.equals(sset)) {

                                                if (pextRemovals == null)
                                                    pextRemovals = new RoaringBitmap();
                                                pextRemovals.add(i);
                                            } else {

                                            }
                                        }
                                    }
                                }
                                if (pextRemovals != null) {
                                    if (pextRemovals.getCardinality() == subPextsN) {

                                        pi.remove();
                                    } else {
                                        pi.set(pair(at, CONJ.the(pdt, subPexts.termsExcept(pextRemovals))));
                                    }
                                    peChange[0] = true;
                                }
                            }
                        }
                    }
                }


                if (pe.isEmpty())
                    return True;


                if (peChange[0]) {

                    int ndt = dtNotDternal ? (int) pe.minBy(LongObjectPair::getOne).getOne() - pre : DTERNAL;
                    Term newPredicate;
                    if (pe.size() == 1) {
                        newPredicate = pe.getOnly().getTwo();
                    } else if (predicate.dt() == DTERNAL) {

                        Conj c = new Conj();
                        for (LongObjectPair<Term> aPe: pe) {
                            if (!c.add(ETERNAL, aPe.getTwo()))
                                break;
                        }
                        newPredicate = c.term();
                    } else {
                        newPredicate = Conj.conj(pe);
                    }

                    return IMPL.the(ndt, subject, newPredicate);
                }


            }


        }


        if ((dtConcurrent || op != IMPL) && (!subject.hasAny(Op.VAR_PATTERN) && !predicate.hasAny(Op.VAR_PATTERN))) {

            Predicate<Term> delim = (op == IMPL) ?
                    recursiveCommonalityDelimeterStrong : Op.recursiveCommonalityDelimeterWeak;


            if ((containEachOther(subject, predicate, delim))) {

                return Null;
            }
            boolean sa = subject instanceof AliasConcept.AliasAtom;
            if (sa) {
                Term sd = ((AliasConcept.AliasAtom) subject).target;
                if (sd.equals(predicate) || containEachOther(sd, predicate, delim))
                    return Null;
            }
            boolean pa = predicate instanceof AliasConcept.AliasAtom;
            if (pa) {
                Term pd = ((AliasConcept.AliasAtom) predicate).target;
                if (pd.equals(subject) || containEachOther(pd, subject, delim))
                    return Null;
            }
            if (sa && pa) {
                if (containEachOther(((AliasConcept.AliasAtom) subject).target, ((AliasConcept.AliasAtom) predicate).target, delim))
                    return Null;
            }

        }

        return op == SIM && subject.compareTo(predicate) > 0 ?
                compound(op, dt, predicate, subject) :
                compound(op, dt, subject, predicate);
    }

    public Term conj(int dt, Term[] u) {
        switch (u.length) {

            case 0:
                return True;

            case 1:
                Term only = u[0];
                if (only instanceof EllipsisMatch) {

                    return conj(dt, only.arrayShared());
                } else {


                    return only instanceof Ellipsislike ?
                            compoundExact(CONJ, dt, only)
                            :
                            only;
                }

        }

        int trues = 0;
        for (Term t : u) {
            if (t == Null || t == False)
                return t;
            else if (t == True)
                trues++;
        }

        if (trues > 0) {

            int sizeAfterTrueRemoved = u.length - trues;
            switch (sizeAfterTrueRemoved) {
                case 0:

                    return True;
                case 1: {

                    for (Term uu : u) {
                        if (uu != True) {
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
                        if (uu != True)
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
                    Term a = u[0];
                    Term b = u[1];
                    if (Term.commonStructure(a, b)) {
                        if (a.equals(b))
                            return u[0];
                        if (a.equalsNeg(b))
                            return False;
                    }

//                    if (!a.hasAny(Op.CONJ.bit) && !b.hasAny(Op.CONJ.bit)) {
//
//
//                        return theExact(CONJ, dt, sorted(u[0], u[1]));
//                    }

                }


                assert u.length > 1;
                Conj c = new Conj();
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
                        return True;

                    case 1:
                        return u[0];

                    default:
                        boolean unordered = false, innerXternal = false;
                        for (int i = 0; i < ul; i++) {
                            Term uu = u[i];
                            if (i < ul-1 && uu.compareTo(u[i + 1]) > 0) {
                                unordered = true;
                                break;
                            }
                            if (uu.op()==CONJ && uu.dt()==XTERNAL)
                                innerXternal = true;
                        }

                        if (innerXternal) {
                            final boolean[] repeats = {false};
                            //flatten inner xternal to top layer
                            SortedSet<Term> uux = new TreeSet();
                            for (int i = 0; i < ul; i++) {
                                Term uu = u[i];
                                if (uu.op() == CONJ && uu.dt() == XTERNAL)
                                    uu.subterms().forEach(uut -> {
                                        if (!uux.add(uut))
                                            repeats[0] = true;
                                    });
                                else
                                    uux.add(uu);
                            }
                            if (uux.size()==1 && repeats[0]) {
                                return compoundExact(CONJ, XTERNAL, uux.first(), uux.first());
                            }
                            return compoundExact(CONJ, XTERNAL, uux.toArray(EmptyTermArray));
                        }

                        if (unordered) {
                            u = u.clone();
                            if (ul == 2) {

                                Term u0 = u[0];
                                u[0] = u[1];
                                u[1] = u0;
                            } else {
                                Arrays.sort(u);
                            }
                        }

                        return compoundExact(CONJ, XTERNAL, u);

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
                    if (Param.DEBUG_EXTRA)
                        throw new RuntimeException("temporal conjunction with n!=2 subterms");
                    return Null;
                }

                return Conj.conjMerge(u[0], u[1], dt);
            }
        }

    }

    public Term root(Compound x) {
        return x.temporalize(Retemporalize.root);
    }
    public Term concept(Compound x) {
        Term term = x.unneg().root();

        Op op = term.op();
        assert (op != NEG): this + " concept() to NEG: " + x.unneg().root();
        if (!op.conceptualizable)
            return Null;


        Term term2 = term.normalize();
        if (term2 != term) {
            if (term2 == null)
                return Null;

            //assert (term2.op() == op): term2 + " not a normal normalization of " + term; //<- allowed to happen when image normalization is involved

            term = term2.unneg();
        }


        return term;
    }

}
