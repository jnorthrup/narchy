package nars.op;

import jcog.learn.Agent;
import jcog.math.FloatRange;
import jcog.math.IntIntToObjectFunc;
import nars.*;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.control.CauseChannel;
import nars.task.ITask;
import nars.truth.Truth;
import nars.util.signal.Signal;
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
    final float[] input;

    @Deprecated final Runnable[] output;

    final int inD, outD;
    private final CauseChannel<ITask> in;
    private final List<SensorConcept> inputs;
    public final FloatRange conf = new FloatRange(0.5f, 0f, 1f);

    public RLBooster(NAgent env, IntIntToObjectFunc<Agent> rl, int actionDiscretization) {
        assert(actionDiscretization>=1);

        this.env = env;


//        env.curiosity().setValue(0f);

        List<SensorConcept> sc = $.newArrayList();
        sc.addAll(env.sensors.keySet());
//        if (env instanceof NAgentX) {
//            ((NAgentX) env).cam.forEach(c -> c.forEach(sc::add));
//        }
        this.inputs = sc;
        this.inD = sc.size();

        input = new float[inD];

        boolean nothingAction = false; //reserve 0 for nothing

        this.outD = (nothingAction ? 1 : 0) /* nothing */ + env.actions.size() * actionDiscretization /* pos/neg for each action */;
        this.output = new Runnable[outD];

        int i = 0;
        if (nothingAction) {
            output[i++] = () -> {            };
        }

        logger.info("{} {} in={} out={}", rl, env, inD, outD);
        this.rl = rl.apply(inD, outD);

        in = env.nar.newCauseChannel(this);

        Concept[] ca = new Concept[env.actions.size()];
        Signal[] ag = new Signal[ca.length];
        int agN = 0;

        float OFFfreq
                = 0f;
                // = Float.NaN;

        for (ActionConcept a : env.actions.keySet()) {
            Signal aGoal = new Signal(GOAL, env.nar.freqResolution)
                    .pri(() -> env.nar.priDefault(GOAL));
            final int aa = agN;
            ca[agN] = a;
            ag[agN++] = aGoal;

            for (int j = 0; j < actionDiscretization; j++) {

                /** TODO support other discretizations */
                float value = actionDiscretization==1 ? 1f /* full */ :
                        ((float)j) / (actionDiscretization-1);

                output[i++] = () -> {
                    NAR nar = env.nar;
                    List<Task> toInput = $.newArrayList(ca.length);
                    long now = nar.time();
                    int dur = nar.dur();
                    Truth off = OFFfreq == OFFfreq ? $.t(OFFfreq, conf.floatValue()) : null;
                    for (int k = 0; k < ag.length; k++) {
                        Truth tK;
                        if (k == aa) {
                            tK = $.t(value, conf.floatValue());
                        } else {
                            tK = off; //cancel all other concept goal signals
                        }
                        Task tt = ag[k].set(ca[k], tK, nar.time::nextStamp, now, dur, nar);
                        if (tt!=null)
                            toInput.add( tt );
                    }
                    if (!toInput.isEmpty())
                        in.input(toInput);
                };
            }
        }
        assert(actionDiscretization>1 || agN>1);

        env.onFrame(this);
    }

    float[] input() {
        //TODO replace with a Tensor API vector function
        int i = 0;
        for (SensorConcept s : inputs) {
            input[i++] = s.asFloat();
        }

        //TODO include previous outputs?
        return input;
    }

    @Override
    public void accept(NAR ignored) {

        //TODO provide actual action vector, not what it thinks it enacted by itself
        int o = rl.act(env.reward, input());
        //System.out.println(now + " "  + o + " " + a.o.floatValue() + " " + " " + a.rewardValue);

        output[o].run();

    }
}
