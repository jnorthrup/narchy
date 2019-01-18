package nars.unify.mutate;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.unify.Unify;
import nars.unify.ellipsis.Ellipsis;
import nars.unify.ellipsis.EllipsisMatch;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

/**
 * choose 1 at a time from a set of N, which means iterating up to N
 */
public class Choose1 extends Termutator.AbstractTermutator {

    private final Term x;
    private final Ellipsis xEllipsis;
    private final Term[] yy;

    private final static Atom CHOOSE_1 = $.the(Choose1.class);

    public Choose1(Ellipsis xEllipsis, Term x, SortedSet<Term> yFree) {
        this(xEllipsis, x, yFree.toArray(Op.EmptyTermArray));
    }

    private Choose1(Ellipsis xEllipsis, Term x, Term[] yFree /* sorted */) {
        super(CHOOSE_1, x, xEllipsis, $.sFast(false, yFree));

        assert(yFree.length >= 2): Arrays.toString(yFree) + " must offer choice";

        yy = yFree;
        


        this.xEllipsis = xEllipsis;
        this.x = x;


    }

    public static boolean choose1(Ellipsis ellipsis, List<Term> xFixed, SortedSet<Term> yFree, Unify u) {
        Term x0 = xFixed.get(0);
        int ys = yFree.size();
        switch (ys) {
            case 1:
                assert (ellipsis.minArity == 0);
                return x0.unify(yFree.first(), u) && ellipsis.unify(EllipsisMatch.empty, u);
            case 2:
                //check if both elements actually could match x0.  if only one can, then no need to termute.
                //TODO generalize to n-terms
                //TODO include volume pre-test
                Term aa = yFree.first();
                boolean a = Subterms.possiblyUnifiable(x0, aa, u.varBits);
                Term bb = yFree.last();
                boolean b = Subterms.possiblyUnifiable(x0, bb, u.varBits);
                if (!a && !b) {
                    return false; //impossible
                } else if (a && !b) {
                    return x0.unify(aa, u) && ellipsis.unify(bb, u);
                } else if (b && !a) {
                    return x0.unify(bb, u) && ellipsis.unify(aa, u);
                } //else: continue below
                break;
//                            default:
//                                throw new TODO();
        }
        return u.termutes.add(new Choose1(ellipsis, x0, yFree));
    }

    @Override
    public int getEstimatedPermutations() {
        return yy.length;
    }

    @Override
    public void mutate(Termutator[] chain, int current, Unify u) {

//        Ellipsis xEllipsis = this.xEllipsis;
//        Term xMatched = u.xy(xEllipsis);
//        if (xMatched !=null && !xMatched.hasAny(u.typeBits))
//            return; //doesnt match, dont need to permute


        Term[] yy = this.yy;

        int l = yy.length-1;
        int shuffle = u.random.nextInt(yy.length); 

        int start = u.size();

        for (Term x = this.x; l >=0; l--) {

            int iy = (shuffle + l) % this.yy.length;
            Term y = this.yy[iy];
            if (x.unify(y, u)) {
                Term m = EllipsisMatch.matchedExcept(xEllipsis.minArity, yy, (byte) iy);
                if (m!=null && xEllipsis.unify(m, u)) {
                    if (!u.tryMutate(chain, current))
                        break;
                }

            }

            if (!u.revertLive(start))
                break;
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
