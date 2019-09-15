package nars.game;

import com.google.common.collect.Iterators;
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

    public final Signal concept;
    protected final Term id;

    /** target freq */
    protected final float freq;
    private final float conf;

    /** whether reinforcement tasks are stamped */
    private final boolean stamped;

    boolean negate;
    protected transient volatile float reward = Float.NaN;

    ScalarReward(Term id, float freq, float conf, boolean stamped, Game g) {
        super(id, g);
        this.id = id;
        this.freq = freq;
        this.conf = conf;
        negate = id.op()==NEG;
        this.stamped = stamped;
        concept = newConcept();
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
        reinforceGoal(concept, freq, conf, stamped);

        Truth RimplA =
            //$.t(0.5f, 0.05f);
            new MutableTruth(0.5f, 0.05f);

        g.actions().forEach(a -> {
            Term A = a.term();
            reinforce(IMPL.the(Rpos, A), BELIEF, RimplA, stamped);
            reinforce(IMPL.the(Rneg, A), BELIEF, RimplA, stamped);
        });
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
