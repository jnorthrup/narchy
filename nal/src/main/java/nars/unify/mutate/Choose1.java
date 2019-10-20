package nars.unify.mutate;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.var.ellipsis.Ellipsis;
import nars.term.var.ellipsis.Fragment;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.SortedSet;

/**
 * choose 1 at a time from a set of N, which means iterating up to N
 */
public class Choose1 extends Termutator.AbstractTermutator {

    private final Term x;
    private final Term /*Ellipsis*/ xEllipsis;
    private final Term[] yy;

    private static final Atom CHOOSE_1 = $.the(Choose1.class);

    public Choose1(Term /*Ellipsis*/ xEllipsis, Term x, SortedSet<Term> yFree) {
        this(xEllipsis, x, yFree.toArray(Op.EmptyTermArray));
    }

    private Choose1(Term /*Ellipsis*/ xEllipsis, Term x, Term[] yFree /* sorted */) {
        super(CHOOSE_1, x, xEllipsis, $.sFast(false, yFree));

        assert(yFree.length >= 2): Arrays.toString(yFree) + " must offer choice";

        yy = yFree;

        this.xEllipsis = xEllipsis;
        this.x = x;

        //int ml = yy.length - 1;
        //assert(ml >= xEllipsis.minArity);
    }

    public static @Nullable Termutator choose1(Ellipsis ellipsis, Term x, SortedSet<Term> yFree, Unify u) {
        int ys = yFree.size();

        if (ellipsis.minArity > ys - 1)
            return null; //impossible

        switch (ys) {
            case 1:
                assert (ellipsis.minArity == 0);
                return x.unify(yFree.first(), u) && ellipsis.unify(Fragment.empty, u) ? ELIDE : null;
            case 2:
                //check if both elements actually could match x0.  if only one can, then no need to termute.
                //TODO generalize to n-terms
                //TODO include volume pre-test
                Term aa = yFree.first();
                boolean a = Subterms.possiblyUnifiable(x, aa, u.varBits);
                Term bb = yFree.last();
                boolean b = Subterms.possiblyUnifiable(x, bb, u.varBits);
                if (!a && !b) {
                    return null; //impossible
                } else if (a && !b) {
                    return x.unify(aa, u) && ellipsis.unify(bb, u) ? ELIDE : null;
                } else if (/*b &&*/ !a) {
                    return x.unify(bb, u) && ellipsis.unify(aa, u) ? ELIDE : null;
                } //else: continue below
                break;
//                            default:
//                                throw new TODO();
        }
        return new Choose1(ellipsis, x, yFree);
    }

    @Override
    public int getEstimatedPermutations() {
        return yy.length;
    }

    @Override
    public @Nullable Termutator preprocess(Unify u) {
        //resolve to constant if possible
        Term xEllipsis = u.resolveTermRecurse(this.xEllipsis);
        if (this.xEllipsis != xEllipsis && this.xEllipsis instanceof Ellipsis && !(xEllipsis instanceof Ellipsis)) {
            //became non-ellipsis
            int es = xEllipsis instanceof Fragment ? xEllipsis.subs() : 1;
            if (((Ellipsis) this.xEllipsis).minArity > es) {
                return null; //assigned to less arity than required
            }
        }

        Term x = u.resolveTermRecurse(this.x);

        TermList yy = new TermList(this.yy);
        Subterms yy2 = u.resolveSubsRecurse(yy);
        if (yy2!=null && (xEllipsis!=this.xEllipsis || x!=this.x || yy!=yy2 )) {

            if (xEllipsis instanceof Ellipsis) {

                return Choose1.choose1((Ellipsis)xEllipsis, x, yy2.toSetSorted(), u);

            } else {
                //can this happen?
                yy2 = yy2.commuted();
                int ys = yy2.subs();

                //TODO reduce to Subterms.unifyCommutive test
                if (ys == 1 && xEllipsis instanceof Fragment && xEllipsis.subs()==0) {
                    return Termutator.result(x.unify(yy.sub(0), u));
                }

                //TODO test any other non-choice cases
                return new Choose1(xEllipsis, x, yy2.arrayShared());
            }


        }

        return this;
    }

    @Override
    public void mutate(Termutator[] chain, int current, Unify u) {

        int l = yy.length-1;
        int shuffle = u.random.nextInt(yy.length);

        int start = u.size();

        for (Term x = u.resolveTerm(this.x); l >=0; l--) {

            int iy = (shuffle + l) % yy.length;
            Term y = yy[iy];
            if (x.unify(y, u)) {
                if (xEllipsis.unify( Fragment.matchedExcept(yy, iy), u)) {
                    if (!u.tryMutate(chain, current))
                        break;
                }
            }

            if (!u.revertLive(start))break;
        }
//
//        if (xEllipsis.minArity == 0) {
//            if (xEllipsis.unify(EllipsisMatch.empty, u)) {
//                if (!u.tryMutate(chain, current) && !u.revertLive(start))
//                    return;
//            }
//        }
    }


}
