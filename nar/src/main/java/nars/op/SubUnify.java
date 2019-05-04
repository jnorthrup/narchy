package nars.op;

import nars.NAL;
import nars.Op;
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
        super(varBits, rng, NAL.unify.UNIFICATION_STACK_CAPACITY);
    }

//    @Override
//    public Versioning clear() {
//        return super.clear();
//    }

    /**
     * terminate after the first match
     * @return
     */
    @Override
    protected final boolean tryMatch() {

        if (transformed != null) {
            Term result = apply(transformed);
            if (result != null && result != Null && tryMatch(result)) {

                use(1);
                this.result = result;

                return false; //done
            }
        }
        return true; //kontinue
    }


    protected boolean tryMatch(Term result) {
        return true;
    }

}