package nars.subterm;

import jcog.math.ShuffledPermutations;
import nars.term.Term;

import java.util.Random;

/**
 * proxy to a TermContainer providing access to its subterms via a shuffling order
 */
public final class ShuffledSubterms extends RemappedSubterms {

    private final ShuffledPermutations shuffle = new ShuffledPermutations();

    public ShuffledSubterms(Subterms subterms, Random rng) {
        super(subterms);
        reset(rng);
    }

    @Override
    public int subMap(int i) {
        return shuffle.permute(i);
    }

    @Override
    public int subs() {
        return ref.subs();
    }

    @Override
    public Term sub(int i) {
        return ref.sub(subMap(i)); //DIRECT
    }

    private void reset(Random rng) {
        shuffle.restart(subs(), rng);
    }

    public boolean shuffle() {
        return shuffle.hasNextThenNext();
    }
}
