package nars;

import com.netflix.servo.Metric;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.StepCounter;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.util.Clock;
import jcog.pri.ScalarValue;
import jcog.signal.meter.ExplainedCounter;
import jcog.signal.meter.FastCounter;
import jcog.signal.meter.Meter;
import jcog.signal.meter.MetricsMapper;
import jcog.signal.meter.event.AtomicMeanFloat;
import nars.control.MetaGoal;
import nars.task.NALTask;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static jcog.Texts.n4;
import static jcog.signal.meter.Meter.meter;

/**
 * emotion - internal mental state
 * manages non-logical/meta states of the system
 * and collects/provides information about them to the system
 * variables used to record emotional values
 */
public class Emotion implements Meter {

    /**
     * priority rate of Task processing attempted
     */
    public final Counter busyPri = new StepCounter(meter("busyPri"));


    public final Counter conceptFire = new FastCounter("concept fire");
    public final Counter taskFire = new FastCounter("task fire");
    public final Counter taskActivation_x100 = new FastCounter("task activation pri sum x100");
    public final Counter premiseFire = new FastCounter("premise fire");
    public final Counter premiseFailMatch = new FastCounter("premise fail");

    /**
     * indicates lack of novelty in premise selection
     */
    public final Counter premiseBurstDuplicate = new FastCounter("premise burst duplicate");

    public final Counter premiseUnderivable = new FastCounter("premise underivable");

    public final Counter deriveTask = new FastCounter("derive task");
    public final Counter deriveTermify = new FastCounter("derive termify");
    public final Counter deriveEval = new FastCounter("derive eval");
    public final ExplainedCounter deriveFailTemporal = new ExplainedCounter("derive fail temporal");
    public final ExplainedCounter deriveFailEval = new ExplainedCounter("derive fail eval");
    public final Counter deriveFailVolLimit = new FastCounter("derive fail vol limit");
    public final Counter deriveFailTaskify = new FastCounter("derive fail taskify");
    public final Counter deriveFailPrioritize = new FastCounter("derive fail prioritize");
    public final Counter deriveFailParentDuplicate = new FastCounter("derive fail parent duplicate");
    public final Counter deriveFailDerivationDuplicate = new FastCounter("derive fail derivation duplicate");


    /**
     * the indices of this array correspond to the ordinal() value of the MetaGoal enum values
     * TODO convert to AtomicFloatArray or something where each value is volatile
     */
    public final float[] want = new float[MetaGoal.values().length];
    public final AtomicMeanFloat busyVol;
    /**
     * happiness rate
     */
    public final AtomicMeanFloat happy;
    private final NAR nar;
    float _happy;
    private float termVolMax = 1;

    /**
     * count of errors
     */


    public Emotion(NAR n) {
        super();

        this.nar = n;

        this.happy = new AtomicMeanFloat("happy");

        this.busyVol = new AtomicMeanFloat("busyV");


    }

    /**
     * sets the desired level for a particular MetaGoal.
     * the value may be positive or negative indicating
     * its desirability or undesirability.
     * the absolute value is considered relative to the the absolute values
     * of the other MetaGoal's
     */
    public void want(MetaGoal g, float v) {
        want[g.ordinal()] = v;
    }

    @Override
    public String name() {
        return "emotion";
    }

    @Override
    public Clock clock() {
        return nar.time;
    }

    public Runnable getter(MonitorRegistry reg, Supplier<Map<String, Object>> p) {
        return new PollRunnable(
                new MonitorRegistryMetricPoller(reg),
                BasicMetricFilter.MATCH_ALL,
                new MetricsMapper(name(), clock(), p) {
                    @Override
                    protected void update(List<Metric> metrics, Map<String, Object> map) {
                        super.update(metrics, map);
                        map.put("emotion", summary());
                    }
                }
        );
    }

    /**
     * new frame started
     */
    public void cycle() {

        termVolMax = nar.termVolumeMax.floatValue();

        _happy = happy.commitSum();


        busyVol.commit();

    }


    public float happy() {
        return _happy;
    }


    @Deprecated
    public void happy(float increment) {
        happy.accept(increment);
    }

    @Deprecated
    public void busy(float pri, int vol) {
        busyPri.increment(Math.round(pri * 1000));
        busyVol.accept(vol);
    }


    public String summary() {


        StringBuilder sb = new StringBuilder()
                .append(" hapy=").append(n4(happy()))
                .append(" busy=").append(n4(busyVol.getSum()));


        return sb.toString();


    }


    /**
     * sensory prefilter
     *
     * @param x is a non-command task
     */
    public void onInput(Task t, NAR nar) {

        float pri = t.priElseZero();
        float vol = t.voluplexity();

        /** heuristic, 3 components:
         *      base penalty for processing
         *      complexity
         *      priority
         */
        float cost = (1f + vol / termVolMax + pri) / 3f;

        MetaGoal.Perceive.learn(t.cause(), cost, nar.causes);

        busy(pri, (int) Math.ceil(vol));
    }

    /**
     * effective activation percentage
     */
    public void onActivate(Task t, float activation) {
        taskFire.increment();
        taskActivation_x100.increment(Math.round(activation * 100));
    }

    public void onAnswer(Task questionTask, Task answer) {


        if (questionTask == null)
            return;

        float qOrig = questionTask.originality();
        float ansConf = answer.conf();

        float qPriBefore = questionTask.priElseZero();
        if (qPriBefore > ScalarValue.EPSILON) {
            //float fraction = ansConf * (1 - qOrig);
            //float fraction = qOrig / 2f;
            float fraction = 0.5f * (1f - qOrig);
            answer.take(questionTask, fraction, false,
                    /*true */ false);

            //HACK append the question as a cause to the task.  ideally this would only happen for dynamic/revised tasks but for all tasks is ok for now
            if (answer instanceof NALTask)
                ((NALTask) answer).causeMerge(questionTask);
        }


        float str = ansConf * qOrig;
        MetaGoal.Answer.learn(answer.cause(), str, nar.causes);
    }


}

























































