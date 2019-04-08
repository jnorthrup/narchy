package nars.agent;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.AttnBranch;
import nars.attention.PriNode;
import nars.concept.Concept;
import nars.concept.sensor.GameLoop;
import nars.control.channel.CauseChannel;
import nars.op.mental.Inperience;
import nars.table.eternal.DefaultOnlyEternalTable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.TermedDelegate;
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

    protected transient volatile float rewardBelief = Float.NaN;

    protected final CauseChannel<ITask> in;

    final static boolean goalUnstamped = false;

    final PriNode attn;

    public Reward(Term id, Game g) {
    //TODO
    //public Reward(NAgent a, FloatSupplier r, float confFactor) {
        this.game = g;

        this.attn = new PriNode(id);
        attn.parent(g.nar(), g.attnReward);

        in = g.nar().newChannel(id);

    }

    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(this, x-> x); //HACK
    }

    @Override
    public FloatRange resolution() {
        return nar().freqResolution;
    }

    /** estimated current happiness/satisfaction of this reward
     *
     * happiness = 1 - Math.abs(rewardBeliefExp - rewardGoalExp)/Math.max(rewardBeliefExp,rewardGoalExp)
     * */
    abstract public float happiness();

    /** scalar value representing the reward state (0..1.0) */
    protected abstract float rewardFreq(boolean beliefOrGoal);

    public final NAR nar() { return game.nar(); }

    public final void update(Game g) {
        rewardBelief = rewardFreq(true);
        updateReward(g);
    }

    protected abstract void updateReward(Game g);

    @Deprecated protected FloatFloatToObjectFunction<Truth> truther() {
        return (prev, next) -> (next == next) ?
                $.t(Util.unitize(next), nar().confDefault(BELIEF)) : null;
    }


    public void setDefault(PreciseTruth t) {
        for (Concept c : this) {
            //TODO assert that it has no eternal tables already
            new DefaultOnlyEternalTable(c,t,nar());

        }

    }
    public void alwaysWantEternally(Term goal) {
        alwaysWantEternally(goal, nar().confDefault(GOAL));
    }

    public void alwaysWantEternally(Term goal, float conf) {
        @Nullable Truth truth = $.t(goal.op()==NEG ? 0f : 1f, conf);
        long[] stamp = goalUnstamped ? Stamp.UNSTAMPED : nar().evidence();
        Task t = NALTask.the(goal.unneg(), GOAL, truth, nar().time(), ETERNAL, ETERNAL, stamp);

        Term at = term().equals(goal) ? $.func(Inperience.want, goal) : $.func(Inperience.want, this.term(), goal);

        PriNode a = new MyAttnBranch(at, t);
        a.parent(nar(), attn);
    }

    private final class MyAttnBranch extends AttnBranch {
        private final Task t;

        public MyAttnBranch(Term at, Task t) {
            super(at, List.of(t));
            this.t = t;
        }

        @Override
        public void update(MapNodeGraph<PriNode,Object> g) {
            super.update(g);
            t.pri(pri());
            in.accept(t, game.what());
        }

    }
}
