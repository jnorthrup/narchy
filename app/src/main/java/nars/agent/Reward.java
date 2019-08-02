package nars.agent;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.AttnBranch;
import nars.attention.PriNode;
import nars.concept.Concept;
import nars.concept.sensor.GameLoop;
import nars.control.channel.CauseChannel;
import nars.op.mental.Inperience;
import nars.table.eternal.EternalDefaultTable;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.TermedDelegate;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/** TODO extends AgentLoop */
public abstract class Reward implements GameLoop, TermedDelegate, Iterable<Concept> {

    //public final FloatRange motivation = new FloatRange(1f, 0, 1f);

    protected final Game game;

    protected final CauseChannel<Task> in;

    protected final short[] cause;

    final PriNode attn;

    public Reward(Term id, Game g) {
    //TODO
    //public Reward(NAgent a, FloatSupplier r, float confFactor) {
        this.game = g;

        this.attn = new PriNode(id);

        input(attn, g.attnReward);

        in = g.nar().newChannel(id);

        cause = new short[] {  in.id };
    }


    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(this, x-> x); //HACK
    }


    /** estimated current happiness/satisfaction of this reward
     *
     * happiness = 1 - Math.abs(rewardBeliefExp - rewardGoalExp)/Math.max(rewardBeliefExp,rewardGoalExp)
     * */
    abstract public float happiness(float dur);

    protected final float rewardFreq(boolean beliefOrGoal) {
        return rewardFreq(beliefOrGoal, game.dur());
    }

    /** scalar value representing the reward state (0..1.0) */
    protected abstract float rewardFreq(boolean beliefOrGoal, float dur);

    public final NAR nar() { return game.nar(); }

    @Deprecated protected FloatFloatToObjectFunction<Truth> truther() {
        return (prev, next) -> (next == next) ?
                $.t(Util.unitize(next), nar().confDefault(BELIEF)) : null;
    }


    public void setDefault(PreciseTruth t) {
        for (Concept c : this) {
            //TODO assert that it has no eternal tables already
            EternalDefaultTable.add(c,t,game.what().nar);
        }
    }

    public void alwaysWantEternally(Termed goal, float freq) {
        alwaysWantEternally(goal, freq, nar().confDefault(GOAL));
    }

    public void alwaysWantEternally(Termed g, float freq, float conf) {
        Term goal = g.term();
        @Nullable Truth truth = $.t(goal.op()==NEG ? 1-freq : freq, conf);

        Term at = term().equals(goal) ? $.func(Inperience.want, goal) : $.func(Inperience.want, this.term(), goal);

        long[] stamp = NAL.REWARD_GOAL_UNSTAMPED ? Stamp.UNSTAMPED : nar().evidence();
        Task[] t = new Task[] { NALTask.the(goal.unneg(), GOAL, truth, nar().time(), ETERNAL, ETERNAL, stamp) };


//        @Nullable EternalTable eteTable = ((BeliefTables) ((TaskConcept) g).goals()).tableFirst(EternalTable.class);
//        int redundancy = eteTable.capacity();
//        Task[] t = new Task[redundancy];
//
//        for (int i = 0; i < redundancy; i++) {
//            long[] stamp = nar().evidence();
//            t[i] = NALTask.the(goal.unneg(), GOAL, truth, nar().time(), ETERNAL, ETERNAL, stamp); //TODO unevaluated?
//            eteTable.insert(t[i]); //insert directly, avoid revision
//        }


        PriNode a = new GoalReinforcement(at, t);

        input(a, attn);
    }

    private final class GoalReinforcement extends AttnBranch {
        private final Task[] t;
        private final long start;

        public GoalReinforcement(Term at, Task... t) {
            super(at, List.of(t));
            this.t = t;
            start = nar().time();
        }

        @Override
        public void update(MapNodeGraph<PriNode,Object> g) {
            super.update(g);

            float p = pri();
            for (Task tt : t) {
                tt.pri(p);
                tt.setCreation(start); //refresh start time
            }

            in.acceptAll(t, game.what());
        }

    }
}
