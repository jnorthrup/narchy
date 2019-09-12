package nars.agent;

import jcog.TODO;
import jcog.math.FloatRange;
import nars.concept.sensor.Signal;
import nars.term.Term;

import static nars.Op.GOAL;

public class BeliefReward extends ScalarReward {

    public BeliefReward(Term id, Game g) {
        super(id, 1f, g.nar().confDefault(GOAL), true, g);
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
