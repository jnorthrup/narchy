package nars.unify;

import nars.Op;
import nars.Param;
import nars.term.Term;

import java.util.Random;
import java.util.function.Predicate;

/**
 * Unifications with callback
 *
 * not thread safe, use 1 per thread (do not interrupt matchAll)
 */
public class UnifySubst extends Unify {


    /** continuator */
    private final Predicate<Term> each;

    private Term input;
    private int matches = 0;

    public UnifySubst(Op varType, Random rng, Predicate<Term> each) {
        super(varType, rng, Param.UnificationStackMax);
        this.each = each;
    }

    /**
     *  x and y are two terms being unified to form the set of substitutions.
     *  the 'input'  term is what transformation will be attempted upon.
     *  ot may be the same as X or Y, or something completely different.
     */
    public int transform(Term input, Term x, Term y, int ttl) {
        setTTL(ttl); assert(ttl > 0);

        this.input = input;

        unify(x, y);

        return matches;
    }


    @Override
    public void tryMatch() {
        Term aa = transform(input);
        if (aa != null) {
            if (aa.op().conceptualizable) {
                matches++;
                if (!each.test(aa)) {
                    stop();
                }
            }
        }
    }

    public int matches() {
        return matches;
    }

}
