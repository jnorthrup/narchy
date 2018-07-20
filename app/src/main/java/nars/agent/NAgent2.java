package nars.agent;

import jcog.Util;
import jcog.WTF;
import jcog.event.*;
import jcog.exe.Loop;
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
import nars.control.NARService;
import nars.control.channel.CauseChannel;
import nars.sensor.Bitmap2DSensor;
import nars.table.DefaultBeliefTable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.time.clock.RealTime;
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
public class NAgent2 extends NARService implements NSense, NAct {


    final Topic<NAR> eventFrame = new ListTopic();


    public static final Logger logger = LoggerFactory.getLogger(NAgent2.class);


    private final Map<Signal, CauseChannel<ITask>> sensors = new LinkedHashMap();

    @Deprecated
    private final Set<DigitizedScalar> senseNums = new LinkedHashSet<>();
    @Deprecated
    private final Set<Bitmap2DSensor<?>> sensorCam = new LinkedHashSet<>();

    private final Map<ActionConcept, CauseChannel<ITask>> actions = new LinkedHashMap();

    public final FrameTrigger frameTrigger;

    public FloatRange curiosity = new FloatRange(0.10f, 0f, 1f);
    public FloatRange motivation = new FloatRange(1f, 0, 2f);

    public final AtomicBoolean enabled = new AtomicBoolean(false);


    public boolean trace;


    @Deprecated
    private CauseChannel<ITask> in = null;

    @Deprecated
    public final List<Supplier<Task>> always = $.newArrayList();




    private volatile long last;


    abstract public static class FrameTrigger  {

        private volatile Off on = null;

        abstract protected Off install(NAgent2 a);

        public void start(NAgent2 a) {
            synchronized (this) {
                assert(on == null);
                on = install(a);
            }
        }

        public void stop() {
            synchronized (this) {
                if (on != null) {
                    on.off();
                    on = null;
                }
            }
        }

        /** measured in realtime */
        public static class FPS extends FrameTrigger {

            private transient final float initialFPS;

            public final Loop loop;
            private NAgent2 agent = null;

            public FPS(float fps) {
                this.initialFPS = fps;
                loop = new Loop(-1) {
                    @Override public boolean next() {
                        agent.frame();
                        return true;
                    }
                };
            }

            @Override protected Off install(NAgent2 a) {
                if (!(a.nar.time instanceof RealTime))
                    throw new UnsupportedOperationException("realtime clock required");

                agent = a;

                loop.setFPS(initialFPS);

                return () -> loop.stop();
            }
        }

        /** measured in # of perceptual durations */
        public static class Durs extends FrameTrigger {

            private transient final float initialDurs;

            public DurService loop = null;

            public Durs(float initialDurs) {
                this.initialDurs = initialDurs;
            }
            @Override protected Off install(NAgent2 a) {
                loop = DurService.on(a.nar, a::frame);
                loop.durs(initialDurs);
                return loop;
            }
        }

        public static FPS fps(float fps) { return new FPS(fps); }
        public static Durs durs(float durs) { return new Durs(durs); }
    }



    public NAgent2(String id, FrameTrigger frameTrigger, NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), frameTrigger, nar);
    }

    public NAgent2(Term id, FrameTrigger frameTrigger, NAR nar) {
        super(id);
        this.nar = nar;
        this.frameTrigger = frameTrigger;
        last = nar.time();
    }


    @Override
    public void addSensor(Signal s, CauseChannel cause) {
        CauseChannel<ITask> existing = sensors.put(s, cause);
        assert (existing == null || existing == cause);
    }

    public final Bitmap2DSensor sense(Bitmap2DSensor bmp) {
        sensorCam.add(bmp);
        onFrame(bmp::input); //TODO support adaptive input mode
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


        //Term id = (this.id == null) ? nar.self() : this.id;

        this.last = nar.time();


        this.in = nar.newChannel(this);


        actions.keySet().forEach(a -> {
            alwaysQuest(a, true);
            //alwaysQuestion(Op.CONJ.the(happy.term, a.term));
            //alwaysQuestion(Op.CONJ.the(happy.term, a.term.neg()));
        });

        super.starting(nar);


        this.frameTrigger.start(this);

        enabled.set(true);
    }


    @Override
    protected void stopping(NAR nar) {

        enabled.set(false);

        if (frameTrigger!=null) {
            frameTrigger.stop();
        } else {
            throw new WTF(this + " stopped twice");
        }

        super.stopping(nar);
    }


    protected void always(float activation) {
        in.input(always.stream().map(Supplier::get).peek(x -> {
            x.pri(
                    activation * nar.priDefault(x.punc())
            );
        }));
    }

    public abstract static class Reward implements Runnable {

        protected final NAgent2 agent;
        private final FloatSupplier rewardFunc;
        volatile float reward = Float.NaN;

        public Reward(NAgent2 a, FloatSupplier r) {
            this.agent = a;
            this.rewardFunc = r;
        }

        public NAR nar() { return agent.nar(); }

        @Override
        public void run() {
            reward = rewardFunc.asFloat();
        }

    }

    public static class SimpleReward extends Reward {

        public final Signal concept;
        private final FloatFloatToObjectFunction<Truth> truther;

        public SimpleReward(Term id, FloatSupplier r, NAgent2 a) {
            super(a, r);
            concept = new Signal(id, new FloatNormalized(() -> reward, -1, +1, true), a.nar);
            truther = ((prev, next) -> next == next ? $.t(next, nar().confDefault(BELIEF)) : null);
            agent.alwaysWantEternally(concept.term, nar().confDefault(GOAL));
        }

        @Override
        public void run() {
            super.run();
            concept.update(agent.last, agent.now(), truther, nar());
        }
    }

    public static class DetailedReward extends Reward {

        public final FilteredScalar concept;

        public DetailedReward(Term id, FloatSupplier r, NAgent2 a) {
            super(a, r);

            NAR nar = a.nar;

            concept = new FilteredScalar( () -> reward,

                    //(prev,next) -> next==next ? $.t(Util.unitize(next), Math.max(nar.confMin.floatValue(),  Math.abs(next-0.5f)*2f * nar.confDefault(BELIEF))) : null,
                    (prev, next) -> next == next ? $.t(Util.unitize(next), nar.confDefault(BELIEF)) : null,

                    nar,

                    pair(id,
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
                 //TODO add these to On/Off
                agent.alwaysWantEternally(concept.filter[0].term, nar.confDefault(GOAL));
                agent.alwaysWantEternally(concept.filter[1].term, nar.confDefault(GOAL) /* * 0.5f */); //chronic
                agent.alwaysWantEternally(concept.filter[2].term, nar.confDefault(GOAL) * 0.5f); //acute
                for (FilteredScalar.Filter x : concept.filter) {
                    ((DefaultBeliefTable) x.beliefs()).eternal.setCapacity(0); //HACK this should be an Empty table

                    //should normally be able to create these beliefs but if you want to filter more broadly:
                    //((DefaultBeliefTable)x.goals()).temporal.setCapacity(0); //HACK this should be an Empty table

                }
            }

        }

        @Override
        public void run() {
            super.run();

            NAR nar = nar();

            concept.update(agent.last, agent.now(), nar);

//            Truth happynowT = nar.beliefTruth(concept, last, now);
//            float happynow = happynowT != null ? (happynowT.freq() - 0.5f) * 2f : 0;
//            nar.emotion.happy(/* motivation.floatValue() * */ dexterity(last, now) * happynow /* /nar.confDefault(GOAL) */);

        }
    }



    public Off reward(FloatSupplier rewardfunc) {
        return reward($.func("reward", id), rewardfunc);
    }

    /** set a default reward supplier */
    public Off reward(Term reward, FloatSupplier rewardfunc) {
        return reward(new SimpleReward(reward, rewardfunc, this));
    }

    @Deprecated public Off rewardDetailed(Term reward, FloatSupplier rewardfunc) {
        DetailedReward r = new DetailedReward(reward, rewardfunc, this);
        return reward(r);
    }

    /** default reward module */
    public Off reward(Reward r) {
        return new Ons( onFrame((Runnable)r) );
    }


    /** runs a frame */
    protected void frame() {
        if (!enabled.getOpaque())
            return;

        long now = nar.time();
        long last = this.last;
        this.last = now;
        if (now <= last)
            return;

        eventFrame.emit(nar);


        FloatFloatToObjectFunction<Truth> truther = (prev, next) -> $.t(Util.unitize(next), nar.confDefault(BELIEF));
        sensors.forEach((key, value) -> value.input(key.update(last, now, truther, nar)));


        always(motivation.floatValue());


        Map.Entry<ActionConcept, CauseChannel<ITask>>[] aa = actions.entrySet().toArray(new Map.Entry[actions.size()]);
        ArrayUtils.shuffle(aa, random());
        for (Map.Entry<ActionConcept, CauseChannel<ITask>> ac : aa) {

            ActionConcept acc = ac.getKey();

            //HACK temporary
            if (acc instanceof GoalActionConcept)
                ((GoalActionConcept) acc).curiosity(curiosity.get());

            Stream<ITask> s = acc.update(last, now, nar);
            if (s != null)
                ac.getValue().input(s);
        }


        if (trace)
            logger.info(summary());
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
