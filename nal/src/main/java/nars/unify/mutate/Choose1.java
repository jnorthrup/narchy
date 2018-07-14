package nars.unify.mutate;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.unify.Unify;
import nars.unify.match.Ellipsis;
import nars.unify.match.EllipsisMatch;

import java.util.Arrays;
import java.util.SortedSet;

/**
 * choose 1 at a time from a set of N, which means iterating up to N
 */
public class Choose1 extends Termutator.AbstractTermutator {

    private final Term x;
    private final Term xEllipsis;
    private final Term[] yy;

    private final static Atom CHOOSE_1 = $.the(Choose1.class);

    public Choose1(Ellipsis xEllipsis, Term x, SortedSet<Term> yFree) {
        this(xEllipsis, x, yFree.toArray(Op.EmptyTermArray));
    }

    private Choose1(Ellipsis xEllipsis, Term x, Term[] yFree /* sorted */) {
        super(CHOOSE_1, x, xEllipsis, $.sFast(false, yFree));

        int ysize = yFree.length;  assert(ysize >= 2): Arrays.toString(yFree) + " must offer choice";

        yy = yFree;
        


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
        int shuffle = u.random.nextInt(yy.length); 

        int start = u.now();


        Term xEllipsis = this.xEllipsis;
        for (Term x = this.x; l >=0; l--) {

            int iy = (shuffle + l) % this.yy.length;
            Term y = this.yy[iy];
            if (x.unify(y, u)) {
                if (xEllipsis.unify(EllipsisMatch.matchedExcept(yy, (byte) iy), u)) {
                    if (!u.tryMutate(chain, current))
                        break;
                }

            }

            if (!u.revertLive(start))
                break;
        }

    }


}
