package nars.unify;

import nars.term.Term;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/** stops after the first unification.  can be used to test whether two terms unify at least one way. */
public class UnifyAny extends UnifySubst {

    public UnifyAny() {
        this(ThreadLocalRandom.current());
    }

    public UnifyAny(Random rng) {
        super(null, rng);
    }

    @Override
    protected boolean each(Term t) {
        return false; //stop after the first
    }


}