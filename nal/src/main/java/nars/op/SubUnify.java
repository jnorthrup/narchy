package nars.op;

import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.term.atom.Bool.Null;

/**
 * Less powerful one-match only unification
 */
public class SubUnify extends Unify {

    @Nullable
    protected Term transformed;


    @Nullable
    protected Term result;


    public SubUnify(Random rng) {
        this(rng, Op.Variable);
    }

    public SubUnify(Random rng, int varBits) {
        super(varBits, rng, Param.UnificationStackMax);
    }

    /**
     * terminate after the first match
     */
    @Override
    protected void tryMatch() {

        if (transformed != null) {
            Term result = transform(transformed);
            if (result != null && result != Null && tryMatch(result)) {

                this.result = result;

                stop();
            }
        }
    }


    @Nullable
    public Term unifySubst(Term x, Term y, @Nullable Term transformed, int ttl) {
        this.transformed = transformed;
        this.result = null;
        setTTL(ttl);
        assert (ttl > 0);

        unify(x, y);

        return result;
    }

    protected boolean tryMatch(Term result) {
        return true;
    }

}
