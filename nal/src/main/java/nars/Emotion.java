package nars;

import com.netflix.servo.Metric;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.StepCounter;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.util.Clock;
import jcog.meter.ExplainedCounter;
import jcog.meter.FastCounter;
import jcog.meter.Meter;
import jcog.meter.MetricsMapper;
import jcog.meter.event.AtomicFloatGuage;
import jcog.pri.Pri;
import nars.control.MetaGoal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static jcog.Texts.n4;
import static jcog.meter.Meter.meter;

/**
 * emotion state: self-felt internal mental states; variables used to record emotional values
 * <p>
 * https://prometheus.io/docs/practices/instrumentation/
 * <p>
 * default impl
 */
public class Emotion implements Meter {

    /**
     * priority rate of Task processing attempted
     */
    public final Counter busyPri = new StepCounter(meter("busyPri"));


    public final Counter conceptFire = new FastCounter(("concept fire"));
    public final Counter taskFire = new FastCounter(("task fire"));
    public final Counter taskActivation_x100 = new FastCounter(("task activation pri sum x100"));
    public final Counter premiseFire = new FastCounter(("premise fire"));
    public final Counter premiseFailMatch = new FastCounter(("premise fail"));
    public final Counter premiseUnderivable = new FastCounter(("premise underivable"));

    public final Counter deriveTask = new FastCounter(("derive task"));
    public final Counter deriveEval = new FastCounter(("derive eval"));
    public final ExplainedCounter deriveFailTemporal = new ExplainedCounter(("derive fail temporal"));
    public final ExplainedCounter deriveFailEval = new ExplainedCounter(("derive fail eval"));
    public final Counter deriveFailVolLimit = new FastCounter(("derive fail vol limit"));
    public final Counter deriveFailTaskify = new FastCounter(("derive fail taskify"));
    public final Counter deriveFailParentDuplicate = new FastCounter(("derive fail parent duplicate"));  //parent copy
    public final Counter deriveFailDerivationDuplicate = new FastCounter(("derive fail derivation duplicate")); //sibling copy

    //public final Counter taskIgnored = new FastCounter(id("task ignored"));


//    /**
//     * setup stage, where substitution is applied to generate a conclusion term from the pattern
//     */
//    public final Counter derivationTry = new FastCounter(id("derivation try"));


//    /** count of times that deriver reached ttl=0. if this is high it means more TTL should
//     * be budgeted for each derivation */
//    public final Counter derivationDeath = new FastCounter(id("derivation death"));


    @NotNull
    public final AtomicFloatGuage busyVol;

//    /**
//     * priority rate of Task processing which affected concepts
//     */
//    @NotNull
//    public final BufferedFloatGuage learnPri, learnVol;

    /**
     * task priority overflow rate
     */
//    public final BufferedFloatGuage stress;
//    public final BufferedFloatGuage confident;

    /**
     * happiness rate
     */
    @NotNull
    public final AtomicFloatGuage happy;
    private final NAR nar;

    float _happy;

    private float termVolMax = 1;


    /**
     * count of errors
     */
//    public final BufferedFloatGuage errrVol;


//    final Map<String,AtomicInteger> counts = new ConcurrentHashMap<>();

    //private transient final Logger logger;

//    /** alertness, % active concepts change per cycle */
//    @NotNull
//    public final FloatGuage alert;


    //final ResourceMeter resourceMeter = new Resource();

    public Emotion(NAR n) {
        super();

        this.nar = n;

        this.happy = new AtomicFloatGuage("happy");

        this.busyVol = new AtomicFloatGuage("busyV");

//        this.learnPri = new BufferedFloatGuage("learnP");
//        this.learnVol = new BufferedFloatGuage("learnV");

        //this.confident = new BufferedFloatGuage("confidence");
        //this.stress = new BufferedFloatGuage("stress");

        //this.alert = new BufferedFloatGuage("alert");

        //this.errrVol = new BufferedFloatGuage("error");

//        if (getClass() == Emotion.class) //HACK
//            registerFields(this);

    }

    @Override
    public String name() {
        return "emotion";
    }

    @Override
    public Clock clock() {
        return nar.time;
    }

    public Runnable getter(MonitorRegistry reg, Supplier<Map<String,Object>> p) {
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

        _happy = happy.getSum();
        happy.commit();

        busyVol.commit();

    }

//    public void clearCounts() {
//        counts.values().forEach(x -> x.set(0));
//    }

//    /**
//     * percentage of business which was not frustration, by aggregate volume
//     */
//    public float learningVol() {
//        double v = busyVol.getSum();
//        if (v > 0)
//            return (float) (learnVol.getSum() / v);
//        return 0;
//    }


//    public float erring() {
//        return errrVol.getSum() / busyVol.getSum();
//    }


//    /** joy = first derivative of happiness, delta happiness / delta business */
//    public float joy() {
//        double b = busyPri.getSum();
//        if (b == 0)
//            return 0;
//        return (float)(happy() / b);
//    }

    public float happy() {
        return _happy;
    }

//    public void print(@NotNull OutputStream output) {
//        final FSTConfiguration conf = FSTConfiguration.createJsonConfiguration(true,false);
//        try {
//            conf.encodeToStream(output, this);
//        } catch (IOException e) {
//            try {
//                output.write(e.toString().getBytes());
//            } catch (IOException e1) {            }
//        }
//    }

//    @Override
//    public String toString() {
//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        PrintStream ps = new PrintStream(os);
//        print(ps);
//        try {
//            return os.toString("UTF8");
//        } catch (UnsupportedEncodingException e) {
//            return e.toString();
//        }
//    }

    //    @Override
//    public final void onFrame() {
//        commitHappy();
//
//        commitBusy();
//    }

//    public float happy() {
//        return (float)happyMeter.get();
//    }
//
//    public float busy() {
//        return (float)busyMeter.get();
//    }


    //TODO use Meter subclass that will accept and transform these float parameters

    @Deprecated
    public void happy(float increment) {
        happy.accept(increment);
    }

    @Deprecated
    public void busy(float pri, int vol) {
        busyPri.increment(Math.round(pri * 1000));
        busyVol.accept(vol);
    }


//    public final void stress(@NotNull MutableFloat pri) {
//        float v = pri.floatValue();
//        if (v > 0)
//            stress.accept(v);
//    }

//    @Deprecated
//    public void learn(float pri, int vol) {
//
//        learnPri.accept(pri);
//        learnVol.accept(vol);
//
//    }

//    public void confident(float deltaConf, @NotNull Compound term) {
//        confident.accept(deltaConf);
//    }

//    @Deprecated public void alert(float percentFocusChange) {
//        alert.accept( percentFocusChange );
//    }

//    public void eror(int vol) {
//        errrVol.accept(vol);
//    }

//    public double happysad() {
//
//        return happy.getSum() + sad.getSum();
//    }

    public String summary() {
        //long now = nar.time();

        StringBuilder sb = new StringBuilder()
                .append(" hapy=").append(n4(happy()))
                .append(" busy=").append(n4(busyVol.getSum()))
//                .append(" lern=").append(n4(learningVol()))
//                .append(" errr=").append(n4(erring()))
//                .append(" strs=").append(n4(stress.getSum()))
                //.append(" cpu=").append(resourceMeter.CYCLE_CPU_TIME)
                //.append(" mem=").append(resourceMeter.CYCLE_RAM_USED)
                //.append(" alrt=").append(n4(alert.getSum()))
                ;

//        counts.forEach((k,v) -> {
//            sb.append(' ').append(k).append('=').append(v.get());
//        });
        return sb.toString();


//                 + "rwrd=[" +
//                     n4( sad.beliefs().truth(now).motivation() )
//                             + "," +
//                     n4( happy.beliefs().truth(now).motivation() )
//                 + "] "


//                + "," + dRewardPos.belief(nar.time()) +
//                "," + dRewardNeg.belief(nar.time());

    }



    /**
     * sensory prefilter
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
        float cost = (1f + (vol / termVolMax) + pri)/3f;

        MetaGoal.learn(MetaGoal.Perceive, t.cause(), cost, nar);

        busy(pri, (int) Math.ceil(vol ));
    }

    /** effective activation percentage */
    public void onActivate(Task t, float activation) {
        taskFire.increment();
        taskActivation_x100.increment(Math.round(activation*100));
    }

    public void onAnswer(Task questionTask, @Nullable Task answer) {
        //transfer budget from question to answer
        //transfer more of the budget from an unoriginal question to an answer than an original question
        //Task questionTask = questionLink.get();
        if (questionTask == null)
            return;

        float qOrig = questionTask.originality();
        float ansConf = answer.conf();

        float qPriBefore = questionTask.priElseZero();
        if (qPriBefore > Pri.EPSILON) {
            float costFraction = ansConf * (1 - qOrig);
            answer.take(questionTask, costFraction, false, false);
//            questionLink.priMult(1f - costFraction);
        }

        //reward answer for answering the question
        float str = ansConf * qOrig;
        MetaGoal.learn(MetaGoal.Answer, answer.cause(), str, nar);
    }



//    /** float to long at the default conversion precision */
//    private static long f2l(float f) {
//        return (long)(f * 1000f); //0.001 precision
//    }
//    /** float to long at the default conversion precision */
//    private static float l2f(long l) {
//        return l / 1000f; //0.001 precision
//    }


//    public void happy(float solution, @NotNull Task task) {
//        happy += ( task.getBudget().summary() * solution );
//    }

//    protected void commitHappy() {
//
//
////        if (lasthappy != -1) {
////            //float frequency = changeSignificance(lasthappy, happy, Global.HAPPY_EVENT_CHANGE_THRESHOLD);
//////            if (happy > Global.HAPPY_EVENT_HIGHER_THRESHOLD && lasthappy <= Global.HAPPY_EVENT_HIGHER_THRESHOLD) {
//////                frequency = 1.0f;
//////            }
//////            if (happy < Global.HAPPY_EVENT_LOWER_THRESHOLD && lasthappy >= Global.HAPPY_EVENT_LOWER_THRESHOLD) {
//////                frequency = 0.0f;
//////            }
////
//////            if ((frequency != -1) && (memory.nal(7))) { //ok lets add an event now
//////
//////                Inheritance inh = Inheritance.make(memory.self(), satisfiedSetInt);
//////
//////                memory.input(
//////                        TaskSeed.make(memory, inh).judgment()
//////                                .truth(frequency, Global.DEFAULT_JUDGMENT_CONFIDENCE)
//////                                .occurrNow()
//////                                .budget(Global.DEFAULT_JUDGMENT_PRIORITY, Global.DEFAULT_JUDGMENT_DURABILITY)
//////                                .reason("Happy Metabelief")
//////                );
//////
//////                if (Global.REFLECT_META_HAPPY_GOAL) { //remind on the goal whenever happyness changes, should suffice for now
//////
//////                    //TODO convert to fluent format
//////
//////                    memory.input(
//////                            TaskSeed.make(memory, inh).goal()
//////                                    .truth(frequency, Global.DEFAULT_GOAL_CONFIDENCE)
//////                                    .occurrNow()
//////                                    .budget(Global.DEFAULT_GOAL_PRIORITY, Global.DEFAULT_GOAL_DURABILITY)
//////                                    .reason("Happy Metagoal")
//////                    );
//////
//////                    //this is a good candidate for innate belief for consider and remind:
//////
//////                    if (InternalExperience.enabled && Global.CONSIDER_REMIND) {
//////                        Operation op_consider = Operation.op(Product.only(inh), consider.consider);
//////                        Operation op_remind = Operation.op(Product.only(inh), remind.remind);
//////
//////                        //order important because usually reminding something
//////                        //means it has good chance to be considered after
//////                        for (Operation o : new Operation[]{op_remind, op_consider}) {
//////
//////                            memory.input(
//////                                    TaskSeed.make(memory, o).judgment()
//////                                            .occurrNow()
//////                                            .truth(1.0f, Global.DEFAULT_JUDGMENT_CONFIDENCE)
//////                                            .budget(Global.DEFAULT_JUDGMENT_PRIORITY * InternalExperience.INTERNAL_EXPERIENCE_PRIORITY_MUL,
//////                                                    Global.DEFAULT_JUDGMENT_DURABILITY * InternalExperience.INTERNAL_EXPERIENCE_DURABILITY_MUL)
//////                                            .reason("Happy Remind/Consider")
//////                            );
//////                        }
//////                    }
//////                }
//////            }
//////        }
////        }
//
//        happyMeter.set(happy);
//
//        /*if (happy > 0)
//            happy *= happinessFade;*/
//    }

//    /** @return -1 if no significant change, 0 if decreased, 1 if increased */
//    private static float changeSignificance(float prev, float current, float proportionChangeThreshold) {
//        float range = Math.max(prev, current);
//        if (range == 0) return -1;
//        if (prev - current > range * proportionChangeThreshold)
//            return -1;
//        if (current - prev > range * proportionChangeThreshold)
//            return 1;
//
//        return -1;
//    }


//    protected void commitBusy() {
//
//        if (lastbusy != -1) {
//            //float frequency = -1;
//            //float frequency = changeSignificance(lastbusy, busy, Global.BUSY_EVENT_CHANGE_THRESHOLD);
//            //            if (busy > Global.BUSY_EVENT_HIGHER_THRESHOLD && lastbusy <= Global.BUSY_EVENT_HIGHER_THRESHOLD) {
////                frequency = 1.0f;
////            }
////            if (busy < Global.BUSY_EVENT_LOWER_THRESHOLD && lastbusy >= Global.BUSY_EVENT_LOWER_THRESHOLD) {
////                frequency = 0.0f;
////            }
//
//
//            /*
//            if (Global.REFLECT_META_BUSY_BELIEF && (frequency != -1) && (memory.nal(7))) { //ok lets add an event now
//                final Inheritance busyTerm = Inheritance.make(memory.self(), BUSYness);
//
//                memory.input(
//                        TaskSeed.make(memory, busyTerm).judgment()
//                                .truth(frequency, Global.DEFAULT_JUDGMENT_CONFIDENCE)
//                                .occurrNow()
//                                .budget(Global.DEFAULT_JUDGMENT_PRIORITY, Global.DEFAULT_JUDGMENT_DURABILITY)
//                                .reason("Busy")
//                );
//            }
//            */
//        }
//
//        busyMeter.set(lastbusy = busy);
//
//        busy = 0;
//
//
//    }
//
//    public void clear() {
//        busy = 0;
//        happy = 0;
//    }
}
//
///**
// * value-reinforceing emotion implementation
// * use:         n.setEmotion(new Emotivation(n));
// */
//public class Emotivation extends Emotion {
//
//    private final NAR nar;
//
//    public final LongGauge cycleDT = new LongGauge(id("cycle time"));
//    public final DescriptiveStatistics cycleDTReal = new DescriptiveStatistics(4 /* cycles */); //realtime
//
//    public final BasicGauge<Float> cycleDTRealMean = new BasicGauge<>(id("cycle time real mean"), () -> (float) cycleDTReal.getMean());
//    public final BasicGauge<Float> cycleDTRealVary = new BasicGauge<>(id("cycle time real vary"), () -> (float) cycleDTReal.getVariance());
//
//    long lastCycleTime, lastRealTime;
//
//    public Emotivation(NAR n) {
//        super(n);
//        this.nar = n;
//        lastCycleTime = n.time();
//        lastRealTime = System.currentTimeMillis();
//
////        final StatsConfig statsConfig = new StatsConfig.Builder()
////                .withSampleSize(100)
////                //.withPercentiles(percentiles)
////                .withComputeFrequencyMillis(2000)
////                .withPublishTotal(false)
////                .withPublishCount(false)
////                .withPublishMean(true)
////                .withPublishVariance(true)
////                //.withPublishStdDev(true)
////                .build();
////        cycleDTReal = new StatsTimer(id("cycle time real"), statsConfig);
//
//        if (getClass()==Emotivation.class) //HACK
//            registerFields(this);
//    }
//
//
//    @Override
//    public void cycle() {
//        long deltaSinceLastCycle = -(lastCycleTime - (lastCycleTime = nar.time()));
//        long deltaRealtimeSinceLastCycle = -(this.lastRealTime - (this.lastRealTime = System.currentTimeMillis()));
//        cycleDT.set(deltaSinceLastCycle);
//        cycleDTReal.addValue(deltaRealtimeSinceLastCycle/1000.0);
//
//        super.cycle();
//    }
//
//
////    public static float preferConfidentAndRelevant(@NotNull Task t, float activation, long when, NAR n) {
////        return 0.001f * activation * (t.isBeliefOrGoal() ? t.conf(when, n.dur()) : 0.5f);
////    }
//
//}
//
