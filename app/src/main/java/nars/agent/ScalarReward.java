package nars.agent;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.sensor.Signal;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.Truth;

import java.util.Iterator;

import static nars.Op.NEG;

/** base class for reward which represents current belief truth as the reward value  */
abstract public class ScalarReward extends Reward {

    public final Signal concept;
    protected final Term id;

    /** target freq */
    protected final float freq;

    boolean negate;
    protected transient volatile float reward = Float.NaN;

    ScalarReward(Term id, float freq, boolean stamped, Game g) {
        super(id, g);
        this.id = id;
        this.freq = freq;
        negate = id.op()==NEG;
        concept = newConcept();
        if (concept == null)
            throw new NullPointerException("concept null for target: " + id);
        alwaysWantEternally(concept, freq, stamped);
    }

    @Override
    public final void update(Game a) {
        this.reward = reward(a);
        concept.update(a);
    }

    protected abstract float reward(Game a);

    @Override
    public final float happiness(float dur) {
        //     belief(reward) - goal(reward)

        float b = rewardFreq(true, dur);
        float g = rewardFreq(false, dur);
        if ((b!=b) || (g!=g))
            return 0; //NaN
        else
            return (1 - Math.abs(b - g)/Math.max(b,g));
    }

    @Override
    protected float rewardFreq(boolean beliefOrGoal, float dur) {
        NAR n = nar();
        BeliefTable bt = beliefOrGoal ? concept.beliefs() : concept.goals();
        Truth t = bt.truth(n.time(), dur, n);
        if (t!=null) {
            float f = t.freq();
            return negate ? 1 - f : f;
        } else
            return Float.NaN;
    }

    abstract protected Signal newConcept();

    @Override
    public final Iterator<Concept> iterator() {
        return Iterators.singletonIterator(concept);
    }

    @Override
    public Term term() {
        return id;
    }
}
