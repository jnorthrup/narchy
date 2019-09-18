package nars.derive;

import jcog.Util;
import jcog.data.ShortBuffer;
import jcog.pri.HashedPLink;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;
import jcog.signal.meter.FastCounter;
import nars.*;
import nars.derive.action.PremiseAction;
import nars.derive.premise.MatchedPremise;
import nars.derive.premise.Premise;
import nars.link.TaskLink;
import nars.term.Term;
import nars.truth.MutableTruth;
import nars.truth.Truth;
import nars.truth.func.TruthFunction;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.Op.COMMAND;

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

    public boolean hasBeliefTruth() {
        return beliefTruth_at_Belief.is() || beliefTruth_at_Task.is();
    }

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

    /** queue of pending premises to fire
     *  TODO use a bag to deduplicate and rank
     * */
    final PLinkArrayBag<Premise> premises = new PLinkArrayBag(PriMerge.max, 0);

    public void add(Premise p) {
        premises.putAsync(new HashedPLink<>(p, pri(p)));
    }

    protected float pri(Premise p) {
        float TASKLINK_RATE = 0.1f; //1 / deriver.links ...
        float TASK_STRUCTURE_RATE = 0.5f;

        Task t = p.task;
        if (t instanceof TaskLink)
            return t.pri() * TASKLINK_RATE;
        else if (p instanceof MatchedPremise)
            return Util.or(t.priElseZero(), p.belief().priElseZero());
        else
            return t.pri() * (p.beliefTerm.equals(t.term()) ? TASK_STRUCTURE_RATE : 1);
    }

    protected void derive(Premise _p) {

        Derivation d = (Derivation) this;
        Premise p = match(d, _p);

        NAR nar = d.nar;
        int deriveTTL = nar.deriveBranchTTL.intValue();

        FastCounter result = d.derive(p, deriveTTL);

        Emotion e = nar.emotion;
        if (result == e.premiseUnderivable1) {
            //System.err.println("underivable1:\t" + p);
        } else {
//				System.err.println("  derivable:\t" + p);
        }

        //ttlUsed = Math.max(0, deriveTTL - d.ttl);

        //e.premiseTTL_used.recordValue(ttlUsed); //TODO handle negative amounts, if this occurrs.  limitation of HDR histogram
        result.increment();
    }

    private Premise match(Derivation d, Premise p) {
        if (p.task.punc()!=COMMAND) //matchable?
            return p.match(d,d.nar.premiseUnifyTTL.intValue());
        else
            return p;
    }

}
