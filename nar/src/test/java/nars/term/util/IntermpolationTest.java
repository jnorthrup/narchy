package nars.term.util;

import jcog.pri.ScalarValue;
import nars.*;
import nars.concept.TaskConcept;
import nars.table.BeliefTables;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.conj.ConjList;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import static jcog.Util.assertUnitized;
import static nars.$.*;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.*;
import static org.junit.jupiter.api.Assertions.*;

class IntermpolationTest {

    private static float dtDiff(String x, String y) {
        return Intermpolate.dtDiff(INSTANCE.$$(x), INSTANCE.$$(y));
    }

    static void permuteChoose(Compound a, Compound b, String expected) {
        assertEquals(expected, permuteIntermpolations(a, b).toString());
    }

    static Set<Term> permuteIntermpolations(Compound a, Compound b) {
        assertEquals(a.op(),a.op());
        {
            float ab = Intermpolate.dtDiff(a, b);
            assertTrue(Float.isFinite(ab), new Supplier<String>() {
                @Override
                public String get() {
                    return "dtDiff(" + a + "," + b + ")=" + ab;
                }
            });
            assertEquals(ab, Intermpolate.dtDiff(b, a), ScalarValue.Companion.getEPSILON()); //commutative
        }

        NAR s = NARS.shell();

        Term concept = a.concept();
        assertEquals(concept, b.concept(), new Supplier<String>() {
            @Override
            public String get() {
                return "concepts differ: " + a + ' ' + b;
            }
        });


        Set<Term> ss = new TreeSet();

        int n = 10 * (a.volume() + b.volume());
        for (int i = 0; i < n; i++) {
            float r = s.random().nextFloat();
            Term ab = Intermpolate.intermpolate(a, b, r, s);
            assertEquals(a.op(),ab.op(), new Supplier<String>() {
                @Override
                public String get() {
                    return a + " + " + b + " @ " + r;
                }
            });
            ss.add(ab);
        }

        //System.out.println(ss);

        return ss;
    }

    @Test void DTSimilarity() {
        assertTrue(Intermpolate.dtDiff(0,1) == Intermpolate.dtDiff(0,-1));
        assertTrue(Intermpolate.dtDiff(0,1) == Intermpolate.dtDiff(0,2));
        assertTrue(Intermpolate.dtDiff(1,2) < Intermpolate.dtDiff(1,4));
        assertTrue(Intermpolate.dtDiff(1,2) < Intermpolate.dtDiff(1,-1));

        for (String o : new String[] { "==>", "&&"}) {
            Term a = INSTANCE.$$("(x " + o + "+1 y)");
            Term b = INSTANCE.$$("(x " + o + "+2 y)");
            Term nb = INSTANCE.$$("(x " + o + "-2 y)");
            Term c = INSTANCE.$$("(x " + o + "+4 y)");
            float ab = Intermpolate.dtDiff(a, b);
            float ac = Intermpolate.dtDiff(a, c);
            assertTrue(
                    ab < ac
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
        assertEquals(0.5f, dtDiff("(x ==>+5 y)", "(x ==> y)"), 0.01f);
        assertEquals(1f, dtDiff("(x ==>+5 y)", "(x ==>-5 y)"), 0.01f);
        assertEquals(0.2f, dtDiff("(x ==>+5 y)", "(x ==>+3 y)"), 0.01f);
        assertEquals(0.4f, dtDiff("(x ==>+5 y)", "(x ==>+1 y)"), 0.01f);
    }

    @Test
    void testDTImpl1() {
        float a52 = dtDiff("(x ==>+5 y)", "(x ==>+2 y)");
        float a54 = dtDiff("(x ==>+5 y)", "(x ==>+4 y)");
        assertTrue(a52 > a54);
    }

    @Test void SimilarConj() {
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
        Compound a = $.INSTANCE.$("((a &&+3 b) &&+3 c)");
        Compound b = $.INSTANCE.$("((a &&+3 b) &&+1 c)");
        permuteChoose(a, b,
                "[((a &&+3 b) &&+1 c), ((a &&+3 b) &&+2 c), ((a &&+3 b) &&+3 c)]"
                //"[((a &&+3 b) &&+1 (c &&+2 c))]"
        );
    }
    @Test
    void testIntermpolationNegConj() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("--(x &&+1 y)");
        Compound b = $.INSTANCE.$("--(x &&+2 y)");
        permuteChoose(a, b,
                "[(--,(x &&+1 y)), (--,(x &&+2 y))]"
        );
    }
    @Test
    void testIntermpolationNegConjInSeq() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(--(x &&+1 y) &&+1 c)");
        Compound b = $.INSTANCE.$("(--(x &&+2 y) &&+1 c)");
        Compound c = $.INSTANCE.$("(--(x &&+3 y) &&+1 c)");

        float ab = Intermpolate.dtDiff(a, b);
        float ac = Intermpolate.dtDiff(a, c);
        assertTrue(ab < ac, new Supplier<String>() {
            @Override
            public String get() {
                return "fail: " + ab + " < " + ac;
            }
        });
        permuteChoose(a, b,
                "[((--,(x &&+1 y)) &&+1 c), ((--,(x &&+2 y)) &&+1 c)]"
        );
    }
    @Test
    void testIntermpolation0b() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a &&+3 (b &&+3 c))");
        Compound b = $.INSTANCE.$("(a &&+1 (b &&+1 c))");
        permuteChoose(a, b,
                "[((a &&+1 b) &&+1 c), ((a &&+2 b) &&+2 c), ((a &&+3 b) &&+3 c)]");
    }

    @Test
    void testIntermpolationOrderMismatch() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(c &&+1 (b &&+1 a))");
        Compound b = $.INSTANCE.$("(a &&+1 (b &&+1 c))");
        permuteChoose(a, b, "[((a &&+1 b) &&+1 c), (b &&+1 (a&|c)), ((a&|c) &&+1 b), ((c &&+1 b) &&+1 a)]");
    }

    @Test
    void testIntermpolationOrderPartialMismatch() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a &&+1 (b &&+1 c))");
        Compound b = $.INSTANCE.$("(a &&+1 (c &&+1 b))");
        permuteChoose(a, b, "[((a &&+1 b) &&+1 c), ((a &&+1 c) &&+1 b), (a &&+2 (b&|c)), (a &&+1 (b&|c))]");
    }

    @Test
    void testIntermpolationImplSubjOppositeOrder() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("((x &&+2 y) ==> z)");
        Compound b = $.INSTANCE.$("((y &&+2 x) ==> z)");
        permuteChoose(a, b, "[((x&&y)==>z), ((y &&+2 x)==>z), ((y &&+1 x)==>z), ((x &&+1 y)==>z), ((x &&+2 y)==>z)]");
    }

    @Test
    void testIntermpolationImplSubjOppositeOrder2() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("((x &&+1 y) ==>+1 z)");
        Compound b = $.INSTANCE.$("((y &&+1 x) ==>+1 z)");
        permuteChoose(a, b,
                "[((x&&y) ==>+1 z), ((y &&+1 x) ==>+1 z), ((x &&+1 y) ==>+1 z)]");
    }
    @Test
    void testIntermpolationImplDirectionMismatch() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a ==>+1 b)");
        Compound b = $.INSTANCE.$("(a ==>-1 b))");
        permuteChoose(a, b, "[(a==>b), (a ==>-1 b), (a ==>+1 b)]");
    }


    @Test
    void testIntermpolationImplSubjImbalance() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("((x &&+1 y) ==> z)");
        Compound b = $.INSTANCE.$("(((x &&+1 y) &&+1 x) ==> z)");
        permuteChoose(a, b, "[(((x &&+1 y) &&+1 x)==>z), ((x &&+1 y)==>z)]");
    }

    @Test
    void testIntermpolationOrderPartialMismatch2() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a &&+1 (b &&+1 (d &&+1 c)))");
        Compound b = $.INSTANCE.$("(a &&+1 (b &&+1 (c &&+1 d)))");
        String expected = "[((a &&+1 b) &&+1 (d &&+1 c)), ((a &&+1 b) &&+1 (c&|d)), ((a &&+1 b) &&+1 (c &&+1 d))]";
        permuteChoose(a, b, expected);
    }

    @Test
    void testIntermpolationOrderMixDternalPre() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a &&+1 (b &&+1 c))");
        Compound b = $.INSTANCE.$("(a &&+1 (b && c))");
        permuteChoose(a, b, "[(a &&+1 (b&&c))]");
    }


    @Test
    void testIntermpolationWrongOrderSoDternalOnlyOption() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(((right-->tetris) &&+2 (rotCW-->tetris)) &&+1 (tetris-->[happy]))");

        Compound b = $.INSTANCE.$("(((tetris-->[happy]) &&+1 (right-->tetris)) &&+2 (rotCW-->tetris))");

        ConjList ae = ConjList.events(a);
        ConjList be = ConjList.events(b);
        assertEquals(
            ae.sortThisByValue().toItemString(),
            be.sortThisByValue().toItemString()
        );

        permuteChoose(a, b, "[(((tetris-->[happy])&&(right-->tetris)) &&+2 (rotCW-->tetris)), (((tetris-->[happy]) &&+1 (right-->tetris)) &&+2 (rotCW-->tetris)), ((right-->tetris) &&+2 ((tetris-->[happy])&&(rotCW-->tetris))), (((right-->tetris) &&+2 (rotCW-->tetris)) &&+1 (tetris-->[happy]))]");

    }

    @Test
    void testIntermpolationOrderMixDternal2() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a &&+1 (b && c))");
        Compound b = $.INSTANCE.$("(a &&+1 (b &&+1 c))");
        permuteChoose(a, b, "[((a &&+1 b) &&+1 c), (a &&+1 (b && c)]");
    }
    @Test
    void testIntermpolationOrderMixDternal3() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a &&+1 (b &&+1 (c &&+1 d)))");
        Compound b = $.INSTANCE.$("(a &&+1 (b &&+1 (c&&d)))");
        permuteChoose(a, b, "[((a &&+1 b) &&+1 (c&&d)), ((a &&+1 b) &&+1 (c &&+1 d))]");
    }

    @Test
    void testIntermpolationOrderMixDternal2Reverse() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a &&+1 (b &&+1 (c &&+1 d)))");
        Compound b = $.INSTANCE.$("((a && b) &&+1 (c &&+1 d))");
        permuteChoose(a, b, "[(((a&&b) &&+1 c) &&+1 d), (((a&&b) &&+1 c) &&+2 d), (((a&&b) &&+2 c) &&+1 d), ((a&&b) &&+2 (c&|d))]");
    }

    @Test
    void testIntermpolationOrderPartialMismatchReverse() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a &&+1 (b &&+1 c))");
        Compound b = $.INSTANCE.$("(b &&+1 (a &&+1 c))");
        permuteChoose(a, b, "[((b &&+1 a) &&+1 c), ((a &&+1 b) &&+1 c)]");
    }

    @Test
    void testIntermpolationOrderPartialMismatchReverse2() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(b &&+1 (a &&+1 (c &&+1 d)))");
        Compound b = $.INSTANCE.$("(a &&+1 (b &&+1 (c &&+1 d)))");
        permuteChoose(a, b,
                "[((b &&+1 a) &&+1 (c &&+1 d)), ((a &&+1 b) &&+1 (c &&+1 d))]"
        );
    }

    @Test
    void testIntermpolationConj2OrderSwap() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a &&+1 b)");
        Compound b = $.INSTANCE.$("(b &&+1 a)");
        Compound c = $.INSTANCE.$("(b &&+2 a)");
        permuteChoose(a, b, "[(a&&b), (b &&+1 a), (a &&+1 b)]");
        permuteChoose(a, c, "[(a&&b), (b &&+2 a), (b &&+1 a), (a &&+1 b)]"); //not within dur

    }



    @Test
    void testImplSimple() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a ==>+4 b)");
        Compound b = $.INSTANCE.$("(a ==>+2 b))");
        permuteChoose(a, b, "[(a ==>+2 b), (a ==>+3 b), (a ==>+4 b)]");
    }
    @Test
    void testIntermpolationImplDirectionDternalAndTemporal() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a ==>+1 b)");
        Compound b = $.INSTANCE.$("(a ==> b))");
        permuteChoose(a, b, "[(a==>b), (a ==>+1 b)]");
    }

    @Test
    void testIntermpolation0invalid() throws Narsese.NarseseException {
        Compound a = $.INSTANCE.$("(a &&+3 (b &&+3 c))");
        Compound b = $.INSTANCE.$("(a &&+1 b)");
        try {
            Set<Term> p = permuteIntermpolations(a, b);
            fail("");
        } catch (Error e) {
            assertTrue(true);
        }
    }

    @Test
    void testIntermpolationConjSeq() throws Narsese.NarseseException {
        Compound f = $.INSTANCE.$("(a &&+1 b)");
        Compound g = $.INSTANCE.$("(a &&-1 b)");
        permuteChoose(f, g, "[(a&&b), (b &&+1 a), (a &&+1 b)]");
    }
    @Test
    void testIntermpolationConjSeq2() throws Narsese.NarseseException {
        Compound h = $.INSTANCE.$("(a &&+1 b)");
        Compound i = $.INSTANCE.$("(a && b)");
        permuteChoose(h, i, "[(a&&b), (a &&+1 b)]");

    }
    @Test void intermpolationConjSeq3() throws Narsese.NarseseException {
        assertUnitized(Intermpolate.dtDiff(
            $.INSTANCE.$("((--,((--,(tetris-->left)) &&+170 (--,(tetris-->left))))&&(--,isRow(tetris,(3,TRUE))))"),
            $.INSTANCE.$("((--,((--,(tetris-->left)) &&+160 (--,(tetris-->left))))&&(--,isRow(tetris,(3,TRUE))))")
        ));
    }

    @Test
    void testIntermpolationConjInImpl2b() throws Narsese.NarseseException {

        Compound h = $.INSTANCE.$("(x==>(a &&+1 b))");
        Compound i = $.INSTANCE.$("(x==>(a &| b))");

        permuteChoose(h, i, "[(x==>(a&&b)), (x==>(a &&+1 b))]");
    }

    @Test
    void testIntermpolationInner() throws Narsese.NarseseException {
        Compound a = nars.$.INSTANCE.$("(x --> (a &&+1 b))");
        Term aRoot = a.root();
        assertEq("(x-->(a &&+- b))", aRoot);
        Compound b = nars.$.INSTANCE.$("(x --> (a && b))");
        permuteChoose(a, b, "[(x-->(a&&b)), (x-->(a &&+1 b))]");
    }

    @Test
    void testEmbeddedIntermpolation() {
        NAR nar = NARS.shell();
        nar.time.dur(8);

        Compound a0 = INSTANCE.$$("(b ==>+6 c)");
        Compound b0 = INSTANCE.$$("(b ==>+10 c)");

        Term c0 = Intermpolate.intermpolate(a0, b0, 0.5f, nar);
        assertEquals("(b ==>+8 c)", c0.toString());


        Compound a = INSTANCE.$$("(a, (b ==>+6 c))");
        Compound b = INSTANCE.$$("(a, (b ==>+10 c))");

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


            assertEquals("(a ==>+- a)", INSTANCE.$$("(a ==>+- a)").concept().toString());
            assertEquals("((a ==>+- b)-->[pill])", INSTANCE.$$("((a ==>+- b)-->[pill])").concept().toString());
            String abpill = "((a==>b)-->[pill])";
            assertEquals("((a ==>+- b)-->[pill])", INSTANCE.$$(abpill).concept().toString());

            TaskConcept cc = (TaskConcept) n.conceptualize(abpill);
            assertNotNull(cc);

            cc.beliefs().print();


            long when = t == Present ? 0 : ETERNAL;
            Task m = cc.beliefs().match(when, when, null, 0, n);
            String correctMerge = "((a ==>+" + ab + " b)-->[pill])";
            assertEquals(correctMerge, m.term().toString());


            ((BeliefTables)cc.beliefs()).tableFirst(TemporalBeliefTable.class).setTaskCapacity(1);

            cc.print();


            assertEquals(correctMerge, cc.beliefs().match(0, 0, null, 0, n).term().toString());
        }
    }

}
