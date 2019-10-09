package nars;


import jcog.Texts;
import jcog.math.FloatAveragedWindow;
import jcog.signal.meter.ExplainedCounter;
import jcog.signal.meter.FastCounter;
import jcog.signal.meter.Meter;
import jcog.signal.meter.Use;
import nars.control.MetaGoal;
import org.HdrHistogram.Histogram;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static jcog.Texts.n4;

/**
 * emotion - internal mental state
 * manages non-logical/meta states of the system
 * and collects/provides information about them to the system
 * variables used to record emotional values
 * <p>
 * TODO cycleCounter, durCounter etc
 */
public class Emotion implements Meter, Consumer<NAR> {

    /**
     * priority rate of Task processing attempted
     */


    private static final int history = 2;
    private static final Field[] EmotionFields = ReflectionUtils.findFields(Emotion.class, (f) -> true, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
            .stream().filter(f-> !Modifier.isPrivate(f.getModifiers())).sorted(Comparator.comparing(Field::getName))
            .toArray(Field[]::new);

    /**
     * TODO
     */
    //public final FastCounter conceptCreate = new FastCounter("concept create");
//    public final Counter conceptCreateFail = new FastCounter("concept create fail");
    public final FastCounter conceptDelete = new FastCounter("concept delete");
    /**
     * perception attempted
     */
    public final FastCounter perceivedTaskStart = new FastCounter("perceived task start");
    /**
     * perception complete
     */
    public final FastCounter perceivedTaskEnd = new FastCounter("perceived task end");
    //public final Counter taskActivation_x100 = new FastCounter("task activation pri sum x100");
    public final FastCounter premiseRun = new FastCounter("premise derivation run");

//    /** amount of remainder TTL from each derivation */
//    public final AtomicHistogram premiseTTL_used = new AtomicHistogram(1, 512, 2);

    /**
     * increment of cycles that a dur loop lags in its scheduling.
     * an indicator of general system lag, especially in real-time operation
     */
    public final FastCounter durLoopLag = new FastCounter("DurLoop lag cycles");
    /**
     * indicates lack of novelty in premise selection
     */
    //public final Counter premiseBurstDuplicate = new FastCounter("premise burst duplicate");

    public final FastCounter premiseUnderivable1 = new FastCounter("premise underivable (prefilter)");
    public final FastCounter premiseUnderivable2 = new FastCounter("premise underivable (truthify)");

    public final FastCounter deriveTask = new FastCounter("derive task");

    public final FastCounter deriveUnified = new FastCounter("derive unified");

    public final ExplainedCounter deriveFailTemporal = new ExplainedCounter("derive fail temporal");
    public final ExplainedCounter deriveFail = new ExplainedCounter("derive fail eval");
    public final FastCounter deriveFailVolLimit = new FastCounter("derive fail vol limit");

    public final FastCounter deriveFailTaskify = new FastCounter("derive fail taskify");
    public final FastCounter deriveFailTaskifyTruthUnderflow = new FastCounter("derive fail taskify truth underflow");

    public final FastCounter deriveFailTaskifyGoalContradiction = new FastCounter("derive fail taskify goal contradiction");
    public final FastCounter deriveFailPrioritize = new FastCounter("derive fail prioritize");
    public final FastCounter deriveFailParentDuplicate = new FastCounter("derive fail parent duplicate");
    public final FastCounter deriveFailDerivationDuplicate = new FastCounter("derive fail derivation duplicate");


    public final Use derive_A_PremiseNew = new Use("derive A");
    public final Use derive_B_PremiseMatch = new Use("derive B");
    public final Use derive_C_Pre =    new Use("derive C");
    public final Use derive_D_Truthify =      new Use("derive D");
    public final Use derive_E_Run =      new Use("derive E");
    public final Use derive_E_Run1_Unify =      new Use("derive unify");
    public final Use derive_E_Run2_Subst =      new Use("derive subst");
    public final Use derive_E_Run3_Taskify =      new Use("derive taskify");
    public final Use derive_E_Run3_Taskify_1_Occurrify =      new Use("derive occurrify");
    public final Use derive_F_Remember =     new Use("derive F");

    /**
     * the indices of this array correspond to the ordinal() value of the MetaGoal enum values
     * TODO convert to AtomicFloatArray or something where each value is volatile
     */
    public final float[] want = new float[MetaGoal.values().length];
    public final FloatAveragedWindow
            busyVol = new FloatAveragedWindow(history, 0.5f, 0f),
            busyVolPriWeighted = new FloatAveragedWindow(history, 0.5f, 0);
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

    @Override
    public String name() {
        return "emotion";
    }

    /**
     * new frame started
     */
    @Override
    public void accept(NAR nar) {
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

        MetaGoal.Perceive.learn(t, pri, nar);

        busy(pri, vol);

        perceivedTaskEnd.increment();
    }

    public void commit(BiConsumer<String, Object> statConsumer) {
        for (Field f : EmotionFields) {
            String fn = f.getName();
            try {
                statConsumer.accept(fn, v(f.get(Emotion.this)));
            } catch (IllegalAccessException e) {
                statConsumer.accept(fn, e.getMessage());
            }
        }
    }

    private static Object v(Object o) {
        if (o instanceof FastCounter)
            return ((FastCounter)o).longValue();
        else if (o instanceof Object[])
            return Arrays.toString((Object[])o);
        else if (o instanceof float[])
            return Texts.n4((float[])o);
        else if (o instanceof FloatAveragedWindow)
            return ((FloatAveragedWindow)o).asFloat();
        else if (o instanceof Histogram) {
            return Texts.histogramString((Histogram)o, true);
        }
        return o;
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

























































