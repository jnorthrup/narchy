package nars.agent;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.math.*;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.action.ActionConcept;
import nars.concept.action.GoalActionConcept;
import nars.concept.sensor.DigitizedScalar;
import nars.concept.sensor.FilteredScalar;
import nars.concept.sensor.Signal;
import nars.control.DurService;
import nars.control.channel.CauseChannel;
import nars.link.Activate;
import nars.sensor.Bitmap2DSensor;
import nars.table.DefaultBeliefTable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.TimeAware;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static jcog.Texts.n2;
import static jcog.Util.compose;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * an integration of sensor concepts and motor functions
 */
abstract public class NAgent extends DurService implements NSense, NAct {


    public static final Logger logger = LoggerFactory.getLogger(NAgent.class);


    public final Map<Signal, CauseChannel<ITask>> sensors = new LinkedHashMap();

    @Deprecated
    public final Set<DigitizedScalar> senseNums = new LinkedHashSet<>();
    @Deprecated
    public final Set<Bitmap2DSensor<?>> sensorCam = new LinkedHashSet<>();

    public final Map<ActionConcept, CauseChannel<ITask>> actions = new LinkedHashMap();
    final Topic<NAR> frame = new ListTopic();

    /**
     * list of concepts involved in this agent
     */
    public final ArrayHashSet<Concept> concepts = new ArrayHashSet();


    /**
     * lookahead time in durations (multiples of duration)
     */


    /**
     * action exploration rate; analogous to epsilon in QLearning
     */
    public FloatRange curiosity;
    public FloatRange motivation = new FloatRange(1f, 0, 2f);

    public final AtomicBoolean enabled = new AtomicBoolean(false);

    public FilteredScalar happy;

    public boolean trace;


    /**
     * range: -1..+1
     */
    public volatile float reward;


    private CauseChannel<ITask> in = null;

    public final List<Supplier<Task>> always = $.newArrayList();


    private volatile long last;


    protected NAgent(NAR nar) {
        this("", nar);
    }

    protected NAgent(String id, NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), nar);
    }

    @Deprecated
    protected NAgent(Term id, NAR nar) {
        super(id);
        this.nar = nar;

        this.curiosity = new FloatRange(0.10f, 0f, 1f);

//        if (nar!=null) {
//            nar.on(this);
//        }
    }

    protected NAgent() {
        this("", null);
    }

    protected NAgent(@Nullable Term id) {
        this(id, null);
    }

//    @Deprecated public void alwaysWant(Iterable<Termed> x, float conf) {
//        x.forEach(xx -> alwaysWant(xx, conf));
//    }

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
            long now = Tense.dither(this.now(), nar);
            long next = Tense.dither(this.now() + nar.dur(), nar);
            return new NALTask(x.term(), GOAL, $.t(1f, confFactor * nar.confDefault(GOAL)), now,
                    now, next,
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
                //nar.evidence()
                Stamp.UNSTAMPED

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


    /**
     * interpret motor states into env actions
     */
    protected abstract float act();


    public Random random() {
        TimeAware timeAware = this.nar;
        return timeAware != null ? timeAware.random() : ThreadLocalRandom.current();
    }


    public String summary() {


        return id + " rwrd=" + n2(reward) +
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

        this.happy =

                new FilteredScalar(
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

        //happy.pri(()->motivation.floatValue()*nar.priDefault(BELIEF));

        this.in = nar.newChannel(this);


        init(nar);

        alwaysWantEternally(happy.filter[0].term, nar.confDefault(GOAL));
        alwaysWantEternally(happy.filter[1].term, nar.confDefault(GOAL) /* * 0.5f */); //chronic
        alwaysWantEternally(happy.filter[2].term, nar.confDefault(GOAL) * 0.5f); //acute
        for (FilteredScalar.Filter x : happy.filter) {
            ((DefaultBeliefTable) x.beliefs()).eternal.setCapacity(0); //HACK this should be an Empty table

            //should normally be able to create these beliefs but if you want to filter more broadly:
            //((DefaultBeliefTable)x.goals()).temporal.setCapacity(0); //HACK this should be an Empty table

        }

        actions.keySet().forEach(a -> {
            alwaysQuest(a, true);
            //alwaysQuestion(Op.CONJ.the(happy.term, a.term));
            //alwaysQuestion(Op.CONJ.the(happy.term, a.term.neg()));
        });

        concepts.addAll(actions.keySet());
        concepts.addAll(sensors.keySet());
        //always.forEach(t -> concepts.add(t.concept(nar,true)));
        Iterables.addAll(concepts, happy);

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


    @Override
    protected void run(NAR n, long dt) {
        if (!enabled.getOpaque())
            return;

        long now = nar.time();
        long last = this.last;
        this.last = now;
        if (now <= last)
            return;

        reward = act();

        frame.emit(nar);


        happy.update(last, now, nar);

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

        Truth happynowT = nar.beliefTruth(happy, last, now);
        float happynow = happynowT != null ? (happynowT.freq() - 0.5f) * 2f : 0;
        nar.emotion.happy(/* motivation.floatValue() * */ dexterity(last, now) * happynow /* /nar.confDefault(GOAL) */);

        if (trace)
            logger.info(summary());
    }


    /**
     * creates an activator specific to this agent context
     */
    public Consumer<Predicate<Activate>> fire() {

        return p -> {
            Activate a;

            final int numConcepts = concepts.size();
            int remainMissing = numConcepts;
            if (remainMissing == 0) return;


            Random rng = nar.random();
            do {
                Concept cc = concepts.get(rng.nextInt(numConcepts));
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

    /**
     * creates an activator specific to this agent context
     */
    public Consumer<Predicate<Activate>> fireActions() {

        Concept[] ac = this.actions.keySet().toArray(Concept.EmptyArray);
        return p -> {
            Activate a;

            final int numConcepts = ac.length;
            int remainMissing = numConcepts;
            if (remainMissing == 0) return;


            Random rng = nar.random();
            do {
                Concept cc = ac[rng.nextInt(numConcepts)];
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


    /**
     * experimental nagging question feed about how to make an agent happy
     */
    public static class AgentPredictions extends DurService {


        /**
         * prediction templates
         */
        public final List<Supplier<Task>> predictors = $.newArrayList();
        private final CauseChannel<ITask> predict;

        public AgentPredictions(NAgent a) {
            super(a.nar);

            this.predict = nar.newChannel(a.id + " predict");


            Variable what = $.varQuery(1);

            predictors.add(question($.impl(what, a.happy.term())));


            for (Concept c : a.actions.keySet()) {
                Term action = c.term();

                Term notAction = action.neg();

                ((FasterList) predictors).addingAll(

                        question($.impl(CONJ.the(what, a.happy.term()), action)),
                        question($.impl(CONJ.the(what, a.happy.term().neg()), action))


                );

            }


        }

        public Supplier<Task> question(Term term) {
            return prediction(term, QUESTION, null);
        }

        public Supplier<Task> quest(Term term) {
            return prediction(term, QUEST, null);
        }

        Supplier<Task> prediction(Term _term, byte punct, Truth truth) {
            Term term = _term.normalize();

            long now = nar.time();


            long start = ETERNAL, end = ETERNAL;

            Task t = new NALTask(term, punct, truth, now,
                    start, end,
                    nar.evidence());

            return () -> {

                Task u;
                if (t.isEternal()) {
                    u = t;
                } else {
                    long nownow = nar.time();

                    u = new NALTask(t.term(), t.punc(), t.truth(), nownow, nownow, nownow, new long[]{nar.time.nextStamp()});
                }

                u.pri(nar.priDefault(u.punc()));

                return u;
            };
        }

        @Override
        protected void run(NAR n, long dt) {
            predict.input(predictions(nar.time(), 1));
        }

        protected Stream<ITask> predictions(long now, float prob) {
            return predictors.stream().filter((x) -> nar.random().nextFloat() <= prob).map(x -> x.get().budget(nar));
        }

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


    @Override
    public void addSensor(Signal s, CauseChannel cause) {
        CauseChannel<ITask> existing = sensors.put(s, cause);
        assert (existing == null);
    }


    @Override
    public On onFrame(Consumer/*<NAR>*/ each) {
        if (each instanceof DigitizedScalar) {
            senseNums.add((DigitizedScalar) each);
        }
        return frame.on(each);
    }

    public On onFrame(Runnable each) {
        //return DurService.on(nar, ()->{ if (enabled.get()) each.run(); });
        return frame.on((x) -> each.run());
    }


}
