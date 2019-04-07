package nars.agent;

import jcog.*;
import jcog.data.list.FastCoWList;
import jcog.event.ListTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.math.FloatClamped;
import jcog.math.FloatNormalized;
import jcog.math.FloatSupplier;
import jcog.service.Part;
import jcog.util.ArrayUtils;
import nars.$;
import nars.NAR;
import nars.attention.PriNode;
import nars.attention.What;
import nars.concept.action.AgentAction;
import nars.concept.action.curiosity.Curiosity;
import nars.concept.action.curiosity.DefaultCuriosity;
import nars.concept.sensor.GameLoop;
import nars.concept.sensor.Signal;
import nars.concept.sensor.VectorSensor;
import nars.control.NARPart;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.slf4j.Logger;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static nars.$.$$;
import static nars.time.Tense.ETERNAL;
import static nars.truth.func.TruthFunctions.w2cSafe;

/**
 * an integration of sensor concepts and motor functions
 * forming a sensori-motor loop.
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
public class Game extends NARPart implements NSense, NAct {

    private final Topic<NAR> eventFrame = new ListTopic();

    private static final Logger logger = Util.logger(Game.class);

    public final GameTime time;

    private final AtomicBoolean busy = new AtomicBoolean(false);
    public final AtomicBoolean trace = new AtomicBoolean(false);

    public final FastCoWList<GameLoop> sensors = new FastCoWList<>(GameLoop[]::new);

    public final FastCoWList<AgentAction> actions = new FastCoWList<>(AgentAction[]::new);

    public final FastCoWList<Reward> rewards = new FastCoWList<>(Reward[]::new);

    public final AtomicInteger iteration = new AtomicInteger(0);

    public final Curiosity curiosity = DefaultCuriosity.defaultCuriosity(this);

    public final PriNode attnReward, attnAction, attnSensor;
    private final PriNode pri;
    private final What what;

    public volatile long prev = ETERNAL;
    public volatile long now = ETERNAL;
    public volatile long next = ETERNAL;


    private final NAgentCycle cycle =
            //Cycles.Biphasic;
            Cycles.Interleaved;


    @Deprecated public Game(String id, NAR n) {
        this(id, n.in);
    }

    @Deprecated public Game(Term id, GameTime time, NAR n) {
        this(id, time, n.in);
    }

    @Deprecated public Game(String id, GameTime time, NAR n) {
        this(id, time, n.in);
    }

    public Game(String id, What w) {
        this(id, GameTime.durs(1), w);
    }

    public Game(String id, GameTime time, What w) {
        this(id.isEmpty() ? null : Atomic.the(id), time, w);
    }

    public Game(Term id, GameTime time, What w) {
        super(id);

        this.what = w;
        this.nar = w.nar;

        this.time = time;

        this.pri = nar.control.newPri(id);
        this.attnAction = new PriNode($.func("action", id))
                .parent(nar, this.pri, nar.goalPriDefaultNode);
        this.attnSensor = new PriNode($.func("sensor", id))
                .parent(nar, this.pri, nar.beliefPriDefaultNode);
        this.attnReward = new PriNode($.func("reward", id))
                .parent(nar, this.pri, nar.goalPriDefaultNode /* TODO avg */);
        /*             float pb = nar.priDefault(BELIEF), pg = nar.priDefault(GOAL);
            //TODO adjustable balance
            attnSensor.factor.set(pb);
            attnAction.factor.set(pg);
            attnReward.factor.set((pb + pg));
        */

        //nar.runLater(()-> nar.on(this));
        nar.start(this);
    }

    @Override
    public final What what() {
        return what;
    }

    /**
     * dexterity = sum(evidence(action))
     * evidence/confidence in action decisions, current measurement
     */
    public double dexteritySum() {
        int n = actions.size();
        if (n == 0)
            return 0;
        else
            return actions.sumBy(AgentAction::dexterity);
    }

    public double dexterityMean() {
        int a = actions.size();
        return a > 0 ? w2cSafe(dexteritySum() / a) : 0;
    }



    /**
     *
     * avg reward satisfaction, current measurement
     */
    @Paper
    public final float happinessMean() {
        return (float) rewards.meanBy(rr -> {
            float r = rr.happiness();
            return r != r ? 0f : r;
        });
    }

    //TODO weighted happiness function

    /**
     * proficiency = happiness * dexterity
     * current measurement
     * professional satori
     */
    public final double proficiency() {
        double x = happinessMean() * dexterityMean();
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
        //TODO check for existing
        sensors.add(s);
        addAttention(attnSensor, s);
        return s;
    }

    protected void addAttention(PriNode target, Object s) {
        if (s instanceof VectorSensor) {
            ((VectorSensor) s).attn.parent(nar, target);
        } else if (s instanceof Signal) {
            ((Signal) s).attn.parent(nar, target);
        } else if (s instanceof Reward) {
            ((Reward) s).attn.parent(nar, target);
        } else if (s instanceof AgentAction) {
            ((AgentAction) s).attn.parent(nar, target);
        } else if (s instanceof PriNode)
            ((PriNode) s).parent(nar, target);
        else
            throw new TODO();
    }

    public Random random() {
        NAR n = this.nar;
        return n != null ? n.random() : ThreadLocalRandom.current();
    }

    public String summary() {


        return id +
                " dex=" + /*n4*/(dexterityMean()) +
                " hapy=" + /*n4*/(happinessMean()) +

                /*" var=" + n4(varPct(nar)) + */ '\t' + nar.memory.summary() + ' ' +
                nar.feel.summary();
    }

    /**
     * registers sensor, action, and reward concepts with the NAR
     * TODO call this in the constructor
     */
    @Override
    protected void starting(NAR nar) {

        super.starting(nar);

        this.time.start(this);
    }


    @Override
    protected void stopping(NAR nar) {

        if (time != null) {
            time.stop();
        } else {
            throw new WTF(this + " stopped twice");
        }

        super.stopping(nar);
    }



    public Reward reward(FloatSupplier rewardfunc) {
        return reward(rewardTerm("reward"), rewardfunc);
    }

    public Reward reward(String reward, FloatSupplier rewardFunc) {
        return reward(rewardTerm(reward), rewardFunc);
    }

    /**
     * default reward target builder from String
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

//                a.reinforce(prev, now, next);

                //long adjustedPrev = Math.max(prev, now - (next-now)); //prevent stretching and evaluating too far in the past
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
//                        a.reinforce(prev, now, next);
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

        if (!busy.compareAndSet(false, true))
            return;

        try {

            if (!isOn())
                return; //can this happen?

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

            prev = Math.max(prev, idealPrev); //assume up to one frame behind

            long next =
                    //(Math.max(now, frameTrigger.next(now)), d);
                    Math.max(now, time.next(now));

            assert (prev < now && now < next);
            this.prev = prev;
            this.now = now;
            this.next = next;

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
        ArrayUtils.shuffle(aaa, random()); //HACK shuffle cloned copy for thread safety

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

    public Off onFrameWeak(Runnable each) {
        return eventFrame.onWeak((x) -> each.run());
    }


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