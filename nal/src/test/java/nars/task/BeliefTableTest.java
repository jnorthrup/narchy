package nars.task;

import nars.*;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthTable;
import nars.term.Term;
import nars.test.TestNAR;
import nars.test.analyze.BeliefAnalysis;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.IMPL;
import static nars.task.RevisionTest.x;
import static nars.time.Tense.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 7/5/15.
 */
class BeliefTableTest {


    private static void assertDuration(NAR n, String c, long start, long end) throws Narsese.NarseseException {
        TaskConcept cc = (TaskConcept) n.conceptualize(c);
        assertNotNull(cc, c + " unconceptualized");


        assertTrue(((BeliefTables)cc.beliefs()).tableFirst(DynamicTruthTable.class)!=null);

        Task t = n.belief(cc, start, end);
        assertNotNull(t);
        assertEquals(start, t.start());
        assertEquals(end, t.end(), ()-> t + " but end should be: " + end + "\n" + t.proof());
    }



    @Test
    void testEternalBeliefRanking() {


//        int cap = 10;

        NAR n = NARS.shell();
        BeliefAnalysis b = new BeliefAnalysis(n, x);

        b.believe(1.0f, 0.5f);
        b.print();

        BeliefTable beliefs = b.concept().beliefs();

        assertEquals(0.5, beliefs.match(ETERNAL, ETERNAL, null, 0, n).conf(), 0.001);
        Truth bt = n.beliefTruth(b, n.time());
        assertNotNull(bt);
        assertEquals(0.5, bt.conf(), 0.001);
        assertEquals(1, beliefs.taskCount());

        b.believe(1.0f, 0.5f);
        n.run();
        b.print();
        assertEquals(3 /* revision */, beliefs.taskCount());
        assertEquals(0.669, beliefs.match(ETERNAL, ETERNAL, null, 0, n).conf(), 0.01);

        b.believe(1.0f, 0.5f);
        n.run();
        b.print();
        assertEquals(5, beliefs.taskCount());
        @NotNull BeliefTable bb = beliefs;
        assertEquals(0.75, bb.match(ETERNAL, ETERNAL, null, 0, n).conf(), 0.001);
        assertEquals(0.75, n.beliefTruth(b, n.time()).conf(), 0.01);

        b.believe(1.0f, 0.5f);
        n.run();
        b.print();
        assertEquals(0.79, beliefs.match(ETERNAL, ETERNAL, null, 0, n).conf(), 0.2);
        assertEquals(7, beliefs.taskCount());

    }

    @Test
    void testPolation0() {

        int spacing = 4;
        float conf = 0.9f;
        float[] freqPattern =

                {0, 0.5f, 1f};
        long[] timing =
                {0, 2, 4};

        int dur = 1;

        NAR n = NARS.shell();
        n.time.dur(dur);

        BeliefAnalysis b = new BeliefAnalysis(n, x);

        assertEquals(timing.length, freqPattern.length);
        int k = 0;
        for (float f : freqPattern) {

            b.believe(0.5f, freqPattern[k], conf, timing[k]);
            k++;
        }
        int c = freqPattern.length;
        assertEquals(c, b.size(true));

        @NotNull BeliefTable table = b.concept().beliefs();

        b.print();
        int margin = spacing * (c / 2);
        for (int i = -margin; i < spacing * c + margin; i++)
            System.out.println(i + "\t" + table.truth(i,    /* relative to zero */  n));


        for (int i = 0; i < c; i++) {
            long w = timing[i];
            Truth truth = table.truth(w, n);
            float fExpected = freqPattern[i];
            assertEquals(fExpected, truth.freq(), 0.01f, "exact truth @" + w + " == " + fExpected);

            Task match = table.match(w, w, null, 0, n);
            assertEquals(fExpected, match.freq(), 0.01f, "exact belief @" + w + " == " + fExpected);
        }


        for (int i = 1; i < c - 1; i++) {
            float f = (freqPattern[i - 1] + freqPattern[i] + freqPattern[i + 1]) / 3f;
            long w = timing[i];
            assertEquals(f, table.truth(w, n).freq(), 0.1f, () -> "t=" + w);
        }


    }

    @Test
    void testLinearTruthpolation() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.time.dur(5);
        n.inputAt(10, "(x). :|:");
        n.run(11);


        n.conceptualize("(x)").print();

        assertEquals(0.85f, n.beliefTruth("(x)", 7).conf(), 0.1f);
        assertEquals(0.86f, n.beliefTruth("(x)", 8).conf(), 0.1f);
        assertEquals(0.88f, n.beliefTruth("(x)", 9).conf(), 0.1f);
        assertEquals(0.90f, n.beliefTruth("(x)", 10).conf(), 0.1f);
        assertEquals(0.88f, n.beliefTruth("(x)", 11).conf(), 0.1f);
        assertEquals(0.86f, n.beliefTruth("(x)", 12).conf(), 0.1f);


    }

    @Test
    void testDurationDithering() {
        NAR n = NARS.tmp();
        n.dtDither.set(3);
        n.time.dur(3);
        TestNAR t = new TestNAR(n);
        t.confTolerance(0.1f);
        t.inputAt(1, "x. :|:");
        t.inputAt(2, "y. :|:");
        t.mustBelieve(5, "(x&|y)", 1f, 0.81f, s -> true  /* TODO test occ = 0..3 */);
        t.mustBelieve(5, "(x=|>y)", 1f, 0.45f, s -> true  /* TODO test occ = 0..3 */);
        n.onTask(tt -> {
            if (!tt.isInput() && tt.start() % 3 != 0 && tt.end() % 3 != 0 || tt.end() > 0 && tt.start() > 3)
                fail();
        });
        t.test();
    }

    @Test
    void testTemporalUnion() throws Narsese.NarseseException {



        NAR n = NARS.tmp();

        n.time.dur(2);
        int a = 1;
        int b = 2;
        n.inputAt(a, "a:x. :|:");
        n.inputAt(b, "a:y. :|:");
        n.run(b + 1);


        for (String t : new String[]{"a:(x|y)", "a:(x&y)", "a:(x~y)", "a:(y~x)"}) {
            assertDuration(n, t, a, b);
        }


    }

    @Test
    void testDurationIntersection() {
        /*
        WRONG: t=25 is not common to both; 30 is however
        $.12 ((happy|i)-->L). 25 %.49;.81% {37: b;k} (((%1-->%2),(%3-->%2),task("."),notSet(%3),notSet(%1),neqRCom(%3,%1)),(((%1|%3)-->%2),((Intersection-->Belief))))
            $.25 (i-->L). 30 %.84;.90% {30: k}
            $.22 (happy-->L). 20⋈30 %.58;.90% {20: b}
        */

    }


    @Test
    void testBestMatchConjSimple() {

    }

    @Test
    void testBestMatchImplSimple() throws Narsese.NarseseException {
        for (Tense t : new Tense[]{Present/*, Eternal*/}) {
            NAR n = NARS.shell();

            n.believe("(a ==>+0 b)", t, 1f, 0.9f);
            n.believe("(a ==>+5 b)", t, 1f, 0.9f);
            n.believe("(a ==>-5 b)", t, 1f, 0.9f);

            long when = t == Present ? 0 : ETERNAL;

            //for (int dt = 3; dt < 7; dt++) {
            { int dt = 3;
                Term tt = IMPL.the((Term)$.$("a"), +dt, $.$("b"));
                Task fwd = n.answer(tt, BELIEF, when);
                assertEquals("(a ==>+3 b)", fwd.term().toString(), ()->tt + " -> " + fwd);
            }

            Task bwd = n.answer($.impl($.$("a"), -5, $.$("b")), BELIEF, when);
            assertEquals("(a ==>-2 b)", bwd.term().toString());


            Task x = n.answer($.impl($.$("a"), DTERNAL, $.$("b")), BELIEF, when);
            System.out.println(x);


        }

    }


}