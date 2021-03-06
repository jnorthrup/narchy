package nars.game;

import jcog.Util;
import jcog.func.IntIntToObjectFunction;
import jcog.learn.Agent;
import jcog.learn.ql.dqn3.DQN3;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.pri.ScalarValue;
import jcog.pri.UnitPri;
import jcog.thing.Part;
import jcog.util.FloatConsumer;
import nars.$;
import nars.Emotion;
import nars.NAR;
import nars.attention.PriAmp;
import nars.attention.PriSource;
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
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import static nars.$.*;

/**
 * supraself agent metavisor
 */
public abstract class MetaAgent extends Game {

	static final Atomic CURIOSITY =
		/** curiosity rate */
		Atomic.the("curi");

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
        RLBooster metaBoost = new RLBooster(true, this, 4, 5, new IntIntToObjectFunction<Agent>() {
            @Override
            public Agent apply(int i, int o) {
                return new DQN3(i, o, Map.of(
                ));
            }
        }
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
		priAction($.INSTANCE.inh(a.id, pri), a);
	}

	void priAction(PriAmp a) {
		floatAction($.INSTANCE.inh(a.id, pri), 2.0F, a.amp).resolution(PRI_ACTION_RESOLUTION);
	}

	void priAction(Term id, PriSource a) {
		floatAction(id, 2.0F, a.pri).resolution(PRI_ACTION_RESOLUTION);
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
		return floatAction(t, 1.0F, r);
	}

	protected GoalActionConcept floatAction(Term t, float exp, FloatRange r) {
		return floatAction(t, r.min, r.max, exp, r::set);
	}
	protected GoalActionConcept floatAction(Term t, float exp, UnitPri r) {
		return floatAction(t, (float) 0, 1.0F, exp, r::pri);
	}

	protected GoalActionConcept floatAction(Term t, float min, float max, float exp, FloatConsumer r) {
		//FloatAveraged f = new FloatAveraged(/*0.75*/ 1);
		//FloatToFloatFunction f = (z)->z;
		return actionUnipolar(t, true, new FloatToFloatFunction() {
            @Override
            public float valueOf(float v) {
                return v;
            }
        }, new FloatToFloatFunction() {
            @Override
            public float valueOf(float x) {
                //float y = f.valueOf(x);
                if (x == x)
                    r.accept(Util.lerp((float) Math.pow((double) x, (double) exp), min, max));
                return x;
            }
        });
	}

//	private float dur(int initialDur, float d) {
//		return Math.max(1, ((d + 0.5f) * 2 * initialDur));
//	}

	/**
	 * core metavisor
	 */
	public static class SelfMetaAgent extends MetaAgent {

		public SelfMetaAgent(NAR nar) {
			this(nar, 2.0F /* nyquist sampling rate - to ensure activity is accurately sampled in the feedback stats */);
		}

		public SelfMetaAgent(NAR nar, float durs) {
			super($.INSTANCE.inh(nar.self(), INSTANCE.$$("meta")), GameTime.durs(durs), nar);
            NAR n = nar;

            Term SELF = n.self();


            Emotion e = n.emotion;

			sense($.INSTANCE.inh(SELF, INSTANCE.$$("busy")), new FloatNormalized(e.busyVol));
			sense($.INSTANCE.inh(SELF, INSTANCE.$$("premiseRun")), new FloatNormalized(e.premiseRun));
			sense($.INSTANCE.inh(SELF, INSTANCE.$$("deriveTask")), new FloatNormalized(e.deriveTask));
			sense($.INSTANCE.inh(SELF, INSTANCE.$$("lag")), new FloatNormalized(e.durLoopLag));

			for (MetaGoal mg : MetaGoal.values()) {
                GoalActionConcept a = actionUnipolar($.INSTANCE.inh(SELF, $.INSTANCE.the(mg.name())), new FloatConsumer() {
                    @Override
                    public void accept(float x) {
                        nar.emotion.want(mg,
                                x >= 0.5f ?
                                        (float) Util.lerp(Math.pow((double) ((x - 0.5f) * 2.0F), 1.0 /* 2 */), (double) 0, (double) +1) //positive (0.5..1)
                                        :
                                        Util.lerp((x) * 2.0F, -0.02f, (float) 0) //negative (0..0.5): weaker
                        );
                    }
                });
//                a.resolution(0.1f);
			}

//        float maxPri = Math.max(n.beliefPriDefault.amp.floatValue(), n.goalPriDefault.amp.floatValue());
//        float dynamic = 10; //ratio max to min pri
//        actionCtl($.inh(SELF, beliefPri), n.beliefPriDefault.amp.subRange(maxPri/dynamic, maxPri));
//        actionCtl($.inh(SELF, goalPri), n.goalPriDefault.amp.subRange(maxPri/dynamic, maxPri));

			actionUnipolar($.INSTANCE.inh(SELF, precise), new FloatToFloatFunction() {
                @Override
                public float valueOf(float x) {
                    float x1 = x;
                    float y;
                    if (x1 >= 0.75f) {
                        x1 = 0.01f;
                        y = 1.0F;
                    } else if (x1 >= 0.5f) {
                        x1 = 0.05f;
                        y = 0.66f;
                    } else if (x1 >= 0.25f) {
                        x1 = 0.10f;
                        y = 0.33f;
                    } else {
                        x1 = 0.20f;
                        y = (float) 0;
                    }
                    nar.freqResolution.set(x1);
                    return y;
                }
            });

//			actionUnipolar($.inh(SELF, careful), (x) -> {
//				float x1 = x;
//				float y;
//				if (x1 >= 0.75f) {
//					x1 = 0.01f;
//					y = 1f;
//				} else if (x1 >= 0.5f) {
//					x1 = 0.02f;
//					y = 0.66f;
//				} else if (x1 >= 0.25f) {
//					x1 = 0.03f;
//					y = 0.33f;
//				} else {
//					x1 = 0.04f;
//					y = 0;
//				}
//				nar.confResolution.set(x1);
//				return y;
//			});

//			//potentially dangerous, may become unable to convince itself to un-ignore things
//			actionUnipolar($.inh(SELF, ignore), (x) -> {
//				float y;
//				if (x >= 0.75f) {
//					x = 0.01f;
//					y = (1f+0.75f)/2;
//				} else if (x >= 0.5f) {
//					x = 0.005f;
//					y = (0.75f+0.5f)/2;
//				} else if (x >= 0.25f) {
//					x = 0.001f;
//					y = (0.5f+0.25f)/2;
//				} else {
//					x = 0.0f;
//					y = 0.25f/2;
//				}
//				nar.confResolution.set(x);
//				return y;
//			});

			//top-level priority controls of other NAR components
			nar.parts(Game.class).filter(new Predicate<Game>() {
                @Override
                public boolean test(Game g) {
                    return g != SelfMetaAgent.this;
                }
            }).forEach(new Consumer<Game>() {
                @Override
                public void accept(Game g) {
                    priAction(g.what().pri);
                }
            });

			//shouldnt be necessary, manipulate the downstream PriNodes instead and leave these constant
//			priAction($.inh(SELF, $.p(belief, pri)), nar.beliefPriDefault);
//			priAction($.inh(SELF, $.p(goal, pri)), nar.goalPriDefault);

			//actionCtl($.inh(SELF, $.p(belief,conf)), nar.beliefConfDefault);
			//actionCtl($.inh(SELF, $.p(goal,conf)), nar.goalConfDefault);

//                .subRange(
//                Math.max(nar.goalPriDefault.amp() /* current value */ * priFactorMin, ScalarValue.EPSILON),
//                nar.goalPriDefault.amp() /* current value */ * priFactorMax)::setProportionally);

			float durMeasured = (float) 0;

			reward($.INSTANCE.inh(SELF, $.INSTANCE.p(happy, now)), new FloatSupplier() {
                @Override
                public float asFloat() {
                    return SelfMetaAgent.this.happiness(nowPercept.start, nowPercept.end, durMeasured, nar);
                }
            });

			/** past happiness ~= gradient momentum / echo effect */
			float emotionalMomentumDurs = 4.0F;
			reward($.INSTANCE.inh(SELF, $.INSTANCE.p(happy, past)), new FloatSupplier() {
                @Override
                public float asFloat() {
                    return SelfMetaAgent.this.happiness((long) Math.round((float) nowPercept.start - SelfMetaAgent.this.dur() * emotionalMomentumDurs), nowPercept.start, durMeasured, nar);
                }
            });

			/** optimism */
			reward($.INSTANCE.inh(SELF, $.INSTANCE.p(happy, future)), new FloatSupplier() {
                @Override
                public float asFloat() {
                    return SelfMetaAgent.this.happiness(nowPercept.end, (long) Math.round((float) nowPercept.end + SelfMetaAgent.this.dur() * emotionalMomentumDurs), durMeasured, nar);
                }
            });

//        ThreadCPUTimeTracker.getCPUTime()
//        reward("lazy", 1, ()->{
//            return 1-nar.loop.throttle.floatValue();
//        });
		}

		float happiness(long start, long end, float dur, NAR nar) {
			return (float) nar.parts(Game.class)
				.filter(new Predicate<Game>() {
                    @Override
                    public boolean test(Game g) {
                        return g != SelfMetaAgent.this;
                    }
                })
				.filter(Part::isOn)
				.mapToDouble(new ToDoubleFunction<Game>() {
                    @Override
                    public double applyAsDouble(Game g) {
                        return g.happiness(start, end, dur);
                    }
                })
				.average()
				.orElseGet(new DoubleSupplier() {
                    @Override
                    public double getAsDouble() {
                        return (double) 0;
                    }
                });
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
		private static final long autoResumePeriod = 256L;

		public GameMetaAgent(Game g, boolean allowPause) {
			super($.INSTANCE.inh(g.what().id, INSTANCE.$$("meta")), g.time.chain(2.0F /* nyquist */), g.nar);

            What w = g.what();

            Term gid = w.id; //$.p(w.nar.self(), w.id);
			//this.what().accept(new EternalTask($.inh(aid,this.id), BELIEF, $.t(1f, 0.9f), nar));


			//actionCtlPriNodeRecursive(g.sensorPri, g.nar.control.graph);
			priAction(g.sensorPri);
			priAction(g.rewardPri);
			priAction(g.actionPri);


			floatAction($.INSTANCE.inh(gid, forget), ((TaskLinkWhat) w).links.decay);
			floatAction($.INSTANCE.inh(gid, grow), ((TaskLinkWhat) w).links.grow);
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


			actionUnipolar($.INSTANCE.inh(gid, duration), new FloatConsumer() {
                @Override
                public void accept(float x) {
                    float ditherDT = (float) nar.dtDither();
                    float nextDur =
                            //(float) Math.pow(initialDur, Util.sqr((x + 0.5f)*2));
                            //ditherDT + initialDur * (float) Math.pow(2, ((x - 0.5f)*2));
                            (float) ((double) ditherDT + Util.lerp(Math.pow((double) x, 4.0), (double) 0, (double) (initialDur * 16.0F)));
                    //System.out.println(x + " dur=" + nextDur);
                    ((TaskLinkWhat) w).dur.set(Math.max(1.0F, nextDur));
                }
            });

			if (w.inBuffer instanceof PriBuffer.BagTaskBuffer)
				floatAction($.INSTANCE.inh(gid, input), ((PriBuffer.BagTaskBuffer) (w.inBuffer)).valve);


			reward($.INSTANCE.inh(gid, happy), new FloatSupplier() {
                @Override
                public float asFloat() {
                    return g.isOn() ? (float) g.happiness(nowPercept.start, nowPercept.end, (float) 0) : Float.NaN;
                }
            });


			rewardNormalized($.INSTANCE.inh(gid, dex), 1.0F, (float) 0, ScalarValue.Companion.getEPSILON(),
                    new FloatSupplier() {
                        @Override
                        public float asFloat() {
////            float p = a.proficiency();
////            float hp = Util.or(h, p);
//            //System.out.println(h + " " + p + " -> " + hp);
////            return hp;
                            return g.isOn() ? (float) g.dexterity() : Float.NaN;
                        }
                    });

			for (GameLoop s : g.sensors) {
				if (s instanceof VectorSensor)
					floatAction($.INSTANCE.inh(((AbstractSensor) s).id, pri), ((VectorSensor) s).pri.amp);
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
				actionPushButton($.INSTANCE.inh(gid, MetaAgent.play), new BooleanProcedure() {

					private volatile int autoResumeID = 0;
					private volatile ScheduledTask autoResume;
					private volatile Runnable resume = null;

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
							autoResume = n.runAt((long) Math.round((float) n.time() + (float) autoResumePeriod * n.dur()), new Runnable() {
                                @Override
                                public void run() {
                                    if (autoResumeID == a)
                                        tryResume();
                                    //else this one has been cancelled
                                }
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
				}, new FloatSupplier() {
                    @Override
                    public float asFloat() {
                        return playThresh;
                    }
                });
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
