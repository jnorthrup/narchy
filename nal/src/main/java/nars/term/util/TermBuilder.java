package nars.term.util;

import nars.Op;
import nars.Param;
import nars.subterm.ArrayTermVector;
import nars.subterm.BiSubterm;
import nars.subterm.Subterms;
import nars.subterm.UniSubterm;
import nars.term.Compound;
import nars.term.Term;
import nars.term.anon.AnonID;
import nars.term.anon.AnonVector;
import nars.term.atom.Bool;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;
import nars.term.util.transform.CompoundNormalization;
import nars.term.util.transform.Retemporalize;
import nars.unify.match.EllipsisMatch;
import nars.unify.match.Ellipsislike;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import static nars.Op.*;
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

    protected Term theCompound(Op o, int dt, Term[] t, @Nullable byte[] key) {
        assert (!o.atomic) : o + " is atomic, with subterms: " + Arrays.toString(t);

        boolean hasEllipsis = false;

        for (Term x : t) {
            if (x == Null)
                return Null;
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

        return theCompound(o, dt, subterms(o, t), key);
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

        boolean dtConcurrent = Conj.concurrent(dt) && dt!=XTERNAL;
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

            if (predicate.op() == NEG)
                return statement(IMPL, dt, subject, predicate.unneg()).neg();



            switch (predicate.op()) {
                case BOOL:
                    return Null;
                case NEG:
                    return statement(IMPL, dt, subject, predicate.unneg()).neg();
                case IMPL: {
                    Term newSubj;
                    Term inner = predicate.sub(0);
                    if (dt==DTERNAL || dt == XTERNAL) {
                        newSubj = CONJ.the(subject, dt, inner);
                    } else {
                        newSubj = Conj.conjMerge(subject, 0, inner, subject.dtRange() + dt);
                    }
                    return statement(IMPL, predicate.dt(), newSubj, predicate.sub(1));
                }
            }





            if (dt != XTERNAL && subject.dt() != XTERNAL && predicate.dt() != XTERNAL) {

                if (dt==DTERNAL && subject.dt()!=DTERNAL) {
                    //parallelize the impl if the subject is a sequence
                    dt = 0;
                }
                //TODO simple case when no CONJ or IMPL are present

                if (Term.commonStructure(subject, predicate)) {
//                ArrayHashSet<LongObjectPair<Term>> se = new ArrayHashSet<>(4);
//                subject.eventsWhile((w, t) -> {
//                    se.add(PrimitiveTuples.pair(w, t));
//                    return true;
//                }, 0, true, true, false, 0);
                    Conj se = new Conj();
                    se.add(subject.dt() != DTERNAL ? 0 : (dt != DTERNAL ? 0 : ETERNAL), subject);

                    final boolean[] subjChange = {false}, predChange = {false};
                    //TODO extract this to a ConjConflict class
                    Conj pe = new Conj(se.termToId, se.idToTerm) {  //share same term map
                        @Override
                        protected int addFilter(long at, Term x, byte id) {
                            int f = se.conflictOrSame(at, id);
                            int f2 = (at == ETERNAL || f == -1) ? f : se.conflictOrSame(ETERNAL, id);
                            if (f == -1 || f2 == -1)
                                return -1;
                            if (f == +1 || f2 == +1) {
                                predChange[0] = true;
                                return +1; //ignore this term (dont repeat in the predicate)
                            }
                            return f;
                        }
                    };
                    long offset = (dt != DTERNAL) ? dt + subject.dtRange() : (predicate.dt() != DTERNAL ? 0 : ETERNAL);
                    if (!pe.add(offset, predicate))
                        return False;

                    if (predChange[0]) {
                        Term newPred = pe.term();
                        if (newPred instanceof Bool)
                            return newPred;

                        if (dt != DTERNAL) {
                            Term f = Conj.firstEventTerm(newPred);
                            int shift = predicate.subTimeFirst(f);
                            if (shift == DTERNAL)
                                return Null; //??
                            dt += shift;
                        }

                        predicate = newPred;
                    }
                    if (subjChange[0]) {
                        Term newSubj = se.term();
                        if (newSubj instanceof Bool) {
                            if (newSubj == True)
                                return predicate;
                        }
                        if (dt != DTERNAL) {
                            //TODO instead of dtRange, it should be calculated according to the time of the last event that matches the last event of the new subject
                            //otherwise it is inaccurate for repeating terms like (x &&+1 x) where it will by default stretch the wrong direction
                            int dr = newSubj.dtRange() - subject.dtRange();
                            dt += dr;
                        }
                        subject = newSubj;
                    }

                    //test this after all of the recursions because they may have logically eliminated an IMPL that was in the input
                    //TODO valid cases where subj has impl?
                    if (subject.hasAny(IMPL))
                        return Null;


                    if (subjChange[0] || predChange[0]) {
                        return statement(IMPL, dt, subject, predicate); //recurse
                    } else {
                        return compound(IMPL, dt, subject, predicate);
                    }
                }

            }


        } else if (op == SIM) {
            if (subject.compareTo(predicate) > 0) {
                //swap order
                Term x = predicate;
                predicate = subject;
                subject = x;
            }
        }

        if ((op != IMPL || dtConcurrent) && (!subject.hasAny(Op.VAR_PATTERN) && !predicate.hasAny(Op.VAR_PATTERN))) {

            Predicate<Term> delim = (op == IMPL) ?
                    recursiveCommonalityDelimeterStrong : Op.recursiveCommonalityDelimeterWeak;

            if ((containEachOther(subject, predicate, delim)))
                return Null;

//            boolean sa = subject instanceof AliasConcept.AliasAtom;
//            if (sa) {
//                Term sd = ((AliasConcept.AliasAtom) subject).target;
//                if (sd.equals(predicate) || containEachOther(sd, predicate, delim))
//                    return Null;
//            }
//            boolean pa = predicate instanceof AliasConcept.AliasAtom;
//            if (pa) {
//                Term pd = ((AliasConcept.AliasAtom) predicate).target;
//                if (pd.equals(subject) || containEachOther(pd, subject, delim))
//                    return Null;
//            }
//            if (sa && pa) {
//                if (containEachOther(((AliasConcept.AliasAtom) subject).target, ((AliasConcept.AliasAtom) predicate).target, delim))
//                    return Null;
//            }
        }

        return compound(op, dt, subject, predicate);
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
                            compound(CONJ, dt, only)
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
                    Term a = u[0], b = u[1];
                    if (Term.commonStructure(a, b)) {
                        if (a.equals(b))
                            return u[0];
                        if (a.equalsNeg(b))
                            return False;
                    }

                    if (!a.hasAny(Op.CONJ.bit) && !b.hasAny(Op.CONJ.bit)) {
                        //fast construct for simple case, verified above to not contradict itself
                        return compound(CONJ, dt, sorted(u[0], u[1]));
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
                        return True;

                    case 1:
                        return u[0];

                    default: {
                        MutableSet<Term> uux = new UnifiedSet(ul);
                        for (int i = 0; i < ul; i++) {
                            Term uu = u[i];
                            if (uu.op() == CONJ && uu.dt() == XTERNAL) {
                                uu.subterms().forEach(uut -> {
                                    uux.add(uut);
                                });
                            } else {
                                uux.add(uu);
                            }
                        }


                        if (uux.size() == 1) {
                            Term only = uux.getOnly();
                            return compound(CONJ, XTERNAL, only, only); //repeat
                        } else {
                            return compound(CONJ, XTERNAL, sorted(uux));
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

    public final Term statement(Op op, int dt, Term[] u) {
        assert (u.length == 2): op + " requires 2 arguments, but got: " + Arrays.toString(u);
        return statement(op, dt, u[0], u[1]);
    }

}
