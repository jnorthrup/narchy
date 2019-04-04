package nars.agent;

import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.AttnBranch;
import nars.attention.PriNode;
import nars.concept.Concept;
import nars.control.channel.CauseChannel;
import nars.op.mental.Inperience;
import nars.table.eternal.DefaultOnlyEternalTable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
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
public abstract class Reward implements TermedDelegate, Iterable<Concept> {

    //public final FloatRange motivation = new FloatRange(1f, 0, 1f);

    protected final NAgent agent;

    protected transient volatile float rewardBelief = Float.NaN;

    protected final CauseChannel<ITask> in;

    final static boolean goalUnstamped = false;

    final PriNode attn;

    public Reward(NAgent a) {
    //TODO
    //public Reward(NAgent a, FloatSupplier r, float confFactor) {
        this.agent = a;

        this.attn = new PriNode(this);
        attn.parent(a.nar(), a.attnReward);

        in = a.nar().newChannel(this);

    }

    /** estimated current happiness/satisfaction of this reward
     *
     * happiness = 1 - Math.abs(rewardBeliefExp - rewardGoalExp)/Math.max(rewardBeliefExp,rewardGoalExp)
     * */
    abstract public float happiness();

    /** scalar value representing the reward state (0..1.0) */
    protected abstract float rewardFreq(boolean beliefOrGoal);

    public final NAR nar() { return agent.nar(); }

    public final void update(long prev, long now) {
        rewardBelief = rewardFreq(true);
        updateReward(prev, now);
    }

    abstract protected void updateReward(long prev, long now);

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
        //HACK
        PriNode a = new AttnBranch(at, List.of(t)) {
            @Override
            public void update(MapNodeGraph<PriNode,Object> g) {
                super.update(g);
                t.pri(pri());
                in.input(t);
            }

        };
        a.parent(nar(), attn);
    }

}
