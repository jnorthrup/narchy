package nars.derive;

import jcog.Util;
import jcog.data.ShortBuffer;
import nars.NAL;
import nars.Op;
import nars.derive.action.PremiseAction;
import nars.term.Term;
import nars.truth.MutableTruth;
import nars.truth.Truth;
import nars.truth.func.TruthFunction;
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
	final PostDerivable[] post;


    protected PreDerivation(@Nullable Op type, Random random, int stackMax) {
        super(type, random, stackMax);
        post = Util.map(MAX_FANOUT, PostDerivable[]::new, i->new PostDerivable());
    }

    public abstract boolean hasBeliefTruth();

    public abstract ShortBuffer preDerive();


    public boolean run(Truth truth, byte punc, boolean single, TruthFunction truthFunction, PremiseAction action) {
        Derivation d = (Derivation) this;
        d.clear();
        d.retransform.clear();
        d.forEachMatch = null;

        d.truth.set(truth);
        d.punc = punc;
        d.single = single;
        d.truthFunction = truthFunction;

        //System.out.println(d + " " + action);
        action.run(d);

        return d.use(NAL.derive.TTL_COST_BRANCH);
    }








}
