package nars.derive;

import jcog.Util;
import jcog.data.ShortBuffer;
import nars.attention.What;
import nars.term.Term;
import nars.time.When;
import nars.truth.MutableTruth;

import java.util.function.IntFunction;

/** contains only information which depends on the premise itself (Task, Belief, BeliefTerm).
 * used for first stage winnowing to determine the (memoizable) set of possible forkable outcomes */
public abstract class PreDerivation extends When<What> {


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
	final PremiseRunnable[] post;


    protected PreDerivation() {

        post = Util.map(MAX_FANOUT, PremiseRunnable[]::new, new IntFunction<PremiseRunnable>() {
            @Override
            public PremiseRunnable apply(int i) {
                return new PremiseRunnable();
            }
        });
    }

    public abstract boolean hasBeliefTruth();

    public abstract ShortBuffer preDerive();



}
