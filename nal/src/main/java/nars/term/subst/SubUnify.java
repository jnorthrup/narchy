package nars.term.subst;

import nars.Op;
import nars.Param;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Less powerful one-match only unification
 */
public class SubUnify extends Unify {

    @Nullable
    protected Term transformed;


    @Nullable
    private Term result;


    public SubUnify(Random rng, @Nullable Op type, int ttl) {
        super(type, rng, Param.UnificationStackMax, ttl);
    }

    /**
     * terminate after the first match
     */
    @Override
    public void tryMatch() {

        if (transformed != null) {
            Term result = transformed.transform(this);
            if (result != null && tryMatch(result)) {
            //if (result != null && ()) {

//                int before = parent.now();
//                if (xy.forEachVersioned(parent::putXY)) {
                    this.result = result;
                    stop();
//                } else {
//                    parent.revert(before); //continue trying
//                }

            }
        }
    }


    @Nullable
    public Term tryMatch(@Nullable Term transformed, Term x, Term y) {
        this.transformed = transformed;
        this.result = null;
        unify(x, y, true);

        return result;
    }

        protected boolean tryMatch(Term result) {
            return true;
        }

}


