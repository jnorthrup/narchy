package nars.term.util.transform;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.concept.util.DefaultConceptBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.CONJ;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.DTERNAL;
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
    void testConjSeqConceptual_2() throws Narsese.NarseseException {
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
    @Test
    void testConjRootMultiConj() throws Narsese.NarseseException {

        Term d = $("(x &&+1 (y &&+1 z))");
        assertEq("( &&+- ,x,y,z)", d.root());

    }
    @Test
    void testEmbeddedChangedRootSeqToMerged() throws Narsese.NarseseException {
        Term x = $("(b &&+1 (c &&+1 d))");
        assertEquals("( &&+- ,b,c,d)", x.root().toString());
    }
    @Test
    void testConceptOfDisjunctionFckup() {
        assertEq("TODO", $$("((--,(&|,(--,(1-->ang)),ang,z))&&(--,(2-->ang)))").concept());
        //TODO ((grid,#1,13) &&+440 (--,((||,(--,(grid,#1,#1)),rotate)&&left))) .concept()
        //TODO ((&|,(tetris-->curi),(--,left),(--,rotate))&&(--,((--,rotate) &&+819 (--,left))))
    }

    @Test
    void testConceptualizationWithoutConjReduction2() {
        String s = "(((--,((--,(joy-->tetris))&|#1)) &&+30 #1) &&+60 (joy-->tetris))";
        assertEq(
                //"(((--,((--,(joy-->tetris))&&#1)) &&+- #1)&&(joy-->tetris))",
                //"(((||+- ,(joy-->tetris),(--,#1)) &&+- #1) &&+- (joy-->tetris))",
                "( &&+- ,(||+- ,(joy-->tetris),(--,#1)),(joy-->tetris),#1)",

                $$(s).concept().toString());
    }
    @Test
    void testConceptualizationWithoutConjReduction2a() {
        String s = "((x &&+1 y) &&+1 z)";
        assertEq(
                "( &&+- ,x,y,z)",
                $$(s).concept().toString());
    }

    @Test
    void testEmbeddedConjNormalizationB() {
        String x = "((((--,noid(0,5)) &&+- noid(11,2)) &&+- noid(11,2)) &&+- noid(11,2))";
        assertEq(x, x);

        assertEq(
                "((--,noid(0,5)) &&+- noid(11,2))",
                $$(x).concept()
        );
    }


    @Test
    void testConceptualizationWithoutConjReduction() throws Narsese.NarseseException {
        String s = "((--,((happy-->#1) &&+345 (#1,zoom))) &&+1215 (--,((#1,zoom) &&+10 (happy-->#1))))";
        assertEq("((--,((happy-->#1) &&+- (#1,zoom))) &&+- (--,((happy-->#1) &&+- (#1,zoom))))",
                $$(s).concept().toString());
    }

    @Test
    void testCoNegatedSubtermConceptConj() throws Narsese.NarseseException {
        assertEq("(x &&+- x)", n.conceptualize($$("(x &&+10 x)")).toString());

        assertEq("((--,x) &&+- x)", n.conceptualize($$("(x &&+10 (--,x))")).toString());
        assertEq("((--,x) &&+- x)", n.conceptualize($$("(x &&-10 (--,x))")).toString());


    }

    @Test
    public void testConjConceptualizationWithNeg1() {
        String s = "(--,((--,(right &&+24 #1)) &&+24 #1))";
        Term t = $$(s);
        assertEquals(s, t.toString());
        assertEq("(--,((--,(right &&+24 #1)) &&+24 #1))", t.normalize().toString());
        assertEq("(--,((--,(right &&+- #1)) &&+- #1))", t.root().toString());
        assertEq("((--,(right &&+- #1)) &&+- #1)", t.concept().toString());
    }


    @Test
    void testStableConceptualization6a() throws Narsese.NarseseException {
        Term s = $$("((tetris($1,#2) &&+290 tetris(isRow,(8,false),true))=|>(tetris(checkScore,#2)&|tetris($1,#2)))");
        assertEq("((tetris(isRow,(8,false),true) &&+- tetris($1,#2)) ==>+- (tetris(checkScore,#2) &&+- tetris($1,#2)))", s.concept().toString());
    }

    @Test
    void testCommutiveTemporalityConcepts() throws Narsese.NarseseException {


        n.input("(goto(#1) &&+5 ((SELF,#1)-->at)).");


        n.input("(goto(#1) &&-5 ((SELF,#1)-->at)).");


        n.input("(goto(#1) &| ((SELF,#1)-->at)).");

        n.input("(((SELF,#1)-->at) &&-3 goto(#1)).");


        n.run(2);

        TaskConcept a = (TaskConcept) n.conceptualize("(((SELF,#1)-->at) && goto(#1)).");
        Concept a0 = n.conceptualize("(goto(#1) && ((SELF,#1)-->at)).");
        assertNotNull(a);
        assertSame(a, a0);


        a.beliefs().print();

        assertTrue(a.beliefs().size() >= 4);
    }
    @Test
    void testConjSeqConceptual2() throws Narsese.NarseseException {
        Term t = $("((--,((--,(--a &&+1 --b)) &&+1 a)) &&+1 a)");
        assertEquals("((--,((--,((--,a) &&+1 (--,b))) &&+1 a)) &&+1 a)", t.toString());

        Term r = t.root();
        {
            assertEquals("((--,((||+- ,a,b) &&+- a)) &&+- a)", r.toString());
        }

        {
            Term c = t.concept();
            assertTrue(c instanceof Compound);
            assertEquals(r, c);
        }
    }

    @Test
    void testConjParallelConceptual() {


        for (int dt : new int[]{ /*XTERNAL,*/ 0, DTERNAL}) {
            assertEquals("( &&+- ,a,b,c)",
                    CONJ.the(dt, new Term[]{$$("a"), $$("b"), $$("c")}).concept().toString(), () -> "dt=" + dt);
        }


        assertEquals(
                "( &&+- ,(bx-->noid),(happy-->noid),#1)",
                $$("(--,(((bx-->noid) &| (happy-->noid)) &| #1))")
                        .concept().toString());
        assertEquals(
                "(x,(--,( &&+- ,a,b,c)))",
                $$("(x,(--,(( a &| b) &| c)))")
                        .concept().toString());
    }

    @Test
    void testCommutiveTemporalityConcepts2() throws Narsese.NarseseException {


        for (String op : new String[]{"&&"}) {
            Concept a = n.conceptualize($("(x " + op + "   y)"));
            Concept b = n.conceptualize($("(x " + op + "+1 y)"));

            assertSame(a, b);

            Concept c = n.conceptualize($("(x " + op + "+2 y)"));

            assertSame(b, c);

            Concept d = n.conceptualize($("(x " + op + "-1 y)"));

            assertSame(c, d);

            Term e0 = $("(x " + op + "+- y)");
            assertEquals("(x " + op + "+- y)", e0.toString());
            Concept e = n.conceptualize(e0);

            assertSame(d, e);

            Term f0 = $("(y " + op + "+- x)");
            assertEquals("(x " + op + "+- y)", f0.toString());
            assertEquals("(x " + op + "+- y)", f0.root().toString());

            Concept f = n.conceptualize(f0);
            assertSame(e, f, e + "==" + f);


            Concept g = n.conceptualize($("(x " + op + "+- x)"));
            assertEquals("(x " + op + "+- x)", g.toString());


            Concept h = n.conceptualize($("(x " + op + "+- (--,x))"));
            assertEquals("((--,x) " + op + "+- x)", h.toString());


        }

    }

}