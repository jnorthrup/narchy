package nars.agent;

import jcog.TODO;
import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.attention.PriNode;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.concept.action.GoalActionConcept;
import nars.task.util.PriBuffer;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.ScheduledTask;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

import static nars.$.$$;
import static nars.Op.SETe;

/**
 * supraself agent metavisor
 */
public class MetaAgent extends Game {

    static final Atomic CURIOSITY =

            /** curiosity rate */
            Atomic.the("curi"),

            /** tasklink forget factor */
            forget = Atomic.the("forget"),

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

    /** in case it forgets to unpause */
    private final long autoResumePeriod = 256;
    private final boolean allowPause;


    public MetaAgent(NAR n) {
        super(env(n.self()), GameTime.durs(1), n);
        throw new TODO();

//        n.parts(Game.class).forEach(a -> {
//            if(MetaAgent.this!=a)
//                add(a, false);
//        });
    }

    /**
     * assumes games are from the same NAR
     */
    public MetaAgent(boolean allowPause, float fps, Game... w) {
        super($.func(Atomic.the("meta"),
                SETe.the(Util.map(p -> p.what().term(), new Term[w.length], w))),
                GameTime.fps(fps), w[0].nar);

        this.allowPause = allowPause;

        NAR n = this.nar = w[0].nar;

        Term SELF = n.self();
        senseNumberDifference($.inh(SELF, $$("busy")), n.emotion.busyVol::asFloat);
        senseNumberDifference($.inh(SELF, $$("deriveTask")), n.emotion.deriveTask::get);

//        float maxPri = Math.max(n.beliefPriDefault.amp.floatValue(), n.goalPriDefault.amp.floatValue());
//        float dynamic = 10; //ratio max to min pri
//        actionCtl($.inh(SELF, beliefPri), n.beliefPriDefault.amp.subRange(maxPri/dynamic, maxPri));
//        actionCtl($.inh(SELF, goalPri), n.goalPriDefault.amp.subRange(maxPri/dynamic, maxPri));

//        actionCtl($.inh(SELF, exact), new FloatRange(0.5f, 0, 1) {
//            @Override
//            public void set(float value) {
//                switch (Math.round(value * 6)) {
//                    case 0: value = 0.25f; break;
//                    case 1: value = 0.2f; break;
//                    case 2: value = 0.1f; break;
//                    case 3: value = 0.05f; break;
//                    case 4: value = 0.025f; break;
//                    case 5: value = 0.01f; break;
//                }
//                nar.freqResolution.set(value);
//                super.set(value);
//            }
//        });

        for (Game ww : w)
            add(ww);
    }

    @Override
    protected void sense() {
        ((TaskLinkWhat)what()).dur.set(durPhysical()*2 /* nyquist */);
        super.sense();
    }

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

    private void add(Game g) {

        What w = g.what();

        Term gid = w.id;
        //this.what().accept(new EternalTask($.inh(aid,this.id), BELIEF, $.t(1f, 0.9f), nar));


        actionCtlPriNodeRecursive(g.attnSensor, g.nar.control.graph);
        actionCtlPriNode(g.attnAction); //non-recursive for now


        actionCtl($.inh(gid, forget), ((TaskLinkWhat) w).links.decay);

        //actionCtl($.inh(gid, amplify), ((TaskLinkWhat) w).links.amp);


//        float priFactorMin = 0.1f, priFactorMax = 4f;
//        beliefPriAction = actionUnipolar($.inh(id, beliefPri), n.beliefPriDefault.subRange(
//                Math.max(n.beliefPriDefault.floatValue() /* current value */ * priFactorMin, ScalarValue.EPSILON),
//                n.beliefPriDefault.floatValue() /* current value */ * priFactorMax)::setProportionally);
//        goalPriAction = actionUnipolar($.inh(id, goalPri), n.goalPriDefault.subRange(
//                Math.max(n.goalPriDefault.floatValue() /* current value */ * priFactorMin, ScalarValue.EPSILON),
//                n.goalPriDefault.floatValue() /* current value */ * priFactorMax)::setProportionally);


//        actionHemipolar($.inh(aid, PRI), (v)->{
//            nar.what.pri(g.what(), v);
//            return v;
//        });

        float priMin = 0.1f, priMax = 1;
        actionCtl($.inh(gid, PRI), w.priAsFloatRange());

        float curiMin = 0.025f, curiMax = 0.09f;
        actionCtl($.inh(gid, CURIOSITY), g.curiosity.rate.subRange(curiMin, curiMax));

        int initialDur = w.dur();
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
                int nextDur = Math.max(1, Math.round(value));
                ((TaskLinkWhat) w).dur.set(nextDur);
                //assert(nar.dur()==nextDur);
            }
        };
        actionCtl($.inh(gid, duration), durRange);

        if (w.in instanceof PriBuffer.BagTaskBuffer)
            actionCtl($.inh(gid, input), ((PriBuffer.BagTaskBuffer) (w.in)).valve.subRange(0.1f, 1f));


//        this.dur = actionUnipolar($.inh(id, duration), (x) -> {
//            n.time.dur(Util.lerp(x * x, n.dtDither(), initialDur * 2));
//            return x;
//        });

        Reward h = rewardNormalized($.inh(gid, happy),  0, Float.MIN_NORMAL, () -> {
            //new FloatFirstOrderDifference(nar::time, (() -> {
            return g.isOn() ? g.happiness(dur() /* supervisory dur of the meta-agent */) : Float.NaN;
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
                    //$.inh(gid, MetaAgent.play);
                    gid; //for efficiency

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
                        autoResume = n.runAt(n.time() + autoResumePeriod * n.dur(), () -> {
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

    private void actionCtlPriNodeRecursive(PriNode s, MapNodeGraph<PriNode,Object> g) {
        actionCtlPriNode(s);
        s.neighbors(g, false,true).forEach((Node<PriNode,Object> x) -> {
            PriNode xid = x.id();
            actionCtlPriNode(xid);//, $.p(base, s.get()));
        });
    }

    private void actionCtlPriNode(PriNode attnSensor) {
        actionCtl(attnSensor.get(), attnSensor.amp);
    }

    void actionCtl(Term t, FloatRange r) {
        //FloatAveraged f = new FloatAveraged(/*0.75*/ 1);
        FloatToFloatFunction f = (z)->z;
        actionUnipolar(t, true, (v)->v, (x)->{
            float y = f.valueOf(x);
            r.set(Util.lerp(y, r.min, r.max));
            return y;
        }).resolution(0.1f);
    }

//    public GoalActionConcept[] dial(Game a, Atomic label, FloatRange var, int steps) {
//        GoalActionConcept[] priAction = actionDial(
//                $.inh(id, $.p(label, $.the(-1))),
//                $.inh(id, $.p(label, $.the(+1))),
//                var,
//                steps);
//        return priAction;
//    }

    private int dur(int initialDur, float d) {
        return Math.max(1, Math.round((d + 0.5f) * 2 * initialDur));
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
