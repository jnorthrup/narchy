package nars.agent;

import jcog.Log;
import jcog.Paper;
import jcog.Skill;
import jcog.TODO;
import jcog.data.list.FastCoWList;
import jcog.event.ListTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.math.FloatClamped;
import jcog.math.FloatNormalized;
import jcog.math.FloatSupplier;
import jcog.thing.Part;
import jcog.util.ArrayUtil;
import nars.$;
import nars.NAR;
import nars.attention.PriNode;
import nars.attention.What;
import nars.concept.action.AgentAction;
import nars.concept.action.BiPolarAction;
import nars.concept.action.curiosity.Curiosity;
import nars.concept.action.curiosity.DefaultCuriosity;
import nars.concept.sensor.GameLoop;
import nars.concept.sensor.Signal;
import nars.concept.sensor.VectorSensor;
import nars.control.NARPart;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.util.Timed;
import org.slf4j.Logger;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static nars.$.$$;
import static nars.time.Tense.ETERNAL;

/**
 * an integration of sensor concepts and motor functions
 * interfacing with an environment forming a sensori-motor loop.
 *
 * these include all forms of problems including
 *   optimization
 *   reinforcement learning
 *   etc
 *
 * the name 'Game' is used, in the most general sense of the
 * word 'game'.
 *
 */
@Paper @Skill({"Game_studies", "Game_theory"})
public class Game extends NARPart implements NSense, NAct, Timed {

    private final Topic<NAR> eventFrame = new ListTopic();

    private static final Logger logger = Log.logger(Game.class);

    public final GameTime time;

    private final AtomicBoolean busy = new AtomicBoolean(false);
    public final AtomicBoolean trace = new AtomicBoolean(false);

    private final static Atom ACTION = Atomic.atom("action"), SENSOR = Atomic.atom("sensor"), REWARD = Atomic.atom("reward");

    public final FastCoWList<GameLoop> sensors = new FastCoWList<>(GameLoop[]::new);
    public final FastCoWList<AgentAction> actions = new FastCoWList<>(AgentAction[]::new);
    public final FastCoWList<Reward> rewards = new FastCoWList<>(Reward[]::new);

    public final AtomicInteger iteration = new AtomicInteger(0);

    public final Curiosity curiosity = DefaultCuriosity.defaultCuriosity(this);

    public final PriNode attnReward, attnAction, attnSensor;
    private final PriNode pri;

    private final What experience;

    public final Term id;

    public volatile long prev = ETERNAL;
    public volatile long now = ETERNAL;
    public volatile long next = ETERNAL;


    private final NAgentCycle cycle =
            //Cycles.Biphasic;
            Cycles.Interleaved;


    @Deprecated public Game(String id, NAR n) {
        this(id, GameTime.durs(1), n);
    }

    @Deprecated public Game(String id, GameTime time, NAR n) {
        this(Atomic.atom(id), time, n);
    }

    public Game(Term id, GameTime time, NAR nar) {
        this(time, nar.the(id,true));
        this.nar = nar;
        nar.start(this);
    }


    static final Atom ENVIRONMENT = Atomic.atom("env");

    static Term env(Term x) {
        return $.func(ENVIRONMENT, x);
    }


    public Game(GameTime time, What experience) {
        super(env(experience.id));
//        this.nar = experience.nar;

        this.id = experience.id;

        this.experience = experience;

        this.time = time;

        this.pri = new PriNode(this.id);
        this.attnAction = new PriNode($.inh(id,ACTION));
        this.attnSensor = new PriNode($.inh(id,SENSOR));
        this.attnReward = new PriNode($.inh(id,REWARD));

        add(time.clock(this));

        //experience.add(this);
    }

    @Override
    public final What what() {
        return experience;
    }

    /**
     * dexterity = mean(conf(action))
     * evidence/confidence in action decisions, current measurement
     */
    public double dexterity() {
        int a = actions.size();
        double result = a == 0 ? 0 : actions.sumBy(AgentAction::dexterity);
        return a > 0 ? result / a : 0;
    }



    public final float happiness() {
        return happiness(dur());
    }

    /**
     *
     * avg reward satisfaction, current measurement
     * happiness metric applied to all reward concepts
     */
    @Paper
    public final float happiness(int dur) {
        return (float) rewards.meanBy(rr -> {
            float r = rr.happiness(dur);
            return r != r ? 0f : r;
        });
    }

    /** happiness metric applied to all sensor concepts */
    @Paper public final float happinessSensorsMean() {
        throw new TODO();
    }
    /** happiness metric applied to all action concepts */
    @Paper public final float happinessActionsMean() {
        throw new TODO();
    }

    //TODO weighted happiness function

    /**
     * proficiency = happiness * dexterity
     * current measurement
     * professional satori
     */
    public final double proficiency() {
        double x = happiness() * dexterity();
        if (x!=x)
            x = 0; //NaN > 0
        return x;
    }

    @Override
    public final <A extends AgentAction> A addAction(A c) {
        actions.add(c);

        nar().add(c);
        addAttention(attnAction, c);
        return c;
    }


    @Override
    public final <S extends GameLoop> S addSensor(S s) {
        if (s instanceof NARPart) {
            nar.start(((NARPart)s));
        }

        //TODO check for existing
        sensors.add(s);
        addAttention(attnSensor, s);
        return s;
    }

    private void addAttention(PriNode target, Object s) {
        if (s instanceof VectorSensor) {

            nar.control.parent(((VectorSensor) s).attn, new PriNode[]{target});
        } else if (s instanceof Signal) {

            nar.control.parent(((Signal) s).attn, new PriNode[]{target});
        } else if (s instanceof Reward) {

            nar.control.parent(((Reward) s).attn, new PriNode[]{target});
        } else if (s instanceof AgentAction) {

            nar.control.parent(((AgentAction) s).attn, new PriNode[]{target});
        } else if (s instanceof BiPolarAction) {

            nar.control.parent(((BiPolarAction) s).attn, new PriNode[]{target});
        } else if (s instanceof PriNode)
            nar.control.parent(((PriNode) s), new PriNode[]{target});
        else
            throw new TODO();
    }

    public Random random() {
        NAR n = this.nar;
        return n != null ? n.random() : ThreadLocalRandom.current();
    }

    @Override
    public long time() {
        return nar.time();
    }

    public String summary() {


        return id +
                " dex=" + /*n4*/(dexterity()) +
                " hapy=" + /*n4*/(happiness()) +

                /*" var=" + n4(varPct(nar)) + */ '\t' + nar.memory.summary() + ' ' +
                nar.emotion.summary();
    }

    /**
     * registers sensor, action, and reward concepts with the NAR
     * TODO call this in the constructor
     */
    //@Override
    protected void starting(NAR nar) {
        nar.control.add(pri);

        nar.control.parent(attnAction, new PriNode[]{this.pri, nar.goalPriDefault});

        nar.control.parent(attnSensor, new PriNode[]{this.pri, nar.beliefPriDefault});

        /* TODO avg */

        nar.control.parent(attnReward, new PriNode[]{this.pri, nar.goalPriDefault});

        sensors.stream().filter(x -> x instanceof NARPart).forEach(s -> nar.start((NARPart) s));

    }


    //@Override
    protected void stopping(NAR nar) {

        sensors.stream().filter(x -> x instanceof NARPart).forEach(s -> nar.stop((NARPart)s));

    }

    @Override
    public boolean delete() {
        if (super.delete()) {
            nar.control.remove(pri);
            nar.control.remove(attnAction);
            nar.control.remove(attnSensor);
            nar.control.remove(attnReward);
            return true;
        }
        return false;

    }

    public Reward reward(FloatSupplier rewardfunc) {
        return reward(rewardTerm("reward"), 1, rewardfunc);
    }

    public Reward reward(String reward, float freq, FloatSupplier rewardFunc) {
        return reward(rewardTerm(reward), freq, rewardFunc);
    }

    /**
     * default reward target builder from String
     */
    protected Term rewardTerm(String reward) {
        //return $.func($$(reward), id);
        return $.inh(id, $$(reward));
    }

    public Reward rewardNormalized(String reward, float min, float max, FloatSupplier rewardFunc) {
        return rewardNormalized(reward, 1, min, max, rewardFunc);
    }

    public Reward rewardNormalized(String reward, float freq, float min, float max, FloatSupplier rewardFunc) {
        return rewardNormalized(rewardTerm(reward), min, max, rewardFunc);
    }

    public Reward reward(Term reward, float freq, FloatSupplier rewardFunc) {
        return reward(new SimpleReward(reward, freq, rewardFunc, this));
    }

    /**
     * set a default (bi-polar) reward supplier
     */
    public Reward rewardNormalized(Term reward, float min, float max, FloatSupplier rewardFunc) {
        //.relax(Param.HAPPINESS_RE_SENSITIZATION_RATE);
        return reward(reward, 1, normalize(rewardFunc, min, max));
    }

    public FloatSupplier normalize(FloatSupplier rewardFunc, float min, float max) {
        return new FloatClamped(new FloatNormalized(rewardFunc, min, max, true), min, max);
    }

//    @Deprecated
//    public Reward rewardDetailed(String reward, FloatSupplier rewardFunc) {
//        return rewardDetailed(rewardTerm(reward), rewardFunc);
//    }
//
//    @Deprecated
//    public Reward rewardDetailed(Term reward, FloatSupplier rewardFunc) {
//        return reward(new DetailedReward(reward, rewardFunc, this));
//    }
//
//    public Reward rewardDetailed(String reward, float min, float max, FloatSupplier rewardFunc) {
//        return rewardDetailed(rewardTerm(reward), min, max, rewardFunc);
//    }
//
//    public Reward rewardDetailed(Term reward, float min, float max, FloatSupplier rewardFunc) {
//        return reward(new DetailedReward(reward, normalize(rewardFunc, min, max), this));
//    }
//
//    @Deprecated
//    public Reward rewardDetailed(FloatSupplier rewardfunc) {
//        return rewardDetailed(rewardTerm("reward"), rewardfunc);
//    }
//
//    @Deprecated
//    public Reward rewardDetailed(float min, float max, FloatSupplier rewardfunc) {
//        return rewardDetailed(rewardTerm("reward"), min, max, rewardfunc);
//    }

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

    /** perceptual duration */
    public final int dur() {
        return what().dur();
    }

    /** physical/sensory duration */
    public final int durPhysical() {
        return time.dur();
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
        void next(Game a, int iteration, long prev, long now);
    }

    public enum Cycles implements NAgentCycle {

        /**
         * original, timing needs analyzed more carefully
         */
        Interleaved() {
            @Override
            public void next(Game a, int iteration, long prev, long now) {
                a.sense();
                a.act();
                a.frame();
            }

        },

        /**
         * iterations occurr in read then act pairs
         */
        Biphasic() {
            @Override
            public void next(Game a, int iteration, long prev, long now) {

                //System.out.println(a.nar.time() + ": " + (iteration%2) + " " + prev + " " + now + " " + next);

                switch (iteration % 2) {
                    case 0:
                        //SENSE
                        a.sense();
                        break;
                    case 1:
                        //ACT
                        a.act();
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

        if (!isOn() || !busy.compareAndSet(false, true))
            return;


        try {

            long now = nar.time();
            long prev = this.now;
            if (prev == ETERNAL) {
                this.now = now;
                prev = now-1;
            }
            if (now <= prev)
                return; //too early


            long idealPrev = time.prev(now);
            if (now <= idealPrev)
                return; //too early

//            prev = Math.max(prev, idealPrev); //assume up to one frame behind

            long next =
                    //(Math.max(now, frameTrigger.next(now)), d);
                    Math.max(now, time.next(now));

            //assert (prev < now && now < next);
            this.prev = prev;
            this.now = now;
            this.next = next;

//            System.out.println(this + " "  + state().toString() + " " + (now - prev));

            cycle.next(this, iteration.getAndIncrement(), prev, now);

            if (trace.getOpaque())
                logger.info(summary());

        } finally {
            busy.set(false);
        }

    }

    @Override
    public final NAR nar() {
        return nar;
    }

    protected void act() {
        //ActionConcept[] aaa = actions.array();
        AgentAction[] aaa = actions.array().clone();
        ArrayUtil.shuffle(aaa, random()); //HACK shuffle cloned copy for thread safety

        //TODO fork here
        for (AgentAction a : aaa)
            update(a);
    }


    protected void sense() {
        //TODO fork here
        sensors.forEach(this::update);
        rewards.forEach(this::update);
    }

    private void update(GameLoop s) {
        if (s instanceof Part) {
            if (!((Part)s).isOn())
                return; //the part is disabled
        }
        s.update(this);
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


    public Off onFrame(Consumer/*<NAR>*/ each) {
        return eventFrame.on(each);
    }

    public Off onFrame(Runnable each) {
        return eventFrame.on((x) -> each.run());
    }

//    public Off onFrameWeak(Runnable each) {
//        return eventFrame.onWeak((x) -> each.run());
//    }


    public FastCoWList<AgentAction> actions() {
        return actions;
    }

//    /**
//     * creates an activator specific to this agent context
//     */
//    public Consumer<Predicate<Activate>> sampleActions() {
//
//
//        return p -> {
//            Activate a;
//
//            final int numConcepts = actions.size();
//            int remainMissing = numConcepts;
//            if (remainMissing == 0) return;
//
//
//            Random rng = nar.random();
//            do {
//                Concept cc = actions.get(rng.nextInt(numConcepts));
//                //Concept cc = nar.conceptualize(cc);
//                if (cc != null) {
//                    a = new Activate(cc, 0f);
//                    //nar.activate(cc, 1f);
//                    ///a = new Activate(cc, 0);
//                    //a.delete();
//                } else {
//                    a = null;
//                    if (remainMissing-- <= 0)
//                        break;
//
//                }
//            } while (a == null || p.test(a));
//        };
//    }

}

//    public final Bitmap2DSensor sense(Bitmap2DSensor bmp) {
//        return addCamera(bmp);
////        addSensor(bmp);
////        onFrame(bmp::input); //TODO support adaptive input mode
////        return bmp;
//    }
//
//    @Deprecated public Task alwaysWantEternally(Termed x, float conf) {
//        Task t = new NALTask(x.target(), GOAL, $.t(1f, conf), nar.time(),
//                ETERNAL, ETERNAL,
//                nar.evidence()
//                //Stamp.UNSTAMPED
//        );
//
//        always.addAt((prev, now, next) -> t);
//        return t;
//    }
//    public void alwaysWant(Termed x, float confFactor) {
//        //long[] evidenceShared = nar.evidence();
//
//        always.addAt((prev, now, next) ->
//
//                new NALTask(x.target(), GOAL, $.t(1f, confFactor * nar.confDefault(GOAL)), now,
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
//        always.addAt((prev, now, next) -> {
//
//            long[] stamp = stamped ? nar.evidence() : Stamp.UNSTAMPED;
//
//            Termed tt = x.get();
//            if (tt == null) return null;
//
//            return new NALTask(tt.target(), questionOrQuest ? QUESTION : QUEST, null, now,
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
//        NALTask etq = new NALTask(x.target(), questionOrQuest ? QUESTION : QUEST, null, nar.time(),
//                ETERNAL, ETERNAL,
//                //evidenceShared
//                stamped ? nar.evidence() : Stamp.UNSTAMPED
//
//        );
//        always.addAt((prev, now, next) -> etq);
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

//    protected <A extends ActionConcept> void actionAdded(A a) {
//
//        //alwaysQuest(a, true);
//        //alwaysQuestionEternally(Op.IMPL.the($.varQuery(1), a.target), true, false);
//
////        alwaysQuestionEternally(a,
////                false,
////                false
////        );
//
//
////        alwaysQuestion(IMPL.the(c.target, 0, $$("reward:#x")), true);
////        alwaysQuestion(IMPL.the(c.target.neg(), 0, $$("reward:#x")), true);
//        //alwaysQuestionEternally(Op.CONJ.the($.varDep(1), a.target.neg()));
//    }