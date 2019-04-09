package nars.op;

import jcog.version.Versioning;
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
    private boolean live = true;


    public SubUnify(Random rng) {
        this(rng, Op.Variable);
    }

    public SubUnify(Random rng, int varBits) {
        super(varBits, rng, Param.unify.UNIFICATION_STACK_CAPACITY);
    }

    @Override
    public Versioning clear() {
        live = true;
        return super.clear();
    }

    @Override
    public void setTTL(int ttl) {
        live = (ttl > 0);
        super.setTTL(ttl);
    }

    /**
     * terminate after the first match
     */
    @Override
    protected final void tryMatch() {

        if (transformed != null) {
            Term result = apply(transformed);
            if (result != null && result != Null && tryMatch(result)) {

                use(1);
                this.result = result;

                stop();
            }
        }
    }

    @Override public int stop() {
        this.live = false;
        return ttl;
    }

    @Override
    public boolean live() {
        return live && super.live();
    }


    protected boolean tryMatch(Term result) {
        return true;
    }

}
