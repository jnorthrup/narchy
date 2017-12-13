package nars.term.subst;

import nars.Op;
import nars.Param;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Less powerful one-match only unification
 */
public class SubUnify extends Unify {

    @NotNull
    private final Unify parent;
    private @Nullable Term transformed;

    @Nullable
    private Term result;


    public SubUnify(Unify parent, @Nullable Op type, int ttl) {
        super(type, parent.random, Param.UnificationStackMax, ttl);
        this.parent = parent;
    }

    /**
     * terminate after the first match
     */
    @Override
    public void tryMatch() {

        if (transformed != null) {
            Term result = transformed.transform(this);//transform(transformed);
            if (result != null && !result.equals(transformed)) {

                int before = parent.now();
                if (xy.forEachVersioned(parent::putXY)) {
                    this.result = result;
                    parent.addTTL(stop()); //stop and refund parent
                } else {
                    parent.revert(before); //continue trying
                }

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

}
