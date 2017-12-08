package nars.test;

import jcog.event.Topic;
import nars.*;
import nars.task.ITask;
import nars.task.Tasked;
import nars.test.condition.NARCondition;
import nars.test.condition.TaskCondition;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    static final Logger logger = LoggerFactory.getLogger(TestNAR.class);

    /**
     * enable this to print reports even if the test was successful.
     * it can cause a lot of output that can be noisy and slow down
     * the test running.
     * TODO separate way to generate a test report containing
     * both successful and unsuccessful tests
     */
    static final boolean collectTrace = false;

    public int temporalTolerance;
    float truthTolerance = Param.TESTS_TRUTH_ERROR_TOLERANCE;

    @NotNull
    public final NAR nar;

    public boolean trace = true;



    /**
     * holds must (positive) conditions
     */
    public final List<NARCondition> succeedsIfAll = $.newArrayList();
    /**
     * holds mustNot (negative) conditions which are tested at the end
     */
    public final List<NARCondition> failsIfAny = $.newArrayList();
    //TODO initialize this once in constructor

    @NotNull
    private final Topic<Tasked>[] outputEvents;
    //public final List<ExplainableTask> explanations = new ArrayList();

    @Nullable
    public Object result;

    boolean finished;
    private boolean exitOnAllSuccess = true;
    public boolean requireConditions = true;

    /** -1 = failure,
     * 0 = hasnt been determined yet by the end of the test,
     * (0..1) = success in > 1 cycles,
     * +1 = success in <= 1 cycles */
    public float score;

    public TestNAR(NAR nar) {
        this.outputEvents = new Topic[]{
                //nar.memory.eventDerived,
                //nar.memory.eventInput,
                nar.eventTask,
                //nar.eventTaskRemoved,
                //nar.memory.eventRevision,
        };

        this.nar = nar;

        //eventMeters = new EventCount(nar).eventMeters;
    }

    public TestNAR truthTolerance(float truthTolerance) {
        this.truthTolerance = truthTolerance;
        return this;
    }

    //    /** returns the "cost", which can be considered the inverse of a "score".
//     * it is proportional to the effort (ex: # of cycles) expended by
//     * this reasoner in attempts to satisfy success conditions.
//     * If the conditions are not successful, the result will be INFINITE,
//     * though this can be normalized to a finite value in comparing multiple tests
//     * by replacing the INFINITE result with a maximum # of cycles limit,
//     * which will be smaller in cases where the success conditions are
//     * completed prior to the limit.
//     * */
//    public double getCost() {
//        return EternalTaskCondition.cost(requires);
//    }


//    /** asserts that (a snapshot of) the termlink graph is fully connected */
//    public TestNAR assertTermLinkGraphConnectivity() {
//        TermLinkGraph g = new TermLinkGraph(nar);
//        assert("termlinks form a fully connected graph:\n" + g.toString(), g.isConnected());
//        return this;
//    }

    @NotNull
    public TestNAR run(long finalCycle, boolean testAndPrintReport /* for use with JUnit */) {


        if (requireConditions)
            assertTrue(!succeedsIfAll.isEmpty() || !failsIfAny.isEmpty(), "no conditions tested");


        //TODO cache requires & logger, it wont change often
        String id = succeedsIfAll.toString();

        for (NARCondition oc : succeedsIfAll) {
            long oce = oc.getFinalCycle();
            if (oce > finalCycle) finalCycle = oce + 1;
        }
        for (NARCondition oc : failsIfAny) {
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
        for (NARCondition t : succeedsIfAll) {
            if (!t.isTrue()) {
                success = false;
                break;
            }
        }
        for (NARCondition t : failsIfAny) {
            if (t.isTrue()) {

                logger.error("mustNot: {}", t);
                t.log(logger);
                ((TaskCondition) t).matched.forEach(shouldntHave -> logger.error("Must not:\n{}", shouldntHave.proof()));


                success = false;
            }
        }


        long time = nar.time();
        int duration = (int)(time - startTime);
        this.score = success ? (+1/(+1f+duration)) : 0;

        if (testAndPrintReport) {

            //if (requires.isEmpty())
            //return this;


            //assertTrue("No cycles elapsed", tester.nar.memory().time/*SinceLastCycle*/() > 0);



            //Task[] inputs = n.inputs.toArray(new Task[n.inputs.size()]);
//            Collection<HitMeter> var = eventMeters.values();
//            HitMeter[] eventMeters1 = var.toArray(new HitMeter[var.size()]);


            String pattern = "{}\n\t{} {} {}IN \ninputs";
            Object[] args = {id, time, result/*, eventMeters1*/};

            if (result != null) {
                logger.error(pattern, args);
            } else {
                logger.info(pattern, args);
            }

            succeedsIfAll.forEach(c ->
                    c.log(logger)
            );

            if (trace != null)
                logger.trace("{}", trace.getBuffer());


            nar.stats(System.out);

        }

        assertSuccess(success);

        return this;
    }

    protected void assertSuccess(boolean success) {

        /** if success is false, the test will end here, throwing the appropriate JUnit exception */
        assertTrue(success);
    }

    @NotNull
    public TestNAR runUntil(long finalCycle) {

        result = null;

//        if (showOutput)
//            nar.trace();


        //preinit
        nar.time.synch(nar);

        //try {
        int frames = Math.max(0,(int) (finalCycle - time()));
        while (frames-- > 0 && !finished)
            nar.cycle();

        //finish anything pending
        nar.time.synch(nar);

        /*}
        catch (Exception e) {
            error = e;
        }*/

        return this;
    }


    @NotNull
    public TestNAR input(@NotNull String... s) {
        finished = false;
        for (String x : s)
            try {
                nar.input(x);
            } catch (Narsese.NarseseException e) {
                fail(e.toString());
            }
        return this;
    }

    @NotNull
    public TestNAR input(@NotNull ITask... s) {
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
    @NotNull
    public TestNAR inputAt(long time, String s) {
        finished = false;
        nar.inputAt(time, s);
        return this;
    }

    @NotNull
    public TestNAR inputAt(long time, Task... t) {
        finished = false;
        nar.inputAt(time, t);
        return this;
    }

    @NotNull
    public TestNAR believe(@NotNull String t, @NotNull Tense tense, float f, float c) {
        finished = false;
        nar.believe(t, tense, f, c);
        return this;
    }

    @NotNull
    public TestNAR goal(@NotNull String t, @NotNull Tense tense, float f, float c) {
        finished = false;
        try {
            nar.goal($.$(t), tense, f, c);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @NotNull
    public TestNAR goal(@NotNull String s) {
        nar.goal(s);
        return this;
    }

    @NotNull
    public TestNAR log() {
        nar.log();
        return this;
    }

    /**
     * fails if anything non-input is processed
     */
    @NotNull
    public TestNAR mustNotOutputAnything() {
        exitOnAllSuccess = false;
        requireConditions = false; //this is the condition
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

    @NotNull
    public TestNAR mustOutput(long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occTimeAbsolute) {
        return mustOutput(cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, occTimeAbsolute, occTimeAbsolute);
    }
    @NotNull
    public TestNAR mustOutput(long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate occ) {
        mustEmit(outputEvents, cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, occ, occ);
        return this;
    }

    @NotNull
    public TestNAR mustOutput(long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long start, long end) {
        mustEmit(outputEvents, cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, (l) -> l == start, (l) -> l == end);
        return this;
    }

    @NotNull
    public TestNAR mustOutput(long withinCycles, @NotNull String task) {
        try {
            return mustEmit(outputEvents, withinCycles, task);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }

//    @NotNull
//    public TestNAR mustEmit(@NotNull Topic<Tasked>[] c, long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax) {
//        return mustEmit(c, cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, ETERNAL, ETERNAL);
//    }

    @NotNull
    TestNAR mustEmit(@NotNull Topic<Tasked>[] c, long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate start, LongPredicate end) {
        try {
            return mustEmit(c, cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax,
                    start, end, true);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    TestNAR mustEmit(@NotNull Topic<Tasked>[] c, long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate start, LongPredicate end, boolean must) throws Narsese.NarseseException {


        if (freqMin == -1)
            freqMin = freqMax;

        int tt = temporalTolerance;
        cycleStart -= tt;
        cycleEnd += tt;

        float h = truthTolerance / 2.0f;
        TaskCondition tc =
                new TaskCondition(nar,
                        cycleStart, cycleEnd,
                        sentenceTerm, punc, freqMin - h, freqMax + h, confMin - h, confMax + h, start, end);


        for (Topic<Tasked> cc : c) {
            cc.on(tc);
        }

        finished = false;

        if (must) {
            succeedsIfAll.add(tc);
        } else {
            exitOnAllSuccess = false; //require entire execution, not just finish early
            failsIfAny.add(tc);
        }

        return this;
//
//        ExplainableTask et = new ExplainableTask(tc);
//        if (showExplanations) {
//            explanations.add(et);
//        }
//        return et;
    }

    public final long time() {
        return nar.time();
    }

    @NotNull
    private TestNAR mustEmit(@NotNull Topic<Tasked>[] c, long withinCycles, @NotNull String task) throws Narsese.NarseseException {
        Task t = Narsese.parse().task(task, nar);
        //TODO avoid reparsing term from string

        long now = time();
        String termString = t.term().toString();
        float freq, conf;
        if (t.truth() != null) {
            freq = t.freq();
            conf = t.conf();

        } else {
            freq = conf = NaN;
        }

        return mustEmit(c, now, now + withinCycles, termString, t.punc(), freq, freq, conf, conf, (l) -> l == t.start(), (l) -> l == t.end());
    }

    @NotNull
    public TestNAR mustOutput(long withinCycles, @NotNull String term, byte punc, float freq, float conf) {
        long now = time();
        return mustOutput(now, now + withinCycles, term, punc, freq, freq, conf, conf, ETERNAL);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freqMin, float freqMax, float confMin, float confMax) {
        return mustBelieve(withinCycles, term, freqMin, freqMax, confMin, confMax, ETERNAL);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freqMin, float freqMax, float confMin, float confMax, long tense) {
        long now = time();
        return mustOutput(now, now + withinCycles, term, BELIEF, freqMin, freqMax, confMin, confMax, tense);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freq, float confidence, @NotNull Tense t) {
        long ttt = nar.time();
        return mustOutput(ttt, ttt + withinCycles, term, BELIEF, freq, freq, confidence, confidence, nar.time(t));
    }

    /**
     * tests for any truth value at the given occurrences
     */
    @NotNull
    public TestNAR mustNotOutput(long withinCycles, @NotNull String term, byte punc, long occ) {
        //assertTrue("no occurrence times specified", occs.length > 0);
        //LongHashSet badTimes = new LongHashSet(occs);
        mustNotOutput(withinCycles, term, punc, 0f, 1f, 0f, 1f, occ);
        return this;
    }
    @NotNull
    public TestNAR mustNotOutput(long withinCycles, @NotNull String term, byte punc, @NotNull LongPredicate occ) {
        //assertTrue("no occurrence times specified", occs.length > 0);
        //LongHashSet badTimes = new LongHashSet(occs);
        mustNotOutput(withinCycles, term, punc, 0f, 1f, 0f, 1f, occ);
        return this;
    }

    @NotNull
    public TestNAR mustNotOutput(long withinCycles, @NotNull String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occ) {
        LongPredicate badTime = (l) -> l == occ;
        return mustNotOutput(withinCycles, term, punc, freqMin, freqMax, confMin, confMax, badTime);
    }

    @NotNull
    public TestNAR mustNotOutput(long withinCycles, @NotNull String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate badTimes) {
        if (freqMin < 0 || freqMin > 1f || freqMax < 0 || freqMax > 1f || confMin < 0 || confMin > 1f || confMax < 0 || confMax > 1f || freqMin != freqMin || freqMax != freqMax)
            throw new UnsupportedOperationException();

        try {
            long time = nar.time();
            return mustEmit(outputEvents,
                    time, time + withinCycles,
                    term, punc, freqMin, freqMax, confMin,
                    confMax, badTimes, badTimes, false);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }

    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freq, float confidence, long occTimeAbsolute) {
        long t = nar.time();
        return mustOutput(t, t + withinCycles, term, BELIEF, freq, freq, confidence, confidence, occTimeAbsolute);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freq, float confidence, long start, long end) {
        long t = nar.time();
        return mustOutput(t, t + withinCycles, term, BELIEF, freq, freq, confidence, confidence, start, end);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freq, float confidence) {
        return mustBelieve(withinCycles, term, freq, confidence, ETERNAL);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float confidence) {
        return mustBelieve(withinCycles, term, 1.0f, confidence);
    }

    @NotNull
    public TestNAR mustGoal(long withinCycles, @NotNull String goalTerm, float freq, float conf) {
        return mustOutput(withinCycles, goalTerm, GOAL, freq, conf);
    }

    @NotNull
    public TestNAR mustQuestion(long withinCycles, @NotNull String qt) {
        return mustOutput(withinCycles, qt, QUESTION);
    }

    @NotNull
    public TestNAR mustQuest(long withinCycles, @NotNull String qt) {
        return mustOutput(withinCycles, qt, QUEST);
    }

    public TestNAR mustOutput(long withinCycles, @NotNull String qt, byte question) {
        return mustOutput(withinCycles, qt, question, NaN, NaN);
    }

    @NotNull
    public TestNAR mustGoal(long withinCycles, @NotNull String goalTerm, float freq, float conf, long occ) {
        long t = nar.time();
        return mustOutput(t, t + withinCycles, goalTerm, GOAL, freq, freq, conf, conf, occ);
    }
    @NotNull
    public TestNAR mustGoal(long withinCycles, @NotNull String goalTerm, float freq, float conf, LongPredicate occ) {
        long t = nar.time();
        return mustOutput(t, t + withinCycles, goalTerm, GOAL, freq, freq, conf, conf, occ);
    }

    @NotNull
    public TestNAR mustGoal(long withinCycles, @NotNull String goalTerm, float freq, float conf, long start, long end) {
        long t = nar.time();
        return mustOutput(t, t + withinCycles, goalTerm, GOAL, freq, freq, conf, conf, start, end);
    }
    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String goalTerm, float freq, float conf, LongPredicate occ) {
        long t = nar.time();
        return mustOutput(t, t + withinCycles, goalTerm, BELIEF, freq, freq, conf, conf, occ);
    }

    public TestNAR ask(@NotNull String termString) throws Narsese.NarseseException {
        nar.question(termString);
        return this;
    }

    public TestNAR quest(@NotNull String termString) throws Narsese.NarseseException {
        nar.quest($.$(termString));
        return this;
    }

    @NotNull
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
    @NotNull
    public TestNAR believe(@NotNull String termString) {
        try {
            nar.believe(termString);
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @NotNull
    public TestNAR believe(@NotNull String termString, float freq, float conf) {
        try {
            nar.believe(termString, freq, conf);
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    @NotNull
    public TestNAR test() {
        return test(trace);
    }


    //    public static final class Report implements Serializable {
//
//        public final long time;
//        @NotNull
//        public final HitMeter[] eventMeters;
//        //@Nullable
//        protected final Serializable error;
//        protected final Task[] inputs;
//        private final TestNAR test;
    //final transient int stackElements = 4;


//
//        public boolean isSuccess() {
//            return true;
//        }

//        public void toString(@NotNull PrintStream out) {
//
//            if (error!=null) {
//                out.print(error);
//            }
//
//            out.print("@" + time + ' ');
//            out.print(Arrays.toString(eventMeters) + '\n');
//
//            for (Task t : inputs) {
//                out.println("IN " + t);
//            }
//
//            cond.forEach(c ->
//                c.toString(out)
//            );
//        }
//    }


    @NotNull
    public TestNAR test(boolean testAndPrintReport /* for use with JUnit */) {
        return run(0, testAndPrintReport);
    }

    @NotNull
    public TestNAR test(long cycles) {
        return run(cycles, true);
    }

    final class EarlyExit implements Consumer<NAR> {

        final int checkResolution; //every # cycles to check for completion
        int cycle;

        public EarlyExit(int checkResolution) {
            this.checkResolution = checkResolution;
            nar.onCycle(this);
        }

        @Override
        public void accept(NAR nar) {

            if (++cycle % checkResolution == 0 && !succeedsIfAll.isEmpty()) {

                boolean finished = true;


                for (int i = 0, requiresSize = succeedsIfAll.size(); i < requiresSize; i++) {
                    if (!succeedsIfAll.get(i).isTrue()) {
                        finished = false;
                        break;
                    }
                }

                if (finished) {
                    stop();
                }

            }
        }
    }


//    @NotNull
//    public TestNAR run(long extraCycles) {
//        return runUntil(time() + extraCycles);
//    }
//
//    @NotNull
//    public TestNAR runUntil(long finalCycle) {
//
//        result = null;
//
//        if (showOutput)
//            nar.trace();
//
//
//        //try {
//        int frames = (int) (finalCycle - time());
//        while (frames-- > 0 && !finished)
//            nar.cycle();
//
//        /*}
//        catch (Exception e) {
//            error = e;
//        }*/
//
//        return this;
//    }


//    /** returns null if there is no error, or a non-null String containing report if error */
//    @Deprecated public String evaluate() {
//        //TODO use report(..)
//
//        int conditions = requires.size();
//        int failures = getError()!=null ? 1 : 0;
//
//        for (TaskCondition tc : requires) {
//            if (!tc.isTrue()) {
//                failures++;
//            }
//        }
//
//        int successes = conditions - failures;
//
//
//        if (error!=null || failures > 0) {
//            String result = "";
//
//            if (error!=null) {
//                result += error.toString() + " ";
//            }
//
//            if (failures > 0) {
//                result += successes + "/ " + conditions + " conditions passed";
//            }
//
//            return result;
//        }
//
//        return null;
//
//    }

//    public void report(PrintStream out, boolean showFail, boolean showSuccess, boolean showExplanations) {
//
//        boolean output = false;
//
//        if (showFail || showSuccess) {
//
//            for (TaskCondition tc : requires) {
//
//                if (!tc.isTrue()) {
//                    if (showFail) {
//                        out.println(tc.getFalseReason());
//                        output = true;
//                    }
//                } else {
//                    if (showSuccess) {
//                        out.println(tc.getTrueReasons());
//                        output = true;
//                    }
//                }
//            }
//
//        }
//
//        if (error!=null) {
//            error.printStackTrace();
//            output = true;
//        }
//
//        if (showExplanations) {
//            for (ExplainableTask x : explanations ) {
//                x.printMeaning(out);
//                output = true;
//            }
//        }
//
//        if (output)
//            out.println();
//    }
//
//
//    public void inputTest(String script) {
//
//        if (script == null)
//            throw new RuntimeException("null input");
//
//        nar.input( new TestInput(script) );
//
//    }

//    class TestInput extends TextInput {
//        public TestInput(String script) {
//            super(nar, script);
//        }
//
//        @Override
//        public void accept(Task task) {
//            super.accept(task);
//            inputs.add(task);
//        }
//    }
}
