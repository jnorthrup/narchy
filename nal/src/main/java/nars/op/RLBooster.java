package nars.op;

import jcog.Util;
import jcog.learn.Agent;
import jcog.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntIntToObjectFunc;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Task;
import nars.concept.action.ActionConcept;
import nars.concept.scalar.Scalar;
import nars.control.CauseChannel;
import nars.task.ITask;
import nars.task.signal.SignalTask;
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
    public final Agent rl;
    public final FloatRange conf = new FloatRange(0.5f, 0f, 1f);
    final float[] input;
    final int inD, outD;
    final ActionConcept[] actions;
    private final CauseChannel<ITask> in;
    private final List<Scalar> inputs;
    private final int actionDiscretization;

    public RLBooster(NAgent env, IntIntToObjectFunc<Agent> rl, int actionDiscretization) {
        this(env, rl, actionDiscretization, true);
    }

    /**
     * @param env
     * @param rl
     * @param actionDiscretization
     * @param nothingAction        reserve 0 for nothing
     */
    public RLBooster(NAgent env, IntIntToObjectFunc<Agent> rl, int actionDiscretization, boolean nothingAction) {
        actionDiscretization = 1; //HACK
        //assert(actionDiscretization>=1);

        this.env = env;


        conf.set(Util.lerp(0.5f, env.nar().confMin.floatValue(), env.nar().confDefault(GOAL)));

//        env.curiosity().setValue(0f);

        List<Scalar> sc = $.newArrayList();
//        env.sensors.keySet().forEach(c -> {
//            if (c!=env.happy) //exclude the happiness sensor
//                sc.add(c);
//        });
        env.senseNums.forEach(c -> c.forEach(sc::add));
        env.sensorCam.forEach(c -> c.forEach(sc::add));

        this.inputs = sc;
        this.inD = sc.size();

        input = new float[inD];

        this.actionDiscretization = actionDiscretization;
        this.actions = env.actions().keySet().toArray(new ActionConcept[0]);
        this.outD = (nothingAction ? 1 : 0) /* nothing */ + actions.length * actionDiscretization /* pos/neg for each action */;

        logger.info("{} {} in={} out={}", rl, env, inD, outD);

//        this.output = new Runnable[outD];
//
//        int i = 0;
//        if (nothingAction) {
//            output[i++] = () -> {            };
//        }

        in = env.nar().newCauseChannel(this);

        this.rl = rl.apply(inD, outD);

        env.onFrame(this);
    }

    public int actions() {
        return outD;
    }

    float[] input() {
        //TODO replace with a Tensor API vector function
        int i = 0;
        for (Scalar s : inputs) {
            input[i++] = s.asFloat();
        }

        //TODO include previous outputs?
        return input;
    }

    @Override
    public void accept(NAR ignored) {

        //TODO provide actual action vector, not what it thinks it enacted by itself

        //NAgent's happiness value, normalized to -1..+1
        float reward = (env.happy.asFloat() - 0.5f) * 2f;

        int O = rl.act(reward, input());
        //System.out.println(now + " "  + o + " " + a.o.floatValue() + " " + " " + a.rewardValue);

        float OFFfreq
                = 0f;
        // = Float.NaN;


        NAR nar = env.nar();
        long start = env.last;
        long end = env.now;
        //int dur = nar.dur();
        List<Task> e = new FasterList(actions.length);
        for (int o = 0; o < actions.length; o++) {
            Truth off = OFFfreq == OFFfreq ? $.t(OFFfreq, conf.floatValue()) : null;
//            float value = actionDiscretization==1 ? 1f /* full */ :
//                    ((float)(j)) / (actionDiscretization-1);
            float value = 1f;

            Truth tK;
            if (o == O) {
                tK = $.t(value, conf.floatValue());
            } else {
                tK = off; //cancel all other concept goal signals
            }
            Task tt = new SignalTask(actions[o].term(), GOAL, tK, start, start, end, nar.time.nextStamp());
            if (tt != null)
                e.add(tt);
        }
        in.input(e);
    }


}
