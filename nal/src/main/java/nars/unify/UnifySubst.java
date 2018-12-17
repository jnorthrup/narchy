package nars.unify;

import nars.Op;
import nars.Param;
import nars.term.Term;

import java.util.Random;

/**
 * Unifications with callback
 *
 * not thread safe, use 1 per thread (do not interrupt matchAll)
 */
abstract public class UnifySubst extends Unify {


    private Term input;

    public UnifySubst(Op varType, Random rng) {
        super(varType, rng, Param.UnificationStackMax);
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
     *  the 'input'  term is what transformation will be attempted upon.
     *  ot may be the same as X or Y, or something completely different.
     */
    public boolean transform(Term input, Term x, Term y, int ttl) {
        setTTL(ttl);

        this.input = input;

        return unify(x, y);
    }


    @Override
    public void tryMatch() {
        Term aa = transform(input).normalize();
        if (aa.op().conceptualizable) {
            if (!each(aa)) {
                stop();
            }
        }
    }


}
