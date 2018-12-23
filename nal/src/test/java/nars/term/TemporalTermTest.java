package nars.term;

import nars.*;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.concept.util.DefaultConceptBuilder;
import nars.term.util.transform.Retemporalize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.TreeSet;

import static nars.$.$;
import static nars.$.$$;
import static nars.term.atom.Bool.Null;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.*;


public class TemporalTermTest {


    private final NAR n = NARS.shell();


    public static Term ceptualStable(String s) throws Narsese.NarseseException {
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
    public void testSortingTemporalImpl() {
        assertEquals(-1, $$("(x ==>+1 y)").compareTo($$("(x ==>+10 y)")));
        assertEquals(+1, $$("(x ==>+1 y)").compareTo($$("(x ==>-1 y)")));
        assertEquals(-1, $$("(x ==>-1 y)").compareTo($$("(x ==>+1 y)")));
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
    void testInvalidInheritanceOfEternalTemporalNegated() throws Narsese.NarseseException {
        assertEquals(
                //"((--,(a &&+1 b))-->(a&&b))",
                Null,
                $("(--(a &&+1 b)-->(a && b))")
        );
        assertEquals(
                //"((a &&+1 b)-->(--,(a&&b)))",
                Null,
                $("((a &&+1 b) --> --(a && b))")//.toString()
        );

    }


    @Test
    void testAtemporalization3a() throws Narsese.NarseseException {

        assertEquals(
                "(--,((x &&+- $1) ==>+- ((--,y) &&+- $1)))",
                $.<Compound>$("(--,(($1&&x) ==>+1 ((--,y) &&+2 $1)))").temporalize(Retemporalize.retemporalizeAllToXTERNAL).toString());
        assertEquals(
                "(--,((x &&+- $1) ==>+- ((--,y) &&+- $1)))",
                $.<Compound>$("(--,(($1&&x) ==>+1 ((--,y) &&+2 $1)))").root().toString());
    }

    @Test
    void testAtemporalization3b() throws Narsese.NarseseException {

        Compound x = $("((--,(($1&&x) ==>+1 ((--,y) &&+2 $1))) &&+3 (--,y))");
        Term y = x.temporalize(Retemporalize.retemporalizeAllToXTERNAL);
        assertEquals("((--,((x &&+- $1) ==>+- ((--,y) &&+- $1))) &&+- (--,y))", y.toString());

    }

    @Test
    void testAtemporalization4() throws Narsese.NarseseException {


        assertEquals("((x &&+- $1) ==>+- (y &&+- $1))",
                $("((x&&$1) ==>+- (y&&$1))").root().toString());
    }

    @Disabled
    @Test /* TODO decide the convention */ void testAtemporalization5() throws Narsese.NarseseException {
        for (String s : new String[]{"(y &&+- (x ==>+- z))", "((x ==>+- y) &&+- z)"}) {
            Term c = $(s);
            assertTrue(c instanceof Compound);
            assertEquals("((x &&+- y) ==>+- z)",
                    c.toString());
            assertEquals("((x &&+- y) ==>+- z)",
                    c.root().toString());


        }
    }

    @Test
    void testAtemporalization6() throws Narsese.NarseseException {
        Compound x0 = $("(($1&&x) ==>+1 ((--,y) &&+2 $1)))");
        assertEquals("((x&|$1) ==>+1 ((--,y) &&+2 $1))", x0.toString());

    }

    @Test
    void testAtemporalizationSharesNonTemporalSubterms() throws Narsese.NarseseException {

        Task a = n.inputTask("(x ==>+10 y).");
        Task c = n.inputTask("(x ==>+9 y).");
        Task b = n.inputTask("(x <-> y).");
        n.run();

        @NotNull Term aa = a.term();
        assertNotNull(aa);

        @Nullable Concept na = n.concept(a.term(), true);
        assertNotNull(na);

        @Nullable Concept nc = n.concept(c.term(), true);
        assertNotNull(nc);

        assertSame(na, nc);

        assertSame(na.sub(0), nc.sub(0));


        assertEquals(n.concept(b.term(), true).sub(0), n.concept(c.term(), true).sub(0));

    }


    @Test
    void testAnonymization2() throws Narsese.NarseseException {
        Termed nn = $("((do(that) &&+1 (a)) ==>+2 (b))");
        assertEquals("((do(that) &&+1 (a)) ==>+2 (b))", nn.toString());


        assertEquals("((do(that) &&+- (a)) ==>+- (b))", n.conceptualize(nn).toString());


    }


    @Test
    void testCommutiveTemporalityDepVar0() throws Narsese.NarseseException {
        Term t0 = $("((SELF,#1)-->at)").term();
        Term t1 = $("goto(#1)").term();
        Term[] a = Terms.sorted(t0, t1);
        Term[] b = Terms.sorted(t1, t0);
        assertEquals(
                Op.terms.subterms(a),
                Op.terms.subterms(b)
        );
    }


    public static void testParse(String s) {
        testParse(s, null);
    }

    public static void testParse(String input, String expected) {
        Termed t = null;
        try {
            t = $(input);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
        if (expected == null)
            expected = input;
        assertEquals(expected, t.toString());
    }


    @Test
    void parseTemporalRelation() throws Narsese.NarseseException {

        assertEquals("(x ==>+5 y)", $("(x ==>+5 y)").toString());
        assertEquals("(x &&+5 y)", $("(x &&+5 y)").toString());

        assertEquals("(x ==>-5 y)", $("(x ==>-5 y)").toString());

        assertEquals("((before-->x) ==>+5 (after-->x))", $("(x:before ==>+5 x:after)").toString());
    }

    @Test
    void temporalEqualityAndCompare() throws Narsese.NarseseException {
        assertNotEquals($("(x ==>+5 y)"), $("(x ==>+0 y)"));
        assertNotEquals($("(x ==>+5 y)").hashCode(), $("(x ==>+0 y)").hashCode());
        assertNotEquals($("(x ==> y)"), $("(x ==>+0 y)"));
        assertNotEquals($("(x ==> y)").hashCode(), $("(x ==>+0 y)").hashCode());

        assertEquals($("(x ==>+0 y)"), $("(x ==>-0 y)"));
        assertNotEquals($("(x ==>+5 y)"), $("(y ==>-5 x)"));


        assertEquals(0, $("(x ==>+0 y)").compareTo($("(x ==>+0 y)")));
        assertEquals(-1, $("(x ==>+0 y)").compareTo($("(x ==>+1 y)")));
        assertEquals(+1, $("(x ==>+1 y)").compareTo($("(x ==>+0 y)")));
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
    void testTransformedImplDoesntActuallyOverlap() throws Narsese.NarseseException {
        assertEquals("(((#1 &&+7 (_1,_2)) &&+143 (_1,_2)) ==>+7 (_1,_2))",
                $("(((#1 &&+7 (_1,_2)) &&+143 (_1,_2)) ==>+- (_1,_2))").dt(7).toString());
    }

    @Test
    void subtermTimeWithConjInImpl() throws Narsese.NarseseException {
        Term t = $("(((a &&+5 b) &&+5 c) ==>-5 d)");
        assertEquals(DTERNAL, t.subTimeOnly($("(a &&+5 b)")));
        assertEquals(DTERNAL, t.subTimeOnly($("d")));
        assertEquals(DTERNAL, t.subTimeOnly($("a")));
        assertEquals(DTERNAL, t.subTimeOnly($("b")));
        assertEquals(DTERNAL, t.subTimeOnly($("c")));
        assertEquals(DTERNAL, t.subTimeOnly($("x")));
    }


    @Test
    void testNonCommutivityImplConcept() throws Narsese.NarseseException {


        n.input("(x ==>+5 y).", "(y ==>-5 x).");
        n.run(5);

        TreeSet d = new TreeSet(Comparator.comparing(Object::toString));
        n.conceptsActive().forEach(x -> d.add(x.get()));


        assertTrue(d.contains($("(x ==>+- y)")));
        assertTrue(d.contains($("(y ==>+- x)")));
    }

    @Test
    void testImplRootDistinct() throws Narsese.NarseseException {

        Term f = $("(x ==> y)");
        assertEquals("(x ==>+- y)", f.root().toString());

        Term g = $("(y ==>+1 x)");
        assertEquals("(y ==>+- x)", g.root().toString());

    }

    @Test
    void testImplRootRepeat() throws Narsese.NarseseException {
        Term h = $("(x ==>+1 x)");
        assertEquals("(x ==>+- x)", h.root().toString());
    }

    @Test
    void testImplRootNegate() throws Narsese.NarseseException {
        Term i = $("(--x ==>+1 x)");
        assertEquals("((--,x) ==>+- x)", i.root().toString());

    }

    @Test void testInvalidIntEventTerms() {

        assertEq(Null, "(1 && x)");
        assertEq(Null, "(/ && x)");
        assertEq(Null, "(1 &&+1 x)");
        assertEq(Null, "(1 ==> x)");
        assertEq(Null, "(x ==> 1)");

//        assertEq(Null, "(--,1)");
//        assertEq(Null, "((--,1) && x)");
    }


    @Disabled
    @Test
    void testEqualsAnonymous3() throws Narsese.NarseseException {


        assertEquals($.<Compound>$("(x && (y ==> z))").temporalize(Retemporalize.retemporalizeAllToXTERNAL),
                $.<Compound>$("(x &&+1 (y ==>+1 z))").temporalize(Retemporalize.retemporalizeAllToXTERNAL));


        assertEquals("((x &&+1 z) ==>+1 w)",
                $("(x &&+1 (z ==>+1 w))").toString());

        assertEquals($.<Compound>$("((x &&+- z) ==>+- w)").temporalize(Retemporalize.retemporalizeAllToXTERNAL),
                $.<Compound>$("(x &&+1 (z ==>+1 w))").temporalize(Retemporalize.retemporalizeAllToXTERNAL));
    }


    @Test
    void testValidTaskTerm() {
        String s = "believe(x,(believe(x,(--,(cam(9,$1) ==>-78990 (ang,$1))))&|(cam(9,$1) ==>+570 (ang,$1))))";
        Term ss = $$(s);
        assertTrue(Task.validTaskCompound((Compound) ss, true));
        assertTrue(Task.taskConceptTerm(ss));
    }

    @Test
    void testSubtimeInDTERNAL() {
        assertArrayEquals(new int[]{0},
                $$("(x && y)").subTimes($$("x"))
        );
        assertNull(
                $$("(x && y)").subTimes($$("a"))
        );
//        assertArrayEquals(new int[] { 0 },
//                $$("((x &| y) && z)").subTimes($$("x"))
//        );
        assertArrayEquals(new int[]{0},
                $$("((x &| y) && (w &| z))").subTimes($$("x"))
        );
        assertArrayEquals(new int[]{0},
                $$("(((--,(_2(_1)&|_3(_1)))&|(--,(_3(_1)&|_4(_1)))) &&+125 _6(_1,_5))").subTimes($$("(_3(_1)&|_4(_1))").neg())
        );
        assertArrayEquals(new int[]{125},
                $$("(z &&+125 ((--,(_2(_1)&|_3(_1)))&|(--,(_3(_1)&|_4(_1)))))").subTimes($$("(_3(_1)&|_4(_1))").neg())
        );

//        assertEquals(0, $$("((--,(_3(_1)&|_4(_1)))&&(_2(_1)&|_3(_1)))").subTimeFirst($$("")));
    }

}
