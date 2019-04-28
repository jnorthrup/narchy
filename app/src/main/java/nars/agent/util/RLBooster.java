package nars.agent.util;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.func.IntIntToObjectFunction;
import jcog.learn.Agent;
import jcog.math.FloatRange;
import jcog.signal.tensor.TensorRing;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.agent.Game;
import nars.attention.What;
import nars.concept.action.AgentAction;
import nars.control.channel.CauseChannel;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.Op.GOAL;

/**
 * NAgent Reinforcement Learning Algorithm Accelerator
 * TODO use AgentBuilder
 */
public class RLBooster  {

    public static final Logger logger = LoggerFactory.getLogger(RLBooster.class);

    public final Game env;
    public final Agent agent;
    public final FloatRange conf = new FloatRange(0.5f, 0f, 1f);
    final float[] input;
    final int inD, outD;
    final AgentAction[] actions;
    private final CauseChannel<Task> in;
    private final List<Term> inputs;
    private final int actionDiscretization;
    private final TensorRing history;

    transient private float[] _in = null;
    public double lastReward = Float.NaN;

//    public RLBooster(NAgent env, IntIntToObjectFunc<Agent> rl, int actionDiscretization) {
//        this(env, rl, true);
//    }

    @Deprecated public RLBooster(Game env, IntIntToObjectFunction<Agent> agent, boolean nothingAction) {
        this(env, agent, 1, 1, nothingAction);
    }

    /**
     * @param g
     * @param agent
     * @param nothingAction        reserve 0 for nothing
     */
    public RLBooster(Game g, IntIntToObjectFunction<Agent> agent, int history, int actionDiscetization, boolean nothingAction) {

        this.actionDiscretization = actionDiscetization;

        this.env = g;


        conf.set(Util.lerp(0f, 2 * g.nar().confMin.floatValue(), g.nar().confDefault(GOAL)));

        List<Term> ii = $.newArrayList();

        g.sensors.forEach(s -> s.components().forEach(t -> ii.add(t.term())));
        if (g.rewards.size() > 1)
            g.rewards.forEach(s -> s.forEach(t -> ii.add(t.term()))); //use individual rewards as sensors if > 1 reward

        this.inputs = ii;
        this.inD = ii.size();

        input = new float[inD];

        this.actions = g.actions().array();
        this.outD = (nothingAction ? 1 : 0) /* nothing */ + actions.length * actionDiscretization /* pos/neg for each action */;

        logger.info("{} {} in={}x{} out={}", agent, g, inD, history, outD);
        assert(inD > 0);
        assert(outD > 0);

        in = g.nar().newChannel(this);

        this.history = new TensorRing(inD, history);
        this.agent = agent.apply(inD * history, outD);

        g.onFrame(()->accept(g.what()));
    }

    public int actions() {
        return outD;
    }

    private float[] input(long start, long end) {

        int i = 0;
        NAR n = env.nar();

        for (Term s : inputs) {
            Truth t = n.beliefTruth(s, start, end);
            input[i++] = t!=null ? t.freq() : 0.5f;
        }

        return input;
    }

    /** uniform [0,1] noise */
    private float noise() {
        return env.nar().random().nextFloat();
    }

    private float[] feedback(long prev, long now) {
        int i = 0;
        NAR n = env.nar();

        float[] feedback = new float[actions()];
        for (AgentAction s : actions) {
            Truth t = n.beliefTruth(s, prev, now);
            feedback[i++] = t!=null ? t.freq() : noise();
        }

        //Util.normalize(feedback); //needs shifted to mid0 and back to mid.5

        return feedback;
    }


    public void accept(What w) {
        NAR nar = env.nar();

        double reward = (env.happiness() - 0.5)*2 /* polarize */;
        lastReward = reward;

//        long start = env.last; //now() - dur/2;
//        long end = env.now(); //+ dur/2;
//        //HACK
//        int dur = nar.dur();
        long now = nar.time();
        long start = now;
        long end = env.next;
        if (end < start)
            return;
//        long start = now - dur/2;
//        long end = now + dur/2;


        int O = agent.act(feedback(env.prev, now), (float) reward,
                _in = history.setSpin(input(start, end)).snapshot(_in) );

        float OFFfreq =
                0f;
                //Float.NaN;
        float ONfreq = 1f;


        float conf = this.conf.floatValue();
        Truth off = OFFfreq == OFFfreq ? $.t(OFFfreq, conf) : null;
        Truth on = $.t(ONfreq, conf);

        List<Task> e = new FasterList(actions.length);
        for (int o = 0; o < actions.length; o++) {

            Truth t;
            if (o == O) {
                t = on;
            } else {
                t = off;
            }

            if (t !=null) {
                Task tt = new SignalTask(actions[o].term(), GOAL, t, start, start, end, new long[] { nar.time.nextStamp() });
                tt.pri(nar);
                e.add(tt);
            }
        }
        in.acceptAll(e, w);
    }




}
