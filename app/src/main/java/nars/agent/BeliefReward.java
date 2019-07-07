package nars.agent;

import jcog.TODO;
import jcog.math.FloatRange;
import nars.concept.sensor.Signal;
import nars.term.Term;

public class BeliefReward extends ScalarReward {

    public BeliefReward(Term id, Game a) {
        super(id, 1f, a);
    }

    @Override
    protected float reward(Game a) {
        return rewardFreq(true, a.dur());
    }

    @Override
    protected Signal newConcept() {
        throw new TODO();
    }

    @Override
    public FloatRange resolution() {
        throw new TODO();
    }
}
