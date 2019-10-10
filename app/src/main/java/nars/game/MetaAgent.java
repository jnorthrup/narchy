package nars.game;

import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.learn.ql.dqn3.DQN3;
import jcog.math.FloatAveragedWindow;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import jcog.thing.Part;
import jcog.util.FloatConsumer;
import nars.$;
import nars.Emotion;
import nars.NAR;
import nars.attention.PriAmp;
import nars.attention.PriNode;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.MetaGoal;
import nars.game.action.GoalActionConcept;
import nars.game.sensor.AbstractSensor;
import nars.game.sensor.GameLoop;
import nars.game.sensor.VectorSensor;
import nars.game.util.RLBooster;
import nars.task.util.PriBuffer;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.ScheduledTask;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

import java.util.Map;

import static nars.$.$$;

/**
 * supraself agent metavisor
 */
abstract public class MetaAgent extends Game {

	private static final float PRI_ACTION_RESOLUTION = 0.05f;

	//private static final Logger logger = Log.logger(MetaAgent.class);

	static final Atomic CURIOSITY =

		/** curiosity rate */
		Atomic.the("curi"),

	/**
	 * tasklink forget factor
	 */
	forget = Atomic.the("forget"),
		grow = Atomic.the("grow"),


	/**
	 * internal truth frequency precision
	 */
	precise = Atomic.the("precise"), //frequency resolution
	careful = Atomic.the("careful"), //conf resolution

	belief = Atomic.the("belief"),
		goal = Atomic.the("goal"),

	conf = Atomic.the("conf"),
		pri = Atomic.the("pri"),

	play = Atomic.the("play"),
		input = Atomic.the("input"),
		duration = Atomic.the("dur"),
		happy = Atomic.the("happy"),
		dex = Atomic.the("dex");


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
		RLBooster metaBoost = new RLBooster(this, (i, o) ->
			//new HaiQae(i, 12, o).alpha(0.01f).gamma(0.9f).lambda(0.9f),
			new DQN3(i, o, Map.of(
			)),
			4, 5, true);
//        window(grid(NARui.rlbooster(metaBoost), 800, 800);
		return this;
	}

	void actionCtlPriNodeRecursive(PriNode s, MapNodeGraph<PriNode, Object> g) {
		if (s instanceof PriAmp)
			priAction((PriAmp) s);
		s.node(g).nodes(false, true).forEach((Node<PriNode, Object> x) -> actionCtlPriNodeRecursive(x.id(), g));
	}

	void priAction(PriNode.Source a) {
		priAction($.inh(a.id,pri), a);
	}
	void priAction(PriAmp a) {
		floatAction($.inh(a.id,pri), a.amp).resolution(PRI_ACTION_RESOLUTION);
	}
	void priAction(Term id, PriNode.Source a) {
		floatAction(id, a.amp).resolution(PRI_ACTION_RESOLUTION);
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
		return floatAction(t, r.min, r.max, r::set);
	}

	protected GoalActionConcept floatAction(Term t, float min, float max, FloatConsumer r) {
		//FloatAveraged f = new FloatAveraged(/*0.75*/ 1);
		//FloatToFloatFunction f = (z)->z;
		return actionUnipolar(t, true, (v) -> v, (x) -> {
			//float y = f.valueOf(x);
			if (x == x)
				r.accept(Util.lerp(x, min, max));
			return x;
		});
		//.resolution(0.1f);
	}

//	private float dur(int initialDur, float d) {
//		return Math.max(1, ((d + 0.5f) * 2 * initialDur));
//	}

	/**
	 * core metavisor
	 */
	public static class SelfMetaAgent extends MetaAgent {

		public SelfMetaAgent(NAR nar) {
			super($.inh(nar.self(), $$("meta")), GameTime.durs(1), nar);
			NAR n = nar;

			Term SELF = n.self();


			Emotion e = n.emotion;

			sense($.inh(SELF, $$("busy")),
				new FloatNormalized(FloatAveragedWindow.get(8, 0.5f, e.busyVol::asFloat), 0, 1));
			sense($.inh(SELF, $$("deriveTask")),
				new FloatNormalized(FloatAveragedWindow.get(8, 0.5f, difference(e.deriveTask::floatValue)), 0, 1));
			sense($.inh(SELF, $$("lag")),
				new FloatNormalized(FloatAveragedWindow.get(8, 0.5f, difference(e.durLoopLag::floatValue)), 0, 1));

			for (MetaGoal mg : MetaGoal.values()) {
                GoalActionConcept a = actionUnipolar($.inh(SELF, $.the(mg.name())), (x) -> {
                    nar.emotion.want(mg, Util.lerp(x,
                        0,//-1,
                        +1));
                });
                a.resolution(0.1f);
			}

//        float maxPri = Math.max(n.beliefPriDefault.amp.floatValue(), n.goalPriDefault.amp.floatValue());
//        float dynamic = 10; //ratio max to min pri
//        actionCtl($.inh(SELF, beliefPri), n.beliefPriDefault.amp.subRange(maxPri/dynamic, maxPri));
//        actionCtl($.inh(SELF, goalPri), n.goalPriDefault.amp.subRange(maxPri/dynamic, maxPri));

			actionUnipolar($.inh(SELF, precise), (x) -> {
			    float y;
				if (x >= 0.75f) {
					x = 0.01f;
					y = (1f+0.75f)/2;
				} else if (x >= 0.5f) {
					x = 0.05f;
					y = (0.75f+0.5f)/2;
				} else if (x >= 0.25f) {
					x = 0.1f;
					y = (0.5f+0.25f)/2;
				} else {
					x = 0.2f;
					y = 0.25f/2;
				}
				nar.freqResolution.set(x);
				return y;
			});
//			actionUnipolar($.inh(SELF, careful), (x) -> {
//				float y;
//				if (x >= 0.75f) {
//					x = 0.01f;
//					y = (1f+0.75f)/2;
//				} else if (x >= 0.5f) {
//					x = 0.02f;
//					y = (0.75f+0.5f)/2;
//				} else if (x >= 0.25f) {
//					x = 0.4f;
//					y = (0.5f+0.25f)/2;
//				} else {
//					x = 0.10f;
//					y = 0.25f/2;
//				}
//				nar.confResolution.set(x);
//				return y;
//			});

			//top-level priority controls of other NAR components
			nar.parts(Game.class).filter(g -> g!=this).forEach(g -> priAction(g.what().pri));

			//shouldnt be necessary, manipulate the downstream PriNodes instead and leave these constant
//			priAction($.inh(SELF, $.p(belief, pri)), nar.beliefPriDefault);
//			priAction($.inh(SELF, $.p(goal, pri)), nar.goalPriDefault);

			//actionCtl($.inh(SELF, $.p(belief,conf)), nar.beliefConfDefault);
			//actionCtl($.inh(SELF, $.p(goal,conf)), nar.goalConfDefault);

//                .subRange(
//                Math.max(nar.goalPriDefault.amp() /* current value */ * priFactorMin, ScalarValue.EPSILON),
//                nar.goalPriDefault.amp() /* current value */ * priFactorMax)::setProportionally);

			rewardNormalized(happy, 1, 0, ScalarValue.EPSILON, () -> {
				float dur = dur();
				return (float) nar.parts(Game.class)
					.filter(g -> g != SelfMetaAgent.this)
					.filter(Part::isOn)
					.mapToDouble(g -> g.happiness(dur))
					.average()
					.orElseGet(() -> 0);
			});

//        ThreadCPUTimeTracker.getCPUTime()
//        reward("lazy", 1, ()->{
//            return 1-nar.loop.throttle.floatValue();
//        });
		}
	}

//    public GoalActionConcept[] dial(Game a, Atomic label, FloatRange var, int steps) {
//        GoalActionConcept[] priAction = actionDial(
//                $.inh(id, $.p(label, $.the(-1))),
//                $.inh(id, $.p(label, $.the(+1))),
//                var,
//                steps);
//        return priAction;
//    }

	public static class GameMetaAgent extends MetaAgent {

		/**
		 * in case it forgets to unpause
		 */
		private final long autoResumePeriod = 256;
		private final boolean allowPause;

		public GameMetaAgent(Game g, boolean allowPause) {
			super($.inh(g.what().id, $$("meta")), g.time.chain(), g.nar);

			this.allowPause = allowPause;

			What w = g.what();

			Term gid = w.id; //$.p(w.nar.self(), w.id);
			//this.what().accept(new EternalTask($.inh(aid,this.id), BELIEF, $.t(1f, 0.9f), nar));


			//actionCtlPriNodeRecursive(g.sensorPri, g.nar.control.graph);
			priAction(g.sensorPri);
			priAction(g.rewardPri);
			priAction(g.actionPri);


			floatAction($.inh(gid, forget), ((TaskLinkWhat) w).links.decay);
			floatAction($.inh(gid, grow), ((TaskLinkWhat) w).links.grow);
			//actionCtl($.inh(gid, remember), ((TaskLinkWhat) w).links.sustain);

			//actionCtl($.inh(gid, amplify), ((TaskLinkWhat) w).links.amp);


//        float priMin = 0.1f, priMax = 1;
//        actionCtl($.inh(gid, PRI), w.priAsFloatRange());

//            float curiMin = 0.005f, curiMax = 0.05f;
//            actionCtl($.inh(gid, CURIOSITY), g.curiosity.rate.subRange(curiMin, curiMax));

			float initialDur = w.dur();
//			FloatRange durRange = new FloatRange(initialDur, Math.max(nar.dtDither(), initialDur / 4), initialDur * 16) {
//				@Override
//				public float get() {
//					super.set(((TaskLinkWhat) w).dur);
//					return super.get();
//				}
//
//				@Override
//				public void set(float value) {
//					super.set(value);
//					value = super.get();
//					float nextDur = Math.max(1, value);
//					//logger.info("{} dur={}" , w.id, nextDur);
//					((TaskLinkWhat) w).dur.set(nextDur);
//					//assert(nar.dur()==nextDur);
//				}
//			};


			actionUnipolar($.inh(gid, duration), (x) -> {
				float ditherDT = nar.dtDither();
				float nextDur =
					//(float) Math.pow(initialDur, Util.sqr((x + 0.5f)*2));
					//ditherDT + initialDur * (float) Math.pow(2, ((x - 0.5f)*2));
					(float) (ditherDT + Util.lerp(Math.pow(x,4), 0, initialDur*16));
				//System.out.println(x + " dur=" + nextDur);
				((TaskLinkWhat) w).dur.set(Math.max(1,nextDur));
			});

			if (w.in instanceof PriBuffer.BagTaskBuffer)
				floatAction($.inh(gid, input), ((PriBuffer.BagTaskBuffer) (w.in)).valve);


			Reward h = rewardNormalized($.inh(gid, happy), 1, 0, ScalarValue.EPSILON, () -> {
				//new FloatFirstOrderDifference(nar::time, (() -> {
				return g.isOn() ? (//(float)((0.01f + g.dexterity()) *
					g.happiness(dur() /* supervisory dur of the meta-agent */)) : Float.NaN;
			});


			Reward d = rewardNormalized($.inh(gid, dex), 1, 0, ScalarValue.EPSILON,
				() -> {
////            float p = a.proficiency();
////            float hp = Util.or(h, p);
//            //System.out.println(h + " " + p + " -> " + hp);
////            return hp;
					return g.isOn() ? (float) g.dexterity() : Float.NaN;
				});

			for (GameLoop s : g.sensors) {
				if (s instanceof VectorSensor) {
					Term t = $.inh(((AbstractSensor) s).id, pri);
					floatAction(t, ((VectorSensor) s).pri.amp);
				}
			}


			//TODO other Emotion sensors

//        Term agentPriTerm =
//                $.inh(a.id, PRI);
//                //$.inh(a.id, id /* self */);
//        GoalActionConcept agentPri = actionUnipolar(agentPriTerm, (FloatConsumer)a.attn.factor::set);
//
//


			if (allowPause) {
				float playThresh = 0.25f;
				Term play =
					$.inh(gid, MetaAgent.play);

				GoalActionConcept enableAction = actionPushButton(play, new BooleanProcedure() {

					private volatile int autoResumeID = 0;
					private volatile ScheduledTask autoResume;
					volatile private Runnable resume = null;

					@Override
					public void value(boolean e) {

						//enableAction = n.actionToggle($.func(enable, n.id), (e)->{
						//TODO integrate and threshold, pause for limited time
						synchronized (this) {
							if (e) {
								tryResume();
							} else {
								tryPause();
							}
						}

					}

					void tryPause() {

						if (resume == null) {

							resume = g.pause();
							NAR n = nar();

							int a = autoResumeID;
							autoResume = n.runAt(Math.round(n.time() + autoResumePeriod * n.dur()), () -> {
								if (autoResumeID == a)
									tryResume();
								//else this one has been cancelled
							});

						}
					}

					void tryResume() {

						if (resume != null) {
							autoResumeID++;
							resume.run();
							autoResume = null;
							resume = null;
						}

					}
				}, () -> playThresh);
			}


//        Reward enableReward = reward("enable", () -> enabled.getOpaque() ? +1 : 0f);

		}
	}

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
