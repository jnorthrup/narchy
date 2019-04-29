package nars.agent;

import nars.term.Term;

public class BeliefReward extends ScalarReward {

    public BeliefReward(Term id, Game a) {
        super(id, a);
    }

    @Override
    protected float reward(Game a) {
        return rewardFreq(true, a.dur());
    }

}
