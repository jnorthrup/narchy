package nars.derive.mutate;

import nars.$;
import nars.Param;
import nars.The;
import nars.term.Term;
import nars.term.Terms;
import nars.term.container.ShuffledSubterms;
import nars.term.container.Subterms;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.SortedSet;

/**
 * Created by me on 12/22/15.
 */
public final class CommutivePermutations extends Termutator.AbstractTermutator {

    @NotNull
    private final Subterms y;
    private final Subterms x;

//    public CommutivePermutations(Set<Term> x, Set<Term> y) {
//        this(
//            The.Subterms.RawSubtermBuilder.apply(Terms.sorted(x)),
//            The.Subterms.RawSubtermBuilder.apply(Terms.sorted(y))
//        );
//
//    }
//
//    public CommutivePermutations(SortedSet<Term> x, SortedSet<Term> y) {
//        this(
//            The.Subterms.RawSubtermBuilder.apply(x.toArray(new Term[x.size()])),
//            The.Subterms.RawSubtermBuilder.apply(y.toArray(new Term[y.size()]))
//        );
//
//    }
    public CommutivePermutations(Subterms x, Subterms y) {
        this(
            $.pFast(x), $.pFast(y)
        );
    }
    /**
     * important note: using raw Set<Term> here to avoid the clobbering of PatternCompound subterms if interned with current impl
     * x and y must have same size
     */
    public CommutivePermutations(Term x, Term y) {
        super(x, y);

        int xs = x.subs();
        assert(xs > 1);
        assert(xs == y.subs());

        this.y = y.subterms();
        this.x = x.subterms();
    }

    @Override
    public int getEstimatedPermutations() {
        throw new UnsupportedOperationException();
        //return perm.total();
    }

    @Override
    public void mutate(Unify f, Termutator[] chain, int current) {
        int start = f.now();

        ShuffledSubterms p = new ShuffledSubterms(x, f.random);


        while (p.hasNextThenNext()) {

            if (p.unifyLinear(y, f)) {
                if (!f.tryMutate(chain, current))
                    break;
            }

            if (!f.revertLive(start))
                break;
        }


    }


}
