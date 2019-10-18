package nars.unify.mutate;

import jcog.TODO;
import jcog.math.Combinations;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Op;
import nars.subterm.ShuffledSubterms;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.var.ellipsis.Ellipsis;
import nars.term.var.ellipsis.Fragment;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.SortedSet;

/**
 * Created by me on 12/22/15.
 */
public class Choose2 extends Termutator.AbstractTermutator {


//    /*@NotNull*/
//    private final Term[] x;
    /*@NotNull*/
    private final Ellipsis xEllipsis;


    private static final Atom CHOOSE_2 = $.the(Choose2.class);

//    public Choose2(Ellipsis xEllipsis, Unify f, MutableSet<Term> x, SortedSet<Term> yFree) {
//        this(xEllipsis, f,
//                Terms.commute(x),
//                yFree);
//    }

    public Choose2(Ellipsis xEllipsis, SortedSet<Term> x, SortedSet<Term> yFree) {
        this(xEllipsis,
            x.toArray(Op.EmptyTermArray),
                yFree);
    }

    private Choose2(Ellipsis xEllipsis, Term[] x, SortedSet<Term> yFree) {
        this(xEllipsis, x, $.sFast(yFree));
    }

    private Choose2(Ellipsis xEllipsis, Term[] x, Compound yFree) {
        super(CHOOSE_2, $.pFast(x), xEllipsis, yFree);

        /*@NotNull*/

        this.xEllipsis = xEllipsis;



    }

    public static @Nullable Termutator choose2(Ellipsis ellipsis, List<Term> xFixed, SortedSet<Term> yFree, Unify u) {
//        int ys = yFree.size();
//        if (ellipsis.minArity > ys - 2)
//            return null; //impossible

        Term a = xFixed.get(0), b = xFixed.get(1);
        boolean av = u.var(a), bv = u.var(b);
        if (!av || !bv) {
            Compound yy = $.sFast(yFree);
            if (!av && !Subterms.possiblyUnifiableAssumingNotEqual(a, yy, u.varBits) ||
                (!bv && !Subterms.possiblyUnifiableAssumingNotEqual(b, yy, u.varBits))) {
                return null;
            }
        }

        return new Choose2(ellipsis, Terms.commute(xFixed), yFree);
    }

    @Override
    public int getEstimatedPermutations() {
        throw new TODO();
        //return comb.getTotal()*2;
    }


    @Override public @Nullable Termutator preprocess(Unify u) {
        //TODO
//        Term xEllipsis = u.resolveTermRecurse(this.xEllipsis);
//        if (this.xEllipsis != xEllipsis && this.xEllipsis instanceof Ellipsis && !(xEllipsis instanceof Ellipsis)) {
//            //became non-ellipsis
//            int es = xEllipsis.op() == FRAG ? xEllipsis.subs() : 1;
//            if (((Ellipsis) this.xEllipsis).minArity > es) {
//                return null; //assigned to less arity than required
//            }
//        }
        Subterms _x = x();
        Subterms x = u.resolveListIfChanged(_x, true);
        Subterms _y = y();
        Subterms y = u.resolveListIfChanged(_y, true);
        if (x!=null || y!=null) {

            //TODO move most of this to Choose2.choose2(..)

            if (y==null) y = _y;
            SortedSet<Term> yy = y.toSetSorted();

            if (x==null) x = _x;
            else {
                Term a = x.sub(0);
                Term b = x.sub(1);
                if (!(a instanceof Variable)) {
                    //check if impossible to match any of Y
                    if (yy.remove(a))
                        a = null; //found exact
                }
                if (!(b instanceof Variable)) {
                    //check if impossible to match any of Y
                    if (yy.remove(b))
                        b = null; //found exact
                }

                if (xEllipsis.minArity > yy.size())
                    return null; //too few remain

                if (a == null && b == null) {
                    //match remainder to xEllipsis and succeed
                    return xEllipsis.unify(Fragment.fragment(yy), u) ? ELIDE : null;
                } else if (a == null) {
                    return Choose1.choose1(xEllipsis, b, yy, u);
                } else if (b == null) {
                    return Choose1.choose1(xEllipsis, a, yy, u);
                }
            }

            return Choose2.choose2(xEllipsis, x.toList(), yy, u);
        }

        return this;
    }


    @Override
    public void mutate(Termutator[] chain, int current, Unify u) {



        Subterms x = x();
        Subterms yFree = y();

        Combinations ccc = new Combinations(yFree.subs(), 2);

        int start = u.size();
        ShuffledSubterms yy = new ShuffledSubterms(yFree, u.random);


        Term xEllipsis = u.resolveTerm(this.xEllipsis);

        TermList tl = new TermList(2);
        tl.setSize(2);
        Term[] tll = tl.array();

        int[] c = null;
        boolean phase = true;
        while (ccc.hasNext() || !phase) {

            c = phase ? ccc.next() : c;

            int c0 = c[0], c1 = c[1];

            tll[0] = yy.sub(c0); tll[1] = yy.sub(c1);

            if (Subterms.unifyLinear(x, tl, u)) {
                if (xEllipsis.unify(Fragment.matchedExcept(yy, c), u)) {
                    if (!u.tryMutate(chain, current))
                        break;
                }
            }

            if (!u.revertLive(start))
                break;

            ArrayUtil.reverse(c);
            phase = !phase;

        }

    }

    private Subterms y() {
        return sub(1).sub(2).subterms();
    }

    private Subterms x() {
        return sub(1).sub(0).subterms();
    }

}
