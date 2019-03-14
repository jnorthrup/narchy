package nars.agent;

import jcog.Util;
import jcog.math.FloatFirstOrderDifference;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import nars.$;
import nars.NAR;
import nars.attention.AttNode;
import nars.concept.action.GoalActionConcept;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

/**
 * supraself agent metavisor
 */
public class MetaAgent {

    static final Atomic curiosity = Atomic.the("curi"),
            forget = Atomic.the("forget"),
            beliefPri = Atomic.the("beliefPri"),
            goalPri = Atomic.the("goalPri"),
            pri = Atomic.the("pri"),
            enable = Atomic.the("enable"), duration = Atomic.the("dur");

    final AttNode attn;
    private final NAgent agent;
    private final long start;

    public final GoalActionConcept enableAction;
    public final Reward enableReward;
    //private final GoalActionConcept durAction;
    public final GoalActionConcept[] forgetAction;
    public final GoalActionConcept[] beliefPriAction;
    private final GoalActionConcept[] goalPriAction;
    private final GoalActionConcept[] agentPri;
    private final GoalActionConcept dur;

    private int disableCountDown = 0;
    private final int disableThreshold = 1;
    private final long disablePeriod = 4;

    int startupDurs = 5000;

    float curiMax = 0.2f;
    float curiMinOld = 0.01f,
            curiMinYoung = 0.04f;


    private final GoalActionConcept curiosityAction;

    public MetaAgent(NAgent n) {
        this(n, false);
    }

    public MetaAgent(NAgent a, boolean allowPause) {
        this.agent = a;

        this.attn = new AttNode(this);
        attn.parent(a.attnReward /* HACK */);

        NAR n = a.nar();
        NAR nar = n;
        start = nar.time();
        curiosityAction = a.actionUnipolar($.inh(a.id, curiosity), (c) -> {
            a.curiosity.rate.set(curiosity(c));
            return c;
        });
        curiosityAction.attn.reparent(attn);


        forgetAction = a.actionDial(
                $.inh(a.id, $.p(forget, $.the(-1))),
                $.inh(a.id, $.p(forget, $.the(+1))),
                n.attn.forgetRate, 40);
        forgetAction[0].attn.reparent(attn); //HACK
        forgetAction[1].attn.reparent(attn);//HACK

        this.beliefPriAction = dial(a, MetaAgent.beliefPri,
                n.beliefPriDefault.subRange(
                        ScalarValue.EPSILON, n.beliefPriDefault.floatValue() /* current value */ * 2),
                20);
        this.goalPriAction = dial(a, MetaAgent.goalPri,
                n.goalPriDefault.subRange(
                        ScalarValue.EPSILON, n.goalPriDefault.floatValue() /* current value */ * 2),
                20);

        int initialDur = n.dur();
        this.dur = a.actionUnipolar($.inh(a.id,duration), (x) -> {
            n.time.dur(Util.lerp(x*x, n.dtDither(), initialDur*2));
            return x;
        });
        this.agentPri = dial(a, MetaAgent.pri,
                a.pri,
                20);


        if (allowPause) {

            //TODO control the agent dur, not the entire NAR
//            int initialDur = nar.dur();
//            durAction = a.actionUnipolar($.func(duration, a.id), (d)->{
//                nar.time.dur(dur(initialDur,d));
//                return d;
//            });
//            durAction.attn.reparent(attn);


            //TODO agent enable
            enableAction = a.actionPushButton($.func(enable, a.id), (e) -> {
                //enableAction = n.actionToggle($.func(enable, n.id), (e)->{
                //TODO integrate and threshold, pause for limited time
                if (!e) {
                    if (disableCountDown++ >= disableThreshold) {
                        a.enabled.set(false);
                        nar.runAt(nar.time() + disablePeriod * nar.dur(), () -> {
                            //re-enable
                            a.enabled.set(true);
                            disableCountDown = 0;
                        });
                    }
                } else {
                    a.enabled.set(true);
                    disableCountDown = 0;
                }
            });


            enableReward = a.reward("enable", () -> a.enabled.getOpaque() ? +1 : 0f);

            enableAction.attn.reparent(attn);
            enableReward.attn.reparent(attn);
        } else {
            enableAction = null;
            enableReward = null;


//            durAction = null;
        }

        //TODO duration control
    }

    @NotNull
    public GoalActionConcept[] dial(NAgent a, Atomic label, FloatRange var, int steps) {
        GoalActionConcept[] priAction = a.actionDial(
                $.inh(a.id, $.p(label, $.the(-1))),
                $.inh(a.id, $.p(label, $.the(+1))),
                var,
                steps);
        priAction[0].attn.reparent(attn);//HACK
        priAction[1].attn.reparent(attn); //HACK
        return priAction;
    }

    private int dur(int initialDur, float d) {
        return Math.max(1, Math.round((d + 0.5f) * 2 * initialDur));
    }

    /**
     * curiosity frequency -> probability mapping curve
     */
    float curiosity(float c) {

        float min;
        float durs = (float) (((double) (agent.nar().time() - start)) / agent.nar().dur());
        if (durs < startupDurs)
            min = curiMinYoung;
        else
            min = curiMinOld;

        return Util.lerp(c, min, curiMax);
    }

    private static NAgent metavisor(NAgent a) {

//        new NARSpeak.VocalCommentary( a.nar());

        //
//        a.nar().onTask(x -> {
//           if (x.isGoal() && !x.isInput())
//               System.out.println(x.proof());
//        });

        int durs = 4;
        NAR nar = a.nar();

        NAgent m = new NAgent($.func("meta", a.id), FrameTrigger.durs(durs), nar);

        m.reward(
                new SimpleReward($.func("dex", a.id),
                        new FloatNormalized(new FloatFirstOrderDifference(a.nar()::time,
                                a::dexterity)).relax(0.01f), m)
        );

//        m.actionUnipolar($.func("forget", a.id), (f)->{
//            nar.memoryDuration.setAt(Util.lerp(f, 0.5f, 0.99f));
//        });
//        m.actionUnipolar($.func("awake", a.id), (f)->{
//            nar.conceptActivation.setAt(Util.lerp(f, 0.1f, 0.99f));
//        });
        m.senseNumber($.func("busy", a.id), new FloatNormalized(() ->
                (float) Math.log(1 + m.nar().emotion.busyVol.getMean()), 0, 1).relax(0.05f));
//
//        for (Sensor s : a.sensors) {
//            if (!(s instanceof Signal)) { //HACK only if compound sensor
//                Term target = s.target();
//
//                //HACK
//                if (s instanceof DigitizedScalar)
//                    target = $.quote(target.toString()); //throw new RuntimeException("overly complex sensor target");
//
//                //HACK TODO divide by # of contained concepts, reported by Sensor interface
//                float maxPri;
//                if (s instanceof Bitmap2DSensor) {
//                    maxPri = 8f / (float) (Math.sqrt(((Bitmap2DSensor) s).concepts.area));
//                } else {
//                    maxPri = 1;
//                }
//
//                m.actionUnipolar($.func("aware", target), (p) -> {
//                    FloatRange pp = s.pri();
//                    pp.setAt(lerp(p, 0f, maxPri * nar.priDefault(BELIEF)));
//                });
//
//            }
//        }

//        actionUnipolar($.inh(this.nar.self(), $.the("deep")), (d) -> {
//            if (d == d) {
//                //deep incrases both duration and max target volume
//                this.nar.time.dur(Util.lerp(d * d, 20, 120));
//                this.nar.termVolumeMax.setAt(Util.lerp(d, 30, 60));
//            }
//            return d;
//        });

//        actionUnipolar($.inh(this.nar.self(), $.the("awake")), (a)->{
//            if (a == a) {
//                this.nar.activateConceptRate.setAt(Util.lerp(a, 0.2f, 1f));
//            }
//            return a;
//        });

//        actionUnipolar($.prop(nar.self(), $.the("focus")), (a)->{
//            nar.forgetRate.setAt(Util.lerp(a, 0.9f, 0.8f)); //inverse forget rate
//            return a;
//        });

//        m.actionUnipolar($.func("curious", a.id), (cur) -> {
//            a.curiosity.setAt(lerp(cur, 0.01f, 0.25f));
//        });//.resolution(0.05f);


        return m;
    }

}
