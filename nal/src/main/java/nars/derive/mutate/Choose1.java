package nars.derive.mutate;

import nars.$;
import nars.derive.match.Ellipsis;
import nars.derive.match.EllipsisMatch;
import nars.term.Term;
import nars.term.subst.Unify;

import java.util.SortedSet;

/**
 * choose 1 at a time from a set of N, which means iterating up to N
 */
public class Choose1 extends Termutator.AbstractTermutator {

    private final Term x;
    private final Term xEllipsis;
    private final Term[] yy;

    public Choose1(Ellipsis xEllipsis, Term x, SortedSet<Term> yFree) {
        super(x, xEllipsis, $.p(yFree));

        int ysize = yFree.size();  assert(ysize >= 2): yFree + " must offer choice";

        yy = yFree.toArray(new Term[ysize]);
        //this.yFree = yFree;


        this.xEllipsis = xEllipsis;
        this.x = x;


    }

    @Override
    public int getEstimatedPermutations() {
        return yy.length;
    }

    @Override
    public void mutate(Unify u, Termutator[] chain, int current) {

        Term[] yy = this.yy;

        int l = yy.length-1;
        int shuffle = u.random.nextInt(yy.length); //randomize starting offset

        int start = u.now();


        Term xEllipsis = this.xEllipsis;
        for (Term x = this.x; l >=0; l--) {

            int iy = (shuffle + l) % this.yy.length;
            Term y = this.yy[iy];
            if (x.unify(y, u)) {
                if (xEllipsis.unify(EllipsisMatch.matchExcept(yy, (byte) iy), u)) {
                    if (!u.tryMutate(chain, current))
                        break;
                }

            }

            if (!u.revertLive(start))
                break;
        }

    }


}
