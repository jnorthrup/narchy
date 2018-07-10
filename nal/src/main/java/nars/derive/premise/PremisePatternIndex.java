package nars.derive.premise;

import jcog.TODO;
import nars.Builtin;
import nars.NAR;
import nars.Op;
import nars.index.concept.MapConceptIndex;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.compound.CachedCompound;
import nars.unify.Unify;
import nars.unify.constraint.MatchConstraint;
import nars.unify.match.Ellipsis;
import nars.unify.match.EllipsisMatch;
import nars.unify.match.Ellipsislike;
import nars.unify.mutate.Choose1;
import nars.unify.mutate.Choose2;
import nars.util.term.transform.Retemporalize;
import nars.util.term.transform.TermTransform;
import nars.util.term.transform.VariableNormalization;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static nars.Op.ATOM;
import static nars.Op.CONJ;
import static nars.time.Tense.XTERNAL;
import static nars.unify.match.Ellipsis.firstEllipsis;

/**
 * Index which specifically holds the term components of a deriver ruleset.
 */
public class PremisePatternIndex extends MapConceptIndex {

    //final Map<InternedSubterms, Subterms> subterms = new HashMap<>(1024);
    //private final Map<Term, PrediTerm<Derivation>> pred = new HashMap<>(1024);
    private final Map<Term, MatchConstraint> constra = new HashMap<>(1024);

    public PremisePatternIndex() {
        super(new HashMap<>(1024));
    }

    public PremisePatternIndex(NAR nar) {
        this();
        this.nar = nar;
    }

    /*@NotNull*/
    private static PremisePatternCompound ellipsis(/*@NotNull*/ Compound seed, /*@NotNull*/ Subterms v, /*@NotNull*/ Ellipsis e) {
        Op op = seed.op();

        boolean commutative = (/*!ellipsisTransform && */op.commutative);

        if (commutative) {
            return new PremisePatternCompound.PremisePatternCompoundWithEllipsisCommutive(seed.op(), seed.dt(), e, v);
        } else {
            return new PremisePatternCompound.PremisePatternCompoundWithEllipsisLinear(seed.op(), seed.dt(), e, v);
        }

    }

    @SuppressWarnings("Java8MapApi")
    @Override
    public Termed get(/*@NotNull*/ Term x, boolean createIfMissing) {
        //return x.term();
        if (!x.op().conceptualizable)
            return x;


        Termed y = concepts.get(x);
        if (y == null) {
            if (nar != null && x.op() == ATOM) {

                Termed xx = nar.concepts.get(x, false);
                if (xx != null) {
                    concepts.put(xx.term(), xx);
                    return xx;
                }
            }

            Term yy = patternify(x);
            concepts.put(yy, yy);
            return yy;
        } else {
            return y;
        }
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
//        PrediTerm<Derivation> y = pred.putIfAbsent(x.term(), x);
//        return y != null ? y : x;
//    }

    public final MatchConstraint intern(MatchConstraint x) {
        MatchConstraint y = constra.putIfAbsent(x.term(), x);
        return y != null ? y : x;
    }

    public final Term intern(Term x) {
        return (Term) get(x, true); //.term();
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
        public Term transformCompound(Compound x) {
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
            } else if (x instanceof Ellipsis || x == Op.imExt || x == Op.imInt) {
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
            //super((Compound) HeapTermBuilder.the.compound(op, subterms.arrayShared()), dt);
        }

        @Override
        public Term the() {
            return null; //super.the();
        }

        abstract protected static class PremisePatternCompoundWithEllipsis extends PremisePatternCompound {

            final Ellipsis ellipsis;
            private final int subtermStructure;


            PremisePatternCompoundWithEllipsis(/*@NotNull*/ Op seed, int dt, Ellipsis ellipsis, Subterms subterms) {
                super(seed, dt, subterms);

                this.subtermStructure = subterms.structure();
                this.ellipsis = ellipsis;

            }

            abstract protected boolean matchEllipsis(Term y, Unify subst);

            @Override
            public final boolean unifySubterms(Term y, Unify u) {

                if (!Terms.commonStructureTest(subtermStructure, y.subterms(), u))
                    return false;

                return matchEllipsis(y, u);
            }
        }


        public static class PremisePatternCompoundWithEllipsisLinear extends PremisePatternCompoundWithEllipsis {

            public PremisePatternCompoundWithEllipsisLinear(/*@NotNull*/ Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
                super(op, dt, ellipsis, subterms);
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
                int i = 0, j = 0;
                int xsize = subs();
                int ysize = Y.subs();


                while (i < xsize) {
                    Term x = sub(i++);

                    if (x instanceof Ellipsis) {
                        int available = ysize - j;

                        Term eMatched = u.resolve(x);
                        if (eMatched != x) {


                            if (i == xsize) {

                                if (!ellipsis.validSize(available))
                                    return false;

                                return ellipsis.unify(EllipsisMatch.match(Y, j, j + available), u);

                            } else {


                                return false;
                            }
                        } else {


                            if (eMatched instanceof EllipsisMatch) {
                                EllipsisMatch ex = (EllipsisMatch) eMatched;
                                if (!ex.linearMatch(Y, j, u))
                                    return false;
                                j += ex.subs();
                            } else {

                                if (!sub(j).unify(eMatched, u))
                                    j++;
                            }
                        }


                    } else {
                        if (ysize <= j || !x.unify(Y.sub(j++), u))
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
                if (Y.op().temporal && (dt != XTERNAL) && !Y.isCommutative())
                    throw new TODO();

                Subterms y = Y.subterms();


                SortedSet<Term> xFixed = new TreeSet();


                Ellipsis ellipsis = this.ellipsis;

                SortedSet<Term> yFree = y.toSetSorted();

                Subterms ss = subterms();
                int s = ss.subs();

                for (int k = 0; k < s; k++) {


                    Term x = ss.sub(k);

                    if (x.equals(ellipsis)) {
                        Term v = u.resolve(x);
                        if (v != x) {
                            if (v instanceof EllipsisMatch) {
                                if (!((EllipsisMatch) v).rematch(y, yFree))
                                    return false;
                                ellipsis = null;
                            } else {

                                if (u.constant(v) && !yFree.remove(v))
                                    return false;
                            }
                        }

                        continue;
                    }

                    
                    
                    
                    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

                    /*if (v instanceof EllipsisMatch) {

                        
                        if (!((EllipsisMatch) v).addWhileMatching(y, alreadyInY, ellipsis.sizeMin())) {
                            return false;
                        } else {
                            
                            ellipsisMatched = true;
                            break; 
                        }


                    } else */


                    boolean xConst = u.constant(x);
                    if (!xConst) {


                        xFixed.add(x);

                    } else {

                        if (!yFree.remove(x)) {
                            return false;
                        }


                    }


                }

                if (ellipsis == null)
                    return yFree.isEmpty();

                final int xs = xFixed.size();
                int ys = yFree.size();
                int numRemainingForEllipsis = ys - xs;


                boolean vs = ellipsis.validSize(numRemainingForEllipsis);
                if (!vs)
                    return false;

                switch (xs) {
                    case 0:

                        Term match = ys > 0 ? EllipsisMatch.match(yFree) : EllipsisMatch.empty;


                        return this.ellipsis.unify(match, u);


                    case 1:
                        Term x0 = xFixed.first();
                        if (yFree.size() == 1) {
                            return this.ellipsis.unify(EllipsisMatch.empty, u) && x0.unify(yFree.first(), u);
                        } else {
                            return u.termutes.add(new Choose1(this.ellipsis, x0, yFree));
                        }

                    case 2:
                        return u.termutes.add(new Choose2(this.ellipsis, u, xFixed, yFree));

                    default:

                        throw new RuntimeException("unimpl: " + xs + " arity combination unimplemented");
                }


            }

        }


    }

    private static final TermTransform.NegObliviousTermTransform Ellipsify = new TermTransform.NegObliviousTermTransform() {

        @Override
        public @Nullable Term transformCompound(Compound x) {
            x = (Compound) NegObliviousTermTransform.super.transformCompound(
                    (Compound) Retemporalize.retemporalizeAllToXTERNAL.transformCompound(x)
            );

            @Nullable Ellipsislike e = firstEllipsis(x.subterms());
            if (e!=null) {
                return ellipsis(x, x.subterms(), (Ellipsis) e);
            }
            return x;
        }
    };
}
