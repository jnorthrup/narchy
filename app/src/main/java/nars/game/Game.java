package nars.game;

import jcog.*;
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
import nars.attention.PriAmp;
import nars.attention.PriNode;
import nars.attention.What;
import nars.control.NARPart;
import nars.game.action.ActionSignal;
import nars.game.action.BiPolarAction;
import nars.game.sensor.GameLoop;
import nars.game.sensor.Signal;
import nars.game.sensor.UniSignal;
import nars.game.sensor.VectorSensor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.time.When;
import nars.truth.Truth;
import nars.util.Timed;
import org.slf4j.Logger;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static nars.Op.BELIEF;
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
public class Game extends NARPart /* TODO extends ProxyWhat -> .. and commit when it updates */ implements NSense, NAct, Timed {

    private final Topic<NAR> eventFrame = new ListTopic();

    private static final Logger logger = Log.logger(Game.class);

    public final GameTime time;

    private final AtomicBoolean busy = new AtomicBoolean(false);
    public final AtomicBoolean trace = new AtomicBoolean(false);

    private final static Atom ACTION = Atomic.atom("action"), SENSOR = Atomic.atom("sensor"), REWARD = Atomic.atom("reward");

    public final FastCoWList<GameLoop> sensors = new FastCoWList<>(GameLoop[]::new);
    public final FastCoWList<ActionSignal> actions = new FastCoWList<>(ActionSignal[]::new);
    public final FastCoWList<Reward> rewards = new FastCoWList<>(Reward[]::new);

    public final AtomicInteger iteration = new AtomicInteger(0);

    public PriAmp rewardPri, actionPri, sensorPri;

    /** the context representing the experience of the game */
    @Deprecated private What what;

    public final Term id;


    public volatile long now = ETERNAL;

    public When<What> nowWhat = null;

    private final NAgentCycle cycle =
            //Cycles.Biphasic;
            Cycles.Interleaved;

    public float confDefaultBelief;



    static final Atom GAME = Atomic.atom(Game.class.getSimpleName().toLowerCase());

    static Term env(Term x) {
        return $.func(GAME, x);
    }

    public Game(String id) {
        this(id, GameTime.durs(1));
    }

    public Game(String id, GameTime time) {
        this($.$$(id), time);
    }

    //@Deprecated private final static InheritableThreadLocal<NAR> _nar = new InheritableThreadLocal<>();

    @Deprecated public Game(Term id, GameTime time, NAR n) {
        this(id, time);
        this.nar = n;
        //_nar.set(n);

        nar.runLater(()->n.start(this));
    }

    public Game(Term id, GameTime time) {
        super(env(id));

        this.id = id;
        this.time = time;

        add(time.clock(this));
    }

    @Deprecated @Override public final What what() {
        return what;
    }

    /**
     * dexterity = mean(conf(action))
     * evidence/confidence in action decisions, current measurement
     */
    public double dexterity() {
        int a = actions.size();
        double result = a == 0 ? 0 : actions.sumBy(ActionSignal::dexterity);
        return a > 0 ? result / a : 0;
    }
    public double coherency() {
        int a = actions.size();
        double result = a == 0 ? 0 : actions.sumBy(ActionSignal::coherency);
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
    public final float happiness(float dur) {
        return (float) rewards.meanBy(rr -> {
            float r = rr.happiness(dur);
            return r != r ? 0.5f : r;
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
    public final <A extends ActionSignal> A addAction(A c) {
        if (actions.OR(e -> e.term().equals(c.term())))
            throw new RuntimeException("action exists with the ID: " + c.term());
        actions.add(c);
        return c;
    }


    @Override
    public final <S extends GameLoop> S addSensor(S s) {
        //TODO check for existing
        sensors.add(s);
        return s;
    }

    private void addAttention(PriNode target, Object s) {
        if (s instanceof NARPart)
            nar.start(((NARPart)s));

        if (s instanceof VectorSensor) {
            nar.control.input(((VectorSensor) s).pri, target);
        } else if (s instanceof Signal) {

            if (s instanceof UniSignal)
                nar.control.input(((UniSignal) s).pri, target);
            else
                throw new TODO();

        } else if (s instanceof Reward) {

            nar.control.input(((Reward) s).attn, target);
        } else if (s instanceof BiPolarAction) {

            nar.control.input(((BiPolarAction) s).attn, target);
        } else if (s instanceof PriNode)
            nar.control.input(((PriNode) s), target);
        else
            throw new TODO();
    }

    public Random random() {
        NAR n = this.nar;
        return n != null ? n.random() : ThreadLocalRandom.current();
    }

    @Override
    public long time() {
        return now;
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
        super.starting(nar);

        this.now = nar.time();
        this.what = nar.fork(id, true);

        PriNode pri = what.pri;

        this.actionPri = nar.control.amp($.inh(id,ACTION), PriNode.Merge.And,
            pri, nar.goalPriDefault);
        this.sensorPri = nar.control.amp($.inh(id,SENSOR), PriNode.Merge.And,
            pri, nar.beliefPriDefault);
        this.rewardPri = nar.control.amp($.inh(id,REWARD), PriNode.Merge.And,
            pri, nar.goalPriDefault
        ); //, /*OR: nar.beliefPriDefault,*/ nar.goalPriDefault);


        sensors.stream().filter(x -> x instanceof NARPart).forEach(s -> nar.start((NARPart) s));
        sensors.forEach(s -> addAttention(sensorPri, s));

        actions.forEach(a -> nar.add(a));
        actions.forEach(a -> addAttention(actionPri, a));

        rewards.forEach(r -> addAttention(rewardPri, r));

        rewards.forEach(r -> r.init(this));
    }


    //@Override
    protected void stopping(NAR nar) {

        nar.control.remove(actionPri); actionPri = null;
        nar.control.remove(rewardPri); rewardPri = null;
        nar.control.remove(sensorPri); sensorPri = null;

        sensors.stream().filter(x -> x instanceof NARPart).forEach(s -> nar.stop((NARPart)s));
        //actions.forEach(a -> nar.remove((PermanentConcept)a)); //TODO

        this.what.close();
        this.what = null;

        this.nar = null;

    }

    @Override
    public boolean delete() {
        if (super.delete()) {
            nar.control.removeAll(actionPri, sensorPri, rewardPri);
            return true;
        }
        return false;

    }

    public Reward reward(FloatSupplier rewardfunc) {
        return reward(rewardTerm("happy"), 1, rewardfunc);
    }

    public Reward reward(String reward, FloatSupplier rewardFunc) {
        return reward(reward, 1f, rewardFunc);
    }
    public Reward reward(String reward, float freq, FloatSupplier rewardFunc) {
        return reward(rewardTerm(reward), freq, rewardFunc);
    }

    /**
     * default reward target builder from String
     */
    protected Term rewardTerm(String reward) {
        //return $.func($$(reward), id);
        return $.inh(id, reward);
    }

    public Reward rewardNormalized(String reward, float min, float max, FloatSupplier rewardFunc) {
        return rewardNormalized(reward, 1, min, max, rewardFunc);
    }

    public Reward rewardNormalized(String reward, float freq, float min, float max, FloatSupplier rewardFunc) {
        return rewardNormalized(rewardTerm(reward), freq, min, max, rewardFunc);
    }


    public SimpleReward reward(Term reward, float freq, FloatSupplier rewardFunc) {
        SimpleReward r = new SimpleReward(reward, freq, rewardFunc, this);
//        r.addGuard(
//            NAL.DEBUG,false
//            //true,false
//            );
        reward(r);
        return r;
    }

    public Reward rewardNormalized(Term reward, float min, float max, FloatSupplier rewardFunc) {
        return rewardNormalized(reward, 1, min, max, rewardFunc);
    }

    /**
     * set a default (bi-polar) reward supplier
     */
    public Reward rewardNormalized(Term reward, float freq, float min, float max, FloatSupplier rewardFunc) {
        //.relax(Param.HAPPINESS_RE_SENSITIZATION_RATE);
        return reward(reward, freq, normalize(rewardFunc, min, max));
    }

    public FloatSupplier normalize(FloatSupplier rewardFunc, float min, float max) {
        return new FloatClamped(new FloatNormalized(rewardFunc, min, max, false), 0, 1);
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
        return r;
    }

    /** perceptual duration */
    public final float dur() {
        return what().dur();
    }

    /** physical/sensory duration */
    public final float durPhysical() {
        return time.dur();
    }

    public final float ditherFreq(float f, float res) {
        return Truth.freqSafe(f, Util.max(res, _freqRes));
    }
    public final float ditherConf(float c) {
        return (float) Truth.confSafe(c, _confRes);
    }

    public void pri(float v) {
        what.pri(v);
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
                a.nextFrame();
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
                        a.nextFrame();
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
            int dither = nar.dtDither();

            long now =
                Tense.dither(nar.time(), dither);

            long prev = this.now;
            if (prev == ETERNAL) {
                this.now = now;
                prev = now-dither;
            }
            if (now <= prev)
                return; //too early

            time.next(this.now = now);

//            System.out.println(this + " "  + state().toString() + " " + (now - prev));

            float dur = dur();
            long lastEnd = nowWhat !=null ? nowWhat.end : Math.round(now-dur/2-1);
            long nextStart = Math.max(lastEnd+1, (long)Math.floor(now - dur/2));
            long nextEnd = Math.max(nextStart, Math.round(Math.ceil(now + dur/2 - 1)));
            this.nowWhat = new When<>(nextStart, nextEnd, dur, what);

            this.confDefaultBelief = nar().confDefault(BELIEF);

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
        ActionSignal[] aaa = actions.array().clone();
        ArrayUtil.shuffle(aaa, random()); //HACK shuffle cloned copy for thread safety

        //TODO fork here
        for (ActionSignal a : aaa)
            update(a);
    }


    private transient float _freqRes = Float.NaN, _confRes = Float.NaN;

    protected void sense() {

        _freqRes = nar.freqResolution.floatValue();
        _confRes = nar.confResolution.floatValue();

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

    final AtomicInteger frame = new AtomicInteger();

    private void nextFrame() {
        eventFrame.emit(nar);
        frame.incrementAndGet();
    }

    public final int frame() {
        return frame.getOpaque();
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


    public FastCoWList<ActionSignal> actions() {
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