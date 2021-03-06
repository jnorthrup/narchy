package nars.game;

import jcog.Skill;
import jcog.Util;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Task;
import nars.attention.PriNode;
import nars.concept.Concept;
import nars.control.op.Rememorize;
import nars.game.sensor.GameLoop;
import nars.table.eternal.EternalDefaultTable;
import nars.task.NALTask;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.TermedDelegate;
import nars.truth.DiscreteTruth;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/** TODO extend UniSignal */
public abstract class Reward implements GameLoop, TermedDelegate, Iterable<Concept> {

    //public final FloatRange motivation = new FloatRange(1f, 0, 1f);

    @Deprecated protected final Game game;
	public final Term id;

    protected Term why;

    final PriNode pri;

	@Skill({"Curiosity", "Central_pattern_generator","Phantom_limb"})
    protected final FasterList<Rememorize> reinforcement = new FasterList<>();

    protected Reward(Term id, Game g) {
    	this.id = id;
        this.game = g;

        this.pri = new PriNode(id);

    }



    @Override
    public Iterable<? extends Termed> components() {
        //return Iterables.transform(this, x-> x); //HACK
		return this;
    }


    /** estimated current happiness/satisfaction of this reward
     *
     * happiness = 1 - Math.abs(rewardBeliefExp - rewardGoalExp)/Math.max(rewardBeliefExp,rewardGoalExp)
     * */
	public abstract double happiness(long start, long end, float dur);

//    protected final float rewardFreq(boolean beliefOrGoal) {
//        return rewardFreq(beliefOrGoal, game.dur());
//    }

    /** scalar value representing the reward state (0..1.0) */
    protected abstract float rewardFreq(boolean beliefOrGoal, long start, long end, float dur);

    public final NAR nar() { return game.nar(); }

    @Deprecated protected FloatFloatToObjectFunction<Truth> truther() {
        return new FloatFloatToObjectFunction<Truth>() {
            @Override
            public Truth value(float prev, float next) {
                return (next == next) ?
                        DiscreteTruth.the(Util.unitize(next), Reward.this.nar().confDefault(BELIEF)) : null;
            }
        };
    }


    public void setDefault(PreciseTruth t) {
        NAR n = this.nar();
        for (Concept c : this) {
            //TODO assert that it has no eternal tables already
            EternalDefaultTable.add(c,t,n);
        }
    }

//	public void reinforceTemporal(Termed x, byte punc, Truth truth, long[] stamp) {
//		Term goal = x.term();
//
//		if (goal instanceof Neg) {
//			throw new UnsupportedOperationException();
//		}
//
//		synchronized (reinforcement) {
//			reinforcement.add(() ->
//				NALTask.the(goal, punc, truth, nar().time(), game.nowPercept.start, game.nowPercept.end, stamp)
//			);
//		}
//	}

    public void reinforce(Termed x, byte punc, Truth truth, long[] stamp) {
        Term goal = x.term();

        //Term at = term().equals(goal) ? $.func(Inperience.want, goal) : $.func(Inperience.want, this.term(), goal);
		if (goal instanceof Neg) {
			throw new UnsupportedOperationException();
//			goal = goal.unneg();
//			truth = truth.neg();
		}
        Task t = NALTask.the(goal, punc, truth, nar().time(), ETERNAL, ETERNAL, stamp);
		//t.setCyclic(true); //TODO permanent

		synchronized(reinforcement) {
			reinforcement.add(Rememorize.the(t, game.what()));
		}
        //return t;
//        @Nullable EternalTable eteTable = ((BeliefTables) ((TaskConcept) g).goals()).tableFirst(EternalTable.class);
//        eteTable.insert(t); //insert directly
        //game.what().accept(t);

//        Term lt = $.inh(t.term(), "curiosity");



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

	@Override
	public void accept(Game a) {
		reinforce();
	}

	/** called each update to refresh reinforcement tasks */
	protected void reinforce() {
        int n = reinforcement.size();
		if (n > 0) {
            float pri = this.pri.pri()
				// *1f/Util.sqrt(n) //not too large or it will compete with the signal itself
			;

//			for (Task t : reinforcement)
//				t.pri(pri);
//			game.what().acceptAll(reinforcement);

			//Supplier<Task> t = reinforcement.get(nar().random());
			for (Rememorize t : reinforcement)
				t.input(pri);
		}

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

	}

	/** channel for reinforcement signals */
	protected abstract void in(Task input);

	@Deprecated public void init(Game g) {

	}
}
