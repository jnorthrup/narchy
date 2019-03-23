package nars.agent;

import jcog.Util;
import jcog.math.FloatFirstOrderDifference;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.concept.action.GoalActionConcept;
import nars.term.atom.Atomic;

import static nars.$.$$;

/**
 * supraself agent metavisor
 */
public class MetaAgent extends NAgent {

    static final Atomic curiosity = Atomic.the("curi"),
            forget = Atomic.the("forget"),
            PRI = Atomic.the("pri"),
            beliefPri = Atomic.the("beliefPri"),
            goalPri = Atomic.the("goalPri"),

            enable = Atomic.the("enable"),
            duration = Atomic.the("dur"),
            happy = Atomic.the("happy")
            ;


//    public final GoalActionConcept forgetAction;
//    public final GoalActionConcept beliefPriAction;
//    private final GoalActionConcept goalPriAction;
//    private final GoalActionConcept dur;

    private int disableCountDown = 0;
    private final int disableThreshold = 1;
    private final long disablePeriod = 4;


    static int curiStartupDurs = 5000;
    static float curiMax = 0.2f;
    static float curiMinOld = 0.01f, curiMinYoung = 0.04f;



    public MetaAgent(NAR n, float fps) {
        super(n.self() /* HACK */,FrameTrigger.fps(fps),  n);

//        forgetAction = actionUnipolar($.inh(id, forget), (FloatConsumer) n.attn.forgetRate::set);
        actionDial($.inh(id, $.p(forget, $.the(1))), $.inh(id, $.p(forget, $.the(-1))), n.attn.decay, 40);

//        float priFactorMin = 0.1f, priFactorMax = 4f;
//        beliefPriAction = actionUnipolar($.inh(id, beliefPri), n.beliefPriDefault.subRange(
//                Math.max(n.beliefPriDefault.floatValue() /* current value */ * priFactorMin, ScalarValue.EPSILON),
//                n.beliefPriDefault.floatValue() /* current value */ * priFactorMax)::setProportionally);
//        goalPriAction = actionUnipolar($.inh(id, goalPri), n.goalPriDefault.subRange(
//                Math.max(n.goalPriDefault.floatValue() /* current value */ * priFactorMin, ScalarValue.EPSILON),
//                n.goalPriDefault.floatValue() /* current value */ * priFactorMax)::setProportionally);

        int initialDur = n.dur();
        FloatRange durRange = new FloatRange(initialDur, initialDur/4, initialDur*4) {
            @Override
            public float get() {
                super.set(nar.time.dur());
                return super.get();
            }

            @Override
            public void set(float value) {
                super.set(value);
                value = super.get();
                int nextDur = Math.round(value);
                nar.time.dur(nextDur);
                //assert(nar.dur()==nextDur);
            }
        };
        actionDial($.inh(id, $.p(duration, $.the(1))), $.inh(id, $.p(duration, $.the(-1))), durRange, 5);
//        this.dur = actionUnipolar($.inh(id, duration), (x) -> {
//            n.time.dur(Util.lerp(x * x, n.dtDither(), initialDur * 2));
//            return x;
//        });


        senseNumberDifference($.inh(id, $$("busy")), n.feel.busyVol::asFloat);
        senseNumberDifference($.inh(id, $$("deriveTask")), n.feel.deriveTask::get);


        n.plugins(NAgent.class).forEach(a -> {
            if(MetaAgent.this!=a)
                add(a, false);
        });
    }

    private void add(NAgent a, boolean allowPause) {

        Reward r = rewardNormalized($.inh(a.id, happy), -Float.MIN_NORMAL, +Float.MIN_NORMAL,
            new FloatFirstOrderDifference(nar::time, ((((() -> {
//            float h = a.happinessMean();
//            float p = a.proficiency();
//            float hp = Util.or(h, p);
            //System.out.println(h + " " + p + " -> " + hp);
//            return hp;
            float d = (float) a.dexterityMean();
            return d;
        }))))));
        //reward($.inh(a.id, happy), a::happiness);

        //TODO other Emotion sensors

//        Term agentPriTerm =
//                $.inh(a.id, PRI);
//                //$.inh(a.id, id /* self */);
//        GoalActionConcept agentPri = actionUnipolar(agentPriTerm, (FloatConsumer)a.attn.factor::set);
//
//
//        GoalActionConcept curiosityAction = actionUnipolar($.inh(a.id, curiosity), (c) -> {
//            a.curiosity.rate.set(curiosity(a, start, c));
//        });

        if (allowPause) {
            //TODO agent enable
            GoalActionConcept enableAction = actionPushButton($.inh(a.id, enable), (e) -> {
                //enableAction = n.actionToggle($.func(enable, n.id), (e)->{
                //TODO integrate and threshold, pause for limited time
                if (!e) {
                    if (disableCountDown++ >= disableThreshold) {
                        a.enabled.set(false);
                        a.nar().runAt(a.nar().time() + disablePeriod * a.nar().dur(), () -> {
                            //re-enable
                            enabled.set(true);
                            disableCountDown = 0;
                        });
                    }
                } else {
                    a.enabled.set(true);
                    disableCountDown = 0;
                }
            });
        }


//        Reward enableReward = reward("enable", () -> enabled.getOpaque() ? +1 : 0f);
    }


    public GoalActionConcept[] dial(NAgent a, Atomic label, FloatRange var, int steps) {
        GoalActionConcept[] priAction = actionDial(
                $.inh(id, $.p(label, $.the(-1))),
                $.inh(id, $.p(label, $.the(+1))),
                var,
                steps);
        return priAction;
    }

    private int dur(int initialDur, float d) {
        return Math.max(1, Math.round((d + 0.5f) * 2 * initialDur));
    }

    /**
     * curiosity frequency -> probability mapping curve
     */
    static float curiosity(NAgent agent, long start, float c) {

        float min;
        float durs = (float) (((double) (agent.nar().time() - start)) / agent.nar().dur());
        if (durs < curiStartupDurs)
            min = curiMinYoung;
        else
            min = curiMinOld;

        return Util.lerp(c, min, curiMax);
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
