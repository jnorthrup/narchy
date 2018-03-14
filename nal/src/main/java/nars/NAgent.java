package nars;

import jcog.TODO;
import jcog.event.On;
import jcog.exe.Loop;
import jcog.list.FasterList;
import jcog.math.*;
import nars.concept.Concept;
import nars.concept.action.ActionConcept;
import nars.concept.scalar.ChronicScalar;
import nars.concept.scalar.DemultiplexedScalar;
import nars.concept.scalar.DigitizedScalar;
import nars.concept.scalar.Scalar;
import nars.control.CauseChannel;
import nars.control.DurService;
import nars.control.NARService;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.var.Variable;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import nars.util.signal.Bitmap2DSensor;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static jcog.Texts.n2;
import static jcog.Util.compose;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * explicit management of sensor concepts and motor functions
 */
abstract public class NAgent extends NARService implements NSense, NAct, Runnable {


    public static final Logger logger = LoggerFactory.getLogger(NAgent.class);
    

    public final Map<Scalar, CauseChannel<ITask>> sensors = new LinkedHashMap();

    @Deprecated public final Set<DigitizedScalar> senseNums = new LinkedHashSet<>();
    @Deprecated public final Set<Bitmap2DSensor<?>> sensorCam = new LinkedHashSet<>();

    public final Map<ActionConcept, CauseChannel<ITask>> actions = new LinkedHashMap();

//    /**
//     * the general reward signal for this agent
//     */
//    @NotNull
//    public final ScalarConcepts reward;


    /**
     * lookahead time in durations (multiples of duration)
     */
//    public final FloatParam predictAheadDurs = new FloatParam(1, 1, 32);


    /**
     * action exploration rate; analogous to epsilon in QLearning
     */
    public FloatRange curiosity;

    /** dampens the dynamically normalized happiness range toward sadness as a motivation strategy */
    public final FloatRange depress = new FloatRange(0.1f, 0f, 1f);


    public final AtomicBoolean enabled = new AtomicBoolean(false);

    public DemultiplexedScalar happy;

    public boolean trace;

    public long now = ETERNAL, last = ETERNAL; //not started


    /**
     * range: -1..+1
     */
    public volatile float reward;

    public NAR nar = null;
    private CauseChannel<ITask> in = null;

    private int dur;

    public final FloatRange motivation = new FloatRange(0f, 0f, 1f);
    protected List<Task> always = $.newArrayList();



    protected NAgent(@NotNull NAR nar) {
        this("", nar);
    }

    protected NAgent(String id, NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), nar);
    }
    @Deprecated protected NAgent(Term id, NAR nar) {
        super(id);
        this.nar = nar;

        this.curiosity = new FloatRange(0.10f, 0f, 1f);

        if (nar!=null)
            nar.on(this);
    }

    protected NAgent() {
        this("", null);
    }

    protected NAgent(@Nullable Term id) {
        this(id, null);
    }

    @Deprecated public void alwaysWant(Iterable<Termed> x, float conf) {
        x.forEach(xx -> alwaysWant(xx, conf));
    }

    @Deprecated public NALTask alwaysWant(Termed x, float conf) {
        NALTask t = new NALTask(x.term(), GOAL, $.t(1f, conf), now,
                ETERNAL, ETERNAL,
                //Stamp.UNSTAMPED
                nar().time.nextStampArray()
        );

        always.add(t);
        return t;
    }

    @Deprecated public NALTask alwaysQuestion(Termed x) {
        NALTask t = new NALTask(x.term(), QUESTION, null, now,
                ETERNAL, ETERNAL,
                //Stamp.UNSTAMPED
                nar().time.nextStampArray()
        );

        always.add(t);
        return t;
    }

    @Deprecated
    public On runDur(int everyDurs) {
        int dur = nar.dur();
        int everyCycles = dur * everyDurs;
        return nar.onCycle(i -> {
            if (nar.time() % everyCycles == 0)
                NAgent.this.run();
        });
    }

    public Loop runFPS(float fps) {
        return new Loop(fps) {
            @Override
            public boolean next() {
                NAgent.this.run();
                return true;
            }
        };
    }

    @Override
    public FloatRange curiosity() {
        return curiosity;
    }

    @NotNull
    @Override
    public final Map<Scalar, CauseChannel<ITask>> sensors() {
        return sensors;
    }

    @NotNull
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
        NAR nar = this.nar;
        return nar!=null ? nar.random() : ThreadLocalRandom.current();
    }

    @NotNull
    public String summary() {

        //sendInfluxDB("localhost", 8089);

        return id + " rwrd=" + n2(reward) +
                " dex=" + /*n4*/(dexterity(now, now)) +
                //"\t" + Op.cache.summary() +
                /*" var=" + n4(varPct(nar)) + */ "\t" + nar.concepts.summary() + " " +
                nar.emotion.summary();
    }

    /**
     * registers sensor, action, and reward concepts with the NAR
     * TODO call this in the constructor
     */
    @Override
    protected void start(NAR nar) {
        synchronized (this) {

            assert(this.nar == null || this.nar == nar);
            this.nar = nar;

            super.start(nar);

            motivation.set(nar.priDefault(GOAL));

            Term happyTerm = id == null ?
                    $.the("happy") : //generally happy
                    $.inh(id, $.the("happy")); //happy in this environment
                    //$.prop(id, $.the("happy")); //happiness of this environment

//            FloatSupplier happyValue = new FloatCached(
//                    new FloatNormalized(
//                    //new FloatPolarNormalized(
//                    //new FloatHighPass(
//                    () -> reward
//                    //)
//            ) {
//                        @Override
//                        public float asFloat() {
//                            float f = super.asFloat();
//                            if (f!=f) return Float.NaN;
//                            else {
//                                f = Util.unitize(f);
//
//                                //assert(f >= 0 && f <= 1f);
//
//                                //depression curve and offset
//                                return Util.max(0,f - depress.floatValue());
//                            }
//                        }
//                        //                        @Override
////                        public float min() {
////                            return Util.lerp(depress.floatValue(), super.max(), super.min());
////                        }
////
////                        @Override
////                        public float max() {
////                            //decrease the max toward min in proportion to the depression setting
////                            return Util.lerp(depress.floatValue(), super.max(), super.min());
////                        }
//                    }.relax(Param.HAPPINESS_RE_SENSITIZATION_RATE),
//                nar::time);

            FloatSupplier happyValue = new FloatCached(
                    () -> reward - depress.floatValue(),
                    nar::time
            );





            this.happy =
                    //new ActionInfluencingScalar(happyTerm, happyValue);
                    ChronicScalar.filter(
                            happyTerm,
                            happyValue,
                            nar,

                            //happiness (raw)
                            new FloatNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE),

                            //long-term happiness
                            compose(

                                    new FloatNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE),
                                    new FloatExpMovingAverage(0.02f)
                            ),

                            //joy
                            compose(
                                new FloatExpMovingAverage(0.1f, false),
                                new FloatPolarNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE_FAST)
                            )
                    );

            //onFrame(happy);

            alwaysWant((Iterable)happy, nar.confDefault(GOAL));

            actions.keySet().forEach(a ->
                    //alwaysQuest(a)
                    alwaysQuestion(Op.IMPL.the(happy.term, XTERNAL, a.term))
            );

            this.in = nar.newCauseChannel(this);
            this.dur = nar.dur();
            this.now = nar.time();
            this.last = now - dur; //head-start

            //finally:
            enabled.set(true);
        }
    }

    @Override
    protected void stopping(NAR nar) {
        //disconnect channel
        //remove all other services
        throw new TODO();
    }


    protected void always(float activation) {
        for (int i = 0, alwaysSize = always.size(); i < alwaysSize; i++) {
            Task x = always.get(i);
            x.priMax(
                    //nar.priDefault(GOAL)
                    activation
            );

            //nar.activate(x, activation);
        }

        in.input(always);
    }


    @Override
    public synchronized void run() {
        if (!enabled.get())
            return;

        this.last = this.now;
        this.now = nar.time();
        if (now == last)
            return;

        this.dur = nar.dur();

        reward = act();

        happy.update(last, now, nar);

        FloatFloatToObjectFunction<Truth> truther = (prev, next) -> $.t(next, nar.confDefault(BELIEF));
        sensors.entrySet().forEach( (sc) -> {
            sc.getValue().input(sc.getKey().update(last, now, truther, nar));
        });

        always(motivation.floatValue() );

        //HACK TODO compile this to re-used array on init like before
        Map.Entry<ActionConcept, CauseChannel<ITask>>[] aa = actions.entrySet().toArray(new Map.Entry[actions.size()]);
        ArrayUtils.shuffle(aa, random()); //fair chance of ordering to all motors
        for (Map.Entry<ActionConcept, CauseChannel<ITask>> ac : aa) {
            Stream<ITask> s = ac.getKey().update(last, now, dur, NAgent.this.nar);
            if (s != null)
                ac.getValue().input(s);
        }

        Truth happynowT = nar.beliefTruth(happy, last, now);
        float happynow = happynowT != null ? (happynowT.freq() - 0.5f) * 2f : 0;
        nar.emotion.happy(motivation.floatValue() * dexterity(last, now) * happynow /* /nar.confDefault(GOAL) */);

        if (trace)
            logger.info(summary());
    }

//    /** creates an activator specific to this agent context */
//    public Consumer<Predicate<Activate>> fire() {
//        return p -> {
//            Activate a;
//
//            final int numConcepts = concepts.size();
//            int remainMissing = numConcepts;
//            if (remainMissing == 0) return;
//
//            float pri = motivation.floatValue();
//            Random rng = nar.random();
//            do {
//                Concept cc = nar.conceptualize(concepts.get(rng.nextInt(numConcepts)));
//                if (cc!=null) {
//                    a = new Activate(cc, 0);
//                    a.delete(); //prevents termlinking
//                } else {
//                    a = null;
//                    if (remainMissing-- <= 0) //safety exit
//                        break;
//                    else
//                        continue;
//                }
//            } while (a==null || p.test(a));
//        };
//    }


    /** default rate = 1 dur/ 1 frame */
    public void runSynch(int frames) {
        DurService d = DurService.on(nar, this);
        nar.run(frames * nar.dur() + 1);
        d.off();
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

            this.predict = nar.newCauseChannel(a.id + " predict");


            //            final Task[] prevHappy = new Task[1];

//                Task se = new NALTask(sad.term(), GOAL, $.t(0f, nar.confDefault(GOAL)), nar.time(), ETERNAL, ETERNAL, nar.time.nextInputStamp());
//                se.pri(happysadPri);
//                predictors.add(() -> {
//                    se.priMax(happysadPri);
//                    return se;
//                });

//            {
//                Task e = nar.goal($.parallel(happy.term(),sad.term().neg())); /* eternal */
//                predictors.add(() -> {
//                    e.priMax(nar.priDefault(GOAL));
//                    return e;
//                });
//                Task f = nar.believe($.sim(happy.term(), sad.term().neg()));
//                predictors.add(() -> f);
//                Task g = nar.believe($.sim(happy.term().neg(), sad.term()));
//                predictors.add(() -> g);
//            }
//            {
//                Task happyEternal = nar.goal(happy.term()); /* eternal */
//                predictors.add(() -> {
//                    happyEternal.priMax(nar.priDefault(GOAL));
//                    return happyEternal;
//                });
//            }
//            {
//                Task sadEternal = nar.goal(sad.term().neg()); /* eternal */
//                predictors.add(() -> {
//                    sadEternal.priMax(nar.priDefault(GOAL));
//                    return sadEternal;
//                });
//            }

            //        p.add(
            //            question(seq($.varQuery(1), dur, happiness),
            //                now)
            //                //ETERNAL)
            //        );


            //        predictors.add( question((Compound)$.parallel(happiness, $.varDep(1)), now) );
            //        predictors.add( question((Compound)$.parallel($.neg(happiness), $.varDep(1)), now) );

            Variable what = $.varQuery(1);

            predictors.add(question($.impl(what, a.happy.term())));
            //predictors.add(question($.impl(sad.term(), 0, what)));

            for (Concept c : a.actions.keySet()) {
                Term action = c.term();

                Term notAction = action.neg();

                ((FasterList) predictors).addingAll(

                        question($.impl(CONJ.the(what, a.happy.term()), action)),
                        question($.impl(CONJ.the(what, a.happy.term().neg()), action))
                        //question($.impl(sad.term(), 0, action)),
//                        question($.impl(action, sad.term())),
//                        question($.impl(notAction, sad.term())),
//                        question($.impl(action, what)),
//                        question($.impl(notAction, what))
                        //quest(action)
//                        quest($.parallel(what, action)),
//                        quest($.parallel(what, notAction))

//                        question(impl(parallel(what, action), happy)),
//                        question(impl(parallel(what, notAction), happy)),

                        //question(seq(action, dur, happiness), now),
                        //question(seq(neg(action), dur, happiness), now),

                        //question(seq(action, dur, $.varQuery(1)), now),
                        //question(seq(neg(action), dur, $.varQuery(1)), now),

                        //dangerous: may lead to immobilizing self-fulfilling prophecy
                        //quest((Compound) (action.term()),now+dur)

                        //                            //ETERNAL)

                        //question((Compound)$.parallel(varQuery(1), (Compound) (action.term())), now),

                        //quest($.parallel(what, action))

                        //quest((Compound)$.parallel(varQuery(1), happy.term(), (Compound) (action.term())), now)


                        //                    question(impl(conj(varQuery(0),action), dur, happiness), now),
                        //                    question(impl(conj(varQuery(0),neg(action)), dur, happiness), now)

                        //                    new PredictionTask($.impl(action, dur, happiness), '?').time(nar, dur),
                        //                    new PredictionTask($.impl($.neg(action), dur, happiness), '?').time(nar, dur),

                        //                    new PredictionTask($.impl($.parallel(action, $.varQuery(1)), happiness), '?')
                        //                            .eternal(),
                        //                            //.time(nar, dur),
                        //                    new PredictionTask($.impl($.parallel($.neg(action), $.varQuery(1)), happiness), '?')
                        //                            .eternal(),
                        //                            //.time(nar, dur)

                        //question(impl(neg(action), dur, varQuery(1)), nar.time()),

                        //                    question(impl(happiness, -dur, conj(varQuery(1),action)), now),
                        //                    question(impl(neg(happiness), -dur, conj(varQuery(1),action)), now)

                        //                    question(impl(happiness, -dur, action), now),
                        //                    question(impl(neg(happiness), -dur, action), now)


                        //                    question(seq(action, dur, happiness), now),
                        //                    question(seq(neg(action), dur, happiness), now),
                        //                    question(seq(action, dur, neg(happiness)), now),
                        //                    question(seq(neg(action), dur, neg(happiness)), now)


                        //                    new PredictionTask($.seq($.varQuery("x"), 0, $.seq(action, dur, happiness)), '?').eternal(),
                        //                    new PredictionTask($.seq($.varQuery("x"), 0, $.seq($.neg(action), dur, happiness)), '?').eternal()


                        //                    new PredictionTask($.seq(action, dur, varQuery(1)), '@')
                        //                        .present(nar),
                        //
                        //
                        //                    new PredictionTask($.seq($.neg(action), dur, varQuery(1)), '@')
                        //                        .present(nar)

                        //                    new TaskBuilder($.impl(action, dur, happiness), '?', null)
                        //                            .present(nar),
                        //                            //.eternal(),
                        //                    new TaskBuilder($.impl($.neg(action), dur, happiness), '?', null)
                        //                            .present(nar)
                        //                            //.eternal()


                        //new TaskBuilder($.seq($.varQuery(0), dur, action), '?', null).eternal(),
                        //new TaskBuilder($.impl($.varQuery(0), dur, action), '?', null).eternal(),

                        //new TaskBuilder($.impl($.parallel($.varDep(0), action), dur, happiness), '?', null).time(now, now + dur),
                        //new TaskBuilder($.impl($.parallel($.varDep(0), $.neg( action )), dur, happiness), '?', null).time(now, now + dur)
                );

            }

            //        predictors.add(
            //                new TaskBuilder($.seq($.varQuery(0 /*"what"*/), dur, happiness), '?', null).time(now, now)
            //        );
            //        predictors.add(
            //                goal(happiness,
            //                        t(1f, Math.max(nar.confDefault(/*BELIEF*/ GOAL),nar.confDefault(/*BELIEF*/ BELIEF))),
            //                        ETERNAL
            //                )
            //        );


            //        predictors.addAll(
            //                //what will imply reward
            //                new TaskBuilder($.equi(what, dt, happiness), '?', null).time(now, now),
            //                //new TaskBuilder($.equi(sth, dt, happiness), '.', null).time(now,now),
            //
            //                //what will imply non-reward
            //                //new TaskBuilder($.equi(what, dt, $.neg(happiness)), '?', null).time(now, now),
            //                //new TaskBuilder($.equi(sth, dt, $.neg(happiness)), '.', null).time(now,now),
            //
            //                //what co-occurs with reward
            //                new TaskBuilder($.parallel(what, happiness), '?', null).time(now, now)
            //
            //                //what co-occurs with non-reward
            //                //new TaskBuilder($.parallel(what, $.neg(happiness)), '?', null).time(now, now)
            //        );

            //        predictors.add(
            //                nar.ask($.seq(what, dt, happy.term()), '?', now)
            //        );
            //        predictors.add( //+2 cycles ahead
            //                nar.ask($.seq(what, dt*2, happy.term()), '?', now)
            //        );


            //System.out.println(Joiner.on('\n').join(predictors));

        }

        public Supplier<Task> question(@NotNull Term term) {
            return prediction(term, QUESTION, null);
        }

        public Supplier<Task> quest(@NotNull Term term) {
            return prediction(term, QUEST, null);
        }

        Supplier<Task> prediction(@NotNull Term _term, byte punct, DiscreteTruth truth) {
            Term term = _term.normalize();

            long now = nar.time();

//        long start = now;
//        long end = now + Math.round(predictAheadDurs.floatValue() * nar.dur());

            long start = ETERNAL, end = ETERNAL;

            NALTask t = new NALTask(term, punct, truth, now,
                    start, end,
                    new long[]{nar.time.nextStamp()});

            return () -> {

                Task u;
                if (t.isEternal()) {
                    u = t;
                } else {
                    long nownow = nar.time();
                    //TODO handle task duration
                    u = new NALTask(t.term(), t.punc(), t.truth(), nownow, nownow, nownow, new long[]{nar.time.nextStamp()});
                }

                u.priMax(nar.priDefault(u.punc()));

                return u;
            };
        }

        @Override
        protected void run(NAR n, long dt) {
            predict.input(predictions(nar.time(), predict.amp()));
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
     * see: http://www.dictionary.com/browse/dexterity?s=t
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
                //c = g.evi();
                c = g.conf();
            } else {
                c = 0;
            }
            m[0] += c;
        });
        //return m[0] > 0 ? w2c(m[0] / n /* avg */) : 0;
        return m[0] > 0 ? m[0] / n /* avg */ : 0;
    }


//    private Task predict(@NotNull Supplier<Task> t, long next, int horizon /* future time range */) {
//
//        Task result;
////        if (t.start() != ETERNAL) {
////
////            //only shift for questions
////            long shift = //horizon > 0 && t.isQuestOrQuestion() ?
////                    nar.random().nextInt(horizon)
////                    //: 0
////            ;
////
////            long range = t.end() - t.start();
////            result = prediction(t.term(), t.punc(), t.truth(), next + shift, next + shift + range);
////
////        } else if (t.isDeleted()) {
////
////            result = prediction(t.term(), t.punc(), t.truth(), ETERNAL, ETERNAL);
////
////        } else {
//            //rebudget non-deleted eternal
////            result = t;
////        }
//
//        return result
//                .budget(nar)
//                ;
//    }


//    public static float varPct(NAR nar) {
//            RecycledSummaryStatistics is = new RecycledSummaryStatistics();
//            nar.forEachConceptActive(xx -> {
//                Term tt = xx.term();
//                float v = tt.volume();
//                int c = tt.complexity();
//                is.accept((v - c) / v);
//            });
//
//            return (float) is.getMean();
//
//    }


    @Override
    public final float alpha() {
        return nar.confDefault(BELIEF);
    }

    @Override
    public DurService onFrame(Consumer/*<NAR>*/ each) {
        if (each instanceof DigitizedScalar) {
            senseNums.add((DigitizedScalar)each);
        }
        return DurService.on(nar, ()->{ if (enabled.get()) each.accept(nar); });
    }

    public DurService onFrame(Runnable each) {
        return DurService.on(nar, ()->{ if (enabled.get()) each.run(); });
    }


    /** adds the actions to its set of termlink templates */
    protected class ActionInfluencingScalar extends Scalar {

        List<Termed> templatesPlusActions;

        public ActionInfluencingScalar(Term id, FloatNormalized value) {
            super(id, value, NAgent.this.nar());
            templatesPlusActions = null;


            addSensor(this);
        }

        @Override
        public List<Termed> templates() {
            List<Termed> superTemplates = super.templates();
            //HACK
            if (templatesPlusActions == null || templatesPlusActions.size() != (superTemplates.size() + actions.size())) {
                List<Termed> l = $.newArrayList(superTemplates.size() + actions.size());
                l.addAll(superTemplates);
                l.addAll(actions.keySet());
                this.templatesPlusActions = l;
            }
            return templatesPlusActions;
        }
    }
}
