package nars.agent.util;

import com.google.common.collect.Streams;
import jcog.Paper;
import jcog.Util;
import jcog.data.list.FasterList;
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
import nars.agent.Game;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.action.GameAction;
import nars.concept.sensor.GameLoop;
import nars.control.channel.CauseChannel;
import nars.task.util.signal.SignalTask;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;
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
    public final FloatRange conf = new FloatRange(0.5f, 0f, 1f);
    public final float[] input;
    final int inD, outD;
    final GameAction[] actions;
    private final CauseChannel<Task> in;
    private final List<Term> inputs;
    private final int actionDiscretization;
    private final WritableTensor history;

    transient private float[] _in = null;
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
     * @param g
     * @param agent
     * @param nothingAction        reserve 0 for nothing
     */
    public RLBooster(Game g, IntIntToObjectFunction<Agent> agent, int history, int actionDiscetization, boolean nothingAction) {

        this.actionDiscretization = actionDiscetization;

        this.env = g;


        conf.set(Util.lerp(conf.get() /* HACK */, 2 * g.nar().confMin.floatValue(), g.nar().confDefault(GOAL)));


        boolean includeActions = true;
        Stream<GameLoop> inputs =
                Stream.concat(g.sensors.stream(), includeActions ? g.actions.stream() : Stream.empty());

        List<Term> ii =
            inputs.flatMap((GameLoop s) -> Streams.stream(s.components()) ).map(Termed::term).collect(toList());
        if (g.rewards.size() > 1)
            g.rewards.forEach(s -> s.forEach(t -> ii.add(t.term()))); //use individual rewards as sensors if > 1 reward

        this.inputs = ii;
        this.inD = ii.size();

        input = new float[inD];

        assert(actionDiscretization>=2): "discretization must be least >=2; minimum two states";

        this.actions = g.actions().array();
        this.outD = (nothingAction ? 1 : 0) /* nothing */ + actions.length * (actionDiscretization-1); /* pos/neg for each action */;

        logger.info("{} {} in={}x{} out={}", agent, g, inD, history, outD);
        assert(inD > 0);
        assert(outD > 0);

        in = g.nar().newChannel(this);

        this.history = history > 1 ? new TensorRing(inD, history) : new ArrayTensor(inD);
        this.agent = agent.apply(inD * history, outD);

        actionFeedback(g.time()); //init

        g.onFrame(()->accept(g.what()));
    }

    public int actions() {
        return outD;
    }

    private float[] input(long start, long end) {

        int i = 0;
        NAR n = env.nar();

        for (Term s : inputs) {
            Concept c = n.concept(s);
            Truth t = c!=null ? c.beliefs().truth(start, end, Tense.occToDT((end-start)), n) : null;
            input[i++] = t!=null ? t.freq() : valueMissing();
        }

        return input;
    }

    private float valueMissing() {
        return 0.5f;
    }

    /** uniform [0,1] noise */
    private float noise() {
        return env.nar().random().nextFloat();
    }

    private float[] feedback(long prev, long now, float[] fb) {
        int k = 0;
        NAR n = env.nar();

        float[] feedback = fb == null ? new float[actions()] : fb;

        for (GameAction s : actions) {
            Truth t = n.beliefTruth(s, prev, now);
            float tf = t!=null ? truthFeedback(t) : Float.NaN;
            float y = tf != tf ? noise() : tf;
            //y = (y - 0.5f) * 2; //polarize
            if (actionDiscretization > 2) {
                for (int d = 0; d < actionDiscretization-1; d++) {
                    //float yd = ((((float)d)/(actionDiscretization-1)) - 0.5f)*2;
                    float yd = ((float)(d))/(actionDiscretization-1);
                    feedback[k++] = y * Util.sqr(1 - Math.abs(yd - y)); //window
                }
            } else {
                feedback[k++] = y;
            }
        }

        Util.normalize(feedback); //needs shifted to mid0 and back to mid.5

        return feedback;
    }

    private float truthFeedback(Truth t) {
        //return t.freq();
        return t.expectation();
    }


    final FloatNormalizer HAPPINESS = new FloatNormalizer();

    public void accept(What w) {
        NAR nar = env.nar();

        double reward = HAPPINESS.valueOf(env.happiness() );
        //System.out.println(reward);
        lastReward = reward;

//        long start = env.last; //now() - dur/2;
//        long end = env.now(); //+ dur/2;
//        //HACK
//        int dur = nar.dur();
        long now = nar.time();

        int dtDither = nar.dtDither();
        long start = Tense.dither(env.now, dtDither, -1);
        long end = Tense.dither(Math.round(env.now + env.durPhysical()), dtDither, +1);

        if (end < start)
            return;
//        long start = now - dur/2;
//        long end = now + dur/2;


        float[] ii = input(start, end);
        _in = ii;
        if (history instanceof TensorRing) {
            ((TensorRing)history).setSpin(ii).snapshot(_in);
        } else {
            ((ArrayTensor)history).set(ii); //TODO just make ArrayTensor wrapping _in
        }

        int a = agent.act(actionFeedback(now), (float) reward, Util.toFloat(history.doubleArray()));
        int buttons = (actionDiscretization-1)*actions.length;
        if (a >= buttons) {
            //nothing action, or otherwise beyond range of action buttons
            return ;
        }
        int A = a / (actionDiscretization-1);
        int level = a % (actionDiscretization-1);

//        float OFFfreq =
//                //0f;
//                Float.NaN;
//        float ONfreq = 1f;


        float conf = this.conf.floatValue();
//        Truth off = OFFfreq == OFFfreq ? $.t(OFFfreq, conf) : null;
//        long range = end-start;
//        long nextStart = end;
//        long nextEnd = nextStart + range;
        //long nextStart = start, nextEnd = end;


        List<Task> e = new FasterList(actions.length);
        int aa = 0;

        for (int o = 0; o < actions.length; o++ ) {
            float freq;
            if (o == A) {
                freq = actionDiscretization > 2 ? (((float)(level+1))/(actionDiscretization-1)) : 1;
            } else{
                freq = 0;
            }
            Truth t = $.t(freq, conf);
            Task tt =
                    new SignalTask(actions[o].term(), GOAL, t, now, start, end, new long[]{nar.time.nextStamp()});
                    //NALTask.the(actions[o].term(), GOAL, t, now, start, end, new long[]{nar.time.nextStamp()});
                tt.pri(nar.priDefault(GOAL));
                e.add(tt);
        }

        in.acceptAll(e, w);
    }

    public float[] actionFeedback(long now) {
        return actionFeedback = feedback(env.prev, now, actionFeedback);
    }


}
