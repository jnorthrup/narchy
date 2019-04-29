package nars.agent;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.sensor.Signal;
import nars.term.Term;
import nars.truth.Truth;

import java.util.Iterator;

import static nars.Op.NEG;

/** base class for reward which represents current belief truth as the reward value  */
abstract public class ScalarReward extends Reward {

    public final Concept concept;
    protected final Term id;
    boolean negate;
    protected transient volatile float reward = Float.NaN;

    ScalarReward(Term id, Game a) {
        super(id, a);
        this.id = id;
        negate = id.op()==NEG;
        concept = newConcept();
        if (concept == null)
            throw new NullPointerException("concept null for target: " + id);
        alwaysWantEternally(id);
    }

    @Override
    public final void update(Game a) {
        this.reward = reward(a);
        if (concept instanceof Signal) //??
            ((Signal)concept).update(a);
    }

    protected abstract float reward(Game a);

    @Override
    public final float happiness(int dur) {
        //     belief(reward) - goal(reward)

        float b = rewardFreq(true, dur);
        float g = rewardFreq(false, dur);
        if ((b!=b) || (g!=g))
            return 0; //dead
        else
            return 1 - Math.abs(b - g)/Math.max(b,g);
    }

    @Override
    protected float rewardFreq(boolean beliefOrGoal, int dur) {
        NAR n = nar();
        Truth t = (beliefOrGoal ? concept.beliefs() : concept.goals()).truth(n.time(), dur, n);
        if (t!=null) {
            float f = t.freq();
            return negate ? 1 - f : f;
        } else
            return Float.NaN;
    }

    protected Concept newConcept() {
        return nar().conceptBuilder.construct(id.unneg());
    }


    @Override
    public final Iterator<Concept> iterator() {
        return Iterators.singletonIterator(concept);
    }

    @Override
    public Term term() {
        return id;
    }
}
