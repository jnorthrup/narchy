package nars.term;

import nars.*;
import nars.concept.Concept;
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
import static nars.term.util.conj.ConjTest.$$c;
import static org.junit.jupiter.api.Assertions.*;


public class TemporalTermTest {


    private static final NAR n = NARS.shell();

    @Test
    public void testSortingTemporalImpl() {
        assertEquals(-1, $$("(x ==>+1 y)").compareTo($$("(x ==>+10 y)")));
        assertEquals(+1, $$("(x ==>+1 y)").compareTo($$("(x ==>-1 y)")));
        assertEquals(-1, $$("(x ==>-1 y)").compareTo($$("(x ==>+1 y)")));
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
        assertEquals("((x&&$1) ==>+1 ((--,y) &&+2 $1))", x0.toString());

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

        assertSame(na.term().sub(0), nc.term().sub(0));


        assertEquals(n.concept(b.term(), true).term().sub(0), n.concept(c.term(), true).term().sub(0));

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
    void testTransformedImplDoesntActuallyOverlap() throws Narsese.NarseseException {
        assertEquals("(((#1 &&+7 (_1,_2)) &&+143 (_1,_2)) ==>+7 (_1,_2))",
                $$c("(((#1 &&+7 (_1,_2)) &&+143 (_1,_2)) ==>+- (_1,_2))").dt(7).toString());
    }



    @Test
    void testNonCommutivityImplConcept() throws Narsese.NarseseException {


        n.input("(x ==>+5 y).", "(y ==>-5 x).");
        n.run(5);

        TreeSet d = new TreeSet(Comparator.comparing(Object::toString));
        n.attn._concepts().forEach(x -> d.add(x.get()));


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
        assertTrue(Task.validTaskTerm(ss));
    }

    @Test void testImplTransformMaintainsTiming() {
        assertEq(
                "((_2-->_1) ==>+3 (_1-->_3))",
                $$("(($1-->_1) ==>-1 ((_2-->$1) &&+4 (_1-->_3)))").replace($.varIndep(1), $$("_2"))
        );
        assertEq(
                "((_1-->_3) ==>+3 (_2-->_1))",
                $$("(((_1-->_3) &&+4 (_2-->$1)) ==>-1 ($1-->_1))").replace($.varIndep(1), $$("_2"))
        );
    }
    @Test void testConjTransformMaintainsTiming() {
        assertEq(
                "((x-->a) &&+3 (z-->a))",
                $$("((x-->a) &&+1 ((y-->b) &&+2 (z -->a)))").replace($$("b"), $$("y"))
        );
    }
}
