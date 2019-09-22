package nars.derive;

import jcog.Util;
import jcog.data.ShortBuffer;
import nars.Op;
import nars.term.Term;
import nars.truth.MutableTruth;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/** contains only information which depends on the premise itself (Task, Belief, BeliefTerm).
 * used for first stage winnowing to determine the (memoizable) set of possible forkable outcomes */
public abstract class PreDerivation extends Unify {


    public Term taskTerm;
    public Term beliefTerm;
    public byte taskPunc;

    public final MutableTruth taskTruth = new MutableTruth();
    public final MutableTruth beliefTruth_at_Belief = new MutableTruth();
    public final MutableTruth beliefTruth_at_Task = new MutableTruth();
    public final MutableTruth beliefTruth_mean_TaskBelief = new MutableTruth();

    /**
     * choices mapping the available post targets
     */
    public final ShortBuffer canCollector = new ShortBuffer(256);


    static final int MAX_FANOUT = 64;
	/**
	 * post-derivation lookahead buffer
	 */
	final PremiseActionable[] post;


    protected PreDerivation(@Nullable Op type, Random random, int stackMax) {
        super(type, random, stackMax);
        post = Util.map(MAX_FANOUT, PremiseActionable[]::new, i->new PremiseActionable());
    }

    public abstract boolean hasBeliefTruth();

    public abstract ShortBuffer preDerive();




}
