package nars.agent;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.list.FastCoWList;
import jcog.event.ListTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.math.FloatClamped;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.util.ArrayUtils;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.attention.AttNode;
import nars.concept.Concept;
import nars.concept.action.AbstractGoalActionConcept;
import nars.concept.action.ActionConcept;
import nars.concept.action.curiosity.Curiosity;
import nars.concept.action.curiosity.DefaultCuriosity;
import nars.concept.sensor.Sensor;
import nars.concept.sensor.Signal;
import nars.concept.sensor.VectorSensor;
import nars.control.NARService;
import nars.control.channel.CauseChannel;
import nars.link.Activate;
import nars.task.ITask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;

/**
 * an integration of sensor concepts and motor functions
 */
public class NAgent extends NARService implements NSense, NAct {

    private final Topic<NAR> eventFrame = new ListTopic();

    private static final Logger logger = LoggerFactory.getLogger(NAgent.class);

    public final FrameTrigger frameTrigger;

    public final FloatRange pri = new FloatRange(1f, 0, 32f);

    public final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean busy = new AtomicBoolean(false);

    public final AtomicBoolean trace = new AtomicBoolean(false);

    public final FastCoWList<Sensor> sensors = new FastCoWList<>(Sensor[]::new);

    public final FastCoWList<ActionConcept> actions = new FastCoWList<>(ActionConcept[]::new);

    public final FastCoWList<Reward> rewards = new FastCoWList<>(Reward[]::new);

    public final AtomicInteger iteration = new AtomicInteger(0);

    public final Curiosity curiosity = DefaultCuriosity.defaultCuriosity(this);

    public final AttNode attn;
    public final AttNode attnReward, attnAction, attnSensor;

    public volatile long prev;
    protected volatile long now;
    public volatile long next;

    @FunctionalInterface
    public interface ReinforcedTask {
        @Nullable Task get(long prev, long now, long next);
    }

//    @Deprecated private final FastCoWList<ReinforcedTask> always = new FastCoWList<>(ReinforcedTask[]::new);

    @Deprecated
    private CauseChannel<ITask> in = null;

    private final NAgentCycle cycle =
            //Cycles.Biphasic;
            Cycles.Interleaved;


    public NAgent(String id, NAR nar) {
        this(id, FrameTrigger.durs(1), nar);
    }

    public NAgent(String id, FrameTrigger frameTrigger, NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), frameTrigger, nar);
    }

    public NAgent(Term id, FrameTrigger frameTrigger, NAR nar) {
        super(id);
        this.nar = nar;

        this.attn = new AttNode(id);
        this.attnAction = new AttNode($.func("action", id) );
        attnAction.parent(attn);
        this.attnSensor = new AttNode($.func("sensor", id) );
        attnSensor.parent(attn);
        this.attnReward = new AttNode($.func("reward", id) );
        attnReward.parent(attn);

        //TEMPORARY assumptions
        attnAction.boost.set(nar.priDefault(GOAL));
        attnReward.boost.set(nar.priDefault(GOAL));
        attnSensor.boost.set(nar.priDefault(BELIEF));



        this.frameTrigger = frameTrigger;
        this.prev = ETERNAL;
//
//        actReward = new AttnDistributor(
//                Iterables.concat(
//                    //() -> Iterators.singletonIterator(nar.conceptualize(term())),
//                    Iterables.concat(actions,
//                    Iterables.concat(rewards))),
//            (_p)->{
//                //float p = 0.5f + _p/2f; //HACK
//                float p = _p;
//                actions.forEach(a->a.pri.pri(p));
////                rewards.forEach(r->r.pri(p));
////                pri.set(p);
//
//                rewards.forEach(r->r.pri(pri.floatValue()));
//        }, nar);

        nar.on(this);
    }


    @Override
    public final <A extends ActionConcept> A addAction(A c) {
        actions.add(c);

        nar().on(c);
        addAttention(attnAction, c);
        return c;
    }

//    protected <A extends ActionConcept> void actionAdded(A a) {
//
//        //alwaysQuest(a, true);
//        //alwaysQuestionEternally(Op.IMPL.the($.varQuery(1), a.term), true, false);
//
////        alwaysQuestionEternally(a,
////                false,
////                false
////        );
//
//
////        alwaysQuestion(IMPL.the(c.term, 0, $$("reward:#x")), true);
////        alwaysQuestion(IMPL.the(c.term.neg(), 0, $$("reward:#x")), true);
//        //alwaysQuestionEternally(Op.CONJ.the($.varDep(1), a.term.neg()));
//    }

    @Override
    public final <S extends Sensor> S addSensor(S s) {
        //TODO check for existing
        sensors.add(s);
        addAttention(attnSensor, s);
        return s;
    }

    static protected void addAttention(AttNode target, Object s) {
        if (s instanceof VectorSensor) {
            ((VectorSensor)s).attn.parent(target);
        } else if (s instanceof Signal) {
            ((Signal)s).attn.parent(target);
        } else if (s instanceof Reward) {
            ((Reward)s).attn.parent(target);
        } else if (s instanceof ActionConcept) {
            ((ActionConcept)s).attn.parent(target);
        } else if (s instanceof AttNode)
            ((AttNode)s).parent(target);
        else
            throw new TODO();
    }

//    public final Bitmap2DSensor sense(Bitmap2DSensor bmp) {
//        return addCamera(bmp);
////        addSensor(bmp);
////        onFrame(bmp::input); //TODO support adaptive input mode
////        return bmp;
//    }
//
//    @Deprecated public Task alwaysWantEternally(Termed x, float conf) {
//        Task t = new NALTask(x.term(), GOAL, $.t(1f, conf), nar.time(),
//                ETERNAL, ETERNAL,
//                nar.evidence()
//                //Stamp.UNSTAMPED
//        );
//
//        always.add((prev, now, next) -> t);
//        return t;
//    }
//    public void alwaysWant(Termed x, float confFactor) {
//        //long[] evidenceShared = nar.evidence();
//
//        always.add((prev, now, next) ->
//
//                new NALTask(x.term(), GOAL, $.t(1f, confFactor * nar.confDefault(GOAL)), now,
//                        now, next,
//                        //evidenceShared
//                        nar.evidence()
//                        //Stamp.UNSTAMPED
//
//                )
//        );
//
//    }
//
//    public void alwaysQuestion(Termed x, boolean stamped) {
//        alwaysQuestionDynamic(() -> x, true, stamped);
//    }
//
//    public void alwaysQuest(Termed x, boolean stamped) {
//        alwaysQuestionDynamic(() -> x, false, stamped);
//    }
//
//    public void alwaysQuestionDynamic(Supplier<Termed> x, boolean questionOrQuest, boolean stamped) {
//
//        always.add((prev, now, next) -> {
//
//            long[] stamp = stamped ? nar.evidence() : Stamp.UNSTAMPED;
//
//            Termed tt = x.get();
//            if (tt == null) return null;
//
//            return new NALTask(tt.term(), questionOrQuest ? QUESTION : QUEST, null, now,
//                    now, next,
//                    stamp
//            )/* {
//                @Override
//                public boolean isInput() {
//                    return false;
//                }
//            }*/;
//        });
//
//    }
//
//    private void alwaysQuestionEternally(Termed x, boolean questionOrQuest, boolean stamped) {
//
//        NALTask etq = new NALTask(x.term(), questionOrQuest ? QUESTION : QUEST, null, nar.time(),
//                ETERNAL, ETERNAL,
//                //evidenceShared
//                stamped ? nar.evidence() : Stamp.UNSTAMPED
//
//        );
//        always.add((prev, now, next) -> etq);
//    }


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

    public Random random() {
        NAR n = this.nar;
        return n != null ? n.random() : ThreadLocalRandom.current();
    }

    public String summary() {


        return id +
                " dex=" + /*n4*/(dexterity()) +

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

        this.prev = nar.time();


        this.in = nar.newChannel(this);



        super.starting(nar);

        this.frameTrigger.start(this);

        enabled.set(true);
    }


    @Override
    protected void stopping(NAR nar) {

        enabled.set(false);

        if (frameTrigger != null) {
            frameTrigger.stop();
        } else {
            throw new WTF(this + " stopped twice");
        }

        super.stopping(nar);
    }


    public Reward reward(FloatSupplier rewardfunc) {
        return reward(rewardTerm("reward"), rewardfunc);
    }

    @Deprecated
    public Reward rewardDetailed(FloatSupplier rewardfunc) {
        return rewardDetailed(rewardTerm("reward"), rewardfunc);
    }

    @Deprecated
    public Reward rewardDetailed(float min, float max, FloatSupplier rewardfunc) {
        return rewardDetailed(rewardTerm("reward"), min, max, rewardfunc);
    }

    public Reward reward(String reward, FloatSupplier rewardFunc) {
        return reward(rewardTerm(reward), rewardFunc);
    }

    /**
     * default reward term builder from String
     */
    protected Term rewardTerm(String reward) {
        //return $.func($$(reward), id);
        return $.inh(id, $$(reward));
    }

    public Reward rewardNormalized(String reward, float min, float max, FloatSupplier rewardFunc) {
        return rewardNormalized(rewardTerm(reward), min, max, rewardFunc);
    }

    public Reward reward(Term reward, FloatSupplier rewardFunc) {
        return reward(new SimpleReward(reward, rewardFunc, this));
    }

    /**
     * set a default (bi-polar) reward supplier
     */
    public Reward rewardNormalized(Term reward, float min, float max, FloatSupplier rewardFunc) {
        //.relax(Param.HAPPINESS_RE_SENSITIZATION_RATE);
        return reward(reward, normalize(rewardFunc, min, max));
    }

    @NotNull
    public FloatNormalized normalize(FloatSupplier rewardFunc, float min, float max) {
        return new FloatNormalized(new FloatClamped(rewardFunc, min, max), min, max, false);
    }

    @Deprecated
    public Reward rewardDetailed(String reward, FloatSupplier rewardFunc) {
        return rewardDetailed(rewardTerm(reward), rewardFunc);
    }

    @Deprecated
    public Reward rewardDetailed(Term reward, FloatSupplier rewardFunc) {
        return reward(new DetailedReward(reward, rewardFunc, this));
    }

    public Reward rewardDetailed(String reward, float min, float max, FloatSupplier rewardFunc) {
        return rewardDetailed(rewardTerm(reward), min, max, rewardFunc);
    }

    public Reward rewardDetailed(Term reward, float min, float max, FloatSupplier rewardFunc) {
        return reward(new DetailedReward(reward, normalize(rewardFunc, min, max), this));
    }

    /**
     * default reward module
     */
    final public synchronized Reward reward(Reward r) {
        if (rewards.OR(e -> e.term().equals(r.term())))
            throw new RuntimeException("reward exists with the ID: " + r.term());

        rewards.add(r);
        addAttention(attnReward, r);
        return r;
    }

    public interface NAgentCycle {
        /**
         * in each iteration,
         * responsible for invoking some or all of the following agent operations, and
         * supplying their necessary time bounds:
         * <p>
         * a.frame() - executes attached per-frame event handlers
         * <p>
         * a.reinforce(...) - inputs 'always' tasks
         * <p>
         * a.sense(...) - reads sensors
         * <p>
         * a.act(...) - reads/invokes goals and feedback
         */
        void next(NAgent a, int iteration, long prev, long now);
    }

    public enum Cycles implements NAgentCycle {

        /**
         * original, timing needs analyzed more carefully
         */
        Interleaved() {
            @Override
            public void next(NAgent a, int iteration, long prev, long now) {

                a.sense(prev, now);

//                a.reinforce(prev, now, next);

                //long adjustedPrev = Math.max(prev, now - (next-now)); //prevent stretching and evaluating too far in the past
                a.act(prev, now);

                a.frame();
            }

        },

        /**
         * iterations occurr in read then act pairs
         */
        Biphasic() {
            @Override
            public void next(NAgent a, int iteration, long prev, long now) {

                //System.out.println(a.nar.time() + ": " + (iteration%2) + " " + prev + " " + now + " " + next);

                switch (iteration % 2) {
                    case 0:
                        //SENSE
                        a.sense(prev, now);
                        break;
                    case 1:
                        //ACT
//                        a.reinforce(prev, now, next);
                        a.act(prev, now);
                        a.frame();
                        break;
                }
            }
        }
    }


    /**
     * iteration
     */
    protected final void next() {

        if (!enabled.getOpaque() || !busy.compareAndSet(false, true))
            return;

        try {

            int d = nar.timeResolution.intValue();
            long now = Tense.dither(nar.time(), d);
                       //nar.time();
            long prev = this.prev;
            if (prev == ETERNAL)
                prev = now;
            else if (now <= prev)
                return;

            prev = Math.max(prev, now - frameTrigger.dur());

            long next = Tense.dither(Math.max(now, frameTrigger.next(now)), d);

            this.now = now;
            this.next = next;

            attn.update(pri.floatValue());

            cycle.next(this, iteration.getAndIncrement(), prev, now);

            this.prev = now;

            if (trace.getOpaque())
                logger.info(summary());

        } finally {
            busy.set(false);
        }

    }

    protected void act(long prev, long now) {
        //ActionConcept[] aaa = actions.array();
        ActionConcept[] aaa = actions.array().clone(); ArrayUtils.shuffle(aaa, random()); //HACK shuffle cloned copy for thread safety


        //curiosity conf initial setting  HACK


        float curiConf =
                nar.confMin.floatValue();
                //nar.confMin.floatValue() * 2;
                //nar.confMin.floatValue() * 4;
                //Util.lerp(1/8f, nar.confMin.floatValue(), Param.TRUTH_MAX_CONF);
                //nar.confDefault(GOAL)/4;
                //nar.confDefault(GOAL)/3;
                //nar.confDefault(GOAL)/2;
                //nar.confDefault(GOAL)/3;
                //w2c(c2w(nar.confDefault(GOAL))/3);
                //w2c(c2w(nar.confDefault(GOAL))/2);
                //nar.confDefault(GOAL);

        curiosity.conf.set(Util.clamp(curiConf, nar.confMin.floatValue(), Param.TRUTH_MAX_CONF));


        for (ActionConcept a : aaa) {

            //HACK temporary
            if (a instanceof AbstractGoalActionConcept)
                ((AbstractGoalActionConcept) a).curiosity(curiosity);

            a.sense(prev, now, nar);
        }
    }

    protected void sense(long prev, long now) {
        sensors.forEach(s -> s.sense(prev, now, nar));
        rewards.forEach(r -> r.update(prev, now));
    }

//    @Deprecated protected void reinforce(long prev, long now, long next) {
//
//        in.input(always.stream().map(x -> x.get(prev, now, next)).filter(Objects::nonNull).peek(x -> {
//            throw new UnsupportedOperationException();
////            x.pri(
////                    pri.floatValue() * nar.priDefault(x.punc())
////            );
//        }));
//    }

    private void frame() {
        eventFrame.emit(nar);
    }


//    public float dexterity() {
//        return dexterity(nar.time());
//    }

//    public float dexterity(long when) {
////        long now = nar.time();
////        long d = frameTrigger.next(now) - now;
////        return dexterity(when - d / 2, when + d / 2);
//        return dexterity(when,when);
//    }

//    /**
//     * average confidence of actions
//     * see: http:
//     */
//    public float dexterity(long start, long end) {
//        int n = actions.size();
//        if (n == 0)
//            return 0;
//
//        final double[] m = {0};
//        actions.forEach(a -> {
//            m[0] += a.dexterity(start, end, nar);
//        });
//
//        return (float) (m[0] / n);
//    }

    public float dexterity() {
        int n = actions.size();
        if (n == 0)
            return 0;

        final double[] m = {0};
        actions.forEach(a -> {
            m[0] += a.dexterity();
        });

        return (float) (m[0] / n);
    }

    public Off onFrame(Consumer/*<NAR>*/ each) {
        return eventFrame.on(each);
    }

    public Off onFrame(Runnable each) {
        return eventFrame.on((x) -> each.run());
    }

    public Off onFrameWeak(Runnable each) {
        return eventFrame.onWeak((x) -> each.run());
    }


    public FastCoWList<ActionConcept> actions() {
        return actions;
    }

    /**
     * creates an activator specific to this agent context
     */
    public Consumer<Predicate<Activate>> sampleActions() {


        return p -> {
            Activate a;

            final int numConcepts = actions.size();
            int remainMissing = numConcepts;
            if (remainMissing == 0) return;


            Random rng = nar.random();
            do {
                Concept cc = actions.get(rng.nextInt(numConcepts));
                //Concept cc = nar.conceptualize(cc);
                if (cc != null) {
                    a = new Activate(cc, 0f);
                    //nar.activate(cc, 1f);
                    ///a = new Activate(cc, 0);
                    //a.delete();
                } else {
                    a = null;
                    if (remainMissing-- <= 0)
                        break;

                }
            } while (a == null || p.test(a));
        };
    }


    public float reward() {
        float total = 0;
        for (Reward r : rewards) {
            total += r.summary();
        }
        return total / rewards.size();
    }
}
