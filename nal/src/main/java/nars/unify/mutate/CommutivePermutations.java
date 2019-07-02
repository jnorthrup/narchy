package nars.unify.mutate;

import nars.$;
import nars.subterm.ShuffledSubterms;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.atom.Atom;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

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
    public @Nullable Termutator preprocess(Unify u) {
        TermList x = u.resolveListIfChanged(this.x);
        TermList y = u.resolveListIfChanged(this.y);
        if (x!=null || y!=null) {
            if (x == null) x = this.x.toList();
            if (y == null) y = this.y.toList();
            boolean preUnified = false;
            switch (Subterms.possiblyUnifiableWhileEliminatingEqualAndConstants(x, y, u)) {
                case 1:
                    preUnified = true;
                    break;
                case -1:
                    return null; //impossible
                case 0: {
                    if (x.subs() == 1) {
                        assert (y.subs() == 1);
                        if (x.sub(0).unify(y.sub(0), u))
                            preUnified = true;
                        else
                            return null; //impossible
                        break;
                    } else {
                        if (!u.varIn(x) && !u.varIn(y)) {
                            //constant
                            if (Subterms.unifyLinear(x.commuted(), y.commuted(), u)) {
                                preUnified = true;
                            } else
                                return null; //impossible
                        }
                    }
                    break;
                }
            }

            if (preUnified)
                return Termutator.NullTermutator; //done

            return new CommutivePermutations(x, y);
        } else
            return this;
    }

    @Override
    public void mutate(Termutator[] chain, int current, Unify u) {


        Subterms x = this.x, y = this.y;
//        Subterms xx = u.resolve(this.x), yy = u.resolve(this.y);

//        Subterms xx = u.resolve(this.x);
//        Subterms yy = u.resolve(this.y);
//        if (this.x!=xx || this.y!=yy) {
//            if (!Subterms.possiblyUnifiable(xx,yy,u))
//                return;
//            if (!u.var(xx) && !u.var(yy)) {
//                //constant
//                if (Subterms.unifyLinear(xx.commuted(), yy.commuted(), u)) {
//                    u.tryMutate(chain, current); //matched
//                }
//                return;
//            }
//
//
//            x = xx;
//            y = yy;
//        } else {
//            x = this.x; y = this.y;
//        }

        int start = u.size();

        ShuffledSubterms p = new ShuffledSubterms(x, u.random);


        while (p.shuffle()) {

            if (Subterms.unifyLinear(p, y, u)) {
                if (!u.tryMutate(chain, current))
                    break;
            }

            if (!u.revertLive(start))
                break;
        }


    }


}
