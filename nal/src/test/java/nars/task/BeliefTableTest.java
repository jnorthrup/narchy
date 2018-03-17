package nars.task;

import nars.*;
import nars.concept.TaskConcept;
import nars.concept.dynamic.DynamicTruthBeliefTable;
import nars.table.BeliefTable;
import nars.test.TestNAR;
import nars.test.analyze.BeliefAnalysis;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static nars.Op.BELIEF;
import static nars.task.RevisionTest.x;
import static nars.time.Tense.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 7/5/15.
 */
public class BeliefTableTest {


    static void assertDuration(NAR n, String c, long start, long end) throws Narsese.NarseseException {
        TaskConcept cc = (TaskConcept) n.conceptualize(c);
        assertNotNull(cc, c + " unconceptualized");

        List<Task> tt = cc.beliefs().streamTasks().collect(toList());
        assertTrue(cc.beliefs() instanceof DynamicTruthBeliefTable || !tt.isEmpty(), c + " not believed");

        if (!tt.isEmpty()) {
            Task t = tt.get(0);
            //System.out.println(sim.proof());
            //System.out.println(sim.start() + ".." + /*sim.occurrence() + ".."*/ + sim.end());
            assertEquals(start, t.start());
            assertEquals(end, t.end());
        }
    }

    static float dtDiff(String x, String y) {
        return Revision.dtDiff($.$$(x), $.$$(y));
    }

    @Test
    public void testEternalBeliefRanking() {

        Param.DEBUG = true;

        int cap = 10;

        NAR n = NARS.shell();
        BeliefAnalysis b = new BeliefAnalysis(n, x);

        b.believe(1.0f, 0.5f);
        b.print();

        BeliefTable beliefs = b.concept().beliefs();

        assertEquals(0.5, beliefs.match(ETERNAL, null, n).conf(), 0.001);
        Truth bt = n.beliefTruth(b, n.time());
        assertNotNull(bt);
        assertEquals(0.5, bt.conf(), 0.001);
        assertEquals(1, beliefs.size());

        b.believe(1.0f, 0.5f);
        n.run();
        b.print();
        assertEquals(3 /* revision */, beliefs.size());
        assertEquals(0.669, beliefs.match(ETERNAL, null, n).conf(), 0.01);

        b.believe(1.0f, 0.5f);
        n.run();
        b.print();
        assertEquals(5, beliefs.size());
        @NotNull BeliefTable bb = beliefs;
        assertEquals(0.75, bb.match(ETERNAL, null, n).conf(), 0.001);
        assertEquals(0.75, n.beliefTruth(b, n.time()).conf(), 0.01);

        b.believe(1.0f, 0.5f);
        n.run();
        b.print();
        assertEquals(0.79, beliefs.match(ETERNAL, null, n).conf(), 0.02);
        assertEquals(6, beliefs.size());

    }

    @Test
    public void testPolation0() {

        int spacing = 4;
        float conf = 0.9f;
        float[] freqPattern =
                //new float[]{0, 0.25f, 0.5f, 0.75f, 1f};
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
            //create linear gradient of belief across time, freq beginning at 0 and increasing to 1
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

        //measure exact timing
        for (int i = 0; i < c; i++) {
            long w = timing[i];
            Truth truth = table.truth(w, n);
            float fExpected = freqPattern[i];
            assertEquals(fExpected, truth.freq(), 0.01f, "exact truth @" + w + " == " + fExpected);

            Task match = table.match(w, null, n);
            assertEquals(fExpected, match.freq(), 0.01f, "exact belief @" + w + " == " + fExpected);
        }

        //measure midpoint interpolation
        for (int i = 0; i < c - 1; i++) {
            float f = (freqPattern[i] + freqPattern[i + 1]) / 2f;
            long w = (timing[i] + timing[i + 1]) / 2;
            assertEquals(f, table.truth(w, n).freq(), 0.1f);
        }


//        /* first */
//        @Nullable Truth firstBeliefTruth = table.truth((long) 0, n);
//        assertEquals(0.43f, firstBeliefTruth.freq(), 0.1f);
//
//        /* last */
//        @Nullable Truth lastBeliefTruth = table.truth((long) (spacing * (c - 1)), n);
//        assertEquals(0.56f, lastBeliefTruth.freq(), 0.1f);
//
//        @Nullable Truth endTruth = table.truth((long) (spacing * (c - 1) + margin), n);
//        assertEquals(0.55f, endTruth.freq(), 0.2f);
//        assertTrue(lastBeliefTruth.conf() >= endTruth.conf());
//
//        @Nullable Truth startTruth = table.truth((long) (0 - margin), n);
//        assertEquals(0.44f, startTruth.freq(), 0.2f);
//        assertTrue(firstBeliefTruth.conf() >= startTruth.conf());
    }

    @Test
    public void testLinearTruthpolation() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.time.dur(5);
        n.inputAt(10, "(x). :|:");
        n.run(11);
        //with duration = 5, the evidence surrounding a point
        // belief/goal will decay in the +/- 2.5 radius of time surrounding it.

        n.conceptualize("(x)").print();

        assertEquals(0.85f, n.beliefTruth("(x)", 7).conf(), 0.1f);
        assertEquals(0.86f, n.beliefTruth("(x)", 8).conf(), 0.1f);
        assertEquals(0.88f, n.beliefTruth("(x)", 9).conf(), 0.1f);
        assertEquals(0.90f, n.beliefTruth("(x)", 10).conf(), 0.1f);
        assertEquals(0.88f, n.beliefTruth("(x)", 11).conf(), 0.1f);
        assertEquals(0.86f, n.beliefTruth("(x)", 12).conf(), 0.1f);
        //assertNull( n.beliefTruth("(x)", 13000) );

    }

    @Test
    public void testDurationDithering() {
        NAR n = NARS.tmp();
        n.dtDither.set(1f);
        n.time.dur(3);
        TestNAR t = new TestNAR(n);
        t.confTolerance(0.1f);
        t.inputAt(1, "x. :|:");
        t.inputAt(2, "y. :|:");
        t.mustBelieve(5, "(x&|y)", 1f, 0.81f, 1);
        t.mustBelieve(5, "(x=|>y)", 1f, 0.45f, 1);
        t.test(true);


//        assertEquals( $.$("((x) &| (y))"), n.term("((x) &&+1 (y))"));
//        assertEquals( $.$("((x) &| (y))"), n.term("((x) &&-1 (y))"));
//        assertEquals( "(&|,(x),(y),(z))", n.term("(((x) &&+1 (y)) &&+1 (z))").toString());
//        assertEquals( $.$("((x) &&+6 (y))"), n.term("((x) &&+6 (y))"));
//        assertEquals( $.$("((x) =|> (y))"), n.term("((x) ==>+1 (y))"));
////        assertEquals( $.$("((x) <|> (y))"), n.term("((x) <=>+1 (y))"));

    }

    @Test
    public void testTemporalIntersection() throws Narsese.NarseseException {

        //this.activeTasks = activeTasks;
        NAR n = NARS.tmp();

        //TODO extend these beliefs so they overlap (intersect), then it will derive the results
        n.inputAt(2, "a:x. :|:");
        n.inputAt(10, "a:y. :|:");
        n.run(128);

        for (String t : new String[]{"a:(x|y)", "a:(x&y)", "a:(x~y)", "a:(y~x)"}) {
            assertDuration(n, t, 2, 10);
        }

        //n.concept("(x-->a)").print();
        //n.concept("(y-->a)").print();
    }

    @Test
    public void testDurationIntersection() {
        /*
        WRONG: t=25 is not common to both; 30 is however
        $.12 ((happy|i)-->L). 25 %.49;.81% {37: b;k} (((%1-->%2),(%3-->%2),task("."),notSet(%3),notSet(%1),neqRCom(%3,%1)),(((%1|%3)-->%2),((Intersection-->Belief))))
            $.25 (i-->L). 30 %.84;.90% {30: k}
            $.22 (happy-->L). 20⋈30 %.58;.90% {20: b}
        */

    }

    @Test
    public void testConceptualizationIntermpolation() throws Narsese.NarseseException {
        for (Tense t : new Tense[]{Present, Eternal}) {
            NAR n = NARS.tmp();
            n.dtMergeOrChoose.set(true);
            n.believe("((a ==>+2 b)-->[pill])", t, 1f, 0.9f);
            n.believe("((a ==>+6 b)-->[pill])", t, 1f, 0.9f);

            //@NotNull Bag<Concept, PLink<Concept>> cb = n.focus.active;
            //assertTrue(5 <= cb.size());

            String abpill = "((a==>b)-->[pill])";
            TaskConcept cc = (TaskConcept) n.conceptualize(abpill); //iterator().next().get();//((ArrayBag<Concept>) cb).get(0).get();
            assertNotNull(cc);

            String correctMerge = "((a ==>+4 b)-->[pill])";
            cc.beliefs().print();

            //test belief match interpolated a result
            long when = t == Present ? 0 : ETERNAL;
            assertEquals(correctMerge, cc.beliefs().match(when, null, n).term().toString());


            //test merge after capacity shrink:

            cc.beliefs().setCapacity(1, 1); //set to capacity=1 to force compression

            cc.print();

            //n.forEachTask(System.out::println);

            //INTERMPOLATION APPLIED AFTER REVECTION:
            assertEquals(correctMerge, cc.beliefs().match(0, null, n).term().toString());
        }
    }

    @Test
    public void testBestMatchConjSimple() throws Narsese.NarseseException {
        //TODO
    }

    @Test
    public void testBestMatchImplSimple() throws Narsese.NarseseException {
        for (Tense t : new Tense[]{Present/*, Eternal*/}) {
            NAR n = NARS.tmp();
            n.dtMergeOrChoose.set(false);
            n.believe("(a ==>+0 b)", t, 1f, 0.9f);
            n.believe("(a ==>+5 b)", t, 1f, 0.9f);
            n.believe("(a ==>-5 b)", t, 1f, 0.9f);

            long when = t == Present ? 0 : ETERNAL;

            for (int dt = 3; dt < 7; dt++) {
                Task fwd = n.match($.impl($.$("a"), +dt, $.$("b")), BELIEF, when);
                assertEquals("(a ==>+5 b)", fwd.term().toString());
            }

            Task bwd = n.match($.impl($.$("a"), -5, $.$("b")), BELIEF, when);
            assertEquals("(a ==>-5 b)", bwd.term().toString());


            Task x = n.match($.impl($.$("a"), DTERNAL, $.$("b")), BELIEF, when);
            System.out.println(x);


        }

    }

    @Test
    public void testDTDiffSame() {

        //+- matches anything
        float same = dtDiff("(x ==>+5 y)", "(x ==>+5 y)");
        assertEquals(0f, same, 0.001f);
        assertEquals(dtDiff("(x ==>+5 y)", "(x ==>+- y)"), same);
    }

    @Test
    public void testDTImpl1() {

        float a52 = dtDiff("(x ==>+5 y)", "(x ==>+2 y)");
        float a54 = dtDiff("(x ==>+5 y)", "(x ==>+4 y)");
        assertEquals(3, a52, 0.001f);
        assertEquals(1, a54, 0.001f);
        assertTrue(a52 > a54);
    }
    @Test
    public void testConjSequence1() {

        float a52 = dtDiff("((x &&+5 y) &&+1 z)", "((x &&+2 y) &&+1 z)");
        float a54 = dtDiff("((x &&+5 y) &&+1 z)", "((x &&+4 y) &&+1 z)");
        assertEquals(4, a52, 0.001f);
        assertEquals(2, a54, 0.001f);
        assertTrue(a52 > a54);
    }

    @Test
    public void testDTImplEmbeddedConj() {

        //difference in the subterm has less impact than at the root
        float a = dtDiff("((x &&+1 y) ==>+1 z)", "((x &&+1 y) ==>+2 z)");
        float b = dtDiff("((x &&+1 y) ==>+1 z)", "((x &&+2 y) ==>+1 z)");
        assertEquals(1, a, 0.1f);
        assertEquals(0.5f, b, 0.1f);
    }

}