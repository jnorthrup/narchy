package nars.unify.mutate;

import nars.$;
import nars.subterm.ShuffledSubterms;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.unify.Unify;

/**
 * Created by me on 12/22/15.
 */
public final class CommutivePermutations extends Termutator.AbstractTermutator {


    private final Subterms x, y;

    final static Atom COMMUTIVE_PERMUTATIONS = $.the(CommutivePermutations.class);

    /**
     * NOTE X and Y should be pre-sorted using Terms.sort otherwise diferent permutations of the same
     * values could result in duplicate termutes HACK
     */
    public CommutivePermutations(Subterms X, Subterms Y) {
        super(COMMUTIVE_PERMUTATIONS, $.sFast(X), $.sFast(Y));

        this.x = X;
        this.y = Y;

        int xs = X.subs();
        assert(xs > 1 && xs == Y.subs());

    }

    @Override
    public int getEstimatedPermutations() {
        throw new UnsupportedOperationException();

    }

    @Override
    public void mutate(Termutator[] chain, int current, Unify u) {
        int start = u.size();

        ShuffledSubterms p = new ShuffledSubterms(x, u.random);


        do {

            if (p.unifyLinear(y, u)) {
                if (!u.tryMutate(chain, current))
                    break;
            }

        } while (u.revertLive(start) && p.shuffle());


    }


}
