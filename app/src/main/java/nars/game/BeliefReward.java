package nars.game;

import nars.term.Term;

public class BeliefReward extends ScalarReward {

    public BeliefReward(Term id, Game g) {
        super(id, true, g);
    }

    @Override
    protected float reward(Game a) {
        return rewardFreq(true, a.dur());
    }

}
