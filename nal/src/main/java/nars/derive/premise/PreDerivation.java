package nars.derive.premise;

import nars.Op;
import nars.derive.UnifyPremise;
import nars.term.Term;
import nars.truth.Truth;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.Random;

/** contains only information which depends on the premise itself (Task, Belief, BeliefTerm).
 * used for first stage winnowing to determine the (memoizable) set of possible forkable outcomes */
public abstract class PreDerivation extends Unify {


    public Term taskTerm;
    public Term beliefTerm;
    public byte taskPunc;

    public Truth taskTruth;
    public Truth beliefTruthBelief, beliefTruthTask;

    /**
     * choices mapping the available post targets
     */
    public final RoaringBitmap canCollector = new RoaringBitmap();

    public final UnifyPremise unifyPremise = new UnifyPremise(); {
        unifyPremise.commonVariables = false; //disable common variables for the query variables matched in premise formation; since the task target is not transformed like the belief target is.
    }

    protected PreDerivation(@Nullable Op type, Random random, int stackMax) {
        super(type, random, stackMax);
    }

    public boolean hasBeliefTruth() {
        return beliefTruthBelief !=null || beliefTruthTask !=null;
    }

}
