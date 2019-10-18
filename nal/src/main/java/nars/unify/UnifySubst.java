package nars.unify;

import nars.NAL;
import nars.term.Term;
import nars.term.atom.Bool;

import java.util.Random;

/**
 * Unifications with callback
 *
 * not thread safe, use 1 per thread (do not interrupt matchAll)
 */
public abstract class UnifySubst extends Unify {


    protected Term input;

    public UnifySubst(int varType, Random rng) {
        super(varType, rng, NAL.unify.UNIFICATION_STACK_CAPACITY);
    }

//    UnifySubst(Op varType, Random rng) {
//        super(varType, rng, NAL.unify.UNIFICATION_STACK_CAPACITY);
//    }

    @Override
    public UnifySubst clear() {
        input = null;
        super.clear();
        return this;
    }

    protected abstract boolean each(Term t);

    /**
     *  x and y are two terms being unified to form the set of substitutions.
     *  the 'input'  target is what transformation will be attempted upon.
     *  ot may be the same as X or Y, or something completely different.
     */
    public final boolean unify(Term input, Term x, Term y) {

        assert(ttl > 0);

        this.input = input;

        return unify(x, y);
    }


    @Override
    public final boolean match() {
        Term aa = apply(input);
		//try again
		return aa instanceof Bool ? true : each(aa);
    }


}
