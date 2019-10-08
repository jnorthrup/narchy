package nars.game;

import jcog.TODO;
import jcog.math.FloatRange;
import nars.term.Term;

public class BeliefReward extends ScalarReward {

    public BeliefReward(Term id, Game g) {
        super(id, 1f, true, g);
    }

    @Override
    protected float reward(Game a) {
        return rewardFreq(true, a.dur());
    }

}
