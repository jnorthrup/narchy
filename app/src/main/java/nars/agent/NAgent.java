package nars.agent;

import jcog.WTF;
import jcog.data.list.FastCoWList;
import jcog.event.*;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.action.ActionConcept;
import nars.concept.action.GoalActionConcept;
import nars.concept.sensor.Sensor;
import nars.control.NARService;
import nars.control.channel.CauseChannel;
import nars.link.Activate;
import nars.sensor.Bitmap2DSensor;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.util.TimeAware;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nars.$.$$;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * an integration of sensor concepts and motor functions
 */
public class NAgent extends NARService implements NSense, NAct {

    private final Topic<NAR> eventFrame = new ListTopic();

    private static final Logger logger = LoggerFactory.getLogger(NAgent.class);

    public final FrameTrigger frameTrigger;

    public final FloatRange curiosity = new FloatRange(0.10f, 0f, 1f);

    public final FloatRange pri = new FloatRange(1f, 0, 2f);

    public final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean busy = new AtomicBoolean(false);

    public final AtomicBoolean trace = new AtomicBoolean(false);

    public final List<Sensor> sensors = new FastCoWList<>(Sensor[]::new);

    public final FastCoWList<ActionConcept> actions = new FastCoWList<>(ActionConcept[]::new);

    public final List<Reward> rewards = new FastCoWList<>(Reward[]::new);

    @Deprecated
    private final List<Supplier<Task>> always = $.newArrayList();

    @Deprecated
    private CauseChannel<ITask> in = null;

    public volatile long last;
    protected volatile long now;


    public NAgent(String id, FrameTrigger frameTrigger, NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), frameTrigger, nar);
    }

    public NAgent(Term id, FrameTrigger frameTrigger, NAR nar) {
        super(id);
        this.nar = nar;
        this.frameTrigger = frameTrigger;
        this.now = this.last = nar.time();

        nar.on(this);
    }


    @Override
    public <A extends ActionConcept> A addAction(A c) {
        actions.add(c);

        actionAdded(c);

        nar().on(c);
        return c;
    }

    protected <A extends ActionConcept> void actionAdded(A c) {
        alwaysQuest(c, true);
        //alwaysQuestion(Op.CONJ.the(happy.term, a.term));
        //alwaysQuestion(Op.CONJ.the(happy.term, a.term.neg()));
    }

    @Override
    public <S extends Sensor> S addSensor(S s) {
        //TODO check for existing
        sensors.add(s);
        return s;
    }

    public final Bitmap2DSensor sense(Bitmap2DSensor bmp) {
        addSensor(bmp);
        onFrame(bmp::input); //TODO support adaptive input mode
        return bmp;
    }

    public Task alwaysWantEternally(Termed x, float conf) {
        Task t = new NALTask(x.term(), GOAL, $.t(1f, conf), now(),
                ETERNAL, ETERNAL,
                //nar.evidence()
                Stamp.UNSTAMPED
        );

        always.add(() -> t);
        return t;
    }

    public void alwaysWant(Termed x, float confFactor) {
        //long[] evidenceShared = nar.evidence();

        always.add(() -> {
            int dur = nar.dur();
            long now = Tense.dither(this.last + dur/*this.now()*/, nar);
            long next = Tense.dither(this.now + dur/*this.now() + nar.dur()*/, nar);
            now = Math.min(now, next);

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

    public void alwaysQuestionDynamic(Supplier<Termed> x, boolean questionOrQuest) {

        boolean stamped = true;
        always.add(() -> {

            long now = Tense.dither(this.now(), nar);
            long next = Tense.dither(this.now() + nar.dur(), nar);

            long[] stamp = stamped ? nar.evidence() : Stamp.UNSTAMPED;

            Termed tt = x.get();
            if (tt==null) return null;

            return new NALTask(tt.term(), questionOrQuest ? QUESTION : QUEST, null, now,
                    now, next,
                    stamp
            )/* {
                @Override
                public boolean isInput() {
                    return false;
                }
            }*/;
        });

    }
    private void alwaysQuestion(Termed x, boolean questionOrQuest, boolean stamped) {

        NALTask etq = new NALTask(x.term(), questionOrQuest ? QUESTION : QUEST, null, nar.time(),
                ETERNAL, ETERNAL,
                //evidenceShared
                stamped ? nar.evidence() : Stamp.UNSTAMPED

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
    public final NAR nar() {
        return nar;
    }


    public Random random() {
        TimeAware timeAware = this.nar;
        return timeAware != null ? timeAware.random() : ThreadLocalRandom.current();
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

        this.last = nar.time();


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


    protected void always(float activation) {
        in.input(always.stream().map(Supplier::get).peek(x -> {
            x.pri(
                    activation * nar.priDefault(x.punc())
            );
        }));
    }


    public Off reward(FloatSupplier rewardfunc) {
        return reward($.func("reward", id), rewardfunc);
    }

    @Deprecated
    public Off rewardDetailed(FloatSupplier rewardfunc) {
        return rewardDetailed($.func("reward", id), rewardfunc);
    }

    public Off reward(String reward, FloatSupplier rewardFunc) {
        return reward($.inh($$(reward), id), rewardFunc);
    }
    public Off reward(String reward, float min, float max, FloatSupplier rewardFunc) {
        return reward($.inh($$(reward), id), min, max, rewardFunc);
    }

    public Off reward(Term reward, FloatSupplier rewardFunc) {
        return reward(reward, 0, 0, rewardFunc);
    }
    /**
     * set a default (bi-polar) reward supplier
     */
    public Off reward(Term reward, float min, float max, FloatSupplier rewardFunc) {
        return reward(new SimpleReward(reward,
                //default normalizer
                new FloatNormalized(rewardFunc, min, max, false).relax(Param.HAPPINESS_RE_SENSITIZATION_RATE),
                this));
    }

    @Deprecated
    public Off rewardDetailed(Term reward, FloatSupplier rewardFunc) {
        DetailedReward r = new DetailedReward(reward, rewardFunc, this);
        return reward(r);
    }

    /**
     * default reward module
     */
    final public Off reward(Reward r) {
        rewards.add(r);
        return new Ons(onFrame(r));
    }


    /**
     * runs a frame
     */
    protected void frame() {
        if (!enabled.getOpaque() || !busy.weakCompareAndSetAcquire(false, true))
            return;

        try {
            long now = nar.time();
            long last = this.last;
            if (now <= last)
                return;

            this.now = now;

            always(pri.floatValue());

            eventFrame.emit(nar);

            sensors.forEach(s -> s.update(last, now, nar));

            ActionConcept[] aaa = actions.copy.clone(); //HACK shuffle cloned copy for thread safety
            ArrayUtils.shuffle(aaa, random());
            for (ActionConcept a : aaa) {

                //HACK temporary
                if (a instanceof GoalActionConcept)
                    ((GoalActionConcept) a).curiosity(curiosity.get());

                //a.update(last, now, nar);
                long d = Math.max(now-last, nar.dur()); a.update(now-d/2, now+d/2, nar);
            }

            if (trace.getOpaque())
                logger.info(summary());

            this.last = now;

        } finally {
            busy.setRelease(false);
        }

    }

    public long now() {
        return now;
    }

    public float dexterity() {
        return dexterity(now());
    }

    public float dexterity(long when) {
        long d = Math.max(nar().dur(), now-last);
        return dexterity(when - d / 2, when + d/2);
    }

    /**
     * average confidence of actions
     * see: http:
     */
    public float dexterity(long start, long end) {
        int n = actions.size();
        if (n == 0)
            return 0;

        final double[] m = {0};
        actions.forEach(a -> {
            m[0] += a.dexterity(start, end, nar);
        });

        return (float) (m[0] / n);
    }


    public On onFrame(Consumer/*<NAR>*/ each) {
        return eventFrame.on(each);
    }

    public On onFrame(Runnable each) {
        //return DurService.on(nar, ()->{ if (enabled.get()) each.run(); });
        return eventFrame.on((x) -> each.run());
    }


    public Collection<ActionConcept> actions() {
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
            total +=r.summary();
        }
        return total;
    }
}
