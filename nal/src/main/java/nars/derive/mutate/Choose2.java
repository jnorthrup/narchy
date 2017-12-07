package nars.derive.mutate;

import jcog.math.Combinations;
import nars.$;
import nars.The;
import nars.derive.match.Ellipsis;
import nars.derive.match.EllipsisMatch;
import nars.term.Term;
import nars.term.container.ShuffledSubterms;
import nars.term.container.Subterms;
import nars.term.subst.Unify;
import org.apache.commons.lang3.ArrayUtils;

import java.util.SortedSet;

/**
 * Created by me on 12/22/15.
 */
public class Choose2 extends Termutator.AbstractTermutator {

    /*@NotNull*/
    final Combinations comb;
    ///*@NotNull*/ private final Term[] yFree;
    /*@NotNull*/
    private final Term[] x;
    /*@NotNull*/
    private final Ellipsis xEllipsis;
    /*@NotNull*/
    private final Unify f;
    /*@NotNull*/
    private final ShuffledSubterms yy;

    public Choose2(/*@NotNull*/ Ellipsis xEllipsis, /*@NotNull*/ Unify f, /*@NotNull*/ SortedSet<Term> x, /*@NotNull*/ SortedSet<Term> yFree) {
        super($.p(x), xEllipsis, $.p(yFree));
        this.f = f;
        this.xEllipsis = xEllipsis;
        this.x = x.toArray(new Term[x.size()]);

        int yFreeSize = yFree.size();

        this.yy = new ShuffledSubterms(The.subterms(yFree), f.random  /*new ArrayTermVector(yFree)*/);

        this.comb = new Combinations(yFreeSize, 2);
    }

    @Override
    public int getEstimatedPermutations() {
        return comb.getTotal()*2;
    }

    @Override
    public void mutate(Unify versioneds, Termutator[] chain, int current) {

        /*@NotNull*/ Combinations ccc = this.comb;
        ccc.reset();

        boolean phase = true;

        int start = f.now();
        /*@NotNull*/ ShuffledSubterms yy = this.yy;

        Term[] m = new Term[this.yy.subs()-2];

        Ellipsis xEllipsis = this.xEllipsis;
        Unify f = this.f;
        Term[] x = this.x;

        int[] c = null;
        while (ccc.hasNext() || !phase) {

            c = phase ? ccc.next() : c;
            phase = !phase;

            int c0 = c[0];
            int c1 = c[1];
            ArrayUtils.reverse(c); //swap to try the reverse next iteration

            Term y1 = yy.sub(c0);

            if (x[0].unify(y1, f)) {

                Term y2 = yy.sub(c1);

                if (x[1].unify(y2, f) &&
                        xEllipsis.unify(EllipsisMatch.match(Subterms.except(yy, y1, y2, m)), f)) {

                    f.tryMutate(chain, current);
                }

            }

            if (!f.revertLive(start))
                break;
        }

    }

}
