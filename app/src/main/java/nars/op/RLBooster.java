package nars.op;

import jcog.learn.Agent;
import jcog.math.FloatParam;
import jcog.math.IntIntToObjectFunc;
import nars.*;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.control.CauseChannel;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.signal.Truthlet;
import nars.task.signal.TruthletTask;
import nars.time.Tense;
import nars.truth.Truth;
import nars.util.signal.Signal;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static nars.Op.GOAL;

/**
 * NAgent Reinforcement Learning Algorithm Accelerator
 */
public class RLBooster implements Consumer<NAR> {

    public static final Logger logger = LoggerFactory.getLogger(RLBooster.class);

    public final NAgent env;
    public final Agent rl;
    final float[] input;
    final Runnable[] output;
    final int inD, outD;
    private final CauseChannel<ITask> in;
    private final List<SensorConcept> inputs;
    public final FloatParam conf = new FloatParam(0.5f, 0f, 1f);

    public RLBooster(NAgent env, IntIntToObjectFunc<Agent> rl, int actionDiscretization) {
        this.env = env;


//        env.curiosity().setValue(0f);

        List<SensorConcept> sc = $.newArrayList();
        sc.addAll(env.sensors.keySet());
        if (env instanceof NAgentX) {

            ((NAgentX) env).cam.forEach(c -> {
                c.pixels.forEach(cc -> sc.add((SensorConcept) cc));
            });
        }
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
        for (ActionConcept a : env.actions.keySet()) {
            Signal aGoal = new Signal(GOAL, env.nar.freqResolution).pri(() -> env.nar.priDefault(GOAL));
            final int aa = agN;
            ca[agN] = a;
            ag[agN++] = aGoal;

            for (int j = 0; j < actionDiscretization; j++) {
                float value = ((float)j) / (actionDiscretization-1);
                output[i++] = () -> {
                    NAR nar = env.nar;
                    List<Task> toInput = $.newArrayList(ca.length);
                    long now = nar.time();
                    int dur = nar.dur();
                    for (int k = 0; k < ag.length; k++) {
                        Truth tK;
                        if (k == aa) {
                            tK = $.t(value, conf.floatValue());
                        } else {
                            tK = null; //cancel all other concept goal signals
                        }
                        Task tt = ag[k].set(ca[k], tK, nar.time::nextStamp, now, dur, nar);
                        if (tt!=null)
                            toInput.add( tt );
                    }
                    if (!toInput.isEmpty())
                        in.input(toInput);
//                    env.nar.goal(env.nar.priDefault(GOAL), a.term(), t, value, conf);
                };
            }
        }

        env.onFrame(this);
    }

    float[] input() {
        int i = 0;
        for (SensorConcept s : inputs) {
            input[i++] = s.asFloat();
        }

        //TODO include previous outputs?
        return input;
    }

    @Override
    public void accept(NAR ignored) {

        int o = rl.act(env.reward, input());
        //System.out.println(now + " "  + o + " " + a.o.floatValue() + " " + " " + a.rewardValue);

        output[o].run();

    }
}
