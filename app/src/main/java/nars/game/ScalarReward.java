package nars.game;

import com.google.common.collect.Iterators;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.channel.CauseChannel;
import nars.game.sensor.ScalarSignal;
import nars.game.sensor.Signal;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.MutableTruth;
import nars.truth.Stamp;
import nars.truth.Truth;

import java.util.Iterator;

import static nars.Op.*;

/** base class for reward which represents current belief truth as the reward value  */
abstract public class ScalarReward extends Reward {

    public ScalarSignal concept;


    /** target freq */


    final MutableTruth goal = new MutableTruth(1, 0.5f);

    /** whether reinforcement tasks are stamped */
    private final boolean stamped;
    protected CauseChannel<Task> in;

    boolean negate;
    protected transient volatile float reward = Float.NaN;

    final MutableTruth RimplAPos =
        new MutableTruth(1, NAL.truth.EVI_MIN);
    final MutableTruth RimplANeg =
        new MutableTruth(0, NAL.truth.EVI_MIN);
    final MutableTruth RimplAMaybe =
        new MutableTruth(0.5f, NAL.truth.EVI_MIN);
//    final MutableTruth RimplRandomP =
//        new MutableTruth(0.5f, NAL.truth.EVI_MIN);
//    final MutableTruth RimplRandomN =
//        new MutableTruth(0.5f, NAL.truth.EVI_MIN);

    ScalarReward(Term id, boolean stamped, Game g) {
        super(id, g);

        negate = id.op()==NEG;
        this.stamped = stamped;
//        if (concept == null)
//            throw new NullPointerException("concept null for target: " + id);

    }

    /** sets the goal confidence */
    public ScalarReward conf(float c) {
        goal.conf(c);
        return this;
    }
    /** sets the goal frequency */
    public ScalarReward freq(float f) {
        goal.freq(f);
        return this;
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

    @Override
    protected final void in(Task input) {
        in.accept(input, game.what());
    }


    /**
     * HACK initialize on first call when all game actions are ready
     * TODO handle dynamic addition/removal of actions
     * */
    protected void reinforceInit(Game g) {
        Term Rpos = concept.term(), Rneg = Rpos.neg();
        Term rTarget = goal.isPositive() ? Rpos : Rneg;
        Term rTargetNeg = rTarget.neg();
        //reinforceTemporal(concept.term(), GOAL, goal, newStamp());
        reinforce(concept.term(), GOAL, goal, newStamp());
//        reinforce(CONJ.the(Rpos, $.varDep(1)), GOAL, RimplAPos);
//        reinforce(CONJ.the(Rpos.neg(), $.varDep(1)), GOAL, RimplAPos);

//        long[] stamp = newStamp(); //shared

        g.actions().forEach(a -> {
            Term A = a.term();
            long[] stampP = newStamp();
            //long[] stampN = newStamp();

            //(goal || A), (goal || --A)
            reinforce(CONJ.the(rTargetNeg, A.neg()), GOAL, RimplANeg, stampP);
            reinforce(CONJ.the(rTargetNeg, A), GOAL, RimplANeg, stampP);

//            reinforce(IMPL.the(Rpos, A), BELIEF, RimplAPos, stampP);
//            reinforce(IMPL.the(Rneg, A), BELIEF, RimplAPos, stampN);
//            reinforce(IMPL.the(Rpos, A), BELIEF, RimplANeg, stampP);
//            reinforce(IMPL.the(Rneg, A), BELIEF, RimplANeg, stampN);

            //TODO setCyclic to prevent decompose
//            reinforce(CONJ.the(rTarget, A), BELIEF, RimplAPos, stampP);
//            reinforce(CONJ.the(rTarget, A.neg()), BELIEF, RimplAPos, stampP);
            //reinforce(CONJ.the(rTarget.neg(), A), GOAL, RimplAPos, stampP);
            //reinforce(CONJ.the(rTarget.neg(), A.neg()), GOAL, RimplAPos, stampP);
//            reinforce(CONJ.the(rTarget.neg(), A), GOAL, RimplANeg, stampP);
//            reinforce(CONJ.the(rTarget.neg(), A.neg()), GOAL, RimplANeg, stampP);

//            reinforce(IMPL.the(A, Rpos), BELIEF, RimplAPos, stamp);
//            reinforce(IMPL.the(A.neg(), Rpos), BELIEF, RimplAPos, stamp);
//            reinforce(IMPL.the(A, Rpos), BELIEF, RimplANeg, stamp);
//            reinforce(IMPL.the(A.neg(), Rpos), BELIEF, RimplANeg, stamp);



//                reinforce(CONJ.the(rTarget, A), BELIEF, RimplRandomP);
//                reinforce(CONJ.the(rTarget, A.neg()), BELIEF, RimplRandomN);

//            reinforce(IMPL.the(Rpos, A), BELIEF, RimplRandomP);
//            reinforce(IMPL.the(Rneg, A), BELIEF, RimplRandomN);

//            reinforce(IMPL.the(Rpos, A), BELIEF, RimplAMaybe, stamp);
//            reinforce(IMPL.the(Rneg, A), BELIEF, RimplAMaybe, stamp);



//            reinforce(IMPL.the(Rpos, A), BELIEF, RimplAPos, stamp);
//            reinforce(IMPL.the(Rpos, A), BELIEF, RimplANeg, stamp);
//            reinforce(IMPL.the(Rneg, A), BELIEF, RimplAPos, stamp);
//            reinforce(IMPL.the(Rneg, A), BELIEF, RimplANeg, stamp);



//            reinforce(IMPL.the(Rpos, A), BELIEF, RimplAMaybe, stamp);
//            reinforce(IMPL.the(Rneg, A), BELIEF, RimplAMaybe, stamp);

//

            //reinforce(IMPL.the(A, Rpos), BELIEF, RimplAPos);
            //reinforce(IMPL.the(A.neg(), Rpos), BELIEF, RimplAPos);
//            reinforce(IMPL.the(A, Rpos), BELIEF, RimplAPos, stamp);
//            reinforce(IMPL.the(A.neg(), Rpos), BELIEF, RimplAPos, stamp);
//            reinforce(IMPL.the(A, Rpos), BELIEF, RimplANeg, stamp);
//            reinforce(IMPL.the(A.neg(), Rpos), BELIEF, RimplANeg, stamp);


//            reinforce(IMPL.the(A, Rpos), BELIEF, RimplAMaybe);
//            reinforce(IMPL.the(A.neg(), Rpos), BELIEF, RimplAMaybe);

            //reinforce(IMPL.the(Op.DISJ(Rpos,Rneg), A), BELIEF, RimplA, stamped);
        });
    }

    protected long[] newStamp() {
        long[] stamp = !stamped ? Stamp.UNSTAMPED : nar().evidence();
        return stamp;
    }

    public void reinforce(Termed x, byte punc, Truth truth) {
        reinforce(x, punc, truth, newStamp());
    }

    @Override
    protected void reinforce() {
        NAR nar = game.nar;

        float cMax = nar.confDefault(GOAL);
        goal.conf(cMax);

        float strength = 1;
        float cMin =
            Math.min(NAL.truth.CONF_MAX, Math.max(nar.confMin.floatValue(), nar.confResolution.floatValue()) * strength);
            //nar.confMin.floatValue() * strength;
            //cMax / 2;

//        RimplRandomP.conf(cMin); RimplRandomN.conf(cMin);
//        RimplRandomP.freq(game.random().nextFloat()); RimplRandomN.freq(game.random().nextFloat());

        RimplAPos.freq(goal.freq());
        RimplANeg.freq(1 - goal.freq());
        RimplAPos.conf(cMin);
        RimplANeg.conf(cMin);
        RimplAMaybe.conf(cMin);
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

    @Override
    public void init(Game g) {
        super.init(g);

        in = g.nar.newChannel(id);

        why = in.why.why;

        concept = new ScalarSignal(id, () -> reward, why, pri, g.nar);
//        if (!concept.pri.equals(attn))
//            nar().control.input(concept.pri, attn);
    }
}
