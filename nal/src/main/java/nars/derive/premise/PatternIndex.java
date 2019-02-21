package nars.derive.premise;

import jcog.TODO;
import jcog.WTF;
import nars.Builtin;
import nars.Op;
import nars.index.concept.MapConceptIndex;
import nars.subterm.BiSubterm;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Variable;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.compound.CachedCompound;
import nars.term.util.transform.Retemporalize;
import nars.term.util.transform.TermTransform;
import nars.term.util.transform.VariableNormalization;
import nars.unify.Unify;
import nars.unify.ellipsis.Ellipsis;
import nars.unify.ellipsis.EllipsisMatch;
import nars.unify.ellipsis.Ellipsislike;
import nars.unify.mutate.Choose1;
import nars.unify.mutate.Choose2;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

import static nars.Op.*;
import static nars.time.Tense.XTERNAL;
import static nars.unify.ellipsis.Ellipsis.firstEllipsis;

/**
 * Index which specifically holds the target components of a deriver ruleset.
 */
public class PatternIndex extends MapConceptIndex {


    public PatternIndex() {
        super(new ConcurrentHashMap<>(1024));
    }


    /*@NotNull*/
    private static PremisePatternCompound ellipsis(/*@NotNull*/ Compound seed, /*@NotNull*/ Subterms v, /*@NotNull*/ Ellipsis e) {
        Op op = seed.op();

        boolean commutative = (/*!ellipsisTransform && */op.commutative);

        if (commutative) {
            return new PremisePatternCompound.PremisePatternCompoundWithEllipsisCommutive(seed.op(), seed.dt(), e, v);
        } else {
            return PremisePatternCompound.PremisePatternCompoundWithEllipsisLinear.the(seed.op(), seed.dt(), e, v);
        }

    }

    @SuppressWarnings("Java8MapApi")
    @Override
    public Term get(/*@NotNull*/ Term x, boolean createIfMissing) {
        //return x.target();
        Op xop = x.op();
        if (xop == NEG) {
            Term u = x.unneg();
            Term v = get(u, createIfMissing);
            return v == u ? x : v.neg();
        }

        if (!xop.conceptualizable)
            return x;

        Termed y = intern.get(x);
        if (y != null) {
            return (Term) y;
        }
        if (nar != null && xop == ATOM) {

            Termed xx = nar.concept(x);
            if (xx != null) {
                intern.put(xx.term(), xx);
                return (Term) xx;
            }
        }

        Term yy = patternify(x);
        intern.put(yy, yy);
        return yy;
    }

    public static Term patternify(Term x) {
        if (x instanceof Compound)
            return Ellipsify.transformCompound((Compound) x);
        return x;
    }


    public /*@NotNull*/ Term rule(Term x) {
        return get(new PremiseRuleNormalization().transform(x), true).term();
    }

//    public final PrediTerm<Derivation> intern(@Nullable PrediTerm<Derivation> x) {
//        if (x == null)
//            return null;
//        PrediTerm<Derivation> y = pred.putIfAbsent(x.target(), x);
//        return y != null ? y : x;
//    }


    public final Term intern(Term x) {
        return get(x, true); //.target();
    }

    public static final class PremiseRuleNormalization extends VariableNormalization {


        @Override
        public Term transform(Term x) {
            /** process completely to resolve built-in functors,
             * to override VariableNormalization's override */
            //return TermTransform.NegObliviousTermTransform.super.transform(x);
            return (x instanceof Compound) ?
                    transformCompound((Compound) x)
                    :
                    transformAtomic((Atomic) x);
        }

        @Override
        protected Term transformNonNegCompound(Compound x) {
            /** process completely to resolve built-in functors,
             * to override VariableNormalization's override */
            return transformCompound(x, x.op(), x.dt());
        }


        @Override
        public Term transformAtomic(Atomic x) {
            if (x instanceof Atom) {
                Functor f = Builtin.functor(x);
                return f != null ? f : x;
            }
            return super.transformAtomic(x);
        }

        /*@NotNull*/
        @Override
        protected Variable newVariable(/*@NotNull*/ Variable x) {


            if (x instanceof Ellipsis.EllipsisPrototype) {
                return Ellipsis.EllipsisPrototype.make((byte) count,
                        ((Ellipsis.EllipsisPrototype) x).minArity);
            } else if (x instanceof Ellipsis) {
                return x;


            } /*else if (v instanceof GenericVariable) {
                return ((GenericVariable) v).normalize(actualSerial); 
            } else {
                return v(v.op(), actualSerial);
            }*/
            return super.newVariable(x);
        }


    }

    /**
     * seems used only if op==CONJ
     */
    @Deprecated
    abstract public static class PremisePatternCompound extends CachedCompound.TemporalCachedCompound {

        PremisePatternCompound(/*@NotNull*/ Op op, int dt, Subterms subterms) {
            super(op, dt, subterms);
        }

        @Override
        public final boolean the() {
            return false;
        }

        @Override
        public final @Nullable Term normalize(byte varOffset) {
            throw new UnsupportedOperationException("normalize before patternify");
        }

        public abstract static class PremisePatternCompoundWithEllipsis extends PremisePatternCompound {

            final Ellipsis ellipsis;

            PremisePatternCompoundWithEllipsis(/*@NotNull*/ Op seed, int dt, Ellipsis ellipsis, Subterms subterms) {
                super(seed, dt, subterms);
                this.ellipsis = ellipsis;
            }

            abstract protected boolean matchEllipsis(Term y, Unify subst);

            @Override
            public final boolean unifySubterms(Term y, Unify u) {
                return matchEllipsis(y, u);
            }
        }


        public static final class PremisePatternCompoundWithEllipsisLinear extends PremisePatternCompoundWithEllipsis {

            public static PremisePatternCompoundWithEllipsisLinear the(Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
                if (op.statement) {
                    //HACK
                    Term x = subterms.sub(0);
                    Term y = subterms.sub(1);
                    if (x instanceof Ellipsislike) {
                        //raw ellipsis, the conjunction got removed somewhere. HACK re-add it
                        x = CONJ.the(x);
                    }
                    if (y instanceof Ellipsislike) {
                        //raw ellipsis, the conjunction got removed somewhere. HACK re-add it
                        y = CONJ.the(y);
                    }
                    subterms = new BiSubterm(x, y); //avoid interning
                }
                return new PremisePatternCompoundWithEllipsisLinear(op, dt, ellipsis, subterms);
            }

            private PremisePatternCompoundWithEllipsisLinear(/*@NotNull*/ Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
                super(op, dt, ellipsis, subterms);
                if (op.statement && subterms.OR(x -> x instanceof Ellipsislike))
                    throw new WTF("raw ellipsis subj/pred makes no sense here");
            }

            /**
             * non-commutive compound match
             * X will contain at least one ellipsis
             * <p>
             * match subterms in sequence
             * <p>
             * WARNING this implementation only works if there is one ellipse in the subterms
             * this is not tested for either
             */
            @Override
            protected boolean matchEllipsis(Term y, Unify u) {
                Subterms Y = y.subterms();
                int xi = 0, yi = 0;
                int xsize = subs();
                int ysize = Y.subs();


                while (xi < xsize) {
                    Term x = sub(xi++);

                    if (x instanceof Ellipsis) {
                        int available = ysize - yi;

                        Term xResolved = u.resolvePosNeg(x);
                        if (xResolved == x) {


                            if (xi == xsize) {
                                //the ellipsis is at the right edge so capture the remainder
                                if (!ellipsis.validSize(available))
                                    return false;

                                return ellipsis.unify(EllipsisMatch.matched(Y, yi, yi + available), u);

                            } else {
                                //TODO ellipsis is in the center or beginning
                                throw new TODO();
                            }
                        } else {


                            if (xResolved instanceof EllipsisMatch) {
                                EllipsisMatch xe = (EllipsisMatch) xResolved;
                                if (!xe.linearMatch(Y, yi, u))
                                    return false;
                                yi += xe.subs();
                            } else {

                                if (!sub(yi).unify(xResolved, u))
                                    yi++;
                            }
                        }


                    } else {
                        if (ysize <= yi || !x.unify(Y.sub(yi++), u))
                            return false;
                    }
                }

                return true;
            }


        }


        public static final class PremisePatternCompoundWithEllipsisCommutive extends PremisePatternCompoundWithEllipsis {


            public PremisePatternCompoundWithEllipsisCommutive(Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
                super(op, dt, ellipsis, subterms);
                assert (op != CONJ || dt == XTERNAL); //CONJ always XTERNAL
            }

            /**
             * commutive compound match: Y into X which contains one ellipsis
             * <p>
             * X pattern contains:
             * <p>
             * one unmatched ellipsis (identified)
             * <p>
             * <p>
             * zero or more "constant" (non-pattern var) terms
             * all of which Y must contain
             * <p>
             * zero or more (non-ellipsis) pattern variables,
             * each of which may be matched or not.
             * matched variables whose resolved values that Y must contain
             * unmatched variables determine the amount of permutations/combinations:
             * <p>
             * if the number of matches available to the ellipse is incompatible with the ellipse requirements, fail
             * <p>
             * (total eligible terms) Choose (total - #normal variables)
             * these are then matched in revertable frames.
             * <p>
             * *        proceed to collect the remaining zero or more terms as the ellipse's match using a predicate filter
             *
             * @param y the compound being matched to this
             */
            @Override
            protected boolean matchEllipsis(Term Y, Unify u) {
                if ((dt != XTERNAL) && Y.op().temporal && !Y.isCommutative())
                    throw new TODO();


                Subterms y = Y.subterms();


//                @Nullable Versioned<MatchConstraint> uc = u.constraints(ellipsis);

                //xFixed is effectively sorte unless eMatch!=nulld


                SortedSet<Term> yFree =
                        //uc==null ? y.toSetSorted() : y.toSetSorted(yy -> MatchConstraint.valid(yy, uc, u));
                        //y.toSetSorted();
                        y.toSetSorted(u::resolvePosNeg);

                Subterms xx = subterms();
                int s = xx.subs();
                TermList xMatch = new TermList(s);

                Ellipsis ellipsis = this.ellipsis;
                for (int k = 0; k < s; k++) {

                    Term xk = xx.sub(k);
                    Term xxk = u.resolvePosNeg(xk);

                    if (xk.equals(ellipsis)) {
                        if (xxk.equals(xk))
                            continue; //unassigned ellipsis

                        ellipsis = null;
                        if (xxk instanceof EllipsisMatch) {
                            for (Term ex : xxk.subterms()) {
                                if (!include(ex, xMatch, yFree, u))
                                    return false;
                            }
                            continue;
                        }
                        //else it is ellipsis that matched a single term, continue below:

                    }

                    if (!include(xxk, xMatch, yFree, u))
                        return false;

                }


                int xs = xMatch.size();
                int ys = yFree.size();

                if (ellipsis == null) {
                    //ellipsis assigned already; match the remainder as usual
                    if (xs == ys) {
                        switch (xs) {
                            case 0:
                                return true;
                            case 1:
                                return xMatch.getFirst().unify(yFree.first(), u);
                            default:
                                xMatch.sortThis();
                                return xMatch.unifyCommute(new TermList(yFree), u);
                        }
                    } else {
                        //arity mismatch
                        return false;
                    }
                }

                int numRemainingForEllipsis = ys - xs;
                if (!ellipsis.validSize(numRemainingForEllipsis))
                    return false;


                if (xs > 0 && ys > 0) {
                    //test matches against the one constant target
                    for (Iterator<Term> xi = xMatch.iterator(); xi.hasNext(); ) {
                        Term ix = xi.next();
                        if (u.var(ix)) continue;

                        boolean canMatch = false;
                        Term onlyY = null;
                        for (Term yy : yFree) {
                            if (Subterms.possiblyUnifiable(ix, yy, u.varBits)) {
                                canMatch = true;
                                if (onlyY == null)
                                    onlyY = yy; //first found and only so far
                                else {
                                    onlyY = null;
                                    break; //found > 1 so stop
                                }
                            }
                        }

                        if (canMatch) {
                            if (onlyY != null) {
                                if (ix.unify(onlyY, u)) {
                                    xi.remove();
                                    yFree.remove(onlyY);
                                    xs--;
                                    ys--;
                                } else
                                    return false; //impossible
                            } //else: continue

                        } else {
                            return false; //nothing from yFree could match xFixed
                        }
                    }
                }

                if (xs == 1 && xs == ys) {
                    return xMatch.getFirst().unify(yFree.first(), u) && ellipsis.unify(EllipsisMatch.empty, u);
                }

                switch (xs) {
                    case 0:
                        return ellipsis.unify(ys > 0 ? EllipsisMatch.matched(yFree) : EllipsisMatch.empty, u);

                    case 1:
                        //no matches possible but need one
                        return ys >= 1 && Choose1.choose1(ellipsis, xMatch, yFree, u);

                    case 2:
                        return ys >= 2 && Choose2.choose2(ellipsis, xMatch, yFree, u);

                    default:
                        throw new RuntimeException("unimpl: " + xs + " arity combination unimplemented");
                }


            }

            private static boolean include(Term x, List<Term> xMatch, SortedSet<Term> yFree, Unify u) {
                if (!u.var(x)) {
                    boolean rem = yFree.remove(x);
                    if (rem)
                        return true;

                    if (x.hasAny(Op.Temporal)) {
                        for (Iterator<Term> iterator = yFree.iterator(); iterator.hasNext(); ) {
                            Term y = iterator.next();
                            if (!u.var(y)) {
                                //at this point volume, structure, etc can be compared
                                if (x.unify(y, u)) {
                                    iterator.remove();
                                    return true;
                                }
                            }
                        }
                    }

                }

                return xMatch.add(x);
            }

        }


    }

    private static final TermTransform.NegObliviousTermTransform Ellipsify = new TermTransform.NegObliviousTermTransform() {


        @Override
        protected @Nullable Term transformNonNegCompound(Compound x) {
            Term __x = Retemporalize.retemporalizeAllToXTERNAL.transformCompound(x);
            if (!(__x instanceof Compound))
                return __x;

            Term _x = super.transformNonNegCompound((Compound) __x);
            if (!(_x instanceof Compound)) {
                return _x;
            }

            x = (Compound) _x;

            Term xx;
            boolean neg = x.op() == NEG;
            if (neg)
                xx = x.unneg();
            else xx = x;

            @Nullable Ellipsislike e = firstEllipsis(xx.subterms());
            return (e != null ? ellipsis((Compound) xx, xx.subterms(), (Ellipsis) e) : xx).negIf(neg);
        }
    };
}
