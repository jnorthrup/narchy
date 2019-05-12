package nars.term.compound;

import jcog.TODO;
import jcog.WTF;
import nars.$;
import nars.Op;
import nars.subterm.BiSubterm;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.util.conj.Conj;
import nars.term.var.ellipsis.Ellipsis;
import nars.term.var.ellipsis.Ellipsislike;
import nars.term.var.ellipsis.Fragment;
import nars.unify.Unify;
import nars.unify.mutate.Choose1;
import nars.unify.mutate.Choose2;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import static nars.Op.CONJ;
import static nars.Op.FRAG;
import static nars.time.Tense.XTERNAL;

/**
 * seems used only if op==CONJ
 */
abstract public class PatternCompound extends CachedCompound.TemporalCachedCompound {

    PatternCompound(/*@NotNull*/ Op op, int dt, Subterms subterms) {
        super(op, dt, subterms);
    }

    /*@NotNull*/
    public static PatternCompound ellipsis(/*@NotNull*/ Compound seed, /*@NotNull*/ Subterms v, /*@NotNull*/ Ellipsis e) {
        Op op = seed.op();
        int dt = seed.dt();

        if ((op.commutative)) {
            return new PatternCompoundEllipsisCommutive(op, dt, e, v);
        } else {
            return PatternCompoundEllipsisLinear.the(op, dt, e, v);
        }

    }

    @Override
    public final boolean the() {
        return false;
    }

    @Override
    public final @Nullable Term normalize(byte varOffset) {
        throw new UnsupportedOperationException("normalize before patternify");
    }

    public abstract static class PatternCompoundWithEllipsis extends PatternCompound {

        final Ellipsis ellipsis;

        PatternCompoundWithEllipsis(/*@NotNull*/ Op seed, int dt, Ellipsis ellipsis, Subterms subterms) {
            super(seed, dt, subterms);
            this.ellipsis = ellipsis;
        }

        abstract protected boolean matchEllipsis(Term y, Unify subst);

        @Override
        public final boolean unifySubterms(Term y, Unify u) {
            return matchEllipsis(y, u);
        }
    }


    public static final class PatternCompoundEllipsisLinear extends PatternCompoundWithEllipsis {

        public static PatternCompoundEllipsisLinear the(Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
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
            return new PatternCompoundEllipsisLinear(op, dt, ellipsis, subterms);
        }

        private PatternCompoundEllipsisLinear(/*@NotNull*/ Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
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
                            return
                                    ellipsis.validSize(available) &&
                                    ellipsis.unify(Fragment.fragment(Y, yi, yi + available), u);


                        } else {
                            //TODO ellipsis is in the center or beginning
                            throw new TODO();
                        }
                    } else {
                        if (xResolved.op()==FRAG) {
                            Fragment xe = (Fragment) xResolved;
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


    public static final class PatternCompoundEllipsisCommutive extends PatternCompoundWithEllipsis {


        PatternCompoundEllipsisCommutive(Op op, int dt, Ellipsis ellipsis, Subterms subterms) {
            super(op, dt, ellipsis, subterms);
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
//            if ((dt != XTERNAL) && Y.op().temporal && !Y.isCommutative())
//                throw new TODO();


            //                @Nullable Versioned<MatchConstraint> uc = u.constraints(ellipsis);

            //xFixed is effectively sorte unless eMatch!=nulld


            SortedSet<Term> yFree;
                    //uc==null ? y.toSetSorted() : y.toSetSorted(yy -> MatchConstraint.valid(yy, uc, u));
                    //y.toSetSorted();
            boolean seq = op() == CONJ && dt() == XTERNAL && Conj.isSeq(Y);
            if (seq) {
                yFree = Y.eventSet();
            } else {
                yFree = Y.subterms().toSetSorted(u::resolvePosNeg);
            }

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
                    if (xxk.op()==FRAG) {
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
                            return Subterms.unifyCommute(xMatch, $.vFast(yFree), u);
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
                    if (ix instanceof Variable && u.var(ix)) continue;

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


            switch (xs) {
                case 0:
                    return ellipsis.unify(ys > 0 ? Fragment.fragment(yFree) : Fragment.empty, u);

                case 1:
                    if (xs == ys) {
                        return xMatch.getFirst().unify(yFree.first(), u) && ellipsis.unify(Fragment.empty, u);
                    } else {
                        //no matches possible but need one
                        return ys >= 1 && Choose1.choose1(ellipsis, xMatch, yFree, u);
                    }

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
