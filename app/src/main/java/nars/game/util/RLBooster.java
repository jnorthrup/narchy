package nars.game.util;

import com.google.common.collect.Streams;
import jcog.Paper;
import jcog.Util;
import jcog.func.IntIntToObjectFunction;
import jcog.learn.Agent;
import jcog.math.FloatNormalizer;
import jcog.math.FloatRange;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.TensorRing;
import jcog.signal.tensor.WritableTensor;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.control.channel.CauseChannel;
import nars.game.Game;
import nars.game.Reward;
import nars.game.action.ActionSignal;
import nars.game.sensor.GameLoop;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;
import nars.time.When;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static nars.Op.GOAL;

/**
 * NAgent Reinforcement Learning Algorithm Accelerator
 * TODO use AgentBuilder
 */
@Paper
public class RLBooster  {

    public static final Logger logger = LoggerFactory.getLogger(RLBooster.class);

    public final Game env;
    public final Agent agent;
    public final FloatRange conf = new FloatRange(0.1f, 0f, 1f);
    public final float[] input;
    final int inD;
    final int outD;
    final ActionSignal[] actions;
    private final CauseChannel<Task> in;
    private final List<Term> inputs;
    private final int actionDiscretization;
    private final WritableTensor history;
    private final boolean nothingAction;

    public double lastReward = Float.NaN;
    public float[] actionFeedback = null;

//    public RLBooster(NAgent env, IntIntToObjectFunc<Agent> rl, int actionDiscretization) {
//        this(env, rl, true);
//    }
//
//    @Deprecated public RLBooster(Game env, IntIntToObjectFunction<Agent> agent, boolean nothingAction) {
//        this(env, agent, 1, 1, nothingAction);
//    }

    /**
     * @param nothingAction        reserve 0 for nothing
     * @param g
     * @param agent
     */
    public RLBooster(boolean nothingAction, Game g, int history, int actionDiscetization, IntIntToObjectFunction<Agent> agent) {

        this.actionDiscretization = actionDiscetization;

        this.env = g;


        conf.set(Util.lerp(conf.get() /* HACK */, 2 * g.nar().confMin.floatValue(), g.nar().confDefault(GOAL)));


        var includeActions = true;
        var inputs =
                Stream.concat(g.sensors.stream(), includeActions ? g.actions.stream() : Stream.empty());

        var ii =
            inputs.flatMap((GameLoop s) -> Streams.stream(s.components()) ).map(Termed::term).collect(toList());
        if (g.rewards.size() > 1) {//use individual rewards as sensors if > 1 reward
            for (var s : g.rewards) {
                for (var t : s) {
                    ii.add(t.term());
                }
            }
        }

        this.inputs = ii;
        this.inD = ii.size();

        input = new float[inD];

        assert(actionDiscretization>=2): "discretization must be least >=2; minimum two states";

        this.actions = g.actions().array();
        this.nothingAction = nothingAction;
        this.outD = (nothingAction ? 1 : 0) /* nothing */ + actions.length * (actionDiscretization-1); /* pos/neg for each action */

		logger.info("{} {} in={}x{} out={}", agent, g, inD, history, outD);
        assert(inD > 0);
        assert(outD > 0);

        in = g.nar().newChannel(this);

        this.history = history > 1 ? new TensorRing(inD, history) : new ArrayTensor(inD);
        this.agent = agent.apply(inD * history, outD);

        //actionFeedback(g.when); //init

        g.onFrame(()->accept(g));
    }

    public int actions() {
        return outD;
    }

    private float[] input(long start, long end) {

        var i = 0;
        var n = env.nar();

        for (var s : inputs) {
            var c = n.concept(s);
            var t = c!=null ? c.beliefs().truth(start, end, Tense.occToDT((end-start)), n) : null;
            input[i++] = t!=null ? t.freq() : valueMissing();
        }

        return input;
    }

    private static float valueMissing() {
        return 0.5f;
    }

    /** uniform [0,1] noise */
    private float noise() {
        return env.nar().random().nextFloat();
    }

    private float[] feedback(long prev, long now, float[] fb) {
        var k = 0;
        var n = env.nar();

        var feedback = fb == null ? new float[actions()] : fb;

        for (var s : actions) {
            var t = n.beliefTruth(s, prev, now);
            var tf = t!=null ? truthFeedback(t) : Float.NaN;
            var y = tf != tf ? noise() : tf;
            //y = (y - 0.5f) * 2; //polarize
            if (actionDiscretization > 2) for (var d = 0; d < actionDiscretization - 1; d++) {
                //float yd = ((((float)d)/(actionDiscretization-1)) - 0.5f)*2;
                var yd = ((float) (d)) / (actionDiscretization - 1);
                feedback[k++] = y * Util.sqr(1 - Math.abs(yd - y)); //window
            }
            else feedback[k++] = y;
        }

        var feedbackSum = Util.sum(feedback);
        Util.normalize(feedback, 0, feedbackSum);
        if (this.nothingAction) {
            feedback[feedback.length-1] = (1-Util.max(feedback))*1f/(1+Util.variance(feedback)); //HACK TODO estimate better
            Util.normalize(feedback, 0, Util.sum(feedback)); //normalize again
        }

        return feedback;
    }

    private static float truthFeedback(Truth t) {
        return t.freq();
        //return t.expectation();
    }


    final FloatNormalizer HAPPINESS = new FloatNormalizer();

    public void accept(Game g) {
        var nar = env.nar();

        double reward = ((HAPPINESS.valueOf((float)env.happiness()) - 0.5f) * 2);

        //System.out.println(reward);
        lastReward = reward;

//        long start = env.last; //now() - dur/2;
//        long end = env.now(); //+ dur/2;
//        //HACK
//        int dur = nar.dur();
//        long start = now - dur/2;
//        long end = now + dur/2;


        var w = g.nowPercept;
        var start = w.start;
        var end = w.end;
        var ii = input(start, end);
        if (history instanceof TensorRing) ((TensorRing) history).setSpin(ii).snapshot(ii);
        else ((ArrayTensor) history).set(ii); //TODO just make ArrayTensor wrapping _in

        var a = agent.act(actionFeedback(w), (float) reward, Util.toFloat(history.doubleArray()));
        var buttons = (actionDiscretization - 1) * actions.length;

        //nothing action, or otherwise beyond range of action buttons
        if (a < buttons) {
            var A = a / (actionDiscretization - 1);

            var level = a % (actionDiscretization - 1);//        float OFFfreq =
//                //0f;
//                Float.NaN;
//        float ONfreq = 1f;
            var conf = this.conf.floatValue();//        Truth off = OFFfreq == OFFfreq ? $.t(OFFfreq, conf) : null;
//        long range = end-start;
//        long nextStart = end;
//        long nextEnd = nextStart + range;
//long nextStart = start, nextEnd = end;
//TODO modify Agent API to provide a continuous output vector and use that to interpolate across the different states


            var W = w.x;
            for (var o = 0; o < actions.length; o++) {
                var freq = (o == A) ?
                    ((actionDiscretization > 2) ? (((float) 1+level) / (actionDiscretization - 1)) : 1)
                    :
                    ((actionDiscretization > 2) ? Float.NaN : 0);

//                System.out.println(actions[o].term() + " " + freq);

                if (freq==freq) {
                    var tt = $.t(freq, conf);

                    in.accept(
                        NALTask.the(
                            actions[o].term(), GOAL,
                            tt, start, start, end,
                            new long[]{nar.time.nextStamp()}
                        ).pri(nar.priDefault(GOAL)), W);
                }
            }

        }


    }

    public float[] actionFeedback(When when) {
        return actionFeedback = feedback(when.start, when.end, actionFeedback);
    }


}
