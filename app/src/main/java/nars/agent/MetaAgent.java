package nars.agent;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import jcog.util.FloatConsumer;
import nars.$;
import nars.NAR;
import nars.concept.action.GoalActionConcept;
import nars.term.Term;
import nars.term.atom.Atomic;

/**
 * supraself agent metavisor
 */
public class MetaAgent extends NAgent {

    static final Atomic curiosity = Atomic.the("curi"),
            forget = Atomic.the("forget"),
            beliefPri = Atomic.the("beliefPri"),
            goalPri = Atomic.the("goalPri"),

            enable = Atomic.the("enable"),
            duration = Atomic.the("dur"),
            happy = Atomic.the("happy")
            ;

    //final AttNode attn;
    //private final NAgent agent;
    //private final long start;

    //public final GoalActionConcept enableAction;
    //public final Reward enableReward;
    //private final GoalActionConcept durAction;
    public final GoalActionConcept forgetAction;
    public final GoalActionConcept beliefPriAction;
    private final GoalActionConcept goalPriAction;
    //    private final GoalActionConcept[] agentPri;
    private final GoalActionConcept dur;

    private int disableCountDown = 0;
    private final int disableThreshold = 1;
    private final long disablePeriod = 4;


    static int curiStartupDurs = 5000;
    static float curiMax = 0.2f;
    static float curiMinOld = 0.01f, curiMinYoung = 0.04f;



    public MetaAgent(NAR n) {
        super(n.self().toString() /* HACK */, n);

        forgetAction = actionUnipolar($.inh(id, forget), (FloatConsumer) n.attn.forgetRate::set);

        float priFactorMin = 0.1f, priFactorMax = 4f;
        beliefPriAction = actionUnipolar($.inh(id, beliefPri), n.beliefPriDefault.subRange(
                Math.max(n.beliefPriDefault.floatValue() /* current value */ * priFactorMin, ScalarValue.EPSILON),
                n.beliefPriDefault.floatValue() /* current value */ * priFactorMax)::setProportionally);
        goalPriAction = actionUnipolar($.inh(id, goalPri), n.goalPriDefault.subRange(
                Math.max(n.goalPriDefault.floatValue() /* current value */ * priFactorMin, ScalarValue.EPSILON),
                n.goalPriDefault.floatValue() /* current value */ * priFactorMax)::setProportionally);

        int initialDur = n.dur();
        this.dur = actionUnipolar($.inh(id, duration), (x) -> {
            n.time.dur(Util.lerp(x * x, n.dtDither(), initialDur * 2));
            return x;
        });



        n.services(NAgent.class).forEach(a -> {
            if(MetaAgent.this!=a)
                add(a, false);
        });
    }

    private void add(NAgent a, boolean allowPause) {

        long start = a.nar().time();


        Reward r = reward($.inh(a.id, happy), () -> Util.or(a.happinessMean(), a.proficiency()));
        //reward($.inh(a.id, happy), a::happiness);

        Term agentPriTerm =
                $.inh(a.id, id /* self */);
                //$.inh(a.id, pri);
        GoalActionConcept agentPri = actionUnipolar(agentPriTerm, (FloatConsumer) a.pri::set);


        GoalActionConcept curiosityAction = actionUnipolar($.inh(a.id, curiosity), (c) -> {
            a.curiosity.rate.set(curiosity(a, start, c));
        });
        //TODO control the agent dur, not the entire NAR
//            int initialDur = nar.dur();
//            durAction = actionUnipolar($.func(duration, id), (d)->{
//                nar.time.dur(dur(initialDur,d));
//                return d;
//            });
//            durAction.attn.reparent(attn);


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
