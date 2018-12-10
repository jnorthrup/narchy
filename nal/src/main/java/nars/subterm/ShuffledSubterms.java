package nars.subterm;

import jcog.math.ShuffledPermutations;
import nars.term.Term;

import java.util.Random;

/**
 * proxy to a TermContainer providing access to its subterms via a shuffling order
 */
public final class ShuffledSubterms extends ProxySubterms {

    private final ShuffledPermutations shuffle;


    public ShuffledSubterms(Subterms subterms, Random rng) {
        super(subterms);
        this.shuffle = new ShuffledPermutations();
        reset(rng);
    }

    @Override
    public Term sub(int i) {
        return super.sub(shuffle.permute(i));
    }

    @Override
    public String toString() {
        return Subterms.toString(this);
    }

    private void reset(Random rng) {
        shuffle.restart(subs(), rng);
    }

    public boolean shuffle() {
        return shuffle.hasNextThenNext();
    }
}
