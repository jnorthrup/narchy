package nars.agent;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import nars.truth.Truth;

import java.util.Iterator;

import static nars.Op.NEG;

/** base class for reward which represents current belief truth as the reward value  */
public class BeliefReward extends Reward {

    public final Concept concept;
    protected final Term id;
    boolean negate;

    public BeliefReward(Term id, Game a) {
        super(a);
        this.id = id;
        negate = id.op()==NEG;
        concept = newConcept();
        if (concept == null)
            throw new NullPointerException("concept null for target: " + id);
        alwaysWantEternally(id);
    }

    @Override
    protected void updateReward(Game g) {
        //..
    }

    @Override
    public final float happiness() {
        //     belief(reward) - goal(reward)

        float b = this.rewardBelief;
        float g = rewardFreq(false);
        if ((b!=b) || (g!=g))
            return 0; //dead
        else
            return 1 - Math.abs(b - g)/Math.max(b,g);
    }

    @Override
    protected float rewardFreq(boolean beliefOrGoal) {
        NAR n = nar();
        Truth t = (beliefOrGoal ? concept.beliefs() : concept.goals()).truth(n.time(), n);
        if (t!=null)
            return negate ? 1 - t.freq() : t.freq();
        else
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
