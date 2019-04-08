package nars;


import jcog.TODO;
import jcog.math.FloatAveragedWindow;
import jcog.signal.meter.ExplainedCounter;
import jcog.signal.meter.FastCounter;
import jcog.signal.meter.Meter;
import nars.control.MetaGoal;

import java.io.PrintStream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static jcog.Texts.n4;

/**
 * emotion - internal mental state
 * manages non-logical/meta states of the system
 * and collects/provides information about them to the system
 * variables used to record emotional values
 *
 * TODO cycleCounter, durCounter etc
 */
public class Emotion implements Meter, Consumer<NAR> {

    /**
     * priority rate of Task processing attempted
     */


//    public final Counter conceptCreate = new FastCounter("concept create");
//    public final Counter conceptCreateFail = new FastCounter("concept create fail");
//    public final Counter conceptDelete = new FastCounter("concept delete");


    /** perception attempted */
    public final FastCounter perceivedTaskStart = new FastCounter("perceived task start");

    /** perception complete */
    public final FastCounter perceivedTaskEnd = new FastCounter("perceived task end");

    //public final Counter taskActivation_x100 = new FastCounter("task activation pri sum x100");
    public final FastCounter premiseFire = new FastCounter("premise fire");


    /**
     * indicates lack of novelty in premise selection
     */
    //public final Counter premiseBurstDuplicate = new FastCounter("premise burst duplicate");

    public final FastCounter premiseUnderivable = new FastCounter("premise underivable");
    public final FastCounter premiseUnbudgetable = new FastCounter("premise unbudgetable");

    public final FastCounter deriveTask = new FastCounter("derive task");
    public final FastCounter deriveTermify = new FastCounter("derive termify");
    public final ExplainedCounter deriveFailTemporal = new ExplainedCounter("derive fail temporal");
    public final ExplainedCounter deriveFailEval = new ExplainedCounter("derive fail eval");
    public final FastCounter deriveFailVolLimit = new FastCounter("derive fail vol limit");
    public final FastCounter deriveFailTaskify = new FastCounter("derive fail taskify");
    public final FastCounter deriveFailPrioritize = new FastCounter("derive fail prioritize");
    public final FastCounter deriveFailParentDuplicate = new FastCounter("derive fail parent duplicate");
    public final FastCounter deriveFailDerivationDuplicate = new FastCounter("derive fail derivation duplicate");


    /**
     * the indices of this array correspond to the ordinal() value of the MetaGoal enum values
     * TODO convert to AtomicFloatArray or something where each value is volatile
     */
    public final float[] want = new float[MetaGoal.values().length];

    static final int history = 4;
    public final FloatAveragedWindow
            busyVol = new FloatAveragedWindow(history, 0.5f,0f),
            busyVolPriWeighted = new FloatAveragedWindow(history, 0.5f, 0)
        ;

//    FastCounter busyVol = new FastCounter("busyVol"), busyVolPriWeighted = new FastCounter("busyVolPriWeighted");

    private final NAR nar;

    /**
     * count of errors
     */


    public Emotion(NAR n) {
        super();
        this.nar = n;
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


//    public Runnable getter(MonitorRegistry reg, Supplier<Map<String, Object>> p) {
//        return new PollRunnable(
//                new MonitorRegistryMetricPoller(reg),
//                BasicMetricFilter.MATCH_ALL,
//                new MetricsMapper(name(), clock(), p) {
//                    @Override
//                    protected void update(List<Metric> metrics, Map<String, Object> map) {
//                        super.update(metrics, map);
//                        map.put("emotion", summary());
//                    }
//                }
//        );
//    }

    /**
     * new frame started
     */
    @Override public void accept(NAR nar) {
        busyVol.reset(0);
        busyVolPriWeighted.reset(0);
    }

    @Deprecated
    public void busy(float pri, int vol) {
        //busyPri.accept(Math.round(pri * 1000));
        busyVol.add(vol);
        busyVolPriWeighted.add(vol * pri);
    }


    public String summary() {
        return "busy=" + n4(busyVol.asDouble());
    }


    /**
     * sensory prefilter
     *
     * @param x is a non-command task
     */
    public void perceive(Task t) {

        int vol = t.volume();
        float pri = t.priElseZero();

        short[] cause = t.cause();
        MetaGoal.PerceiveCmplx.learn(cause, ((float)vol) / Param.COMPOUND_VOLUME_MAX, nar.control.why);
        MetaGoal.PerceivePri.learn(cause, pri, nar.control.why);

        busy(pri, vol);

        perceivedTaskEnd.increment();
    }

    public void print(PrintStream out) {
        throw new TODO();
    }

    public void commit(BiConsumer<String,Object> statConsumer) {
        //TODO
    }

//    public void onAnswer(Task questionTask, Task answer) {
//
//
//        if (questionTask == null)
//            return;
//
//        float ansConf = answer.conf();
////        float qOrig = questionTask.originality();
////
////        float qPriBefore = questionTask.priElseZero();
////        if (qPriBefore > ScalarValue.EPSILON) {
////            //float fraction = ansConf * (1 - qOrig);
////            //float fraction = qOrig / 2f;
////            float fraction = 0.5f * (1f - qOrig);
////            answer.take(questionTask, fraction, false,
////                    /*true */ false);
////
////            //HACK append the question as a cause to the task.  ideally this would only happen for dynamic/revised tasks but for all tasks is ok for now
////            if (answer instanceof NALTask)
////                ((NALTask) answer).priCauseMerge(questionTask);
////        }
//
//
//        float str = ansConf;// * qOrig;
//        MetaGoal.Answer.learn(answer.cause(), str, nar.causes);
//    }



}

























































