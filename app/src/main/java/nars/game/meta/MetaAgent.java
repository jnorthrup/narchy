package nars.game.meta;

import jcog.Util;
import jcog.learn.ql.dqn3.DQN3;
import jcog.math.FloatRange;
import jcog.pri.UnitPri;
import jcog.util.FloatConsumer;
import nars.$;
import nars.NAR;
import nars.attention.PriAmp;
import nars.attention.PriSource;
import nars.game.Game;
import nars.game.GameTime;
import nars.game.action.GoalActionConcept;
import nars.game.util.RLBooster;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.util.Map;

/**
 * supraself agent metavisor
 */
public abstract class MetaAgent extends Game {

//	static final Atomic CURIOSITY =
//		/** curiosity rate */
//		Atomic.the("curi");

	//private static final Logger logger = Log.logger(MetaAgent.class);
	static final Atomic /**
	 * tasklink forget factor
	 */
		forget = Atomic.the("forget");
	static final Atomic grow = Atomic.the("grow");
	static final Atomic /**
	 * internal truth frequency precision
	 */
		precise = Atomic.the("precise"); //frequency resolution
	static final Atomic careful = Atomic.the("careful"); //conf resolution
	static final Atomic ignore = Atomic.the("ignore"); //min conf

	static final Atomic belief = Atomic.the("belief");
	static final Atomic goal = Atomic.the("goal");

	static final Atomic question = Atomic.the("question");
	static final Atomic quest = Atomic.the("quest");

	static final Atomic conf = Atomic.the("conf");
	static final Atomic pri = Atomic.the("pri");
	static final Atomic play = Atomic.the("play");
	static final Atomic input = Atomic.the("input");
	static final Atomic duration = Atomic.the("dur");
	static final Atomic happy = Atomic.the("happy");
	static final Atomic dex = Atomic.the("dex");
	static final Atomic now = Atomic.the("now");
	static final Atomic past = Atomic.the("past");
	static final Atomic future = Atomic.the("future");
	private static final float PRI_ACTION_RESOLUTION =
		//0.01f;
		0.05f;


//    public final GoalActionConcept forgetAction;
//    public final GoalActionConcept beliefPriAction;
//    private final GoalActionConcept goalPriAction;
//    private final GoalActionConcept dur;
//    static int curiStartupDurs = 5000;
//    static float curiMax = 0.2f;
//    static float curiMinOld = 0.01f, curiMinYoung = 0.04f;


//    public MetaAgent(NAR n) {
//        super(env(n.self()), GameTime.durs(1), n);
//        throw new TODO();
//
////        n.parts(Game.class).forEach(a -> {
////            if(MetaAgent.this!=a)
////                add(a, false);
////        });
//    }


	protected MetaAgent(Term id, GameTime t, NAR nar) {
		super(id, t);
		this.nar = nar;
	}

	public MetaAgent addRLBoost() {
//        meta.what().pri(0.05f);
		RLBooster metaBoost = new RLBooster(true, this, 4, 5, (i, o) ->
			//new HaiQae(i, 12, o).alpha(0.01f).gamma(0.9f).lambda(0.9f),
			new DQN3(i, o, Map.of(
			))
		);
//        window(grid(NARui.rlbooster(metaBoost), 800, 800);
		return this;
	}

//	void actionCtlPriNodeRecursive(PriNode s, MapNodeGraph<PriNode, Object> g) {
//		if (s instanceof PriAmp)
//			priAction((PriAmp) s);
//		for (Node<PriNode, Object> x : s.node(g).nodes(false, true)) {
//			actionCtlPriNodeRecursive(x.id(), g);
//		}
//	}

	void priAction(PriSource a) {
		priAction($.inh(a.id, pri), a);
	}

	void priAction(PriAmp a) {
		floatAction($.inh(a.id, pri), 2, a.amp).resolution(PRI_ACTION_RESOLUTION);
	}

	void priAction(Term id, PriSource a) {
		floatAction(id, 2, a.pri).resolution(PRI_ACTION_RESOLUTION);
	}

//    @Override
//    protected void sense() {
//        ((TaskLinkWhat)what()).dur.set(durPhysical()*2 /* nyquist */);
//        super.sense();
//    }

//    /**
//     * curiosity frequency -> probability mapping curve
//     */
//    static float curiosity(Game agent, long start, float c) {
//
//        float min;
//        float durs = (float) (((double) (agent.nar().time() - start)) / agent.dur());
//        if (durs < curiStartupDurs)
//            min = curiMinYoung;
//        else
//            min = curiMinOld;
//
//        return Util.lerp(c, min, curiMax);
//    }

	protected GoalActionConcept floatAction(Term t, FloatRange r) {
		return floatAction(t, 1, r);
	}

	protected GoalActionConcept floatAction(Term t, float exp, FloatRange r) {
		return floatAction(t, r.min, r.max, exp, r::set);
	}
	protected GoalActionConcept floatAction(Term t, float exp, UnitPri r) {
		return floatAction(t, 0, 1, exp, r::pri);
	}

	protected GoalActionConcept floatAction(Term t, float min, float max, float exp, FloatConsumer r) {
		//FloatAveraged f = new FloatAveraged(/*0.75*/ 1);
		//FloatToFloatFunction f = (z)->z;
		return actionUnipolar(t, true, (v) -> v, (x) -> {
			//float y = f.valueOf(x);
			if (x == x)
				r.accept(Util.lerp((float) Math.pow(x, exp), min, max));
			return x;
		});
	}

//	private float dur(int initialDur, float d) {
//		return Math.max(1, ((d + 0.5f) * 2 * initialDur));
//	}

	//    public GoalActionConcept[] dial(Game a, Atomic label, FloatRange var, int steps) {
//        GoalActionConcept[] priAction = actionDial(
//                $.inh(id, $.p(label, $.the(-1))),
//                $.inh(id, $.p(label, $.the(+1))),
//                var,
//                steps);
//        return priAction;
//    }

	//    /** creates a base agent that can be used to interface with external controller
//     *  it will be consistent as long as the NAR architecture remains the same.
//     *  TODO kill signal notifying changed architecture and unwiring any created WiredAgent
//     *  */
//    public Agenterator agent(FloatSupplier reward, IntIntToObjectFunction<Agent> a) {
//        AgentBuilder b = new AgentBuilder(reward);
//        for (MetaGoal m : MetaGoal.values()) {
//            b.out(5, i->{
//                float w;
//                switch(i) {
//                    default:
//                    case 0: w = -1; break;
//                    case 1: w = -0.5f; break;
//                    case 2: w = 0; break;
//                    case 3: w = +0.5f; break;
//                    case 4: w = +1; break;
//                }
//                nar.emotion.want(m, w);
//            });
//        }
//
//        for (Why c : why) {
//
//            b.in(() -> {
//                float ca = c.amp();
//                return ca==ca ? ca : 0;
//            });
//
////            for (MetaGoal m : MetaGoal.values()) {
////                Traffic mm = c.credit[m.ordinal()];
////                b.in(()-> mm.current);
////            }
//            //TODO other data
//        }
//
//        for (How c : nar.how) {
//            b.in(() -> {
//                PriNode cp = c.pri;
//                return Util.unitize(cp.priElseZero());
//            });
//            //TODO other data
//        }
//
//        return b.get(a);
//    }


//    private static NAgent metavisor(NAgent a) {
//
////        new NARSpeak.VocalCommentary( nar());
//
//        //
////        nar().onTask(x -> {
////           if (x.isGoal() && !x.isInput())
////               System.out.println(x.proof());
////        });
//
//        int durs = 4;
//        NAR nar = nar();
//
//        NAgent m = new NAgent($.func("meta", id), FrameTrigger.durs(durs), nar);
//
//        m.reward(
//                new SimpleReward($.func("dex", id),
//                        new FloatNormalized(new FloatFirstOrderDifference(nar()::time,
//                                a::dexterity)).relax(0.01f), m)
//        );
//
////        m.actionUnipolar($.func("forget", id), (f)->{
////            nar.memoryDuration.setAt(Util.lerp(f, 0.5f, 0.99f));
////        });
////        m.actionUnipolar($.func("awake", id), (f)->{
////            nar.conceptActivation.setAt(Util.lerp(f, 0.1f, 0.99f));
////        });
//        m.senseNumber($.func("busy", id), new FloatNormalized(() ->
//                (float) Math.log(1 + m.nar().emotion.busyVol.getMean()), 0, 1).relax(0.05f));
////
//
////        actionUnipolar($.inh(this.nar.self(), $.the("deep")), (d) -> {
////            if (d == d) {
////                //deep incrases both duration and max target volume
////                this.nar.time.dur(Util.lerp(d * d, 20, 120));
////                this.nar.termVolumeMax.setAt(Util.lerp(d, 30, 60));
////            }
////            return d;
////        });
//
////        actionUnipolar($.inh(this.nar.self(), $.the("awake")), (a)->{
////            if (a == a) {
////                this.nar.activateConceptRate.setAt(Util.lerp(a, 0.2f, 1f));
////            }
////            return a;
////        });
//
////        actionUnipolar($.prop(nar.self(), $.the("focus")), (a)->{
////            nar.forgetRate.setAt(Util.lerp(a, 0.9f, 0.8f)); //inverse forget rate
////            return a;
////        });
//
////        m.actionUnipolar($.func("curious", id), (cur) -> {
////            curiosity.setAt(lerp(cur, 0.01f, 0.25f));
////        });//.resolution(0.05f);
//
//
//        return m;
//    }
//    public static AgentBuilder newController(NAgent a) {
//        NAR n = a.nar;
//
//        Emotion ne = n.emotion;
//        Arrays.fill(ne.want, 0);
//
//        AgentBuilder b = new AgentBuilder(
//
//                HaiQae::new,
//
//                () -> a.enabled.get() ? (0.1f + a.dexterity()) * Util.tanhFast(a.reward) /* - lag */ : 0f)
//
//                .in(a::dexterity)
//                .in(a.happy)
//
//
//
//
//                .in(new FloatNormalized(
//
//                        new FloatFirstOrderDifference(n::time, () -> n.emotion.deriveTask.getValue().longValue())
//                ).relax(0.1f))
//                .in(new FloatNormalized(
//
//                                new FloatFirstOrderDifference(n::time, () -> n.emotion.premiseFire.getValue().longValue())
//                        ).relax(0.1f)
//                ).in(new FloatNormalized(
//                                n.emotion.busyVol::getSum
//                        ).relax(0.1f)
//                );
//
//
//        for (MetaGoal g : values()) {
//            final int gg = g.ordinal();
//            float min = -2;
//            float max = +2;
//            b.in(new FloatPolarNormalized(() -> ne.want[gg], max));
//
//            float step = 0.5f;
//
//            b.out(2, (w) -> {
//                float str = 0.05f + step * Math.abs(ne.want[gg] / 4f);
//                switch (w) {
//                    case 0:
//                        ne.want[gg] = Math.min(max, ne.want[gg] + str);
//                        break;
//                    case 1:
//                        ne.want[gg] = Math.max(min, ne.want[gg] - str);
//                        break;
//                }
//            });
//        }
//
//
//
//
//
//
//
//
//
//
//
//
//        return b;
//    }

}
