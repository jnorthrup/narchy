package nars.agent;

import com.google.common.collect.Iterables;
import jcog.Util;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.PriNode;
import nars.concept.Concept;
import nars.concept.sensor.GameLoop;
import nars.control.channel.CauseChannel;
import nars.table.eternal.EternalDefaultTable;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.TermedDelegate;
import nars.truth.DiscreteTruth;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/** TODO extends AgentLoop */
public abstract class Reward implements GameLoop, TermedDelegate, Iterable<Concept> {

    //public final FloatRange motivation = new FloatRange(1f, 0, 1f);

    protected final Game game;

    protected final CauseChannel<Task> in;

    protected final short[] cause;

    final PriNode attn;

    protected Reward(Term id, Game g) {
        this.game = g;

        this.attn = new PriNode(id);

        g.nar.control.input(attn, g.rewardPri);

        in = g.nar.newChannel(id);

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

//    protected final float rewardFreq(boolean beliefOrGoal) {
//        return rewardFreq(beliefOrGoal, game.dur());
//    }

    /** scalar value representing the reward state (0..1.0) */
    protected abstract float rewardFreq(boolean beliefOrGoal, float dur);

    public final NAR nar() { return game.nar(); }

    @Deprecated protected FloatFloatToObjectFunction<Truth> truther() {
        return (prev, next) -> (next == next) ?
                DiscreteTruth.the(Util.unitize(next), nar().confDefault(BELIEF)) : null;
    }


    public void setDefault(PreciseTruth t) {
        for (Concept c : this) {
            //TODO assert that it has no eternal tables already
            EternalDefaultTable.add(c,t,game.what().nar);
        }
    }

    public void alwaysWantEternally(Termed goal, float freq, boolean stamped) {
        alwaysWantEternally(goal, freq, nar().confDefault(GOAL), stamped);
    }

    public void alwaysWantEternally(Termed g, float freq, float conf, boolean stamped) {
        Term goal = g.term();
        @Nullable Truth truth = $.t(goal.op()==NEG ? 1-freq : freq, conf);

        //Term at = term().equals(goal) ? $.func(Inperience.want, goal) : $.func(Inperience.want, this.term(), goal);

        long[] stamp = stamped ? Stamp.UNSTAMPED : nar().evidence();
        Task t = NALTask.the(goal.unneg(), GOAL, truth, nar().time(), ETERNAL, ETERNAL, stamp);

//        @Nullable EternalTable eteTable = ((BeliefTables) ((TaskConcept) g).goals()).tableFirst(EternalTable.class);
//        eteTable.insert(t); //insert directly
        //game.what().accept(t);

//        Term lt = $.inh(t.term(), "curiosity");

        game.onFrame(()->{
            float pri =
                attn.pri();
                //1;
            t.pri(pri);
            game.what().accept(t);


//            DynamicTaskLink l = new DynamicTaskLink(lt) {
//
//                Termed randomSensor(Random rng) {
//                    return game.sensors.get(rng).get(rng);
//                }
//
//                Termed randomAction(Random rng) {
//                    return game.actions.get(rng).get(rng);
//                }
//
//                @Override
//                public Termed src(When<NAR> when) {
//                    return t.term();
//                    //Random rng = when.x.random();
////                    return CONJ.the(t.term(), randomSensor(rng).term().negIf(rng.nextBoolean()));
//                    //return CONJ.the(t.term(), randomSensor(rng).term().negIf(rng.nextBoolean()));
//                    //return CONJ.the(randomAction(rng).term().negIf(rng.nextBoolean()), randomSensor(rng).term().negIf(rng.nextBoolean()));
//                }
//
//                @Override
//                public Term target(Task task, Derivation d) {
//                    Random rng = d.random;
////                    return CONJ.the(
////                        randomAction(rng).term().negIf(rng.nextBoolean()),
////                        randomSensor(rng).term().negIf(rng.nextBoolean()));
//
//                    return rng.nextBoolean() ? randomAction(rng).term() : randomSensor(rng).term();
////                    return game.sensors.get(rng).get(rng).term();
//                }
//            };
//            //l.pri(pri);
//            l.priSet(GOAL,pri/2);
//            l.priSet(QUEST,pri/2);
//            game.what().link(l);
        });


//        int redundancy = eteTable.capacity();
//        Task[] t = new Task[redundancy];
//
//        for (int i = 0; i < redundancy; i++) {
//            long[] stamp = nar().evidence();
//            t[i] = NALTask.the(goal.unneg(), GOAL, truth, nar().time(), ETERNAL, ETERNAL, stamp); //TODO unevaluated?
//            eteTable.insert(t); //insert directly, avoid revision
//        }


//        PriNode a = new GoalReinforcement(at, t);

    }

//    private final class GoalReinforcement extends AttnBranch {
//        private final Task[] t;
//        private final long start;
//
//        public GoalReinforcement(Term at, Task... t) {
//            super(at, List.of(t));
//            this.t = t;
//            start = nar().time();
//        }
//
//        @Override
//        public void update(MapNodeGraph<PriNode,Object> g) {
//            super.update(g);
//
//            float p = pri();
//            for (Task tt : t) {
//                tt.pri(p);
//                tt.setCreation(start); //refresh start time
//            }
//
//            in.acceptAll(t, game.what());
//        }
//
//    }
}
