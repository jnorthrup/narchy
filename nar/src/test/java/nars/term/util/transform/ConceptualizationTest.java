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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static nars.$.*;
import static nars.$.*;
import static nars.Op.CONJ;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/** TODO move more conceptualization/root tests from TemporalTermTest to here */
class ConceptualizationTest {

    private final NAR n = NARS.shell();


    private static Term ceptualStable(String s) throws Narsese.NarseseException {
        Term c = INSTANCE.$(s);
        Term c1 = c.concept();

        Term c2 = c1.concept();
        assertEquals(c1, c2, new Supplier<String>() {
            @Override
            public String get() {
                return "unstable: irst " + c1 + "\n\t, then " + c2;
            }
        });
        return c1;
    }

    public static void assertConceptual(String cexp, String c) throws Narsese.NarseseException {
        assertEq(cexp, INSTANCE.$(c).concept().toString());
    }


    @Test
    void testFlattenAndDeduplicateAndUnnegateConj_Conceptualization() {
        Term t = INSTANCE.$$("((||+- ,((--,(right-->fz)) &&+- (--,(right-->fz))),(fz-->race)) &&+- (fz-->race))");
        assertEq("( &&+- ,(--,(fz-->race)),(--,(right-->fz)),(fz-->race))", Conceptualization.FlattenAndDeduplicateAndUnnegateConj.apply(t));
    }

    @Test
    void testConceptualization() throws Narsese.NarseseException {

        Term t = INSTANCE.$("(x==>y)");
        Term x = t.root();
        assertEquals(XTERNAL, x.dt());
        assertEquals("(x ==>+- y)", x.toString());

        n.input("(x ==>+0 y).", "(x ==>+1 y).").run(2);

        TaskConcept xImplY = (TaskConcept) n.conceptualize(t);
        assertNotNull(xImplY);

        assertEquals("(x ==>+- y)", xImplY.toString());

        assertEquals(2, xImplY.beliefs().taskCount());

        int indexSize = n.memory.size();
        n.memory.print(System.out);

        n.input("(x ==>+1 y). :|:");
        n.run();


        assertEquals(3, xImplY.beliefs().taskCount());

        n.memory.print(System.out);
        assertEquals(indexSize, n.memory.size());

        //n.conceptualize("(x==>y)").print();
    }

    @Test
    void testEmbeddedChangedRoot() throws Narsese.NarseseException {
        assertEquals("(a ==>+- (b &&+- c))",
                INSTANCE.$("(a ==> (b &&+1 c))").root().toString());
    }


    @Test
    void testEmbeddedChangedRootVariations() throws Narsese.NarseseException {
        {

            Term x = INSTANCE.$("(a ==> (b &&+1 (c && d)))");
            assertEquals("(a==>(b &&+1 (c&&d)))", x.toString());
            assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
        }
        {
            Term x = INSTANCE.$("(a ==> (b &&+1 (c && d)))");
            assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
        }


        {
            Term x = INSTANCE.$("(a ==> (b &&+1 (c &&+1 d)))");
            assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
        }

//        {
//            Term x = $("(a ==> (b &&+1 --(c &&+1 d)))");
//            assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
//        }

    }

    @Test void ConceptualizeSequence() {
        assertEq("((2,(g,y)) &&+- (2,(g,y)))",
                INSTANCE.$$("((2,(g,y)) &&+260 (2,(g,y)))").root());

        assertEq("((--,(2,(g,y))) &&+- (--,(2,(g,y))))",
                INSTANCE.$$("(--(2,(g,y)) &&+260 --(2,(g,y)))").root());

        assertEq("((2,(g,y)) &&+- (2,(g,y)))",
                INSTANCE.$$("(((2,(g,y)) &&+260 (2,(g,y))) &&+710 (2,(g,y)))").root());

        assertEq("((--,(2,(g,y))) &&+- (--,(2,(g,y))))",
                INSTANCE.$$("(((--,(2,(g,y))) &&+260 (--,(2,(g,y)))) &&+710 (--,(2,(g,y))))").root());
    }




    @Test void Conceptualize_Not_NAL3_seq() {
        assertEq("(x-->(a &&+- a))", INSTANCE.$$("(x-->(a &&+1 a))").root());
    }

    @Test
    void testStableNormalizationAndConceptualizationComplex() {
        String s = "(((--,checkScore(#1))&&#1) &&+14540 ((--,((#1)-->$2)) ==>+17140 (isRow(#1,(0,false),true)&&((#1)-->$2))))";
        Term t = INSTANCE.$$(s);
        assertEquals(s, t.toString());
        assertEquals(s, t.normalize().toString());
        assertEquals(s, t.normalize().normalize().toString());
        String c = "( &&+- ,((--,((#1)-->$2)) ==>+- (isRow(#1,(0,false),true) &&+- ((#1)-->$2))),(--,checkScore(#1)),#1)";
        assertEquals(c, t.concept().toString());
        assertEquals(c, t.concept().concept().toString());
        Termed x = new DefaultConceptBuilder(new Consumer<Concept>() {
            @Override
            public void accept(Concept z) {
            }
        }).apply(t);
        assertTrue(x instanceof TaskConcept);
    }

    @Test
    void testAtemporalization() throws Narsese.NarseseException {
        assertEquals("(x ==>+- y)", n.conceptualize(INSTANCE.$("(x ==>+10 y)")).toString());
    }
    @Test
    void testCoNegatedSubtermConceptImpl() throws Narsese.NarseseException {
        assertEquals("(x ==>+- x)", n.conceptualize(INSTANCE.$("(x ==>+10 x)")).toString());
        assertEquals("((--,x) ==>+- x)", n.conceptualize(INSTANCE.$("((--,x) ==>+10 x)")).toString());

        Term xThenNegX = INSTANCE.$("(x ==>+10 (--,x))");
        assertEquals("(x ==>+- x)", n.conceptualize(xThenNegX).toString());

        assertEquals("(x ==>+- x)", n.conceptualize(INSTANCE.$("(x ==>-10 (--,x))")).toString());

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
        assertEq("(( &&+- ,b,c,d) ==>+- b)", INSTANCE.$$("((c &&+5 (b&|d)) ==>-10 b)").concept());
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

        Term d = INSTANCE.$("(x &&+1 (y &&+1 z))");
        assertEq("( &&+- ,x,y,z)", d.root());

    }
    @Test
    void testEmbeddedChangedRootSeqToMerged() throws Narsese.NarseseException {
        Term x = INSTANCE.$("(b &&+1 (c &&+1 d))");
        assertEquals("( &&+- ,b,c,d)", x.root().toString());
    }
    @Test
    void testConceptOfDisjunctionFckup() {
        assertEq("((||+- ,(1-->ang),(--,z),(--,ang)) &&+- (--,(2-->ang)))",
                INSTANCE.$$("((--,(&|,(--,(1-->ang)),ang,z))&&(--,(2-->ang)))").concept());

        //TODO ((grid,#1,13) &&+440 (--,((||,(--,(grid,#1,#1)),rotate)&&left))) .concept()
        //TODO ((&|,(tetris-->curi),(--,left),(--,rotate))&&(--,((--,rotate) &&+819 (--,left))))
    }


    @Test
    void testConceptualizationWithoutConjReduction2a() {
        String s = "((x &&+1 y) &&+1 z)";
        assertEq(
                "( &&+- ,x,y,z)",
                INSTANCE.$$(s).concept().toString());
    }

    @Test
    void testEmbeddedConjNormalizationB() {
        String x = "((((--,noid(0,5)) &&+- noid(11,2)) &&+- noid(11,2)) &&+- noid(11,2))";
        assertEq(x, x);

        assertEq(
                "((--,noid(0,5)) &&+- noid(11,2))",
                INSTANCE.$$(x).concept()
        );
    }


    @Test
    void testConceptualizationWithoutConjReduction() {
        String s = "((--,((happy-->#1) &&+345 (#1,zoom))) &&+1215 (--,((#1,zoom) &&+10 (happy-->#1))))";
        assertEq("((--,((happy-->#1) &&+- (#1,zoom))) &&+- (--,((happy-->#1) &&+- (#1,zoom))))",
                INSTANCE.$$(s).concept().toString());
    }

    @Test
    void testCoNegatedSubtermConceptConj() {
        assertEq("(x &&+- x)", n.conceptualize(INSTANCE.$$("(x &&+10 x)")).toString());

        assertEq("((--,x) &&+- x)", n.conceptualize(INSTANCE.$$("(x &&+10 (--,x))")).toString());
        assertEq("((--,x) &&+- x)", n.conceptualize(INSTANCE.$$("(x &&-10 (--,x))")).toString());


    }

    @Test
    public void testConjConceptualizationWithNeg1() {
        String s = "(--,((--,(right &&+24 #1)) &&+24 #1))";
        Term t = INSTANCE.$$(s);
        assertEquals(s, t.toString());
        assertEq("(--,((--,(right &&+24 #1)) &&+24 #1))", t.normalize().toString());
        assertEq("(--,((--,(right &&+- #1)) &&+- #1))", t.root().toString());
        assertEq("((--,(right &&+- #1)) &&+- #1)", t.concept().toString());
    }


    @Test
    void testStableConceptualization6a() {
        Term s = INSTANCE.$$("((tetris($1,#2) &&+290 tetris(isRow,(8,false),true))=|>(tetris(checkScore,#2)&|tetris($1,#2)))");
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

        assertTrue(a.beliefs().taskCount() >= 4);
    }
    @Test
    void testConjSeqConceptual2() throws Narsese.NarseseException {
        Term t = INSTANCE.$("((--,((--,(--a &&+1 --b)) &&+1 a)) &&+1 a)");
        assertEquals("((--,((--,((--,a) &&+1 (--,b))) &&+1 a)) &&+1 a)", t.toString());

        Term r = t.root();
        {
            assertEquals("((--,((a ||+- b) &&+- a)) &&+- a)", r.toString());
        }

        {
            Term c = t.concept();
            assertTrue(c instanceof Compound);
            assertEquals(r, c);
        }
    }

    @Disabled @Test
    void testConjParallelConceptual() {


        for (int dt : new int[]{ /*XTERNAL,*/ 0, DTERNAL}) {
            assertEquals("( &&+- ,a,b,c)",
                    CONJ.the(dt, INSTANCE.$$("a"), INSTANCE.$$("b"), INSTANCE.$$("c")).concept().toString(), new Supplier<String>() {
                        @Override
                        public String get() {
                            return "dt=" + dt;
                        }
                    });
        }


        assertEquals(
                "( &&+- ,(bx-->noid),(happy-->noid),#1)",
                INSTANCE.$$("(--,(((bx-->noid) &| (happy-->noid)) &| #1))")
                        .concept().toString());
        assertEquals(
                "(x,(--,( &&+- ,a,b,c)))",
                INSTANCE.$$("(x,(--,(( a &| b) &| c)))")
                        .concept().toString());
    }

    @Test
    void testCommutiveTemporalityConcepts2() throws Narsese.NarseseException {


        for (String op : new String[]{"&&"}) {
            Concept a = n.conceptualize(INSTANCE.$("(x " + op + "   y)"));
            Concept b = n.conceptualize(INSTANCE.$("(x " + op + "+1 y)"));

            assertSame(a, b);

            Concept c = n.conceptualize(INSTANCE.$("(x " + op + "+2 y)"));

            assertSame(b, c);

            Concept d = n.conceptualize(INSTANCE.$("(x " + op + "-1 y)"));

            assertSame(c, d);

            Term e0 = INSTANCE.$("(x " + op + "+- y)");
            assertEquals("(x " + op + "+- y)", e0.toString());
            Concept e = n.conceptualize(e0);

            assertSame(d, e);

            Term f0 = INSTANCE.$("(y " + op + "+- x)");
            assertEquals("(x " + op + "+- y)", f0.toString());
            assertEquals("(x " + op + "+- y)", f0.root().toString());

            Concept f = n.conceptualize(f0);
            assertSame(e, f, new Supplier<String>() {
                @Override
                public String get() {
                    return e + "==" + f;
                }
            });


            Concept g = n.conceptualize(INSTANCE.$("(x " + op + "+- x)"));
            assertEquals("(x " + op + "+- x)", g.toString());


            Concept h = n.conceptualize(INSTANCE.$("(x " + op + "+- (--,x))"));
            assertEquals("((--,x) " + op + "+- x)", h.toString());


        }

    }

    @Test void testInhConj() {
        Term a = INSTANCE.$$("(x-->(y&&z))");
        Term ac = a.concept();
        assertEq("(x-->(y&&z))", ac);
        assertEq("(x-->(y &&+- z))", INSTANCE.$$("(x-->(y &&+1 z))").concept());
        assertEq("((tetris-->((--,score)&&rotate))-->(plan,z,/))", INSTANCE.$$("((tetris-->((--,score)&&rotate))-->(plan,z,/))").concept());
    }
    /** TODO make consistent with new conceptualization */
    @Disabled @Test void ConceptualizeNAL3() {
        //direct inh subterm
        assertEq("(x-->(a &&+- b))", INSTANCE.$$("(x-->(a&&b))").root());
        assertEq("(x-->(a &&+- b))", INSTANCE.$$("(x-->(a&&b))").concept());

        assertEq(//TODO "(x-->(a||b))",
            "(--,(x-->((--,a) &&+- (--,b))))",
            INSTANCE.$$("(x-->(a||b))").root());
        assertEq(//TODO "(x-->(a||b))",
            "(x-->((--,a) &&+- (--,b)))",
            INSTANCE.$$("(x-->(a||b))").concept());


        assertEq("(x-->( &&+- ,a,b,c))", INSTANCE.$$("(x-->(&&,a,b,c))").root());

        //direct sim subterm
        assertEq("((a &&+- b)<->x)", INSTANCE.$$("(x<->(a&&b))").root());
        assertEq("((a &&+- b)<->x)", INSTANCE.$$("(x<->(a&&b))").concept());
        assertEq("((a &&+- b)<->(c &&+- d))", INSTANCE.$$("((c&&d)<->(a&&b))").root());

        //indirect inh subterm (thru product)
        assertEq("x((a &&+- b))", INSTANCE.$$("x((a&&b))").root());
        assertEq("x((a ||+- b))", INSTANCE.$$("x((a||b))").root());
        assertEq(//"x(( &&+- ,(--,b),(--,c),a))",
            "x(((b ||+- c) &&+- a))",
            INSTANCE.$$("x((a&&(b||c)))").root());

        assertEq("(a-->(x &&+- y))", INSTANCE.$$("(a-->(x && y))").concept());
        assertEq("((x &&+- y)-->a)", INSTANCE.$$("((x && y)-->a)").concept());
        assertEq("((x &&+- y)<->a)", INSTANCE.$$("((x && y)<->a)").concept());
        assertEq("(a-->( &&+- ,x,y,z))", INSTANCE.$$("(a-->(&&,x,y,z))").concept());

        assertEq("(a-->((--,x) &&+- (--,y)))", INSTANCE.$$("(a-->(x || y))").concept());
    }

}