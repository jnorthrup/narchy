package nars.unify.mutate;

import jcog.TODO;
import jcog.math.Combinations;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Op;
import nars.subterm.ShuffledSubterms;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.var.ellipsis.Ellipsis;
import nars.term.var.ellipsis.Fragment;
import nars.unify.Unify;
import org.eclipse.collections.api.set.MutableSet;

import java.util.List;
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
                Terms.commute(x),
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

    public static boolean choose2(Ellipsis ellipsis, List<Term> xFixed, SortedSet<Term> yFree, Unify u) {
        return u.termutes.add(new Choose2(ellipsis, u, Terms.commute(xFixed), yFree));
    }

    @Override
    public int getEstimatedPermutations() {
        throw new TODO();
        //return comb.getTotal()*2;
    }

    /*
    @Override public @Nullable Termutator preprocess(Unify u) {
        //TODO
        return this;
    }
    */

    @Override
    public void mutate(Termutator[] chain, int current, Unify u) {

        Subterms yFree = u.resolve(sub(1).sub(2).subterms());


        Combinations ccc = new Combinations(yFree.subs(), 2);

        boolean phase = true;

        int start = f.size();
        ShuffledSubterms yy = new ShuffledSubterms(yFree, u.random);


        Term xEllipsis = u.resolvePosNeg(this.xEllipsis);
        Unify f = this.f;
        Subterms x = u.resolve(new TermList(this.x));

        int[] c = null;
        while (ccc.hasNext() || !phase) {

            c = phase ? ccc.next() : c;

            int c0 = c[0], c1 = c[1];

            if (Subterms.unifyLinear(x, new TermList(yy.sub(c0), yy.sub(c1)), u)) {
                if (xEllipsis.unify(Fragment.matchedExcept(yy, (byte)c0, (byte)c1), f)) {
                    if (!f.tryMutate(chain, current))
                        break;
                }
            }


            if (!f.revertLive(start))
                break;

            ArrayUtil.reverse(c);
            phase = !phase;

        }

    }

}
