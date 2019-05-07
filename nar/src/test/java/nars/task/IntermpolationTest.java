package nars.task;

import nars.*;
import nars.concept.TaskConcept;
import nars.table.BeliefTables;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static nars.time.Tense.*;
import static org.junit.jupiter.api.Assertions.*;

public class IntermpolationTest {

    private static float dtDiff(String x, String y) {
        return Intermpolate.dtDiff($$(x), $$(y));
    }

    @Test void testDTSimilarity() {
        for (String o : new String[] { "==>", "&&"}) {
            Term a = $$("(x " + o + "+0 y)");
            Term b = $$("(x " + o + "+1 y)");
            Term nb = $$("(x " + o + "-1 y)");
            Term c = $$("(x " + o + "+2 y)");
            assertTrue(
                    Intermpolate.dtDiff(a, b) < Intermpolate.dtDiff(a, c)
            );
            assertTrue(
                    Intermpolate.dtDiff(b, c) < Intermpolate.dtDiff(b, nb)
            );
        }
    }

    @Test
    void testDTDiffSame() {
        assertEquals(0f, dtDiff("(x ==>+5 y)", "(x ==>+5 y)"));
    }

    @Test
    void testDTDiffVariety() {
        assertEquals(0f, dtDiff("(x ==>+5 y)", "(x ==>+- y)"), 0.01f);
        assertEquals(0f, dtDiff("(x ==>+5 y)", "(x ==> y)"), 0.01f);
        assertEquals(0.4f, dtDiff("(x ==>+5 y)", "(x ==>+3 y)"), 0.01f);
        assertEquals(0.8f, dtDiff("(x ==>+5 y)", "(x ==>+1 y)"), 0.01f);
        assertEquals(1f, dtDiff("(x ==>+5 y)", "(x =|> y)"), 0.01f);
    }

    @Test
    void testDTImpl1() {
        float a52 = dtDiff("(x ==>+5 y)", "(x ==>+2 y)");
        float a54 = dtDiff("(x ==>+5 y)", "(x ==>+4 y)");
        assertTrue(a52 > a54);
    }

    @Test void testSimilarConj() {
        assertTrue(Float.isFinite(dtDiff("(&&,a,b,c)","((b&|c)&&a)")));
    }

    @Test
    void testConjSequence1() {
        float a52 = dtDiff("((x &&+5 y) &&+1 z)", "((x &&+2 y) &&+1 z)");
        float a54 = dtDiff("((x &&+5 y) &&+1 z)", "((x &&+4 y) &&+1 z)");
        assertTrue(a52 > a54);
    }

    @Test
    void testDTImplEmbeddedConj() {
        float a = dtDiff("((x &&+1 y) ==>+1 z)", "((x &&+1 y) ==>+2 z)");
        float b = dtDiff("((x &&+1 y) ==>+1 z)", "((x &&+2 y) ==>+1 z)");
        assertTrue(a > b);
    }

    @Test
    void testIntermpolation0() throws Narsese.NarseseException {
        Compound a = $.$("((a &&+3 b) &&+3 c)");
        Compound b = $.$("((a &&+3 b) &&+1 c)");
        RevisionTest.permuteChoose(a, b,
                "[((a &&+3 b) &&+1 c), ((a &&+3 b) &&+2 c), ((a &&+3 b) &&+3 c)]"
                //"[((a &&+3 b) &&+1 (c &&+2 c))]"
        );
    }
    @Test
    void testIntermpolationNegConj() throws Narsese.NarseseException {
        Compound a = $.$("--(x &&+1 y)");
        Compound b = $.$("--(x &&+2 y)");
        RevisionTest.permuteChoose(a, b,
                "[(--,(x &&+1 y)), (--,(x &&+2 y))]"
        );
    }
    @Test
    void testIntermpolationNegConjInSeq() throws Narsese.NarseseException {
        Compound a = $.$("(--(x &&+1 y) &&+1 c)");
        Compound b = $.$("(--(x &&+2 y) &&+1 c)");
        RevisionTest.permuteChoose(a, b,
                "[((--,(x &&+1 y)) &&+1 c), ((--,(x &&+2 y)) &&+1 c)]"
        );
    }
    @Test
    void testIntermpolation0b() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+3 (b &&+3 c))");
        Compound b = $.$("(a &&+1 (b &&+1 c))");
        RevisionTest.permuteChoose(a, b,
                "[((a &&+1 b) &&+1 c), ((a &&+2 b) &&+2 c), ((a &&+3 b) &&+3 c)]");
    }

    @Test
    void testIntermpolationOrderMismatch() throws Narsese.NarseseException {
        Compound a = $.$("(c &&+1 (b &&+1 a))");
        Compound b = $.$("(a &&+1 (b &&+1 c))");
        RevisionTest.permuteChoose(a, b, "[((a &&+1 b) &&+1 c), (b &&+1 (a&|c)), ((a&|c) &&+1 b), ((c &&+1 b) &&+1 a)]");
    }

    @Test
    void testIntermpolationOrderPartialMismatch() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+1 (b &&+1 c))");
        Compound b = $.$("(a &&+1 (c &&+1 b))");
        RevisionTest.permuteChoose(a, b, "[((a &&+1 b) &&+1 c), ((a &&+1 c) &&+1 b), (a &&+2 (b&|c)), (a &&+1 (b&|c))]");
    }

    @Test
    void testIntermpolationImplSubjOppositeOrder() throws Narsese.NarseseException {
        Compound a = $.$("((x &&+2 y) ==> z)");
        Compound b = $.$("((y &&+2 x) ==> z)");
        RevisionTest.permuteChoose(a, b, "[((x&&y)=|>z), ((y &&+2 x)=|>z), ((y &&+1 x)=|>z), ((x &&+1 y)=|>z), ((x &&+2 y)==>z)]");
    }

    @Test
    void testIntermpolationImplSubjOppositeOrder2() throws Narsese.NarseseException {
        Compound a = $.$("((x &&+1 y) ==>+1 z)");
        Compound b = $.$("((y &&+1 x) ==>+1 z)");
        RevisionTest.permuteChoose(a, b,
                "[((x&&y) ==>+1 z), ((y &&+1 x) ==>+1 z), ((x &&+1 y) ==>+1 z)]");
    }
    @Test
    void testIntermpolationImplDirectionMismatch() throws Narsese.NarseseException {
        Compound a = $.$("(a ==>+1 b)");
        Compound b = $.$("(a ==>-1 b))");
        RevisionTest.permuteChoose(a, b, "[(a ==>-1 b), (a=|>b), (a ==>+1 b)]");
    }


    @Test
    void testIntermpolationImplSubjImbalance() throws Narsese.NarseseException {
        Compound a = $.$("((x &&+1 y) ==> z)");
        Compound b = $.$("(((x &&+1 y) &&+1 x) ==> z)");
        RevisionTest.permuteChoose(a, b, "[(((x &&+1 y) &&+1 x)==>z), ((x &&+1 y)==>z)]");
    }

    @Test
    void testIntermpolationOrderPartialMismatch2() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+1 (b &&+1 (d &&+1 c)))");
        Compound b = $.$("(a &&+1 (b &&+1 (c &&+1 d)))");
        String expected = "[((a &&+1 b) &&+1 (d &&+1 c)), ((a &&+1 b) &&+1 (c&|d)), ((a &&+1 b) &&+1 (c &&+1 d))]";
        RevisionTest.permuteChoose(a, b, expected);
    }

    @Test
    void testIntermpolationOrderMixDternalPre() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+1 (b &&+1 c))");
        Compound b = $.$("(a &&+1 (b && c))");
        RevisionTest.permuteChoose(a, b, "[(a &&+1 (b&&c))]");
    }

    @Test
    void testIntermpolationOrderMixDternalPost() throws Narsese.NarseseException {
        Term e = $$("(a && (b &&+1 c))");
        assertEquals("((b &&+1 c)&&a)", e.toString());

        Compound a = $.$("(a &&+1 (b &&+1 c))");
        Compound b = $.$("(a && (b &&+1 c))");

        RevisionTest.permuteChoose(a, b, "[" + e + ']');
    }

    @Test
    void testIntermpolationWrongOrderSoDternalOnlyOption() throws Narsese.NarseseException {
        Compound a = $.$("(((right-->tetris) &&+5 (rotCW-->tetris)) &&+51 (tetris-->[happy]))");
        Compound b = $.$("(((tetris-->[happy])&&(right-->tetris)) &&+11 (rotCW-->tetris))");
        RevisionTest.permuteChoose(a, b, "[(&&,(tetris-->[happy]),(right-->tetris),(rotCW-->tetris))]");
    }

    @Test
    void testIntermpolationOrderMixDternal2() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+1 (b &&+1 (c &&+1 d)))");
        Compound b = $.$("(a &&+1 (b &&+1 (c&&d)))");
        RevisionTest.permuteChoose(a, b, "[((a &&+1 b) &&+1 (c&|d)), ((a &&+1 b) &&+1 (c &&+1 d))]");
    }

    @Test
    void testIntermpolationOrderMixDternal2Reverse() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+1 (b &&+1 (c &&+1 d)))");
        Compound b = $.$("((a && b) &&+1 (c &&+1 d))");
        RevisionTest.permuteChoose(a, b, "[(((a&&b) &&+1 c) &&+1 d), (((a&&b) &&+1 c) &&+2 d), (((a&&b) &&+2 c) &&+1 d), ((a&&b) &&+2 (c&|d))]");
    }

    @Test
    void testIntermpolationOrderPartialMismatchReverse() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+1 (b &&+1 c))");
        Compound b = $.$("(b &&+1 (a &&+1 c))");
        RevisionTest.permuteChoose(a, b, "[((a&&b) &&+1 c)]");
    }

    @Test
    void testIntermpolationOrderPartialMismatchReverse2() throws Narsese.NarseseException {
        Compound a = $.$("(b &&+1 (a &&+1 (c &&+1 d)))");
        Compound b = $.$("(a &&+1 (b &&+1 (c &&+1 d)))");
        RevisionTest.permuteChoose(a, b,
                //"[(((a&|b) &&+1 c) &&+1 d), (((a&|b) &&+2 c) &&+1 d), ((b &&+1 a) &&+1 (c &&+1 d)), ((a &&+1 b) &&+1 (c &&+1 d))]"
                "[(((a&&c)&|b) &&+2 ((a&&c)&|d))]"
        );
    }

    @Test
    void testIntermpolationConj2OrderSwap() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+1 b)");
        Compound b = $.$("(b &&+1 a))");
        Compound c = $.$("(b &&+2 a))");
        RevisionTest.permuteChoose(a, b, "[(a&&b), (b &&+1 a), (a &&+1 b)]");
        RevisionTest.permuteChoose(a, b, "[(a&|b)]");
        RevisionTest.permuteChoose(a, c, "[(b &&+2 a), (a &&+1 b)]"); //not within dur
        RevisionTest.permuteChoose(a, c, "[(a&|b)]");

    }



    @Test
    void testImplSimple() throws Narsese.NarseseException {
        Compound a = $.$("(a ==>+4 b)");
        Compound b = $.$("(a ==>+2 b))");
        RevisionTest.permuteChoose(a, b, "[(a ==>+2 b), (a ==>+3 b), (a ==>+4 b)]");
    }
    @Test
    void testIntermpolationImplDirectionDternalAndTemporal() throws Narsese.NarseseException {
        Compound a = $.$("(a ==>+1 b)");
        Compound b = $.$("(a ==> b))");
        RevisionTest.permuteChoose(a, b, "[(a=|>b), (a ==>+1 b)]");
    }

    @Test
    void testIntermpolation0invalid() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+3 (b &&+3 c))");
        Compound b = $.$("(a &&+1 b)");
        try {
            Set<Term> p = RevisionTest.permuteIntermpolations(a, b);
            fail("");
        } catch (Error e) {
            assertTrue(true);
        }
    }

    @Test
    void testIntermpolationConjSeq() throws Narsese.NarseseException {
        Compound f = $.$("(a &&+1 b)");
        Compound g = $.$("(a &&-1 b)");
        RevisionTest.permuteChoose(f, g, "[(a&&b), (b &&+1 a), (a &&+1 b)]");
    }
    @Test
    void testIntermpolationConjSeq2() throws Narsese.NarseseException {
        Compound h = $.$("(a &&+1 b)");
        Compound i = $.$("(a &| b)");
        RevisionTest.permuteChoose(h, i, "[(a&&b), (a &&+1 b)]");

    }

    @Test
    void testIntermpolationConjInImpl2b() throws Narsese.NarseseException {

        Compound h = $.$("(x==>(a &&+1 b))");
        Compound i = $.$("(x==>(a &| b))");

        RevisionTest.permuteChoose(h, i, "[(x=|>(a&&b)), (x==>(a &&+1 b))]");
    }

    @Test
    void testIntermpolationInner() throws Narsese.NarseseException {
        RevisionTest.permuteChoose($.$("(x --> (a &&+1 b))"), $.$("(x --> (a &| b))"),
                "[(x-->(a&&b)), (x-->(a &&+1 b))]");
    }

    @Test
    void testEmbeddedIntermpolation() {
        NAR nar = NARS.shell();
        nar.time.dur(8);

        Compound a0 = $$("(b ==>+6 c)");
        Compound b0 = $$("(b ==>+10 c)");

        Term c0 = Intermpolate.intermpolate(a0, b0, 0.5f, nar);
        assertEquals("(b ==>+8 c)", c0.toString());


        Compound a = $$("(a, (b ==>+6 c))");
        Compound b = $$("(a, (b ==>+10 c))");

        Term c = Intermpolate.intermpolate(a, b, 0.5f, nar);
        assertEquals("(a,(b ==>+8 c))", c.toString());

        {

            assertEquals("(a,(b ==>+6 c))",
                    Intermpolate.intermpolate(a, b, 1f, nar).toString());
            assertEquals("(a,(b ==>+10 c))",
                    Intermpolate.intermpolate(a, b, 0f, nar).toString());


        }
    }
    @Test
    void testConceptualizationIntermpolation() throws Narsese.NarseseException {


        for (Tense t : new Tense[]{Present, Eternal}) {
            NAR n = NARS.shell();
            //n.log();
            n.time.dur(8);

            //extreme example: too far distance, so results in DTERNAL
//            assertEquals(DTERNAL, Intermpolate.chooseDT(1,100,0.5f,n));

            int a = 2;
            int b = 4;
            int ab = 3; //expected

            assertEquals(ab, Intermpolate.chooseDT(a,b,0.5f,n));

            n.believe("((a ==>+" + a + " b)-->[pill])", t, 1f, 0.9f);
            n.believe("((a ==>+" + b + " b)-->[pill])", t, 1f, 0.9f);
            n.run(1);


            String abpill = "((a==>b)-->[pill])";
            assertEquals("((a ==>+- b)-->[pill])", $$("((a ==>+- b)-->[pill])").concept().toString());
            assertEquals("((a ==>+- b)-->[pill])", $$(abpill).concept().toString());

            TaskConcept cc = (TaskConcept) n.conceptualize(abpill);
            assertNotNull(cc);

            String correctMerge = "((a ==>+" + ab +" b)-->[pill])";
            cc.beliefs().print();


            long when = t == Present ? 0 : ETERNAL;
            Task m = cc.beliefs().match(when, when, null, 0, n);
            assertEquals(correctMerge, m.term().toString());


            ((BeliefTables)cc.beliefs()).tableFirst(TemporalBeliefTable.class).setTaskCapacity(1);

            cc.print();


            assertEquals(correctMerge, cc.beliefs().match((long) 0, (long) 0, null, 0, n).term().toString());
        }
    }

}
