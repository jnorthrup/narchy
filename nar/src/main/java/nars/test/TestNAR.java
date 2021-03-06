package nars.test;

import jcog.data.list.FasterList;
import jcog.event.ByteTopic;
import nars.*;
import nars.term.Term;
import nars.term.util.TermException;
import nars.test.condition.DefaultTaskCondition;
import nars.test.condition.LambdaTaskCondition;
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
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.lang.Float.NaN;
import static nars.$.*;
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
    private final FasterList<NARCondition> failsIfAny = new FasterList();
    private boolean requireConditions = true;
    
    /**
     * -1 = failure,
     * 0 = hasnt been determined yet by the end of the test,
     * (1..n) = success in > 1 cycles,
     * +1 = success in <= 1 cycles
     */
    public float score;
    public float freqTolerance = NAL.test.TRUTH_ERROR_TOLERANCE;
    private float confTolerance = NAL.test.TRUTH_ERROR_TOLERANCE;
    private final ByteTopic<Task> taskEvent;
    private boolean finished;
    private boolean exitOnAllSuccess = true;

    private static final int maxSimilars = 3;
    private boolean reportStats = false;

    public TestNAR(NAR nar) {
        this.nar = nar;

        this.taskEvent = nar.eventTask();
    }

    public TestNAR confTolerance(float t) {
        this.confTolerance = t;
        return this;
    }

    public void freqTolerance(float f) {
        this.freqTolerance = f;
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

    public void run() {
        run(-1L);
    }

    public void run(long finalCycle) {
        TestNARResult result = _run(finalCycle);
        assertTrue(result.success);
    }

    private TestNARResult _run(long finalCycle) {


        score = (float) 0; //Float.NEGATIVE_INFINITY;

        if (requireConditions)
            assertTrue(!succeedsIfAll.isEmpty() || !failsIfAny.isEmpty(), "no conditions tested");


        String id = succeedsIfAll.toString();

        if (finalCycle <= 0L) {
            //infer final cycle
            for (NARCondition oc : succeedsIfAll) {
                long oce = oc.getFinalCycle();
                if (oce >= 0L && oce > finalCycle) finalCycle = oce + 1L;
            }
            for (NARCondition oc : failsIfAny) {
                long oce = oc.getFinalCycle();
                if (oce >= 0L && oce > finalCycle) finalCycle = oce + 1L;
            }
        }

        score = (float) -finalCycle; //default score
        score = (float) Math.min(-1L, finalCycle);

        StringWriter trace;
        /**
         * enable this to print reports even if the test was successful.
         * it can cause a lot of output that can be noisy and slow down
         * the test running.
         * TODO separate way to generate a test report containing
         * both successful and unsuccessful tests
         */
        boolean trace1 = false;
        if (trace1)
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

        long endTime = nar.time();
        int runtime = Math.max(0, (int) (endTime - startTime));

        assert((long) runtime <= finalCycle);

        boolean success =
                succeedsIfAll.allSatisfy(NARCondition::isTrue)
                &&
                !failsIfAny.anySatisfy(NARCondition::isTrue);

        if (success)
            score = (float) -runtime;

        if (reportStats) {
            logger.info("{}\n", id);

            for (NARCondition t : succeedsIfAll)
                t.log("must", t.isTrue(), logger);
            for (NARCondition t : failsIfAny)
                t.log("mustNot", t.isFalse(), logger);


            nar.stats(true, true, System.out);
            //nar.control.stats(System.out);

        }

        if (trace != null)
            logger.trace("{}", trace.getBuffer());

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
                fail(e::toString);
            }
        return this;
    }


    public TestNAR input(Task... s) {
        finished = false;
        for (Task x : s) {
            if (x.pri() == (float) 0 || x.isDeleted())
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
            nar.want($.INSTANCE.$(t), tense, f, c);
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
        reportStats = true;
        return this;
    }

    public TestNAR logDebug() {
        if (!NAL.DEBUG) {
            logger.warn("WARNING: debug mode enabled statically");
            NAL.DEBUG = true;
        }
        return log();
    }

    /**
     * fails if anything non-input is processed
     */

    public TestNAR mustNotOutputAnything() {

        //TODO use LambaTaskCondition
        requireConditions = false;
        nar.onTask(new Consumer<Task>() {
            @Override
            public void accept(Task c) {
                if (!c.isInput())
                    fail(new Supplier<String>() {
                        @Override
                        public String get() {
                            return c + " output, but must not output anything";
                        }
                    });
            }
        });
        return this;
    }


    public TestNAR dur(int newDur) {
        nar.time.dur((float) newDur);
        return this;
    }

    public void stop() {
        finished = true;
    }




    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate occ) {
        return mustEmit(taskEvent, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, new LongLongPredicate() {
            @Override
            public boolean accept(long s, long e) {
                return occ.test(s) && occ.test(e);
            }
        });
    }


    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occ) {
        return mustOutput(cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, occ, occ);
    }

    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long start, long end) {
        return mustOutput(cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, new LongLongPredicate() {
            @Override
            public boolean accept(long s, long e) {
                return start == s && end == e;
            }
        });
    }
    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time) {
        return mustEmit(taskEvent, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, time);
    }


    public TestNAR mustOutput(long cyclesAhead, String task) {
        try {
            return mustEmit(taskEvent, cyclesAhead, task);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }


    private TestNAR mustEmit(ByteTopic<Task> c, long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time) {
        try {
            return mustEmit(c, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, time, true);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }


    private TestNAR mustEmit(ByteTopic<Task> c, long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time, boolean must) throws Narsese.NarseseException {
        long now = time();
        cyclesAhead = (long) Math.round((float) cyclesAhead * NAL.test.TIME_MULTIPLIER);
        return mustEmit(c, now, now + cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, time, must);
    }

    private TestNAR mustEmit(ByteTopic<Task> c, long cycleStart, long cycleEnd, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time, boolean mustOrMustNot) throws Narsese.NarseseException {


        if (freqMin == -1.0F)
            freqMin = freqMax;

        int temporalTolerance = 0;
        int tt = temporalTolerance;
        cycleStart = cycleStart - (long) tt;
        cycleEnd = cycleEnd + (long) tt;

        Term term =
                $.INSTANCE.$(sentenceTerm);
        int tv = term.volume();
        int tvMax = nar.termVolMax.intValue();
        if (tv > tvMax) {
            throw new TermException("condition term volume (" + tv + ") exceeds volume max (" + tvMax + ')', term);
        }

        float hf = freqTolerance / 2.0f, hc = confTolerance / 2.0f;

        return must(c, punc, mustOrMustNot,
                new DefaultTaskCondition(nar,
                    cycleStart, cycleEnd,
                    term, punc,
                    freqMin - hf, freqMax + hf,
                    confMin - hc, confMax + hc, time));


    }


    public TestNAR must(byte punc, Predicate<Task> tc) {
        return must(punc, true, tc);
    }

    public TestNAR mustNot(byte punc, Predicate<Task> tc) {
        return must(punc, false, tc);
    }

    public TestNAR must(byte punc, boolean mustOrMustNot, Predicate<Task> tc) {
        return must(punc, mustOrMustNot, new LambdaTaskCondition(tc));
    }

    public TestNAR must(byte punc, boolean mustOrMustNot, TaskCondition tc) {
        return must(taskEvent, punc, mustOrMustNot, tc);
    }

    public TestNAR must(ByteTopic<Task> c, byte punc, boolean mustOrMustNot, TaskCondition tc) {

        c.on(tc::test, punc);

        if (reportStats && tc instanceof DefaultTaskCondition)
            tc.similars(maxSimilars);

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


    private TestNAR mustEmit(ByteTopic<Task> c, long cyclesAhead, String task) throws Narsese.NarseseException {
        Task t = Narsese.task(task, nar);


        String termString = t.term().toString();
        float freq, conf;
        if (t.truth() != null) {
            freq = t.freq();
            conf = t.conf();

        } else {
            freq = conf = NaN;
        }

        return mustEmit(c, cyclesAhead, termString, t.punc(), freq, freq, conf, conf, new LongLongPredicate() {
            @Override
            public boolean accept(long s, long e) {
                return s == t.start() && e == t.end();
            }
        });
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
        return mustNotOutput(cyclesAhead, term, punc, new LongPredicate() {
            @Override
            public boolean test(long t) {
                return true;
            }
        });
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, LongPredicate occ) {


        mustNotOutput(cyclesAhead, term, punc, 0f, 1f, 0f, 1f, occ);
        return this;
    }


    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax) {
        return mustNotOutput(cyclesAhead, term, punc, freqMin, freqMax, confMin,confMax, new LongPredicate() {
            @Override
            public boolean test(long t) {
                return true;
            }
        });
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occ) {
        LongPredicate badTime = new LongPredicate() {
            @Override
            public boolean test(long l) {
                return l == occ;
            }
        };
        return mustNotOutput(cyclesAhead, term, punc, freqMin, freqMax, confMin, confMax, badTime);
    }


    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate badTimes) {
        return mustNotOutput(cyclesAhead, term, punc, freqMin, freqMax, confMin, confMax, new LongLongPredicate() {
            @Override
            public boolean accept(long s, long e) {
                return badTimes.test(s) || (s != e && badTimes.test(e));
            }
        });
    }
    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate timeFilter) {
        if (freqMin < (float) 0 || freqMin > 1f || freqMax < (float) 0 || freqMax > 1f || confMin < (float) 0 || confMin > 1f || confMax < (float) 0 || confMax > 1f || freqMin != freqMin || freqMax != freqMax)
            throw new UnsupportedOperationException();

        try {
            return mustEmit(taskEvent,
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

    public TestNAR mustBelieveAtOnly(long cyclesAhead, String term, float freq, float confidence, long startTime) {
        return mustBelieveAtOnly(cyclesAhead, term, freq, confidence, startTime, startTime);
    }

    public TestNAR mustBelieveAtOnly(long cyclesAhead, String term, float freq, float confidence, long startTime, long endTime) {
        mustBelieve(cyclesAhead, term, freq, confidence, startTime);
        return mustNotBelieve(cyclesAhead, term, new LongLongPredicate() {
            @Override
            public boolean accept(long s, long e) {
                return s != startTime || e != endTime;
            }
        });
    }

    public TestNAR mustNotBelieve(long cyclesAhead, String term, float freq, float confidence, LongLongPredicate occTimeAbsolute) {
        return mustNotOutput(cyclesAhead, term, BELIEF, freq, freq, confidence, confidence, occTimeAbsolute);
    }
    public TestNAR mustNotBelieve(long cyclesAhead, String term, LongLongPredicate occTimeAbsolute) {
        return mustNotOutput(cyclesAhead, term, BELIEF, (float) 0, 1.0F, (float) 0, 1.0F, occTimeAbsolute);
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

    @Deprecated public TestNAR ask(String termString) {
        nar.question(INSTANCE.$$(termString));
        return this;
    }

    public TestNAR quest(String termString)  {
        nar.quest(INSTANCE.$$(termString));
        return this;
    }


    public TestNAR askAt(int i, String term) {
        try {
            nar.inputAt((long) i, term + '?');
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
        run();
    }

    public TestNAR termVolMax(int i) {
        nar.termVolMax.set(i);
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
