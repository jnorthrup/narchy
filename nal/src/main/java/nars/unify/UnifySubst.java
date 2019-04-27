package nars.unify;

import nars.NAL;
import nars.Op;
import nars.term.Term;

import java.util.Random;

/**
 * Unifications with callback
 *
 * not thread safe, use 1 per thread (do not interrupt matchAll)
 */
abstract public class UnifySubst extends Unify {


    private Term input;

    public UnifySubst(int varType, Random rng) {
        super(varType, rng, NAL.unify.UNIFICATION_STACK_CAPACITY);
    }

    UnifySubst(Op varType, Random rng) {
        super(varType, rng, NAL.unify.UNIFICATION_STACK_CAPACITY);
    }

    @Override
    public UnifySubst clear() {
        input = null;
        super.clear();
        return this;
    }

    abstract protected boolean each(Term t);

    /**
     *  x and y are two terms being unified to form the set of substitutions.
     *  the 'input'  target is what transformation will be attempted upon.
     *  ot may be the same as X or Y, or something completely different.
     */
    public final boolean transform(Term input, Term x, Term y, int ttl) {
        setTTL(ttl);

        this.input = input;

        return unify(x, y);
    }


    @Override
    public final boolean tryMatch() {
        Term aa = apply(input);
        return aa.op().conceptualizable && each(aa);
    }


}
