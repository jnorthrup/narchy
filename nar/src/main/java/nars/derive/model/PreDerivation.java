package nars.derive.model;

import nars.Op;
import nars.term.Term;
import nars.truth.Truth;
import nars.unify.Unify;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/** contains only information which depends on the premise itself (Task, Belief, BeliefTerm).
 * used for first stage winnowing to determine the (memoizable) set of possible forkable outcomes */
public abstract class PreDerivation extends Unify {


    public Term taskTerm;
    public Term beliefTerm;
    public byte taskPunc;

    public Truth taskTruth;
    public Truth beliefTruth_at_Belief, beliefTruth_at_Task;

    /**
     * choices mapping the available post targets
     */
    public final ShortArrayList canCollector = new ShortArrayList();


    protected PreDerivation(@Nullable Op type, Random random, int stackMax) {
        super(type, random, stackMax);
    }

    public boolean hasBeliefTruth() {
        return beliefTruth_at_Belief !=null || beliefTruth_at_Task !=null;
    }

}
