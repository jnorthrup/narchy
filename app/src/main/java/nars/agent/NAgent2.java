package nars.agent;

import jcog.Util;
import jcog.event.*;
import jcog.math.*;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.action.ActionConcept;
import nars.concept.action.GoalActionConcept;
import nars.concept.sensor.DigitizedScalar;
import nars.concept.sensor.FilteredScalar;
import nars.concept.sensor.Signal;
import nars.control.DurService;
import nars.control.channel.CauseChannel;
import nars.sensor.Bitmap2DSensor;
import nars.table.DefaultBeliefTable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.truth.Truth;
import nars.util.TimeAware;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static jcog.Util.compose;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * an integration of sensor concepts and motor functions
 */
public class NAgent2 extends DurService implements NSense, NAct {


    final Topic<NAR> eventFrame = new ListTopic();


    public static final Logger logger = LoggerFactory.getLogger(NAgent2.class);


    private final Map<Signal, CauseChannel<ITask>> sensors = new LinkedHashMap();

    @Deprecated
    private final Set<DigitizedScalar> senseNums = new LinkedHashSet<>();
    @Deprecated
    private final Set<Bitmap2DSensor<?>> sensorCam = new LinkedHashSet<>();

    private final Map<ActionConcept, CauseChannel<ITask>> actions = new LinkedHashMap();


    public FloatRange curiosity = new FloatRange(0.10f, 0f, 1f);
    public FloatRange motivation = new FloatRange(1f, 0, 2f);

    public final AtomicBoolean enabled = new AtomicBoolean(false);


    public boolean trace;


    @Deprecated
    private CauseChannel<ITask> in = null;

    @Deprecated
    public final List<Supplier<Task>> always = $.newArrayList();

    /**
     * non-null if an independent loop process has started
     */

    public int sensorLag;

    private volatile long last;


    protected NAgent2(NAR nar) {
        this("", nar);
    }

    protected NAgent2(String id, NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), nar);
    }

    @Deprecated
    protected NAgent2(Term id, NAR nar) {
        super(id);
        this.nar = nar;


//        if (nar!=null) {
//            nar.on(this);
//        }
    }


    @Override
    public void addSensor(Signal s, CauseChannel cause) {
        CauseChannel<ITask> existing = sensors.put(s, cause);
        assert (existing == null || existing == cause);
    }

    public final Bitmap2DSensor sense(Bitmap2DSensor bmp) {
        sensorCam.add(bmp);
        return bmp;
    }

    public Task alwaysWantEternally(Termed x, float conf) {
        Task t = new NALTask(x.term(), GOAL, $.t(1f, conf), now(),
                ETERNAL, ETERNAL,
                nar.evidence()
                //Stamp.UNSTAMPED

        );

        always.add(() -> t);
        return t;
    }

    public void alwaysWant(Termed x, float confFactor) {
        //long[] evidenceShared = nar.evidence();

        always.add(() -> {
            long now = Tense.dither(this.now(), nar);
            long next = Tense.dither(this.now() + nar.dur(), nar);
            return new NALTask(x.term(), GOAL, $.t(1f, confFactor * nar.confDefault(GOAL)), now,
                    now, next,
                    //evidenceShared
                    nar.evidence()
                    //Stamp.UNSTAMPED

            );
        });

    }

    public void alwaysQuestion(Termed x, boolean stamped) {
        alwaysQuestion(x, true, stamped);
    }

    public void alwaysQuest(Termed x, boolean stamped) {
        alwaysQuestion(x, false, stamped);
    }

    private void alwaysQuestion(Termed x, boolean questionOrQuest, boolean stamped) {

//        always.add(() -> {
//
//            long now = Tense.dither(this.now(), nar);
//            long next = Tense.dither(this.now() + nar.dur(), nar);
//
//            long[] stamp = stamped ? nar.evidence() : Stamp.UNSTAMPED;
//
//            return new NALTask(x.term(), questionOrQuest ? QUESTION : QUEST, null, now,
//                    now, next,
//                    stamp
//            )/* {
//                @Override
//                public boolean isInput() {
//                    return false;
//                }
//            }*/;
//        });

        NALTask etq = new NALTask(x.term(), questionOrQuest ? QUESTION : QUEST, null, nar.time(),
                ETERNAL, ETERNAL,
                //evidenceShared
                nar.evidence()
                //Stamp.UNSTAMPED

        );
        always.add(() -> etq);
    }


//    /** creates a new loop to run this */
//    public Loop startFPS(float fps) {
//        synchronized (this) {
//            if (this.loop == null) {
//                return this.loop = new Loop(fps) {
//                    @Override
//                    public boolean next() {
//                        NAgent.this.run();
//                        return true;
//                    }
//                };
//            } else {
//                throw new RuntimeException("already started: " + loop);
//            }
//        }
//    }

    @Override
    public FloatRange curiosity() {
        return curiosity;
    }


    @Override
    public final Map<ActionConcept, CauseChannel<ITask>> actions() {
        return actions;
    }

    @Override
    public final NAR nar() {
        return nar;
    }


    public Random random() {
        TimeAware timeAware = this.nar;
        return timeAware != null ? timeAware.random() : ThreadLocalRandom.current();
    }


    public String summary() {


        return id +
                " dex=" + /*n4*/(dexterity(now(), now())) +

                /*" var=" + n4(varPct(nar)) + */ "\t" + nar.concepts.summary() + " " +
                nar.emotion.summary();
    }

    /**
     * registers sensor, action, and reward concepts with the NAR
     * TODO call this in the constructor
     */
    @Override
    protected void starting(NAR nar) {


        Term id = (this.id == null) ? nar.self() : this.id;

        this.last = nar.time();


        this.in = nar.newChannel(this);


        init(nar);


        actions.keySet().forEach(a -> {
            alwaysQuest(a, true);
            //alwaysQuestion(Op.CONJ.the(happy.term, a.term));
            //alwaysQuestion(Op.CONJ.the(happy.term, a.term.neg()));
        });


        super.starting(nar);

        enabled.set(true);
    }

    protected void init(NAR nar) {


    }

    @Override
    protected void stopping(NAR nar) {
        enabled.set(false);

        super.stopping(nar);
    }


    protected void always(float activation) {
        in.input(always.stream().map(x -> x.get()).peek(x -> {
            x.pri(
                    activation * nar.priDefault(x.punc())
            );
        }));
    }

    public Off reward(FloatSupplier rewardfunc) {
        return new Ons(
                onFrame(new Runnable() {

                    volatile float reward = Float.NaN;

                    final FilteredScalar happy = new FilteredScalar(
                            new FloatCached(() -> reward, nar::time),

                            //(prev,next) -> next==next ? $.t(Util.unitize(next), Math.max(nar.confMin.floatValue(),  Math.abs(next-0.5f)*2f * nar.confDefault(BELIEF))) : null,
                            (prev, next) -> next == next ? $.t(Util.unitize(next), nar.confDefault(BELIEF)) : null,

                            nar,

                            pair(id, ///$.inh(id, "happy"),
                                    new FloatNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE)),


                            pair($.func("chronic", id), compose(
                                    new FloatNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE),
                                    new FloatExpMovingAverage(0.02f)
                            )),


                            pair($.func("acute", id), compose(
                                    new FloatExpMovingAverage(0.1f, false),
                                    new FloatPolarNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE_FAST)
                            ))
                    );

                    {
                        alwaysWantEternally(happy.filter[0].term, nar.confDefault(GOAL));
                        alwaysWantEternally(happy.filter[1].term, nar.confDefault(GOAL) /* * 0.5f */); //chronic
                        alwaysWantEternally(happy.filter[2].term, nar.confDefault(GOAL) * 0.5f); //acute
                        for (FilteredScalar.Filter x : happy.filter) {
                            ((DefaultBeliefTable) x.beliefs()).eternal.setCapacity(0); //HACK this should be an Empty table

                            //should normally be able to create these beliefs but if you want to filter more broadly:
                            //((DefaultBeliefTable)x.goals()).temporal.setCapacity(0); //HACK this should be an Empty table

                        }
                    }

                    @Override
                    public void run() {
                        long now = nar.time();

                        reward = rewardfunc.asFloat();

                        happy.update(last, now, sensorLag, nar);

                        Truth happynowT = nar.beliefTruth(happy, last, now);
                        float happynow = happynowT != null ? (happynowT.freq() - 0.5f) * 2f : 0;
                        nar.emotion.happy(/* motivation.floatValue() * */ dexterity(last, now) * happynow /* /nar.confDefault(GOAL) */);
                    }
                })
        ) /* { off... } */;
    }


    @Override
    protected void run(NAR n, long dt) {
        if (!enabled.getOpaque())
            return;

        long now = nar.time();
        long last = this.last;
        this.last = now;
        if (now <= last)
            return;

        this.sensorLag = Math.max(nar.dur(), (int) (now - last));

        eventFrame.emit(nar);


        FloatFloatToObjectFunction<Truth> truther = (prev, next) -> $.t(Util.unitize(next), nar.confDefault(BELIEF));
        sensors.forEach((key, value) -> value.input(key.update(last, now, truther, sensorLag, nar)));


        always(motivation.floatValue());


        Map.Entry<ActionConcept, CauseChannel<ITask>>[] aa = actions.entrySet().toArray(new Map.Entry[actions.size()]);
        ArrayUtils.shuffle(aa, random());
        for (Map.Entry<ActionConcept, CauseChannel<ITask>> ac : aa) {

            ActionConcept acc = ac.getKey();

            //HACK temporary
            if (acc instanceof GoalActionConcept)
                ((GoalActionConcept) acc).curiosity(curiosity.get());

            Stream<ITask> s = acc.update(last, now, sensorLag, nar);
            if (s != null)
                ac.getValue().input(s);
        }


        if (trace)
            logger.info(summary());
    }



    /**
     * default rate = 1 dur/ 1 frame
     */
    @Deprecated
    public void runSynch(int frames) {
//        DurService d = DurService.on(nar, this);
        nar.run(frames * nar.dur() + 1);
//        d.off();
    }

    public long now() {
        return nar.time();
    }




    public float dexterity() {
        return dexterity(nar.time());
    }

    public float dexterity(long when) {
        return dexterity(when, when);
    }

    /**
     * average confidence of actions
     * see: http:
     */
    public float dexterity(long start, long end) {
        int n = actions.size();
        if (n == 0)
            return 0;

        final float[] m = {0};
        actions.keySet().forEach(a -> {
            Truth g = nar.goalTruth(a, start, end);
            float c;
            if (g != null) {

                c = g.conf();
            } else {
                c = 0;
            }
            m[0] += c;
        });

        return m[0] > 0 ? m[0] / n /* avg */ : 0;
    }


    public On onFrame(Consumer/*<NAR>*/ each) {
        if (each instanceof DigitizedScalar) {
            senseNums.add((DigitizedScalar) each);
        }
        return eventFrame.on(each);
    }

    public On onFrame(Runnable each) {
        //return DurService.on(nar, ()->{ if (enabled.get()) each.run(); });
        return eventFrame.on((x) -> each.run());
    }


}
