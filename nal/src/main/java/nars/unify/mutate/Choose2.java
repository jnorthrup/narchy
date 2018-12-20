package nars.unify.mutate;

import jcog.TODO;
import jcog.math.Combinations;
import jcog.util.ArrayUtils;
import nars.$;
import nars.Op;
import nars.subterm.ShuffledSubterms;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.unify.Unify;
import nars.unify.ellipsis.Ellipsis;
import nars.unify.ellipsis.EllipsisMatch;
import org.eclipse.collections.api.set.MutableSet;

import java.util.SortedSet;

/**
 * Created by me on 12/22/15.
 */
public class Choose2 extends Termutator.AbstractTermutator {


    /*@NotNull*/
    private final Term[] x;
    /*@NotNull*/
    private final Ellipsis xEllipsis;
    /*@NotNull*/
    private final Unify f;


    private final static Atom CHOOSE_2 = $.the(Choose2.class);

    public Choose2(Ellipsis xEllipsis, Unify f, MutableSet<Term> x, SortedSet<Term> yFree) {
        this(xEllipsis, f,
                Terms.sorted(x),
                yFree);
    }

    public Choose2(Ellipsis xEllipsis, Unify f, SortedSet<Term> x, SortedSet<Term> yFree) {
        this(xEllipsis, f,
                x.toArray(Op.EmptyTermArray),
                yFree);
    }

    public Choose2(Ellipsis xEllipsis, Unify u, Term[] x, SortedSet<Term> yFree) {
        super(CHOOSE_2, $.pFast(x), xEllipsis, $.sFast(yFree));

        this.f = u;

        this.xEllipsis = xEllipsis;

        this.x = x;

    }

    @Override
    public int getEstimatedPermutations() {
        throw new TODO();
        //return comb.getTotal()*2;
    }

    @Override
    public void mutate(Unify u, Termutator[] chain, int current) {

        Subterms yFree = sub(1).sub(2).subterms();


        Combinations ccc = new Combinations(yFree.subs(), 2);
        ccc.reset();

        boolean phase = true;

        int start = f.size();
        ShuffledSubterms yy = new ShuffledSubterms(yFree, u.random  /*new ArrayTermVector(yFree)*/);


        Ellipsis xEllipsis = this.xEllipsis;
        Unify f = this.f;
        Term[] x = this.x;

        int[] c = null;
        while (ccc.hasNext() || !phase) {

            c = phase ? ccc.next() : c;

            byte c0 = (byte) c[0], c1 = (byte) c[1];

            Term y1 = yy.sub(c0);

            if (x[0].unify(y1, f)) {

                Term y2 = yy.sub(c1);

                if (x[1].unify(y2, f) &&
                        xEllipsis.unify(EllipsisMatch.matchedExcept(yy, c0, c1), f)) {

                    if (!f.tryMutate(chain, current))
                        break;
                }

            }

            if (!f.revertLive(start))
                break;

            ArrayUtils.reverse(c);
            phase = !phase;

        }

    }

}
