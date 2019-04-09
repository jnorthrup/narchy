package nars.test;

import jcog.data.list.FasterList;
import jcog.event.ByteTopic;
import nars.*;
import nars.control.MetaGoal;
import nars.task.ITask;
import nars.task.Tasked;
import nars.term.Term;
import nars.term.util.TermException;
import nars.test.condition.NARCondition;
import nars.test.condition.TaskCondition;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.function.LongPredicate;

import static java.lang.Float.NaN;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * TODO use a countdown latch to provide early termination for successful tests
 */
public class TestNAR {

    static {  Param.class.toString();     } //force Param's static initialization first

    private static final Logger logger = LoggerFactory.getLogger(TestNAR.class);
    public final NAR nar;
    /**
     * holds must (positive) conditions
     */
    private final FasterList<NARCondition> succeedsIfAll = new FasterList();
    /**
     * holds mustNot (negative) conditions which are tested at the end
     */
    private final FasterList<NARCondition> failsIfAny = new FasterList();
    public boolean quiet = false;
    public boolean requireConditions = true;
    
    /**
     * -1 = failure,
     * 0 = hasnt been determined yet by the end of the test,
     * (1..n) = success in > 1 cycles,
     * +1 = success in <= 1 cycles
     */
    public float score;
    /**
     * enable this to print reports even if the test was successful.
     * it can cause a lot of output that can be noisy and slow down
     * the test running.
     * TODO separate way to generate a test report containing
     * both successful and unsuccessful tests
     */
    private final boolean collectTrace = false;
    private final int temporalTolerance = 0;
    private final float freqTolerance = Param.TESTS_TRUTH_ERROR_TOLERANCE;
    private float confTolerance = Param.TESTS_TRUTH_ERROR_TOLERANCE;
    private final ByteTopic<Tasked>[] taskEvents;
    private boolean finished;
    private boolean exitOnAllSuccess = true;
    private final boolean reportStats = false;

    public TestNAR(NAR nar) {
        this.nar = nar;

        this.taskEvents = new ByteTopic[]{
                nar.eventTask,
        };

    }

    public TestNAR confTolerance(float t) {
        this.confTolerance = t;
        return this;
    }

    public static class TestNARResult implements Serializable {
        final boolean success;
        public final boolean error;

        TestNARResult(boolean success, boolean error) {
            this.success = success;
            this.error = error;
        }
        //TODO long wallTimeNS;
        //TODO etc
    }

    public void run(long finalCycle) {
        //NDC.push(this.toString());
//        try {
            TestNARResult result = _run(finalCycle);
            assertTrue(result.success);
//        } finally {
//            NDC.pop();
//        }
    }

    private TestNARResult _run(long finalCycle) {


        score = 0; //Float.NEGATIVE_INFINITY;

        if (requireConditions)
            assertTrue(!succeedsIfAll.isEmpty() || !failsIfAny.isEmpty(), "no conditions tested");


        String id = succeedsIfAll.toString();

        if (finalCycle <= 0) {
            //auto-compute final cycle
            for (NARCondition oc : succeedsIfAll) {
                long oce = oc.getFinalCycle();
                if (oce > finalCycle) finalCycle = oce + 1;
            }
            for (NARCondition oc : failsIfAny) {
                long oce = oc.getFinalCycle();
                if (oce > finalCycle) finalCycle = oce + 1;
            }
        }

        score = -finalCycle; //default score
        score = Math.min(-1, finalCycle);

        StringWriter trace;
        if (collectTrace)
            nar.trace(trace = new StringWriter());
        else
            trace = null;

        if (exitOnAllSuccess) {
            new EarlyExit(1);
        }

        long startTime = nar.time();


        boolean error;
        try {
            runUntil(finalCycle);
            error = false;
        } catch (Throwable t) {
            logger.error("{} {}", this, t);
            t.printStackTrace();
            error = true;
        }

        boolean success = !error;
        if (success) {
            for (NARCondition t : succeedsIfAll) {
                if (!t.isTrue()) {
                    success = false;
                    break;
                }
            }
        }
        if (success) {
            for (NARCondition t : failsIfAny) {
                if (t.isTrue()) {
                    success = false;
                    break;
                }
            }
        }


        long endTime = nar.time();
        int runtime = Math.max(0, (int) (endTime - startTime));

        assert(runtime <= finalCycle);

        if (success)
            score = -runtime;

//        this.score = success ?
//
////                1 + (+1f / (1f + ((1f + runtime) / (1f + Math.max(0,finalCycle - startTime)))))
////                :
////                0
//
//                ;


        if (!quiet) {
            if (reportStats) {
                logger.info("{}\n\t{} {} {}IN \ninputs", id, endTime);
            }

            for (NARCondition t : failsIfAny) {
                if (t.isTrue()) {

                    if (!quiet) {
                        t.log(logger);

                        //TODO move this to TaskCondition for negative-specific cases
                        ((TaskCondition) t).matched.forEach(shouldntHave ->
                                logger.warn("shouldNot: \t{}\n{}", shouldntHave.proof(), MetaGoal.proof(shouldntHave, nar))
                        );
                    }

                }
            }

            if (!quiet) {
                succeedsIfAll.forEach(t -> {
                    if (!t.isTrue())
                        logger.warn("should: {}", t);
                });


                if (trace != null)
                    logger.trace("{}", trace.getBuffer());
            }

            if (reportStats) {
                nar.feel.print(System.out);
                nar.stats(System.out);
            }
        }



        return success ? new TestNARResult(true, false) : new TestNARResult(false, error);
    }


    private TestNAR runUntil(long finalCycle) {


        nar.synch();


        int frames = Math.max(0, (int) (finalCycle - time()));
        while (frames-- > 0 && !finished)
            nar.run();


        nar.synch();

        /*}
        catch (Exception e) {
            error = e;
        }*/

        return this;
    }


    public TestNAR input(String... s) {
        finished = false;
        for (String x: s)
            try {
                nar.input(x);
            } catch (Narsese.NarseseException e) {
                fail(e.toString());
            }
        return this;
    }


    public TestNAR input(ITask... s) {
        finished = false;
        for (ITask x : s) {
            if (x.pri() == 0 || x.isDeleted())
                throw new RuntimeException("input task has zero or deleted priority");
            nar.input(x);
        }
        return this;
    }

    /**
     * warning may not work with time=0
     */

    public TestNAR inputAt(long time, String s) {
        finished = false;
        nar.inputAt(time, s);
        return this;
    }


    public TestNAR inputAt(long time, Task... t) {
        finished = false;
        nar.inputAt(time, t);
        return this;
    }


    public TestNAR believe(String t, Tense tense, float f, float c) {
        finished = false;
        nar.believe(t, tense, f, c);
        return this;
    }


    public TestNAR goal(String t, Tense tense, float f, float c) {
        finished = false;
        try {
            nar.want($.$(t), tense, f, c);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    public TestNAR goal(String s) {
        nar.want(s);
        return this;
    }


    public TestNAR log() {
        nar.log();
        return this;
    }

    public TestNAR logDebug() {
        if (!Param.DEBUG) {
            logger.warn("WARNING: debug mode enabled statically");
            Param.DEBUG = true;
        }
        return log();
    }

    /**
     * fails if anything non-input is processed
     */

    public TestNAR mustNotOutputAnything() {
        //exitOnAllSuccess = false;
        requireConditions = false;
        nar.onTask(c -> {
            if (!c.isInput())
                fail(c + " output, but must not output anything");
        });
        return this;
    }


    public TestNAR dur(int newDur) {
        nar.time.dur(newDur);
        return this;
    }

    public void stop() {
        finished = true;
    }




    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate occ) {
        return mustEmit(taskEvents, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, (s, e)-> occ.test(s) && occ.test(e));
    }


    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occ) {
        return mustOutput(cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, occ, occ);
    }

    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long start, long end) {
        return mustOutput(cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, (s,e)->start==s && end==e);
    }
    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time) {
        return mustEmit(taskEvents, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, time);
    }


    public TestNAR mustOutput(long cyclesAhead, String task) {
        try {
            return mustEmit(taskEvents, cyclesAhead, task);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }


    private TestNAR mustEmit(ByteTopic<Tasked>[] c, long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time) {
        try {
            return mustEmit(c, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, time, true);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }


    private TestNAR mustEmit(ByteTopic<Tasked>[] c, long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time, boolean must) throws Narsese.NarseseException {
        long now = time();
        cyclesAhead = Math.round(cyclesAhead * Param.Testing.TEST_TIME_MULTIPLIER);
        return mustEmit(c, now, now + cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, time, must);
    }

    private TestNAR mustEmit(ByteTopic<Tasked>[] c, long cycleStart, long cycleEnd, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time, boolean mustOrMustNot) throws Narsese.NarseseException {


        if (freqMin == -1)
            freqMin = freqMax;

        int tt = temporalTolerance;
        cycleStart -= tt;
        cycleEnd += tt;

        Term term =
                Narsese.term(sentenceTerm, true);
        int tv = term.volume();
        int tvMax = nar.termVolumeMax.intValue();
        if (tv > tvMax) {
            throw new TermException("condition term volume (" + tv + ") exceeds volume max (" + tvMax + ')', term);
        }

        float hf = freqTolerance / 2.0f, hc = confTolerance / 2.0f;
        TaskCondition tc =
                new TaskCondition(nar,
                        cycleStart, cycleEnd,
                        term, punc,
                        freqMin - hf, freqMax + hf,
                        confMin - hc, confMax + hc, time);

        for (ByteTopic<Tasked> cc: c)
            cc.on(tc, punc);

        finished = false;

        if (mustOrMustNot) {
            succeedsIfAll.add(tc);
        } else {
            exitOnAllSuccess = false;
            failsIfAny.add(tc);
        }

        return this;


    }

    public final long time() {
        return nar.time();
    }


    private TestNAR mustEmit(ByteTopic<Tasked>[] c, long cyclesAhead, String task) throws Narsese.NarseseException {
        Task t = Narsese.task(task, nar);


        String termString = t.term().toString();
        float freq, conf;
        if (t.truth() != null) {
            freq = t.freq();
            conf = t.conf();

        } else {
            freq = conf = NaN;
        }

        return mustEmit(c, cyclesAhead, termString, t.punc(), freq, freq, conf, conf, (s, e) -> s == t.start() && e == t.end());
    }


    public TestNAR mustOutput(long cyclesAhead, String term, byte punc, float freq, float conf) {
        return mustOutput(cyclesAhead, term, punc, freq, freq, conf, conf, ETERNAL);
    }


    public TestNAR mustBelieve(long cyclesAhead, String term, float freqMin, float freqMax, float confMin, float confMax) {
        return mustBelieve(cyclesAhead, term, freqMin, freqMax, confMin, confMax, ETERNAL);
    }


    public TestNAR mustBelieve(long cyclesAhead, String term, float freqMin, float freqMax, float confMin, float confMax, long when) {
        return mustOutput(cyclesAhead, term, BELIEF, freqMin, freqMax, confMin, confMax, when);
    }


    public TestNAR mustBelieve(long cyclesAhead, String term, float freq, float confidence, Tense t) {
        return mustOutput(cyclesAhead, term, BELIEF, freq, freq, confidence, confidence, nar.time(t));
    }

    /**
     * tests for any truth value at the given occurrences
     */

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, long occ) {


        mustNotOutput(cyclesAhead, term, punc, 0f, 1f, 0f, 1f, occ);
        return this;
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc) {
        return mustNotOutput(cyclesAhead, term, punc, (t)->true);
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, LongPredicate occ) {


        mustNotOutput(cyclesAhead, term, punc, 0f, 1f, 0f, 1f, occ);
        return this;
    }


    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occ) {
        LongPredicate badTime = (l) -> l == occ;
        return mustNotOutput(cyclesAhead, term, punc, freqMin, freqMax, confMin, confMax, badTime);
    }


    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate badTimes) {
        return mustNotOutput(cyclesAhead, term, punc, freqMin, freqMax, confMin, confMax, (s,e)->badTimes.test(s) || badTimes.test(e));
    }
    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate timeFilter) {
        if (freqMin < 0 || freqMin > 1f || freqMax < 0 || freqMax > 1f || confMin < 0 || confMin > 1f || confMax < 0 || confMax > 1f || freqMin != freqMin || freqMax != freqMax)
            throw new UnsupportedOperationException();

        try {
            return mustEmit(taskEvents,
                    cyclesAhead,
                    term, punc,
                    freqMin, freqMax, confMin, confMax,
                    timeFilter, false);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }

    }


    public TestNAR mustBelieve(long cyclesAhead, String term, float freq, float confidence, long occTimeAbsolute) {
        return mustOutput(cyclesAhead, term, BELIEF, freq, freq, confidence, confidence, occTimeAbsolute);
    }
    public TestNAR mustNotBelieve(long cyclesAhead, String term, float freq, float confidence, LongLongPredicate occTimeAbsolute) {
        return mustNotOutput(cyclesAhead, term, BELIEF, freq, freq, confidence, confidence, occTimeAbsolute);
    }


    public TestNAR mustBelieve(long cyclesAhead, String term, float freq, float confidence, long start, long end) {
        return mustOutput(cyclesAhead, term, BELIEF, freq, freq, confidence, confidence, start, end);
    }


    public TestNAR mustBelieve(long cyclesAhead, String term, float freq, float confidence) {
        return mustBelieve(cyclesAhead, term, freq, confidence, ETERNAL);
    }


    public TestNAR mustBelieve(long cyclesAhead, String term, float confidence) {
        return mustBelieve(cyclesAhead, term, 1.0f, confidence);
    }


    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, conf);
    }


    public TestNAR mustQuestion(long cyclesAhead, String qt) {
        return mustOutput(cyclesAhead, qt, QUESTION);
    }


    public TestNAR mustQuest(long cyclesAhead, String qt) {
        return mustOutput(cyclesAhead, qt, QUEST);
    }

    public TestNAR mustOutput(long cyclesAhead, String qt, byte question) {
        return mustOutput(cyclesAhead, qt, question, NaN, NaN);
    }


    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf, long occ) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, freq, conf, conf, occ);
    }

    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf, LongPredicate occ) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, freq, conf, conf, occ);
    }
    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf, LongLongPredicate occ) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, freq, conf, conf, occ);
    }


    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf, long start, long end) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, freq, conf, conf, start, end);
    }

    public TestNAR mustBelieve(long cyclesAhead, String goalTerm, float freq, float conf, LongPredicate occ) {
        return mustOutput(cyclesAhead, goalTerm, BELIEF, freq, freq, conf, conf, occ);
    }
    public TestNAR mustBelieve(long cyclesAhead, String goalTerm, float freq, float conf, LongLongPredicate occ) {
        return mustOutput(cyclesAhead, goalTerm, BELIEF, freq, freq, conf, conf, occ);
    }

    public TestNAR ask(String termString) {
        nar.question(termString);
        return this;
    }

    public TestNAR quest(String termString)  {
        nar.quest($.$$(termString));
        return this;
    }


    public TestNAR askAt(int i, String term) {
        try {
            nar.inputAt(i, term + '?');
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * TODO make this throw NarseseException
     */

    public TestNAR believe(String termString) {
        try {
            nar.believe(termString);
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    public TestNAR believe(String termString, float freq, float conf) {
        nar.believe(termString, freq, conf);
        return this;
    }

    public void test(/* for use with JUnit */) {
        nar.synch();
        run(0);
    }

    public TestNAR termVolMax(int i) {
        nar.termVolumeMax.set(i);
        return this;
    }

    public TestNAR confMin(float c) {
        nar.confMin.set(c);
        return this;
    }

    final class EarlyExit implements Consumer<NAR> {

        final int checkResolution;
        int cycle;

        EarlyExit(int checkResolution) {
            this.checkResolution = checkResolution;
            nar.onCycle(this);
        }

        @Override
        public void accept(NAR nar) {

            if (++cycle % checkResolution == 0) {
                if (succeedsIfAll.allSatisfy(NARCondition::isTrue))
                    stop();

                if (failsIfAny.anySatisfy(NARCondition::isTrue))
                    stop();
            }

        }
    }
}
