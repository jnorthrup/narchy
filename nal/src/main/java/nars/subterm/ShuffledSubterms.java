package nars.subterm;

import jcog.math.ShuffledPermutations;

import java.util.Random;

/**
 * proxy to a TermContainer providing access to its subterms via a shuffling order
 */
public final class ShuffledSubterms extends MappedSubterms {

    private final ShuffledPermutations shuffle;

    public ShuffledSubterms(Subterms subterms, Random rng) {
        super(subterms);
        this.shuffle = new ShuffledPermutations();
        reset(rng);
    }

    @Override
    protected int subMap(int i) {
        return shuffle.permute(i);
    }

    private void reset(Random rng) {
        shuffle.restart(subs(), rng);
    }

    public boolean shuffle() {
        return shuffle.hasNextThenNext();
    }
}
