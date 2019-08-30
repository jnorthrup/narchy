package nars.agent;

import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.learn.ql.dqn3.DQN3;
import jcog.math.FloatAveragedWindow;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import jcog.util.FloatConsumer;
import nars.$;
import nars.NAR;
import nars.agent.util.RLBooster;
import nars.attention.PriNode;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.concept.action.GoalActionConcept;
import nars.control.MetaGoal;
import nars.task.util.PriBuffer;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.ScheduledTask;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

import java.util.Map;

import static nars.$.$$;

/**
 * supraself agent metavisor
 */
abstract public class MetaAgent extends Game {

    //private static final Logger logger = Log.logger(MetaAgent.class);

    static final Atomic CURIOSITY =

            /** curiosity rate */
            Atomic.the("curi"),

            /** tasklink forget factor */
            forget = Atomic.the("forget"),
            grow = Atomic.the("forget"),
            remember = Atomic.the("remember"),

            /** tasklink activation factor */
            amplify = Atomic.the("amplify"),

            /** internal truth frequency precision */
            exact = Atomic.the("exact"),

            PRI = Atomic.the("pri"),
            beliefPri = Atomic.the("beliefPri"),
            goalPri = Atomic.the("goalPri"),

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


    protected MetaAgent(Term id, float fps, NAR nar) {
        super(id, GameTime.fps(fps), nar);
        this.nar = nar;
    }

    public MetaAgent addRLBoost() {
//        meta.what().pri(0.05f);
        RLBooster metaBoost = new RLBooster(this, (i, o) ->
                //new HaiQae(i, 12, o).alpha(0.01f).gamma(0.9f).lambda(0.9f),
                new DQN3(i, o, Map.of(
                )),
                4, 5, true);
        curiosity.rate.set(0);
//        window(grid(NARui.rlbooster(metaBoost), 800, 800);
        return this;
    }

    /** core metavisor */
    public static class SelfMetaAgent extends MetaAgent {

        public SelfMetaAgent(NAR nar, float fps) {
            super($.inh(nar.self(), $$("meta")),  fps, nar);
            NAR n = this.nar = nar;

            Term SELF = n.self();



            sense($.inh(SELF, $$("busy")),
                new FloatNormalized(FloatAveragedWindow.get(8, 0.5f, n.emotion.busyVol::asFloat), 0, 1));
            sense($.inh(SELF, $$("deriveTask")),
                new FloatNormalized(FloatAveragedWindow.get(8, 0.5f, difference(n.emotion.deriveTask::floatValue)), 0, 1));
            sense($.inh(SELF, $$("lag")),
                new FloatNormalized(FloatAveragedWindow.get(8, 0.5f, difference(n.emotion.durLoopLag::floatValue)), 0, 1));

        for (MetaGoal mg : MetaGoal.values()) {
            actionUnipolar($.inh(SELF, $.the(mg.name())), (x)-> {
                nar.emotion.want(mg, Util.lerpLong(x, -1, +1));
            });
        }

//        float maxPri = Math.max(n.beliefPriDefault.amp.floatValue(), n.goalPriDefault.amp.floatValue());
//        float dynamic = 10; //ratio max to min pri
//        actionCtl($.inh(SELF, beliefPri), n.beliefPriDefault.amp.subRange(maxPri/dynamic, maxPri));
//        actionCtl($.inh(SELF, goalPri), n.goalPriDefault.amp.subRange(maxPri/dynamic, maxPri));

            actionUnipolar($.inh(SELF, exact), (value) -> {
                    if (value < 0.5f) {
                        value = 0.01f;
                    } else if (value < 0.75f) {
                        value = 0.05f;
                    } else {
                        value = 0.1f;
                    }
                nar.freqResolution.set(value);
//                switch (Util.clamp((int) Math.floor(value * 4),0,3)) {
////                    case 0: value = 0.5f; break; //binary emulation
////                    case 1: value = 0.25f; break;
//                    case 0: value = 0.1f; break;
//                    case 1: value = 0.05f; break;
//                    case 2: value = 0.01f; break;
//                    default:
//                        throw new UnsupportedOperationException();
//                }

            });

            float priFactorMin = 0.1f, priFactorMax = 4f;
            actionUnipolar($.inh(SELF, beliefPri), nar.beliefPriDefault.amp.subRange(
                Math.max(nar.beliefPriDefault.amp() /* current value */ * priFactorMin, ScalarValue.EPSILON),
                nar.beliefPriDefault.amp() /* current value */ * priFactorMax)::setProportionally);
            actionUnipolar($.inh(SELF, goalPri), nar.goalPriDefault.amp.subRange(
                Math.max(nar.goalPriDefault.amp() /* current value */ * priFactorMin, ScalarValue.EPSILON),
                nar.goalPriDefault.amp() /* current value */ * priFactorMax)::setProportionally);

            reward("happy", ()->{
                float dur = dur();
                return (float)nar.parts(Game.class)
                    .filter(g -> g!=SelfMetaAgent.this)
                    .mapToDouble(g -> g.happiness(dur))
                    .average()
                    .getAsDouble();
            });

//        ThreadCPUTimeTracker.getCPUTime()
//        reward("lazy", 1, ()->{
//            return 1-nar.loop.throttle.floatValue();
//        });
        }
    }

    public static class GameMetaAgent extends MetaAgent {

        /** in case it forgets to unpause */
        private final long autoResumePeriod = 256;
        private final boolean allowPause;

        public GameMetaAgent(Game g, float fps, boolean allowPause) {
            super($.inh(g.what().id, $$("meta")), fps, g.nar);

            this.allowPause = allowPause;

            What w = g.what();

            Term gid = w.id; //$.p(w.nar.self(), w.id);
            //this.what().accept(new EternalTask($.inh(aid,this.id), BELIEF, $.t(1f, 0.9f), nar));


            actionCtlPriNodeRecursive(g.sensorPri, g.nar.control.graph);
            actionCtlPriNode(g.actionPri); //non-recursive for now


            actionCtl($.inh(gid, forget), ((TaskLinkWhat) w).links.decay);
            actionCtl($.inh(gid, grow), ((TaskLinkWhat) w).links.grow);
            actionCtl($.inh(gid, remember), ((TaskLinkWhat) w).links.sustain);

            //actionCtl($.inh(gid, amplify), ((TaskLinkWhat) w).links.amp);






//        float priMin = 0.1f, priMax = 1;
//        actionCtl($.inh(gid, PRI), w.priAsFloatRange());

            float curiMin = 0.005f, curiMax = 0.05f;
            actionCtl($.inh(gid, CURIOSITY), g.curiosity.rate.subRange(curiMin, curiMax));

            float initialDur = w.dur();
            FloatRange durRange = new FloatRange(initialDur, initialDur / 4, initialDur * 4) {
                @Override
                public float get() {
                    super.set(((TaskLinkWhat) w).dur);
                    return super.get();
                }

                @Override
                public void set(float value) {
                    super.set(value);
                    value = super.get();
                    float nextDur = Math.max(1, value);
                    //logger.info("{} dur={}" , w.id, nextDur);
                    ((TaskLinkWhat) w).dur.set(nextDur);
                    //assert(nar.dur()==nextDur);
                }
            };
            actionCtl($.inh(gid, duration), durRange);

            if (w.in instanceof PriBuffer.BagTaskBuffer)
                actionCtl($.inh(gid, input), ((PriBuffer.BagTaskBuffer) (w.in)).valve);



            Reward h = rewardNormalized($.inh(gid, happy),  0, ScalarValue.EPSILON, () -> {
                //new FloatFirstOrderDifference(nar::time, (() -> {
                return g.isOn() ? (float)((0.01f + g.dexterity()) *
                    Math.max(0,g.happiness(dur() /* supervisory dur of the meta-agent */)-0.5f)) : Float.NaN;
            });


            Reward d = rewardNormalized($.inh(gid, dex),0, nar.confMin.floatValue()/2,
                () -> {
////            float p = a.proficiency();
////            float hp = Util.or(h, p);
//            //System.out.println(h + " " + p + " -> " + hp);
////            return hp;
                    return g.isOn() ? (float) g.dexterity() : Float.NaN;
                });


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

    protected void actionCtlPriNodeRecursive(PriNode s, MapNodeGraph<PriNode,Object> g) {
        actionCtlPriNode(s);
        s.neighbors(g, false,true).forEach((Node<PriNode,Object> x) -> {
            PriNode xid = x.id();
            actionCtlPriNode(xid);//, $.p(base, s.get()));
        });
    }

    protected void actionCtlPriNode(PriNode attnSensor) {
        actionCtl(attnSensor.get(), attnSensor.amp);
    }

    protected void actionCtl(Term t, FloatRange r) {
        actionCtl(t, r.min, r.max, r::set);
    }
    protected void actionCtl(Term t, float min, float max, FloatConsumer r) {
        //FloatAveraged f = new FloatAveraged(/*0.75*/ 1);
        FloatToFloatFunction f = (z)->z;
        actionUnipolar(t, true, (v)->v, (x)->{
            float y = f.valueOf(x);
            r.accept(Util.lerp(y, min, max));
            return y;
        });
        //.resolution(0.1f);
    }

//    public GoalActionConcept[] dial(Game a, Atomic label, FloatRange var, int steps) {
//        GoalActionConcept[] priAction = actionDial(
//                $.inh(id, $.p(label, $.the(-1))),
//                $.inh(id, $.p(label, $.the(+1))),
//                var,
//                steps);
//        return priAction;
//    }

    private float dur(int initialDur, float d) {
        return Math.max(1, ((d + 0.5f) * 2 * initialDur));
    }

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
////        for (Sensor s : sensors) {
////            if (!(s instanceof Signal)) { //HACK only if compound sensor
////                Term target = s.target();
////
////                //HACK
////                if (s instanceof DigitizedScalar)
////                    target = $.quote(target.toString()); //throw new RuntimeException("overly complex sensor target");
////
////                //HACK TODO divide by # of contained concepts, reported by Sensor interface
////                float maxPri;
////                if (s instanceof Bitmap2DSensor) {
////                    maxPri = 8f / (float) (Math.sqrt(((Bitmap2DSensor) s).concepts.area));
////                } else {
////                    maxPri = 1;
////                }
////
////                m.actionUnipolar($.func("aware", target), (p) -> {
////                    FloatRange pp = s.pri();
////                    pp.setAt(lerp(p, 0f, maxPri * nar.priDefault(BELIEF)));
////                });
////
////            }
////        }
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

}
