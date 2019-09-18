package nars.game;

import com.google.common.collect.Iterators;
import nars.NAL;
import nars.NAR;
import nars.concept.Concept;
import nars.game.sensor.Signal;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.MutableTruth;
import nars.truth.Truth;

import java.util.Iterator;

import static nars.Op.*;

/** base class for reward which represents current belief truth as the reward value  */
abstract public class ScalarReward extends Reward {

    public Signal concept;


    /** target freq */


    final MutableTruth goal = new MutableTruth();

    /** whether reinforcement tasks are stamped */
    private final boolean stamped;

    boolean negate;
    protected transient volatile float reward = Float.NaN;

    final MutableTruth RimplA =
        //$.t(0.5f, 0.05f);
        new MutableTruth(1, NAL.truth.EVI_MIN);

    ScalarReward(Term id, float freq, boolean stamped, Game g) {
        super(id, g);

        goal.freq(freq);
        negate = id.op()==NEG;
        this.stamped = stamped;
//        if (concept == null)
//            throw new NullPointerException("concept null for target: " + id);


    }


    @Override
    public final void update(Game g) {
        if (reinforcement.isEmpty()) {
            reinforceInit(g);
        }

        super.update(g);

        this.reward = reward(g);

        concept.update(g);
    }

    /**
     * HACK initialize on first call when all game actions are ready
     * TODO handle dynamic addition/removal of actions
     * */
    protected void reinforceInit(Game g) {
        Term Rpos = concept.term(), Rneg = Rpos.neg();
        reinforce(Rpos, GOAL, goal, stamped);

        g.actions().forEach(a -> {
            Term A = a.term();

            reinforce(IMPL.the(Rpos, A), BELIEF, RimplA, stamped);
            reinforce(IMPL.the(Rneg, A), BELIEF, RimplA, stamped);

            //reinforce(IMPL.the(Op.DISJ(Rpos,Rneg), A), BELIEF, RimplA, stamped);
        });
    }

    @Override
    protected void reinforce() {
        NAR nar = game.nar;

        goal.conf(nar.confDefault(GOAL));

        float strength = 1;
        RimplA.conf( Math.min(NAL.truth.CONF_MAX, Math.max(nar.confMin.floatValue(), nar.confResolution.floatValue()) * strength ));
        super.reinforce();
    }

    protected abstract float reward(Game a);

    @Override
    public final float happiness(float dur) {

        float b = rewardFreq(true, dur);
        float g = rewardFreq(false, dur);
        if ((b!=b) || (g!=g))
            return 0; //NaN
        else
            return (1 - Math.abs(b - g));
    }

    @Override
    protected float rewardFreq(boolean beliefOrGoal, float dur) {
        Signal c = this.concept;
        if (c==null)
            return Float.NaN;

        BeliefTable bt = beliefOrGoal ? c.beliefs() : c.goals();


        Truth t = bt.truth(game.time(), dur, nar());

        if (t!=null) {
            float f = t.freq();
            return negate ? 1 - f : f;
        } else
            return Float.NaN;
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
