package nars.test;

import jcog.event.Topic;
import jcog.list.FasterList;
import nars.*;
import nars.task.ITask;
import nars.task.Tasked;
import nars.test.condition.NARCondition;
import nars.test.condition.TaskCondition;
import nars.time.Tense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;
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

    private static final Logger logger = LoggerFactory.getLogger(TestNAR.class);
    public final NAR nar;
    /**
     * holds must (positive) conditions
     */
    private final FasterList<NARCondition> succeedsIfAll = new FasterList();
    /**
     * holds mustNot (negative) conditions which are tested at the end
     */
    private final List<NARCondition> failsIfAny = $.newArrayList();
    public boolean quiet = false;
    public boolean trace = true;
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
    private boolean collectTrace = false;
    private int temporalTolerance = 0;
    private float freqTolerance = Param.TESTS_TRUTH_ERROR_TOLERANCE;
    private float confTolerance = Param.TESTS_TRUTH_ERROR_TOLERANCE;
    private Topic<Tasked>[] outputEvents;
    private boolean finished;
    private boolean exitOnAllSuccess = true;
    private boolean reportStats = false;

    public TestNAR(NAR nar) {
        this.nar = nar;

        this.outputEvents = new Topic[]{
                nar.eventTask,
        };

    }

    public TestNAR confTolerance(float t) {
        this.confTolerance = t;
        return this;
    }

    public TestNAR run(long finalCycle  /* for use with JUnit */) {


        if (requireConditions)
            assertTrue(!succeedsIfAll.isEmpty() || !failsIfAny.isEmpty(), "no conditions tested");


        String id = succeedsIfAll.toString();

        for (NARCondition oc: succeedsIfAll) {
            long oce = oc.getFinalCycle();
            if (oce > finalCycle) finalCycle = oce + 1;
        }
        for (NARCondition oc: failsIfAny) {
            long oce = oc.getFinalCycle();
            if (oce > finalCycle) finalCycle = oce + 1;
        }

        StringWriter trace;
        if (collectTrace)
            nar.trace(trace = new StringWriter());
        else
            trace = null;

        if (exitOnAllSuccess) {
            new EarlyExit(1);
        }

        long startTime = nar.time();


        runUntil(finalCycle);

        boolean success = true;
        for (NARCondition t: succeedsIfAll) {
            if (!t.isTrue()) {
                success = false;
                break;
            }
        }
        for (NARCondition t: failsIfAny) {
            if (t.isTrue()) {

                if (!quiet) {
                    logger.error("mustNot: {}", t);
                    t.log(logger);
                    ((TaskCondition) t).matched.forEach(shouldntHave -> logger.error("Must not:\n{}", shouldntHave.proof()));
                }


                success = false;
            }
        }


        long time = nar.time();
        int runtime = Math.max(0, (int) (time - startTime));

        this.score = success ?

                1 + (+1f / (1f + ((1f + runtime) / (1f + Math.max(0,finalCycle - startTime)))))
                :
                0;

        {


            if (!quiet && reportStats) {
                String pattern = "{}\n\t{} {} {}IN \ninputs";
                Object[] args = {id, time};

                logger.info(pattern, args);

            }

            if (!quiet) {
                succeedsIfAll.forEach(c ->
                        c.log(logger)
                );


                if (trace != null)
                    logger.trace("{}", trace.getBuffer());
            }


        }


        if (!quiet && reportStats) {
            nar.emotion.printer(System.out).run();
            nar.stats(System.out);
        }

        assertSuccess(success);

        return this;
    }

    private void assertSuccess(boolean success) {

        /** if success is false, the test will end here, throwing the appropriate JUnit exception */
        assertTrue(success);
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
        for (ITask x: s) {
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
            nar.goal($.$(t), tense, f, c);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    public TestNAR goal(String s) {
        nar.goal(s);
        return this;
    }


    public TestNAR log() {
        nar.log();
        return this;
    }

    /**
     * fails if anything non-input is processed
     */

    public TestNAR mustNotOutputAnything() {
        exitOnAllSuccess = false;
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


    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occTimeAbsolute) {
        return mustOutput(cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, occTimeAbsolute, occTimeAbsolute);
    }

    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate occ) {
        mustEmit(outputEvents, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, occ, occ);
        return this;
    }


    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long start, long end) {
        mustEmit(outputEvents, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, (l) -> l == start, (l) -> l == end);
        return this;
    }


    public TestNAR mustOutput(long cyclesAhead, String task) {
        try {
            return mustEmit(outputEvents, cyclesAhead, task);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }


    private TestNAR mustEmit(Topic<Tasked>[] c, long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate start, LongPredicate end) {
        try {
            return mustEmit(c, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax,
                    start, end, true);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }


    private TestNAR mustEmit(Topic<Tasked>[] c, long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate start, LongPredicate end, boolean must) throws Narsese.NarseseException {
        long now = time();
        cyclesAhead = Math.round(cyclesAhead * Param.TEST_TIME_MULTIPLIER);
        return mustEmit(c, now, now + cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, start, end, must);
    }

    private TestNAR mustEmit(Topic<Tasked>[] c, long cycleStart, long cycleEnd, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate start, LongPredicate end, boolean must) throws Narsese.NarseseException {


        if (freqMin == -1)
            freqMin = freqMax;

        int tt = temporalTolerance;
        cycleStart -= tt;
        cycleEnd += tt;

        float hf = freqTolerance / 2.0f;
        float hc = confTolerance / 2.0f;
        TaskCondition tc =
                new TaskCondition(nar,
                        cycleStart, cycleEnd,
                        sentenceTerm, punc, freqMin - hf, freqMax + hf, confMin - hc, confMax + hc, start, end);


        for (Topic<Tasked> cc: c) {
            cc.on(tc);
        }

        finished = false;

        if (must) {
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


    private TestNAR mustEmit(Topic<Tasked>[] c, long cyclesAhead, String task) throws Narsese.NarseseException {
        Task t = Narsese.the().task(task, nar);


        String termString = t.term().toString();
        float freq, conf;
        if (t.truth() != null) {
            freq = t.freq();
            conf = t.conf();

        } else {
            freq = conf = NaN;
        }

        return mustEmit(c, cyclesAhead, termString, t.punc(), freq, freq, conf, conf, (l) -> l == t.start(), (l) -> l == t.end());
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

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, LongPredicate occ) {


        mustNotOutput(cyclesAhead, term, punc, 0f, 1f, 0f, 1f, occ);
        return this;
    }


    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occ) {
        LongPredicate badTime = (l) -> l == occ;
        return mustNotOutput(cyclesAhead, term, punc, freqMin, freqMax, confMin, confMax, badTime);
    }


    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate badTimes) {
        if (freqMin < 0 || freqMin > 1f || freqMax < 0 || freqMax > 1f || confMin < 0 || confMin > 1f || confMax < 0 || confMax > 1f || freqMin != freqMin || freqMax != freqMax)
            throw new UnsupportedOperationException();

        try {
            return mustEmit(outputEvents,
                    cyclesAhead,
                    term, punc, freqMin, freqMax, confMin,
                    confMax, badTimes, badTimes, false);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }

    }


    public TestNAR mustBelieve(long cyclesAhead, String term, float freq, float confidence, long occTimeAbsolute) {
        return mustOutput(cyclesAhead, term, BELIEF, freq, freq, confidence, confidence, occTimeAbsolute);
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


    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf, long start, long end) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, freq, conf, conf, start, end);
    }

    public TestNAR mustBelieve(long cyclesAhead, String goalTerm, float freq, float conf, LongPredicate occ) {
        return mustOutput(cyclesAhead, goalTerm, BELIEF, freq, freq, conf, conf, occ);
    }

    public TestNAR ask(String termString) throws Narsese.NarseseException {
        nar.question(termString);
        return this;
    }

    public TestNAR quest(String termString) throws Narsese.NarseseException {
        nar.quest($.$(termString));
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
        try {
            nar.believe(termString, freq, conf);
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    public TestNAR test(/* for use with JUnit */) {
        return run(0);
    }

    public TestNAR test(long cycles) {
        return run(cycles);
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

            if (++cycle % checkResolution == 0 && !succeedsIfAll.isEmpty()) {

                if (succeedsIfAll.allSatisfy(NARCondition::isTrue))
                    stop();
            }

        }
    }
}
