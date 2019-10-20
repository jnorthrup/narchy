package nars.unify;

import jcog.random.XoRoShiRo128PlusRandom;
import nars.NAL;
import nars.term.Term;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/** stops after the first unification.  can be used to test whether two terms unify at least one way. */
public class UnifyAny extends Unify {

    public UnifyAny() {
        this(new XoRoShiRo128PlusRandom(ThreadLocalRandom.current().nextLong()));
    }

    public UnifyAny(Random rng) {
        super(null, rng, NAL.unify.UNIFICATION_STACK_CAPACITY);
    }

    @Deprecated private int matches = 0;

    @Override
    protected boolean match() {
        matches++;
        return false; //stop after the first
    }

    public boolean unifies(Term x, Term y) {
        clear();
        var matchesBefore = matches;
        return unify(x, y) && matchesBefore < matches;
    }

}