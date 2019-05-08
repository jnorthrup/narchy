package nars.term;

import jcog.random.XoRoShiRo128PlusRandom;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.subterm.*;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.anon.AnonWithVarShift;
import nars.term.util.TermTest;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.builder.InterningTermBuilder;
import nars.term.util.builder.TermConstructor;
import nars.term.util.transform.UnifyTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.CONJ;
import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.*;

public class IntrinTest {

    private static Anon assertAnon(String expect, String test) {
        return assertAnon(expect, $$(test));
    }

    /**
     * roundtrip test
     */
    private static Anon assertAnon(String expect, Term x) {
        Anon a = new Anon();
        Term y = a.put(x);
        Term z = a.get(y);
        assertEquals(expect, y.toString());
        assertEquals(x, z);
        return a;
    }

    private static void testAnonTermVectorProducedByTermBuilder(TermConstructor b) {
        {
            Subterms tri = b.subterms($.varDep(1), Anom.the(2), Anom.the(1));
            assertEquals(IntrinSubterms.class, tri.getClass());
        }

        {
            Term triSub = b.compound(Op.PROD, $.varDep(1), Anom.the(2), Anom.the(1));
            assertEquals(IntrinSubterms.class, triSub.subterms().getClass());
        }

        {
            Subterms bi = b.subterms($.varDep(1), Anom.the(2));
            assertEquals(IntrinSubterms.class, bi.getClass());
        }

        {
            Term biSub = b.compound(Op.PROD, $.varDep(1), Anom.the(2));
            assertEquals(IntrinSubterms.class, biSub.subterms().getClass());
        }

        {
            Subterms uni = b.subterms($.varDep(1));
            assertEquals(IntrinSubterms.class, uni.getClass());
        }
//        {
//            Term uniSub = b.compound(PROD, $.varDep(1));
//            assertEquals(IntrinSubterms.class, uniSub.subterms().getClass());
//        }
    }

    @Test
    void testAtoms() {
        assertAnon("_1", "a");
        assertAnon("#1", $.varDep(1));
        assertAnon("_1", $.the(2));

        assertNotEquals(Anom.the(0), $.the(0));
        assertNotEquals(Anom.the(2), $.the(2));
    }

    @Test
    void testThatAnonDoesntEatEllipsis() throws Narsese.NarseseException {
        assertEquals("(_1,%1..*)", UnifyTest.pattern("(a, %X..*)").anon().toString());
    }

    @Test
    void testCompounds() {
        assertAnon("(_1-->_2)", "(a-->b)");



        assertAnon("{_1}", "{a}");
        assertAnon("{_1,_2}", "{a,b}");
        assertAnon("{_1,_2,_3}", "{a,b,c}");
        assertAnon("(_1 ==>+- _2)", "(x ==>+- y)");
        {
            Anon a = assertAnon("((_1&&_2) ==>+- _3)", "((x&&y) ==>+- z)");
            assertEquals("(x&&y)", a.get(CONJ.the(Anom.the(1), Anom.the(2))).toString());
        }


        assertAnon("(((_1-->(_2,_3,#1))==>(_4,_5)),?2)",
                "(((a-->(b,c,#2))==>(e,f)),?1)");

    }

    @Test void testNormalizedVariables() {
        assertAnon("(_1-->#1)", "(a-->#1)");
    }
    @Test void testIntegers() {
        assertAnon("(_1-->1)", "(a-->1)");
        assertAnon("(_1-->0)", "(a-->0)");
        assertAnon("(_1-->-1)", "(a-->-1)");

        assertAnon("(--,0)", "(--,0)");
        assertAnon("(--,-1)", "(--,-1)");
        assertAnon("(--,1)", "(--,1)");
    }

    @Test
    void testCompoundsWithNegations() {
        assertAnon("((--,_1),_1,_2)", "((--,a), a, c)");
        assertAnon("(--,((--,_1),_1,_2))", "--((--,a), a, c)");
    }

    @Test
    @Disabled
    void testIntRange() throws Narsese.NarseseException {
        assertEquals("(4..6-->x)", $("((|,4,5,6)-->x)").toString());
        assertAnon("(_0-->_1)", "((|,4,5,6)-->x)");
    }

    @Test
    void testAnomVector() {

        Term[] x = {Anom.the(3), Anom.the(1), Anom.the(2)};

        TermTest.assertEq(new UniSubterm(x[0]), new IntrinSubterms(x[0]));
        TermTest.assertEq(new UniSubterm(x[0]), new TermList(x[0]));

        TermTest.assertEq(new BiSubterm(x[0], x[1]), new IntrinSubterms(x[0], x[1]));
        TermTest.assertEq(new BiSubterm(x[0], x[1]), new TermList(x[0], x[1]));

        TermTest.assertEq(new ArrayTermVector(x), new IntrinSubterms(x));
        TermTest.assertEq(new ArrayTermVector(x), new TermList(x));

    }

    @Test
    void testAnomVectorNegations() {

        Term[] x = {Anom.the(3), Anom.the(1), Anom.the(2).neg()};

        IntrinSubterms av = new IntrinSubterms(x);
        ArrayTermVector bv = new ArrayTermVector(x);
        TermTest.assertEq(bv, av);

        assertFalse(av.contains(x[0].neg()));
        assertFalse(av.containsRecursively(x[0].neg()));

        assertFalse(av.contains($$("x")));
        assertFalse(av.containsRecursively($$("x")));
        assertFalse(av.contains($$("x").neg()));
        assertFalse(av.containsRecursively($$("x").neg()));

        Term twoNeg = x[2];
        assertTrue(av.contains(twoNeg));
        assertTrue(av.containsRecursively(twoNeg));
        assertTrue(av.containsRecursively(twoNeg.neg()), () -> av + " containsRecursively " + twoNeg.neg());


    }

    @Test
    void testMixedAnonVector() {

        Term[] x = {$.varDep(1), $.varIndep(2), $.varQuery(3), Anom.the(4)};
        Random rng = new XoRoShiRo128PlusRandom(1);
        for (int i = 0; i < 4; i++) {

            ArrayUtil.shuffle(x, rng);

            TermTest.assertEq(new UniSubterm(x[0]), new IntrinSubterms(x[0]));
            TermTest.assertEq(new BiSubterm(x[0], x[1]), new IntrinSubterms(x[0], x[1]));
            TermTest.assertEq(new ArrayTermVector(x), new IntrinSubterms(x));
        }
    }

//    @Test public void testAnonSortingOfRepeats() {
//        assertAnon("(_1,_1,_2)", "(1,1,2)");
//        assertAnon("(_2,_1,_1)", "(1,2,2)");
//    }

    @Test
    void testAnonSorting() {
        assertAnon("(&&,(--,_1),_2,_3,_4,_5)", "(&&,x1,x2,--x3,x4,x5)");
        assertAnon("(&&,(--,_1),_2,_3,_4,_5)", "(&&,--x1,x2,x3,x4,x5)");
        assertAnon("(_2(_1)&&_3)", "(&&,1(2),x3)");
        assertAnon("(_2(_1)&&_3)", "(&&,3(2),x1)");
        assertAnon("((_2(_1)&&_3) &&+- _4)", "((&&,x3(2),x1) &&+- x4)");
        assertAnon("((_2(_1)&&_3) &&+- _4)", "(x1 &&+- (&&,3(2),x4))");
    }

    @Test
    void testTermSubs() {
        Term x = $$("(%1,%2)").normalize();
        assertEquals(IntrinSubterms.class, x.subterms().getClass());
        for (Termlike t: new Termlike[]{x, x.subterms()}) {
            assertEquals(2, t.count(Op.VAR_PATTERN));
            assertEquals(0, t.count(Op.VAR_DEP));
        }

        Term y = $$("(%1,%2,(--,$3))").normalize();
        assertEquals(IntrinSubterms.class, y.subterms().getClass());
        for (Termlike t: new Termlike[]{y, y.subterms()}) {
            assertEquals(2, t.count(Op.VAR_PATTERN));
            assertEquals(2, t.count(Op.VAR_PATTERN));
            assertEquals(0, t.count(Op.VAR_INDEP));
            assertEquals(1, t.count(Op.NEG));
        }
    }

    @Test
    void testAutoNormalization() throws Narsese.NarseseException {
        for (String s: new String[]{"($1)", "($1,$2)", "($1,#2)", "(%1,%1,%2)"}) {
            Term t = $$(s);
            assertEquals(s, t.toString());
            assertTrue(
                    UniSubterm.class == t.subterms().getClass() ||
                            IntrinSubterms.class == t.subterms().getClass());
            assertTrue(t.isNormalized(), () -> t + " not auto-normalized but it could be");
        }
        for (String s: new String[]{"($2)", "($2,$1)", "($1,#3)", "(%1,%3,%2)"}) {
            Term t = Narsese.term(s, false);
            assertEquals(s, t.toString());
            assertTrue(
                    UniSubterm.class == t.subterms().getClass() ||
                            IntrinSubterms.class == t.subterms().getClass(),
                    () -> t.getClass().toString() + ' ' + t.subterms().getClass());
            assertFalse(t.isNormalized(), () -> t + " auto-normalized but should not be");
        }
    }

    @Test
    void testAnonTermVectorProducedByHeapTermBuilder() {
        testAnonTermVectorProducedByTermBuilder(HeapTermBuilder.the);
    }

    @Test
    void testAnonTermVectorProducedByInterningTermBuilder() {
        testAnonTermVectorProducedByTermBuilder(new InterningTermBuilder());
    }

    @Test
    void testAnonVectorTransform() {
        //TODO
    }

    @Test
    void testAnonVectorReplace() {
        IntrinSubterms x = (IntrinSubterms)
            Op.terms.subterms($.varDep(1), Anom.the(2).neg(), Anom.the(1));

        {
            Subterms yAnon = x.replaceSub($.varDep(1), Anom.the(3));
            assertEquals("(_3,(--,_2),_1)", yAnon.toString());
            assertEquals(x.getClass(), yAnon.getClass(), "should remain AnonVector, not something else");

            Subterms yNotFound = x.replaceSub($.varDep(4), Anom.the(3));
            assertSame(x, yNotFound);
        }

        {
            Subterms yAnon = x.replaceSub(Anom.the(2).neg(), Anom.the(3));
            assertEquals("(#1,_3,_1)", yAnon.toString());
            assertEquals(x.getClass(), yAnon.getClass(), "should remain AnonVector, not something else");

            Subterms yNotFound = x.replaceSub(Anom.the(1).neg(), Anom.the(3));
            assertSame(x, yNotFound);
        }


        {
            Subterms yAnon = x.replaceSub(Anom.the(2), Anom.the(3));
            assertEquals("(#1,(--,_3),_1)", yAnon.toString());
            assertEquals(x.getClass(), yAnon.getClass(), "should remain AnonVector, not something else");
        }
        {
            Subterms yAnon = x.replaceSub(Anom.the(2), Anom.the(3).neg());
            assertEquals("(#1,_3,_1)", yAnon.toString());
            assertEquals(x.getClass(), yAnon.getClass(), "should remain AnonVector, not something else");
        }

        {
            Subterms yNonAnon = x.replaceSub($.varDep(1), PROD.the($.the("X")));
            assertEquals("((X),(--,_2),_1)", yNonAnon.toString());
            assertNotEquals(x.getClass(), yNonAnon.getClass());

            Subterms yNotFound = x.replaceSub(PROD.the($.the("X")), PROD.the($.the("Y")));
            assertSame(yNotFound, x);

        }

        {
            Subterms xx = Op.terms.subterms($.varDep(1), Anom.the(2).neg(), Anom.the(2));
            assertEquals("(#1,(--,_3),_3)", xx.replaceSub(Anom.the(2), Anom.the(3)).toString());
            assertEquals("(#1,_3,_2)", xx.replaceSub(Anom.the(2).neg(), Anom.the(3)).toString());
        }


        {
            Subterms xx = Op.terms.subterms($.varDep(1), Anom.the(2).neg(), Anom.the(2));
            assertEquals("(#1,(--,()),())", xx.replaceSub(Anom.the(2), Op.EmptyProduct).toString());
            assertEquals("(#1,(),_2)", xx.replaceSub(Anom.the(2).neg(), Op.EmptyProduct).toString());
        }

    }

    @Test void testShiftWithIndexGap() {
        //in ths example,
        // because there is no #1 variable,
        // the shift must replace x's #2 with #3 (not #2) which would collapse against itself
        Term x = $$("(Level(low) ==>+1 ((--,At(#1))&&At(#2)))");
        Term b = $$("(_1($1) ==>+1 ((--,_1($1))&&_1(#2)))");
        Term y = new AnonWithVarShift(16, Op.VAR_DEP.bit | Op.VAR_QUERY.bit).putShift(x, b);
        assertEquals("(_2(_1) ==>+1 ((--,_3(#3))&&_3(#4)))", y.toString());
    }

}