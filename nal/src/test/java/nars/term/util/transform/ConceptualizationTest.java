package nars.term.util.transform;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.TaskConcept;
import nars.concept.util.DefaultConceptBuilder;
import nars.term.Term;
import nars.term.Termed;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/** TODO move more conceptualization/root tests from TemporalTermTest to here */
class ConceptualizationTest {

    private static final NAR n = NARS.shell();


    private static Term ceptualStable(String s) throws Narsese.NarseseException {
        Term c = $(s);
        Term c1 = c.concept();

        Term c2 = c1.concept();
        assertEquals(c1, c2, () -> "unstable: irst " + c1 + "\n\t, then " + c2);
        return c1;
    }

    public static void assertConceptual(String cexp, String c) throws Narsese.NarseseException {
        assertEq(cexp, $(c).concept().toString());
    }


    @Test
    void testFlattenAndDeduplicateAndUnnegateConj_Conceptualization() {
        Term t = $$("((||+- ,((--,(right-->fz)) &&+- (--,(right-->fz))),(fz-->race)) &&+- (fz-->race))");
        assertEq("( &&+- ,(--,(fz-->race)),(--,(right-->fz)),(fz-->race))", t.temporalize(Conceptualization.FlattenAndDeduplicateAndUnnegateConj));
    }

    @Test
    void testConceptualization() throws Narsese.NarseseException {

        Term t = $("(x==>y)");
        Term x = t.root();
        assertEquals(XTERNAL, x.dt());
        assertEquals("(x ==>+- y)", x.toString());

        n.input("(x ==>+0 y).", "(x ==>+1 y).").run(2);

        TaskConcept xImplY = (TaskConcept) n.conceptualize(t);
        assertNotNull(xImplY);

        assertEquals("(x ==>+- y)", xImplY.toString());

        assertEquals(3, xImplY.beliefs().size());

        int indexSize = n.concepts.size();
        n.concepts.print(System.out);

        n.input("(x ==>+1 y). :|:");
        n.run();


        assertEquals(4, xImplY.beliefs().size());

        n.concepts.print(System.out);
        assertEquals(indexSize, n.concepts.size());

        //n.conceptualize("(x==>y)").print();
    }

    @Test
    void testEmbeddedChangedRoot() throws Narsese.NarseseException {
        assertEquals("(a ==>+- (b &&+- c))",
                $("(a ==> (b &&+1 c))").root().toString());
    }


    @Test
    void testEmbeddedChangedRootVariations() throws Narsese.NarseseException {
        {

            Term x = $("(a ==> (b &&+1 (c && d)))");
            assertEquals("(a==>(b &&+1 (c&|d)))", x.toString());
            assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
        }
        {
            Term x = $("(a ==> (b &&+1 (c &| d)))");
            assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
        }

        Term x = $("(a ==> (b &&+1 (c &&+1 d)))");
        assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
    }

    @Test
    void testStableNormalizationAndConceptualizationComplex() {
        String s = "(((--,checkScore(#1))&|#1) &&+14540 ((--,((#1)-->$2)) ==>+17140 (isRow(#1,(0,false),true)&|((#1)-->$2))))";
        Term t = $$(s);
        assertEquals(s, t.toString());
        assertEquals(s, t.normalize().toString());
        assertEquals(s, t.normalize().normalize().toString());
        String c = "( &&+- ,((--,((#1)-->$2)) ==>+- (isRow(#1,(0,false),true) &&+- ((#1)-->$2))),(--,checkScore(#1)),#1)";
        assertEquals(c, t.concept().toString());
        assertEquals(c, t.concept().concept().toString());
        Termed x = new DefaultConceptBuilder((z) -> {
        }).apply(t);
        assertTrue(x instanceof TaskConcept);
    }

    @Test
    void testAtemporalization() throws Narsese.NarseseException {
        assertEquals("(x ==>+- y)", n.conceptualize($("(x ==>+10 y)")).toString());
    }
    @Test
    void testCoNegatedSubtermConceptImpl() throws Narsese.NarseseException {
        assertEquals("(x ==>+- x)", n.conceptualize($("(x ==>+10 x)")).toString());
        assertEquals("((--,x) ==>+- x)", n.conceptualize($("((--,x) ==>+10 x)")).toString());

        Term xThenNegX = $("(x ==>+10 (--,x))");
        assertEquals("(x ==>+- x)", n.conceptualize(xThenNegX).toString());

        assertEquals("(x ==>+- x)", n.conceptualize($("(x ==>-10 (--,x))")).toString());

    }

    @Test
    void testStableConceptualization2() throws Narsese.NarseseException {
        Term c1 = ceptualStable("((a&&b)&|do(that))");
        assertEq(
                "( &&+- ,do(that),a,b)",

                c1.toString());
    }
    @Test
    void testStableConceptualization1() throws Narsese.NarseseException {
        Term c1 = ceptualStable("((((#1,(2,true),true)-->#2)&|((gameOver,(),true)-->#2)) &&+29 tetris(#1,(11,true),true))");
        assertEq("( &&+- ,((#1,(2,true),true)-->#2),tetris(#1,(11,true),true),((gameOver,(),true)-->#2))",
                c1.toString());
    }
    @Test
    void conceptualizability() {
        assertEq("(( &&+- ,b,c,d) ==>+- b)", $$("((c &&+5 (b&|d)) ==>-10 b)").concept());
    }

    @Test
    void testStableConceptualization0() throws Narsese.NarseseException {
        Term c1 = ceptualStable("((a &&+5 b) &&+5 c)");
        assertEq("( &&+- ,a,b,c)", c1.toString());
    }

    @Test
    void testStableConceptualization4() throws Narsese.NarseseException {
        Term c1 = ceptualStable("((--,((#1-->happy)&|(#1-->neutral)))&|(--,(#1-->sad)))");
        assertEq("((--,((#1-->happy) &&+- (#1-->neutral))) &&+- (--,(#1-->sad)))", c1.toString());
    }

    @Test
    void testStableConceptualization6() throws Narsese.NarseseException {
        assertEq("( &&+- ,(--,(\"-\"-->move)),(--,(joy-->cart)),(\"+\"-->move),(happy-->cart))",
                ceptualStable("((((--,(\"-\"-->move))&|(happy-->cart)) &&+334 (\"+\"-->move)) &&+5 (--,(joy-->cart)))").toString());
    }


    @Test
    void testConjSeqConceptual1() throws Narsese.NarseseException {
        assertConceptual("((--,(nario,zoom)) &&+- happy)", "((--,(nario,zoom)) && happy)");
        assertConceptual("((--,(nario,zoom)) &&+- happy)", "--((--,(nario,zoom)) && happy)");
        assertConceptual("((--,(nario,zoom)) &&+- happy)", "((--,(nario,zoom)) &&+- happy)");
    }

    @Test
    void testConjSeqConceptual2() throws Narsese.NarseseException {
        assertConceptual("( &&+- ,(--,(x,(--,x))),(--,(nario,zoom)),happy)", "(((--,(nario,zoom)) &&+- happy) &&+- (--,(x,(--,x))))");

        String c =

                "( &&+- ,(--,(nario,zoom)),vx,vy)";
        assertConceptual(
                c, "((vx &&+97 vy) &&+156 (--,(nario,zoom)))");
        assertConceptual(
                c, "((vx &&+97 vy) &&+100 (--,(nario,zoom)))");
        assertConceptual(
                c,
                "((vx &&+97 vy) &&-100 (--,(nario,zoom)))");
    }

}