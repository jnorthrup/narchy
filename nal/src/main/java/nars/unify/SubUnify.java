package nars.unify;

import nars.NAL;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.term.atom.theBool.Null;

/**
 * Less powerful one-match only unification
 */
public class SubUnify extends Unify {

    protected @Nullable Term transformed;


    protected @Nullable Term result;


    public SubUnify(Random rng) {
        this(rng, Op.Variable);
    }

    protected SubUnify(Random rng, int varBits) {
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
    protected final boolean match() {

        if (transformed != null) {
            var result = apply(transformed);
//            assert(result!=null); //HACK TEMPORARY
            if (result != Null && accept(result)) {

                use(1);
                this.result = result;

                return false; //done
            }
        }
        return true; //kontinue
    }


    protected boolean accept(Term result) {
        return true;
    }

    public void clear(int varBits) {
        clear();
        this.varBits = varBits;
    }
}
