package nars;

import jcog.Util;
import jcog.event.On;
import jcog.exe.Loop;
import jcog.list.FasterList;
import jcog.math.FloatNormalized;
import jcog.math.FloatParam;
import jcog.math.FloatPolarNormalized;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.control.Activate;
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
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static jcog.Texts.n2;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * explicit management of sensor concepts and motor functions
 */
abstract public class NAgent extends NARService implements NSense, NAct, Runnable {


    public static final Logger logger = LoggerFactory.getLogger(NAgent.class);
    

    public final Map<SensorConcept, CauseChannel<ITask>> sensors = new LinkedHashMap();

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
    public final FloatParam curiosity;


    public final AtomicBoolean enabled = new AtomicBoolean(false);

    public final SensorConcept happy;
    private final CauseChannel<ITask> in;
    ///public final SensorConcept sad;

    public boolean trace;

    public long now;


    /**
     * range: -1..+1
     */
    public float reward;
    public final NAR nar;
    private int dur;

    public final FloatParam motivation = new FloatParam(1f, 0f, 1f);
    protected List<Task> always = $.newArrayList();

    /** concepts (which are present at start time) */
    private List<Termed> concepts;

    protected NAgent(@NotNull NAR nar) {
        this("", nar);
    }

    protected NAgent(@NotNull String id, @NotNull NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), nar);
    }

    protected NAgent(@Nullable Term id, @NotNull NAR nar) {
        super(null, id);

        this.nar = nar;
        this.in = nar.newCauseChannel(this);

        this.now = ETERNAL; //not started

        Term happyTerm = id == null ?
                $.the("happy") : //generally happy
                $.inh(id, $.the("happy")); //happy in this environment

        FloatNormalized happyValue = new FloatPolarNormalized(
                //new FloatHighPass(
                () -> reward
                //)
        ).relax(Param.HAPPINESS_RELAXATION_RATE);

        this.happy = new ActionInfluencingSensorConcept(happyTerm, happyValue);



//        this.reward = senseNumber(new FloatPolarNormalized(() -> rewardCurrent), ScalarConcepts.Mirror,
//                id == null ?
//                        $.the("happy") : //generally happy
//                        $.p(id, $.the("happy")), //happy in this environment
//                id == null ?
//                        $.the("sad") : //generally sad
//                        $.p(id, $.the("sad")) //sad in this environment
//        );
        //happy = this.reward.sensors.get(0);
        //sad = this.reward.sensors.get(1);

        //fireHappy = Activation.get(happy, 1f, new ConceptFire(happy, 1f);

        curiosity = new FloatParam(0.10f, 0f, 1f);


//        if (id == null) id = $.quote(getClass().toString());

        nar.on(this);
    }

    public NALTask alwaysWant(Termed x, float conf) {
        NALTask t = new NALTask(x.term(), GOAL, $.t(1f, conf), now,
                ETERNAL, ETERNAL,
                Stamp.UNSTAMPED);

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
    public FloatParam curiosity() {
        return curiosity;
    }

    @NotNull
    @Override
    public final Map<SensorConcept, CauseChannel<ITask>> sensors() {
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


    @NotNull
    public String summary() {

        //sendInfluxDB("localhost", 8089);

        return id + " rwrd=" + n2(reward) +
                " dex=" + /*n4*/(dexterity(now, now)) +
                //"\t" + Op.cache.summary() +
                /*" var=" + n4(varPct(nar)) + */ "\t" + nar.terms.summary() + " " +
                nar.emotion.summary();
    }

    /**
     * registers sensor, action, and reward concepts with the NAR
     * TODO call this in the constructor
     */
    @Override
    protected void start(NAR nar) {

        this.now = nar.time();

        super.start(nar);

        enabled.set(true);

        List<Termed> cc = new FasterList();
        cc.add(happy);
        cc.addAll(actions.keySet());
        cc.addAll(sensors.keySet());
        this.concepts = cc;

        alwaysWant(happy, nar.confDefault(GOAL));


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
    public void run() {
        if (!enabled.get()) {
            return;
        }


        this.dur = nar.dur();
        this.now = nar.time();

        reward = act();

        sensors.forEach((s, c) -> {
            c.input(s.update(now, dur, nar));
        });

        always(motivation.floatValue());


        actions.forEach((a, c) -> {
            Stream<ITask> s = a.update(now, dur, nar);
            if (s != null)
                c.input(s);
        });


        Truth happynowT = nar.beliefTruth(happy, now);
        float happynow = happynowT != null ? (happynowT.freq() - 0.5f) * 2f : 0;
        nar.emotion.happy(motivation.floatValue() * dexterity(now, now) * happynow /* /nar.confDefault(GOAL) */);

        if (trace)
            logger.info(summary());
    }

    /** creates an activator specific to this agent context */
    public Consumer<Predicate<Activate>> fire() {
        return p -> {
            Activate a;

            final int numConcepts = concepts.size();
            int remainMissing = numConcepts;
            if (remainMissing == 0) return;

            float pri = motivation.floatValue();
            Random rng = nar.random();
            do {
                Concept cc = nar.conceptualize(concepts.get(rng.nextInt(numConcepts)));
                if (cc!=null)
                    a = new Activate(cc, pri);
                else {
                    a = null;
                    if (remainMissing-- <= 0) //safety exit
                        break;
                    else {
                        continue;
                    }
                }
            } while (a==null || p.test(a));
        };
    }

    /** concepts involved in this agent */
    public List<Termed> concepts() {
        return concepts;
    }


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

                        question($.impl($.conj(what, a.happy.term()), action)),
                        question($.impl($.conj(what, a.happy.term().neg()), action))
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
        return DurService.on(nar, ()->{ if (enabled.get()) each.accept(nar); });
    }

    public DurService onFrame(Runnable each) {
        return DurService.on(nar, ()->{ if (enabled.get()) each.run(); });
    }


    /** adds the actions to its set of termlink templates */
    protected class ActionInfluencingSensorConcept extends SensorConcept {

        List<Termed> templatesPlusActions;

        public ActionInfluencingSensorConcept(Term id, FloatNormalized value) {
            super(id, NAgent.this.nar(), value,
                    (x) -> $.t(Util.unitize(x),
                    NAgent.this.nar().confDefault(Op.BELIEF)));
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
