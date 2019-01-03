package nars.agent.util;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.learn.Agent;
import jcog.math.FloatRange;
import jcog.math.IntIntToObjectFunc;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.agent.NAgent;
import nars.concept.action.ActionConcept;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

import static nars.Op.GOAL;

/**
 * NAgent Reinforcement Learning Algorithm Accelerator
 * TODO use AgentBuilder
 */
public class RLBooster implements Consumer<NAR> {

    public static final Logger logger = LoggerFactory.getLogger(RLBooster.class);

    public final NAgent env;
    public final Agent agent;
    public final FloatRange conf = new FloatRange(0.26f, 0f, 1f);
    final float[] input;
    final int inD, outD;
    final ActionConcept[] actions;
    private final CauseChannel<ITask> in;
    private final List<Term> inputs;
    private final int actionDiscretization;

//    public RLBooster(NAgent env, IntIntToObjectFunc<Agent> rl, int actionDiscretization) {
//        this(env, rl, true);
//    }

    /**
     * @param env
     * @param agent
     * @param nothingAction        reserve 0 for nothing
     */
    public RLBooster(NAgent env, IntIntToObjectFunc<Agent> agent, boolean nothingAction) {



        this.env = env;


        conf.set(Util.lerp(0.5f, env.nar().confMin.floatValue(), env.nar().confDefault(GOAL)));



        List<Term> sc = $.newArrayList();




        env.sensors.forEach(s -> s.components().forEach(t -> sc.add(t.term())));


        this.inputs = sc;
        this.inD = sc.size();

        input = new float[inD];

        this.actionDiscretization = 1;
        this.actions = env.actions().array();
        this.outD = (nothingAction ? 1 : 0) /* nothing */ + actions.length * actionDiscretization /* pos/neg for each action */;

        logger.info("{} {} in={} out={}", agent, env, inD, outD);
        assert(inD > 0);
        assert(outD > 0);








        in = env.nar().newChannel(this);

        this.agent = agent.apply(inD, outD);

        env.onFrame(this);
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

    private float[] feedback(long prev, long now) {
        int i = 0;
        NAR n = env.nar();

        float[] feedback = new float[actions()];
        for (ActionConcept s : actions) {
            Truth t = n.beliefTruth(s, prev, now);
            feedback[i++] = t!=null ? t.freq() : 0.5f;
        }

        //Util.normalize...

        return feedback;
    }

    @Override
    public void accept(NAR ignored) {
        NAR nar = env.nar();

        float reward = env.reward();

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

        int O = agent.act(feedback(env.prev, now), reward, input(start, end) );

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
                Task tt = new SignalTask(actions[o].term(), GOAL, t, start, start, end, nar.time.nextStamp());
                tt.pri(nar);
                if (tt != null)
                    e.add(tt);
            }
        }
        in.input(e);
    }




}
