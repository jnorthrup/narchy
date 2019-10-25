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
import java.util.function.Consumer;

import static nars.$.*;
import static nars.$.*;
import static nars.term.atom.IdempotentBool.Null;
import static nars.term.util.TermTest.assertEq;
import static nars.term.util.TermTest.assertInvalidTerms;
import static org.junit.jupiter.api.Assertions.*;


class TemporalTermTest {


    private final NAR n = NARS.shell();

    @Test
    void testSortingTemporalImpl() {
        assertEquals(-1, INSTANCE.$$("(x ==>+1 y)").compareTo(INSTANCE.$$("(x ==>+10 y)")));
        assertEquals(+1, INSTANCE.$$("(x ==>+1 y)").compareTo(INSTANCE.$$("(x ==>-1 y)")));
        assertEquals(-1, INSTANCE.$$("(x ==>-1 y)").compareTo(INSTANCE.$$("(x ==>+1 y)")));
    }




    @Test void validInh() {
        assertEq("(x-->(x))", "(x-->(x))"); //valid
        assertEq("x(x)", "((x)-->x)"); //valid
        assertEq("(x-->(x,x))", "(x-->(x,x))"); //valid
    }
    @Test void InvalidInh() {
        assertInvalidTerms("(x-->{x,y})");
        assertInvalidTerms("(x<->{x,y})");
        assertInvalidTerms("(x-->(x<->y))");
        assertInvalidTerms("(x<->(x<->y))");
    }

    @Disabled @Test void InvalidInh_ConjComponent() {
        assertInvalidTerms("((x-->r)-->(r&&c))");
        assertInvalidTerms("((x-->r)-->((--,r)&&c))");
    }

    @Test
    void testInvalidInheritanceOfEternalTemporalNegated() throws Narsese.NarseseException {
        assertEquals(
                //"((--,(a &&+1 b))-->(a&&b))",
                Null,
                INSTANCE.$("(--(a &&+1 b)-->(a && b))")
        );
        assertEquals(
                //"((a &&+1 b)-->(--,(a&&b)))",
                Null,
                INSTANCE.$("((a &&+1 b) --> --(a && b))")//.toString()
        );

    }


    @Test
    void testAtemporalization3a() throws Narsese.NarseseException {

		assertEquals(
                "(--,((x &&+- $1) ==>+- ((--,y) &&+- $1)))",
                Retemporalize.retemporalizeAllToXTERNAL.apply($.INSTANCE.<Compound>$("(--,(($1&&x) ==>+1 ((--,y) &&+2 $1)))")).toString());
        assertEquals(
                "(--,((x &&+- $1) ==>+- ((--,y) &&+- $1)))",
                $.INSTANCE.<Compound>$("(--,(($1&&x) ==>+1 ((--,y) &&+2 $1)))").root().toString());
    }

    @Test
    void testAtemporalization3b() throws Narsese.NarseseException {

        Compound x = INSTANCE.$("((--,(($1&&x) ==>+1 ((--,y) &&+2 $1))) &&+3 (--,y))");
		Term y = Retemporalize.retemporalizeAllToXTERNAL.apply(x);
        assertEquals("((--,((x &&+- $1) ==>+- ((--,y) &&+- $1))) &&+- (--,y))", y.toString());

    }

    @Test
    void testAtemporalization4() throws Narsese.NarseseException {


        assertEquals("((x &&+- $1) ==>+- (y &&+- $1))",
                INSTANCE.$("((x&&$1) ==>+- (y&&$1))").root().toString());
    }

    @Disabled
    @Test /* TODO decide the convention */ void testAtemporalization5() throws Narsese.NarseseException {
        for (String s : new String[]{"(y &&+- (x ==>+- z))", "((x ==>+- y) &&+- z)"}) {
            Term c = INSTANCE.$(s);
            assertTrue(c instanceof Compound);
            assertEquals("((x &&+- y) ==>+- z)",
                    c.toString());
            assertEquals("((x &&+- y) ==>+- z)",
                    c.root().toString());


        }
    }

    @Test
    void testAtemporalization6() throws Narsese.NarseseException {
        Compound x0 = INSTANCE.$("(($1&&x) ==>+1 ((--,y) &&+2 $1)))");
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
        Termed nn = INSTANCE.$("((do(that) &&+1 (a)) ==>+2 (b))");
        assertEquals("((do(that) &&+1 (a)) ==>+2 (b))", nn.toString());


        assertEquals("((do(that) &&+- (a)) ==>+- (b))", n.conceptualize(nn).toString());


    }


    @Test
    void testCommutiveTemporalityDepVar0() throws Narsese.NarseseException {
        Term t0 = INSTANCE.$("((SELF,#1)-->at)").term();
        Term t1 = INSTANCE.$("goto(#1)").term();
        Term[] a = Terms.commute(t0, t1);
        Term[] b = Terms.commute(t1, t0);
        assertEquals(
                Op.terms.subterms(a),
                Op.terms.subterms(b)
        );
    }


    @Test
    void parseTemporalRelation() throws Narsese.NarseseException {

        assertEquals("(x ==>+5 y)", INSTANCE.$("(x ==>+5 y)").toString());
        assertEquals("(x &&+5 y)", INSTANCE.$("(x &&+5 y)").toString());

        assertEquals("(x ==>-5 y)", INSTANCE.$("(x ==>-5 y)").toString());

        assertEquals("((before-->x) ==>+5 (after-->x))", INSTANCE.$("(x:before ==>+5 x:after)").toString());
    }

    @Test
    void temporalEqualityAndCompare() throws Narsese.NarseseException {
        assertNotEquals(INSTANCE.$("(x ==>+5 y)"), INSTANCE.$("(x ==>+0 y)"));
        assertNotEquals(INSTANCE.$("(x ==>+5 y)").hashCode(), INSTANCE.$("(x ==>+0 y)").hashCode());
        //assertNotEquals($("(x ==> y)").hashCode(), $("(x ==>+0 y)").hashCode());

        assertEquals(INSTANCE.$("(x ==>+0 y)"), INSTANCE.$("(x ==>-0 y)"));
        assertNotEquals(INSTANCE.$("(x ==>+5 y)"), INSTANCE.$("(y ==>-5 x)"));


        assertEquals(0, INSTANCE.$("(x ==>+0 y)").compareTo(INSTANCE.$("(x ==>+0 y)")));
        assertEquals(-1, INSTANCE.$("(x ==>+0 y)").compareTo(INSTANCE.$("(x ==>+1 y)")));
        assertEquals(+1, INSTANCE.$("(x ==>+1 y)").compareTo(INSTANCE.$("(x ==>+0 y)")));
    }




    @Test
    void testTransformedImplDoesntActuallyOverlap() {
        assertEquals("(((#1 &&+7 (_1,_2)) &&+143 (_1,_2)) ==>+7 (_1,_2))",
                ((Compound) INSTANCE.$$("(((#1 &&+7 (_1,_2)) &&+143 (_1,_2)) ==>+- (_1,_2))")).dt(7).toString());
    }



    @Test
    void testNonCommutivityImplConcept() throws Narsese.NarseseException {


        n.input("(x ==>+5 y).", "(y ==>-5 x).");
        n.run(5);

        TreeSet d = new TreeSet(Comparator.comparing(Object::toString));
        n.what().concepts().forEach(new Consumer<Concept>() {
            @Override
            public void accept(Concept x) {
                d.add(x.term());
            }
        });


        assertTrue(d.contains(INSTANCE.$("(x ==>+- y)")));
        assertTrue(d.contains(INSTANCE.$("(y ==>+- x)")));
    }

    @Test
    void testImplRootDistinct() throws Narsese.NarseseException {

        Term f = INSTANCE.$("(x ==> y)");
        assertEquals("(x ==>+- y)", f.root().toString());

        Term g = INSTANCE.$("(y ==>+1 x)");
        assertEquals("(y ==>+- x)", g.root().toString());

    }

    @Test
    void testImplRootRepeat() throws Narsese.NarseseException {
        Term h = INSTANCE.$("(x ==>+1 x)");
        assertEquals("(x ==>+- x)", h.root().toString());
    }

    @Test
    void testImplRootNegate() throws Narsese.NarseseException {
        Term i = INSTANCE.$("(--x ==>+1 x)");
        assertEquals("((--,x) ==>+- x)", i.root().toString());

    }

//    @Test void InvalidIntEventTerms() {
//
//        assertEq(Null, "(/ && x)");
////        assertEq(Null, "(1 && x)");
////        assertEq(Null, "(1 &&+1 x)");
////        assertEq(Null, "(1 ==> x)");
////        assertEq(Null, "(x ==> 1)");
//
////        assertEq(Null, "(--,1)");
////        assertEq(Null, "((--,1) && x)");
//    }


    @Disabled
    @Test
    void testEqualsAnonymous3() throws Narsese.NarseseException {


		assertEquals(Retemporalize.retemporalizeAllToXTERNAL.apply($.INSTANCE.<Compound>$("(x && (y ==> z))")),
			Retemporalize.retemporalizeAllToXTERNAL.apply($.INSTANCE.<Compound>$("(x &&+1 (y ==>+1 z))")));


        assertEquals("((x &&+1 z) ==>+1 w)",
                INSTANCE.$("(x &&+1 (z ==>+1 w))").toString());

		assertEquals(Retemporalize.retemporalizeAllToXTERNAL.apply($.INSTANCE.<Compound>$("((x &&+- z) ==>+- w)")),
			Retemporalize.retemporalizeAllToXTERNAL.apply($.INSTANCE.<Compound>$("(x &&+1 (z ==>+1 w))")));
    }


    @Test
    void testValidTaskTerm() {
        String s = "believe(x,(believe(x,(--,(cam(9,$1) ==>-78990 (ang,$1))))&|(cam(9,$1) ==>+570 (ang,$1))))";
        Term ss = INSTANCE.$$(s);
        assertTrue(Task.validTaskCompound((Compound) ss, true));
        assertTrue(Task.validTaskTerm(ss));
    }

    @Test void ImplTransformMaintainsTiming() {
        assertEq(
                "((_2-->_1) ==>+3 (_1-->_3))",
                INSTANCE.$$("(($1-->_1) ==>-1 ((_2-->$1) &&+4 (_1-->_3)))").replace($.INSTANCE.varIndep(1), INSTANCE.$$("_2"))
        );
        assertEq(
                "((_1-->_3) ==>+3 (_2-->_1))",
                INSTANCE.$$("(((_1-->_3) &&+4 (_2-->$1)) ==>-1 ($1-->_1))").replace($.INSTANCE.varIndep(1), INSTANCE.$$("_2"))
        );
    }
    @Test void ConjTransformMaintainsTiming() {
        assertEq(
                "((x-->a) &&+3 (z-->a))",
                INSTANCE.$$("((x-->a) &&+1 ((y-->b) &&+2 (z -->a)))").replace(INSTANCE.$$("b"), INSTANCE.$$("y"))
        );
    }
}
