package nars.derive.premise;

import nars.NAR;
import nars.Op;
import nars.index.concept.MapConceptIndex;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.compound.LightCompoundDT;
import nars.term.var.Variable;
import nars.unify.Unify;
import nars.unify.match.Ellipsis;
import nars.unify.match.EllipsisMatch;
import nars.unify.mutate.Choose1;
import nars.unify.mutate.Choose2;
import nars.unify.mutate.CommutivePermutations;
import nars.util.term.InternedSubterms;
import nars.util.term.transform.VariableNormalization;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static nars.Op.*;
import static nars.util.time.Tense.XTERNAL;

/**
 * Index which specifically holds the term components of a deriver ruleset.
 */
public class PremisePatternIndex extends MapConceptIndex {

    final Map<InternedSubterms,Subterms> subterms = new HashMap<>(512);

    public PremisePatternIndex() {
        super(new HashMap<>(512));
    }

    public PremisePatternIndex(NAR nar) {
        this();
        this.nar = nar;
    }


//    /**
//     * installs static and NAR-specific functors
//     */
//    public PatternIndex(NAR nar) {
//        this();
////        for (Termed t : Derivation.ruleFunctors(nar)) {
////            set(t);
////        }
//    }

    @SuppressWarnings("Java8MapApi")
    @Override
    public Termed get(/*@NotNull*/ Term x, boolean createIfMissing) {
        if (!x.op().conceptualizable)
            return x;



        //avoid recursion-caused concurrent modification exception
        Termed y = concepts.get(x);
        if (y == null) {
            if (nar!=null && x.op()==ATOM) {
                //resolve NAR built-in functors
                Termed xx = nar.concepts.get(x,false);
                if (xx!=null) {
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

    private Term patternify(Term x) {
        if (x instanceof Compound)
            return patternify((Compound)x);
        return x;
    }

    /*@NotNull*/
    public Term patternify(/*@NotNull*/ Compound x) {

        if (x.op()==NEG)
            return patternify(x.unneg()).neg();

        Subterms s = x.subterms();
        int ss = s.subs();
        Term[] bb = new Term[ss];
        boolean changed = false;//, temporal = false;
        for (int i = 0; i < ss; i++) {
            Term a = s.sub(i);
            Term b = get(a, true).term();
            if (a != b) {
                changed = true;
            }
            bb[i] = b;
        }

        if (!changed && Ellipsis.firstEllipsis(s) == null)
            return x;

        Subterms v = subterms.computeIfAbsent(new InternedSubterms(bb), InternedSubterms::compute);

//        Subterms v = changed ? Subterms.subtermsInterned(bb.length > 1 && x.op().commutative && (concurrent(x.dt())) ?
//                Terms.sorted(bb) :
//                bb) : s;

        Ellipsis e = Ellipsis.firstEllipsis(v);
        return e != null ?
                ellipsis(x, v, e) :
                theCompound(x.op(), x.dt(), v);
                    //x.op().the(x.dt(), v.arrayShared()); //new PatternCompound.PatternCompoundSimple(x.op(), x.dt(), v);
    }


    //    static boolean canBuildConcept(/*@NotNull*/ Term y) {
//        if (y instanceof Compound) {
//            return y.op() != NEG;
//        } else {
//            return !(y instanceof Variable);
//        }
//
//    }


    /*@NotNull*/
    private static PremisePatternCompound ellipsis(/*@NotNull*/ Compound seed, /*@NotNull*/ Subterms v, /*@NotNull*/ Ellipsis e) {


        //this.ellipsisTransform = hasEllipsisTransform(this);
        //boolean hasEllipsisTransform = false;
        //int xs = seed.size();
//        for (int i = 0; i < xs; i++) {
//            if (seed.sub(i) instanceof EllipsisTransform) {
//                hasEllipsisTransform = true;
//                break;
//            }
//        }

        Op op = seed.op();

        //boolean ellipsisTransform = hasEllipsisTransform;
        boolean commutative = (/*!ellipsisTransform && */op.commutative);

        if (commutative) {
//            if (ellipsisTransform)
//                throw new RuntimeException("commutative is mutually exclusive with ellipsisTransform");

            return new PremisePatternCompound.PremisePatternCompoundWithEllipsisCommutive(seed.op(),
                    //seed.dt(),
                    seed.op() != CONJ ? seed.dt() : XTERNAL,
                    e, v);
        } else {
//            if (ellipsisTransform) {
//                if (op != Op.PROD)
//                    throw new RuntimeException("imageTransform ellipsis must be in an Image or Product compound");
//
//                return new PatternCompound.PatternCompoundWithEllipsisLinearImageTransform(
//                        seed, (EllipsisTransform)e, v);
//            } else {
            return new PremisePatternCompound.PremisePatternCompoundWithEllipsisLinear(seed.op(), seed.dt(), e, v);
//            }
        }

    }

//    public Term pattern(Term x) {
//        return x instanceof Compound ? pattern((Compound) x) : get(x, true).term();
//    }

    /**
     * returns an normalized, optimized pattern term for the given compound
     */
    public /*@NotNull*/ Term pattern(Term x) {
        return get(x.transform(new PremiseRuleVariableNormalization()), true).term();
    }

    public static final class PremiseRuleVariableNormalization extends VariableNormalization {

        @Override
        protected boolean hasVars(Compound t) {
            return t.hasVars() || t.varPattern() > 0;
        }

        /*@NotNull*/
        @Override
        protected Variable newVariable(/*@NotNull*/ Variable x) {


//            if (x instanceof Ellipsis.EllipsisTransformPrototype) {
//                //special
//
//                Ellipsis.EllipsisTransformPrototype ep = (Ellipsis.EllipsisTransformPrototype) x;
//
////                Term from = ep.from;
////                if (from != Op.Imdex) from = applyAfter((GenericVariable)from);
////                Term to = ep.to;
////                if (to != Op.Imdex) to = applyAfter((GenericVariable)to);
////
//                return EllipsisTransform.make(varPattern(actualSerial + ELLIPSIS_TRANSFORM_ID_OFFSET), ep.from, ep.to, this);
//
//            } else
            if (x instanceof Ellipsis.EllipsisPrototype) {
                return Ellipsis.EllipsisPrototype.make((byte) count,
                        ((Ellipsis.EllipsisPrototype) x).minArity);
            } else if (x instanceof Ellipsis || x == Op.imExt || x == Op.imInt) {
                return x;

                //throw new UnsupportedOperationException("?");
//                int idOffset;
//                if (v instanceof EllipsisTransform) {
//                    idOffset = ELLIPSIS_TRANSFORM_ID_OFFSET;
//                } else if (v instanceof EllipsisZeroOrMore) {
//                    idOffset = ELLIPSIS_ZERO_OR_MORE_ID_OFFSET;
//                } else if (v instanceof EllipsisOneOrMore) {
//                    idOffset = ELLIPSIS_ONE_OR_MORE_ID_OFFSET;
//                } else {
//                    throw new RuntimeException("N/A");
//                }
//
//                Variable r = ((Ellipsis) v).clone(varPattern(actualSerial + idOffset), this);
//                offset = 0; //return to zero
//                return r;
            } /*else if (v instanceof GenericVariable) {
                return ((GenericVariable) v).normalize(actualSerial); //HACK
            } else {
                return v(v.op(), actualSerial);
            }*/
            return super.newVariable(x);
        }

//        @Override
//        public final boolean testSuperTerm(/*@NotNull*/ Compound t) {
//            //descend all, because VAR_PATTERN is not yet always considered a variable
//            return true;
//        }

//        /*@NotNull*/
//        public Term applyAfter(/*@NotNull*/ Variable secondary) {
//            if (secondary.equals(Op.Imdex))
//                return secondary; //dont try to normalize any imdex
//            else
//                return apply(secondary);
//        }
    }

    @Deprecated
    abstract public static class PremisePatternCompound extends LightCompoundDT {

    //    final int sizeCached;
    //    final int structureNecessary;
    //    private final boolean commutative; //cached
    //    transient final private Op op; //cached
    //    private final int minVolumeNecessary;
    //    private final int size;

    //    @Nullable public final Set<Variable> uniqueVars;

        PremisePatternCompound(/*@NotNull*/ Op op, int dt, Subterms subterms) {
            super((Compound) op.the(subterms.arrayShared()), dt);
        }

        abstract protected static class PremisePatternCompoundWithEllipsis extends PremisePatternCompound {

            final Ellipsis ellipsis;
            //final int structureRequired;

            PremisePatternCompoundWithEllipsis(/*@NotNull*/ Op seed, int dt, Ellipsis ellipsis, Subterms subterms) {
                super(seed, dt, subterms);

                this.ellipsis = ellipsis;
                //this.structureRequired = subterms.structure() & ~(Op.VariableBits);
            }

            abstract protected boolean matchEllipsis(Subterms y, Unify subst);

            @Override
            public final boolean unifySubterms(Term y, Unify u) {
    //            if (y.volume() < volume())
    //                return false;
                return /*y.hasAll(structureRequired) && */matchEllipsis(y.subterms(), u);
            }
        }


        public static class PremisePatternCompoundWithEllipsisLinear extends PremisePatternCompoundWithEllipsis {

            public PremisePatternCompoundWithEllipsisLinear(/*@NotNull*/ Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
                super(op, dt, ellipsis, subterms);
            }

            @Override
            protected boolean matchEllipsis(Subterms y, Unify subst) {
                return matchEllipsedLinear(y, subst);
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
            final boolean matchEllipsedLinear(Subterms Y, Unify u) {

                int i = 0, j = 0;
                int xsize = subs();
                int ysize = Y.subs();

                //TODO check for shim and subtract xsize?

                while (i < xsize) {
                    Term x = sub(i++);

                    if (x instanceof Ellipsis) {
                        int available = ysize - j;

                        Term eMatched = u.xy(x); //EllipsisMatch, or null
                        if (eMatched == null) {

                            //COLLECT
                            if (i == xsize) {
                                //SUFFIX
                                if (!ellipsis.validSize(available))
                                    return false;

                                return ellipsis.unify(EllipsisMatch.match(Y, j, j + available), u);

                            } else {
                                //PREFIX the ellipsis occurred at the start and there are additional terms following it
                                //TODO
                                return false;
                            }
                        } else {
                            //assert(false): "TODO check this case in PatternCompound ellipsis linear";

                            if (eMatched instanceof EllipsisMatch) {
                                EllipsisMatch ex = (EllipsisMatch) eMatched;
                                if (!ex.linearMatch(Y, j, u))
                                    return false;
                                j += ex.subs();
                            } else {
                                //it is a single ellipsis term to unify against
                                if (!sub(j).unify(eMatched, u))
                                    j++;
                            }
                        }
                        //previous match exists, match against what it had
    //                        if (i == xsize) {
    //                        //SUFFIX - match the remaining terms against what the ellipsis previously collected
    //                        //HACK this only works with EllipsisMatch type
    //                        Term[] sp = ((EllipsisMatch) eMatched).term;
    //                        if (sp.length!=available)
    //                            return false; //incorrect size
    //
    //                        //match every item
    //                        for (Term aSp : sp) {
    //                            if (!match(aSp, Y.term(j++)))
    //                                return false;
    //                        }
    //                        } else {
    //                            //TODO other cases
    //                            return false;
    //                        }
    //                    }
                    } else {
                        if (ysize <= j || !x.unify(Y.sub(j++), u))
                            return false;
                    }
                }

                return true;
            }


        }


        public static final class PremisePatternCompoundWithEllipsisCommutive extends PremisePatternCompoundWithEllipsis {


    //        /** the components of this pattern compound other than the ellipsis "*/
    //        final ImmutableSet<Term> fixed;

            public PremisePatternCompoundWithEllipsisCommutive(Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
                super(op,
                      op==CONJ ? XTERNAL : dt,
                      ellipsis, subterms);


    //            MutableSet<Term> f = new UnifiedSet();
    //            subterms.forEach(s -> {
    //                if (!s.equals(ellipsis))
    //                    f.add(s);
    //            });
    //            this.fixed = f.toImmutable();

            }

            /**
             * commutive compound match: Y into X which contains one ellipsis
             * <p>
             * X pattern contains:
             * <p>
             * one unmatched ellipsis (identified)
             * <p>                    //HACK should not need new list
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
            protected boolean matchEllipsis(Subterms y, Unify u) {
                //return subst.matchEllipsedCommutative(
                //        this, ellipsis, y
                //);
                //public final boolean matchEllipsedCommutative(Compound X, Ellipsis Xellipsis, Compound Y) {

                SortedSet<Term> xFixed = new TreeSet();//$.newHashSet(0); //Global.newHashSet(0);


                final Ellipsis ellipsis = this.ellipsis;

                SortedSet<Term> yFree = y.toSetSorted();

                Subterms ss = subterms();
                int s = ss.subs();

                for (int k = 0; k < s; k++) {


                    Term x = ss.sub(k);

                    if (x.equals(ellipsis)) {
                        Term v = u.xy(x);
                        if (v != null) {
                            if (v instanceof EllipsisMatch) {
                                return ((EllipsisMatch) v).rematch(y, yFree);
                            } else {
                                //single-term matched for the ellipsis, so wont be EllipsisMatch instance
                                if (u.constant(v) && !yFree.remove(v))
                                    return false;
                            }
                        }

                        continue;
                    }

                    //find (randomly) at least one element of 'y' which unifies with this fixed variable
                    //if one matches, remove it from yFree
                    //if none match, fail
                    //TODO this should be part of the termutator since there could be more than one match
    //                int dir = subst.random.nextBoolean() ? +1 : -1;
    //                int u = subst.random.nextInt(s);
    //                boolean matched = false;
    //                for (int w = 0; w < s; w++, u+=dir) {
    //                    if (u == -1) u = s - 1;
    //                    else if (u == s) u = 0;
    //                    //if (!yFree.contains(yu)) continue //?? would this help?
    //                    Term yu = y.sub(u);
    //                    if (subst.putXY(x, yu)) {
    //                        matched = true;
    //                        yFree.remove(yu);
    //                        break;
    //                    }
    //                }
    //                if (!matched)
    //                    return false;

                    /*if (v instanceof EllipsisMatch) {

                        //assume it's THE ellipsis here, ie. x == xEllipsis by testing that Y contains all of these
                        if (!((EllipsisMatch) v).addWhileMatching(y, alreadyInY, ellipsis.sizeMin())) {
                            return false;
                        } else {
                            //Xellipsis = null;
                            ellipsisMatched = true;
                            break; //continued below
                        }


                    } else */
    //                if (v != null) {
    //
    //                    if (!yFree.remove(v)) {
    //                        //required but not actually present in Y
    //                        return false;
    //                    }
    //
    //                } else {


                    boolean xConst = u.constant(x);
                    if (!xConst) {
    //                    //try to resolve an already assigned and thus resolvable to constant
    //                    @Nullable Term previouslyMatched = u.xy(x);
    //                    if (previouslyMatched != null) {
    //                        x = previouslyMatched;
    //                    }

                        xFixed.add(x); //x is a variable which must be termuted when everything non-X is assigned

                    } else {

                        if (!yFree.remove(x)) {
                            return false;
                        }
    //                        //matched constant
    //                        //xFixed.remove(x); //<- probably not necessary
    //                    } else {
    //                        if ((u.type == null && (x.vars()+x.varPattern()==0)) || (u.type!=null && !x.hasAny(u.type)))
    //                            return false; //unmatched constant offers no possibility of eventual unification
    //
    //                        xFixed.add(x);
    //                    }

    //                    if (!yFree.remove(x))
    //                        xFixed.add(x);
                    }

                    //         }


                }

                final int xs = xFixed.size();
                int ys = yFree.size();
                int numRemainingForEllipsis = ys - xs;

                //if not invalid size there wouldnt be enough remaining matches to satisfy ellipsis cardinality
                boolean vs = ellipsis.validSize(numRemainingForEllipsis);
                if (!vs)
                    return false;

                switch (xs) {
                    case 0: //match everything to everything

                        Term match = ys > 0 ? EllipsisMatch.match(yFree) : EllipsisMatch.empty;

                        if (subs() == 1 || match.subs()==0 || xFixed.isEmpty()) {
                            return this.ellipsis.unify(match, u);
                        } else {
                            //permute may be necessary to unify the correct dep/indep terms for 2nd layer
                            if (xFixed.size()==match.subs())
                                return u.termutes.add(new CommutivePermutations(
                                        Subterms.subterms(xFixed),
                                        match.subterms().sorted()));
                            else
                                return false; //?
                        }

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
                        //3 or more combination
                        throw new RuntimeException("unimpl: " + xs + " arity combination unimplemented");
                }


            }

        }


    }
}
