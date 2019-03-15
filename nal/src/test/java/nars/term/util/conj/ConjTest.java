package nars.term.util.conj;

import jcog.data.list.FasterList;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.*;
import nars.io.IO;
import nars.op.SubUnify;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermTestMisc;
import nars.term.atom.Bool;
import nars.term.util.TermException;
import nars.term.util.transform.Retemporalize;
import nars.term.var.ellipsis.Ellipsis;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;

import java.util.List;
import java.util.Random;

import static nars.$.*;
import static nars.Op.CONJ;
import static nars.io.NarseseTest.assertInvalidTerms;
import static nars.term.TemporalTermTest.testParse;
import static nars.term.TermTestMisc.assertValid;
import static nars.term.atom.Bool.*;
import static nars.term.util.TermTest.*;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
import static org.junit.jupiter.api.Assertions.*;

/**
 * tests specific to conjunction (and disjunction) compounds
 * TODO use assertEq() where possible for target equality test (not junit assertEquals). it applies more rigorous testing
 */
public class ConjTest {

    @Test
    void conjWTFF() {
        ConjBuilder x = new Conj();
        x.add(ETERNAL, $$("a:x"));
        x.add(0, $$("a:y"));
        assertEq("((x-->a)&|(y-->a))", x.term());
    }

    @Test
    void conjWTFF2() {
        assertEq(False,
                $$("(&&, ((--,#1) &&+232 (--, (tetris --> curi))),(--, right),right)")
        );
    }



    @Test
    void testParallelizeDTERNALWhenInSequence() {
        assertEq("((a&|b) &&+1 c)", "((a&&b) &&+1 c)");
        assertEq("((a&&b) &&+- c)", "((a&&b) &&+- c)"); //xternal: unaffected
        assertEq("((a&|b) &&+- c)", "((a&|b) &&+- c)"); //xternal: unaffected
        assertEq("(c &&+1 (a&|b))", "(c &&+1 (a&&b))");
    }

    @Test
    void testParallelizeImplAFTERSequence() {
        assertEq("((a&|b)==>x)", "((a &| b) ==> x)");
        assertEq("((a &&+1 b)==>x)", "((a &&+1 b) ==> x)");
        assertEq("((a&|b) ==>+- x)", "((a &| b) ==>+- x)"); //xternal: unaffected
        //assertEq("(x =|> (a &| b))","(x ==> (a &| b))");
        //assertEq("(x =|> (a &&+1 b))","(x ==> (a &&+1 b))");
    }
//
//    //WhenInXTERNAL?
//    @Test
//    void testParallelizeNegatedDTERNALWhenInSequence() {
//        assertEq("((--,(a&|b)) &&+1 c)", "(--(a&&b) &&+1 c)");
//        assertEq("(c &&+1 (--,(a&|b)))", "(c &&+1 --(a&&b))");
//    }


    @Test
    void testDternalize() {
        assertEq("((a &&+3 b)&&c)"
                /*"((a&|c) &&+3 (b&|c))"*/, $$("((a &&+3 b) &&+3 c)").dt(DTERNAL));
    }

    @Test
    void testElimDisjunctionDTERNAL_WTF() {
        assertEq("(--,right)", "((--,(left&&right))&&(--,right))");
    }

    @Test void testSequenceInEternal() {
        assertEq("((#NIGHT &&+1 #DAY1)&&(#DAY2 &&+1 #NIGHT))",
            CONJ.the(DTERNAL,
                $$$("(#NIGHT &&+1 #DAY1)"),
                $$$("(#DAY2 &&+1 #NIGHT)")
        ));
    }

    @Test
    void testSimpleEternals() {
        Conj c = new Conj();
        c.add(ETERNAL, x);
        c.add(ETERNAL, y);
        assertEquals("(x&&y)", c.term().toString());
        assertEquals(1, c.event.size());
        assertEquals(byte[].class, c.event.get(ETERNAL).getClass());
    }

    @Test
    void testSimpleEternalsNeg() {
        ConjBuilder c = new Conj();
        c.add(ETERNAL, x);
        c.add(ETERNAL, y.neg());
        assertEquals("((--,y)&&x)", c.term().toString());
    }

    @Test
    void testSimpleEvents() {
        Conj c = new Conj();
        c.add(1, x);
        c.add(2, y);
        assertEquals("(x &&+1 y)", c.term().toString());
        assertEquals(1, c.shift());
        assertEquals(2, c.event.size());
    }

    @Test
    void testRoaringBitmapNeededManyEventsAtSameTime() {
        Conj b = new Conj();
        for (int i = 0; i < Conj.ROARING_UPGRADE_THRESH - 1; i++)
            b.add(1, $.the(String.valueOf((char) ('a' + i))));
        assertEquals("(&|,a,b,c,d,e,f,g)", b.term().toString());
        assertEquals(1, b.event.size());
        assertEquals(byte[].class, b.event.get(1).getClass());

        Conj c = new Conj();
        for (int i = 0; i < Conj.ROARING_UPGRADE_THRESH + 1; i++)
            c.add(1, $.the(String.valueOf((char) ('a' + i))));
        assertEquals("(&|,a,b,c,d,e,f,g,h,i)", c.term().toString());
        assertEquals(1, c.event.size());
        assertEquals(RoaringBitmap.class, c.event.get(1).getClass());
    }

    @Test
    void testSimpleEventsNeg() {
        ConjBuilder c = new Conj();
        c.add(1, x);
        c.add(2, y.neg());
        assertEquals("(x &&+1 (--,y))", c.term().toString());
    }

    @Test
    void testEventContradiction() {
        ConjBuilder c = new Conj();
        c.add(1, x);
        assertFalse(c.add(1, x.neg()));
        assertEquals(False, c.term());
    }

    @Test
    void testEventContradictionAmongNonContradictions() {
        ConjBuilder c = new Conj();
        c.add(1, x);
        c.add(1, y);
        c.add(1, z);
        assertFalse(c.add(1, x.neg()));
        assertEquals(False, c.term());
    }

    @Test
    void testEventContradictionAmongNonContradictionsRoaring() {
        ConjBuilder c = new Conj();
        c.add(ETERNAL, $$("(&&,a,b,c,d,e,f,g,h)"));
        assertEquals(8, c.eventCount(ETERNAL));
        boolean added = c.add(1, a.neg());
        assertFalse(added);
        assertEquals(False, c.term());
    }


    @Test
    void testGroupNonDTemporalParallelComponents() throws Narsese.NarseseException {


        Term c1 = $("((--,(ball_left)) &&-270 (ball_right)))");

        assertEquals("((ball_right) &&+270 (--,(ball_left)))", c1.toString());
        assertEquals(
                "(((ball_left)&|(ball_right)) &&+270 (--,(ball_left)))",

                parallel($("(ball_left)"), $("(ball_right)"), c1)
                        .toString());

    }

    @Test
    void testDisjConjElim() {
        assertEq("(--,L)", "((||,R,(--,L))&&(--,L))");
    }

    @Test
    void testEventContradictionWithEternal() {
        ConjBuilder c = new Conj();
        c.add(ETERNAL, x);
        boolean added = c.add(1, x.neg());
        assertFalse(added);
        assertEquals(False, c.term());
    }

    @Test
    void testEventNonContradictionWithEternal() {
        ConjBuilder c = new Conj();
        c.add(ETERNAL, x);
        boolean added = c.add(1, y);
        assertTrue(added);
        assertEquals("(x&&y)", c.term().toString());
    }

    @Test
    void testEventNonContradictionWithEternal2() {
        ConjBuilder c = new Conj();
        c.add(ETERNAL, x);
        c.add(1, y);
        c.add(2, z);
        assertEq("((y &&+1 z)&&x)", c.term());

    }

    @Test
    void testConjEventConsistency3ary() {
        for (int i = 0; i < 100; i++) {
            assertConsistentConj(3, 0, 7);
        }
    }

    @Test
    void testConjEventConsistency4ary() {
        for (int i = 0; i < 100; i++) {
            assertConsistentConj(4, 0, 11);
        }
    }

    @Test
    void testConjEventConsistency5ary() {
        for (int i = 0; i < 300; i++) {
            assertConsistentConj(5, 0, 17);
        }
    }

    private void assertConsistentConj(int variety, int start, int end) {
        FasterList<LongObjectPair<Term>> x = newRandomEvents(variety, start, end);

        Term y = conj(x.clone());
        List<LongObjectPair<Term>> z = y.eventList();


        //System.out.println(x + "\t" + y + "\t" + z);


        if (!x.equals(z)) {
            Term y2 = conj(x.clone());
        }

        assertEquals(x, z);
    }

    private FasterList<LongObjectPair<Term>> newRandomEvents(int variety, int start, int end) {
        FasterList<LongObjectPair<Term>> e = new FasterList<>();
        long earliest = Long.MAX_VALUE;
        for (int i = 0; i < variety; i++) {
            long at = (long) rng.nextInt(end - start) + start;
            earliest = Math.min(at, earliest);
            e.add(pair(at, $.the(String.valueOf((char) ('a' + i)))));
        }

        long finalEarliest = earliest;
        e.replaceAll((x) -> pair(x.getOne() - finalEarliest, x.getTwo()));
        e.sortThisByLong(LongObjectPair::getOne);
        return e;
    }

    @Test
    void testConjComplexAddRemove() {
        Term x = $$("(( ( (x,_3) &| (--,_4)) &| (_5 &| _6)) &&+8 ( ((x,_3) &| (--,_4)) &| (_5 &|_6))))");
        Conj c = Conj.from(x);
        assertEquals(x, c.term());
        boolean removedLast = c.remove(c.event.keysView().max(), $$("(x,_3)"));
        assertTrue(removedLast);
        assertEquals(
                "((&|,(x,_3),(--,_4),_5,_6) &&+8 (&|,(--,_4),_5,_6))",
                c.term().toString());
        boolean removedFirst = c.remove(c.event.keysView().min(), $$("(x,_3)"));
        assertTrue(removedFirst);
        assertEquals(
                "((&|,(--,_4),_5,_6) &&+8 (&|,(--,_4),_5,_6))",
                c.term().toString());

    }

    @Test
    void testPromoteEternalToParallel2() {
        assertEq("((b&|c)&&a)", "(a&&(b&|c))");
    }

    @Test
    void negatedConjunctionAndNegatedSubterm() throws Narsese.NarseseException {


        assertEquals("(--,x)", $.$("((--,(x &| y)) &| (--,x))").toString());
        assertEquals("(--,x)", $.$("((--,(x && y)) && (--,x))").toString());


        assertEquals("(--,x)", $.$("((--,(x && y)) && (--,x))").toString());
        assertEquals("(--,x)", $.$("((--,(&&,x,y,z)) && (--,x))").toString());

        assertEquals("((--,(y&&z))&&x)", $.$("((--,(&&,x,y,z)) && x)").toString());
    }

    @Disabled
    @Test
    void testCoNegatedConjunctionParallelEternal1() {

        assertEq(False,
                "(((--,(z&&y))&&x)&|(--,x))");
    }

    @Disabled
    @Test
    void testCoNegatedConjunctionParallelEternal2() {
        assertEq(False,
                "(((--,(y&&z))&|x)&&(--,x))");

    }

    @Test
    void testTemporalConjunctionReduction5() {
        Term t = assertEq(
                "((a&|b)&&(a &&+1 b))",
                "( (a&|b) && (a &&+1 b) )");
        Term ta = t.anon();
        assertEquals(t.volume(), ta.volume());
        assertEq(
                "((_1&|_2)&&(_1 &&+1 _2))",
                ta);

    }

    @Test void eteConjInSequenceShouldBeParallel() {
        assertEq("((x&|y) &&+1 w)", "((x && y) &&+1 w)");
        assertEq("(w &&+1 (x&|y))", "(w &&+1 (x && y))");
        assertEq("((a&|b) &&+1 (x&|y))", "((a && b) &&+1 (x && y))");
        assertEq("((a&|b) &&+1 (c&|d))", ConjSeq.sequence($$("(a&&b)"), 0, $$("(c&&d)"), 1, Op.terms));
    }

    @Test void eteConjInParallelShouldBeParallel() {
        assertEq("(&|,a,b,z)", "((a && b) &| z)");
        assertEq("((--,(a&|b))&|z)", "(--(a && b) &| z)");
    }

    @Test void negatedEteConjInSequenceShouldBeParallel() {
        assertEq("((--,(a&|b)) &&+1 (x&|y))", "(--(a && b) &&+1 (x && y))");
        assertEq("(--,((a&|b) &&+1 (x&|y)))", "--((a && b) &&+1 (x && y))");
    }

    @Test
    void testConjParallelOverrideEternal2() {

        String y = "(a&|b)";

        ConjBuilder c = new Conj();
        c.add(ETERNAL, $$("(a&&b)"));
        c.add(ETERNAL, $$("(a&|b)"));
        assertEquals(y, c.term().toString());

        assertEq(
                y,
                "( (a&&b) && (a&|b) )");

    }

    @Test
    void testConjPosNegElimination1() throws Narsese.NarseseException {


        assertEquals("((--,b)&&a)", $.$("(a && --(a && b))").toString());
    }

    @Test
    void testConjPosNegElimination2() throws Narsese.NarseseException {


        assertEquals("((--,a)&&b)", $.$("(--a && (||,a,b))").toString());
    }

    @Test
    void testXternalRepeats() {
        assertEq("(x &&+- y)", "(&&+-,x,x,y)");
        assertEq("(x &&+- x)", "(&&+-,x,x,x)");
    }

    @Test
    void testConjRepeatPosNeg() {
        Term x = $.the("x");
        assertEquals(+1, CONJ.the(-1, new Term[]{x, x}).dt());
        assertEquals(+1, CONJ.the(+1, new Term[]{x, x}).dt());
        assertArrayEquals(IO.termToBytes(CONJ.the(+32, new Term[]{x, x})), IO.termToBytes(CONJ.the(-32, new Term[]{x, x})));
        assertEquals(+1, CONJ.the(XTERNAL, new Term[]{x, x}).dt(-1).dt());
        assertEquals(+1, CONJ.the(XTERNAL, new Term[]{x, x}).dt(+1).dt());
        assertEquals(CONJ.the(-1, new Term[]{x, x}), CONJ.the(+1, new Term[]{x, x}));
        assertEquals(CONJ.the(XTERNAL, new Term[]{x, x}).dt(-1), CONJ.the(XTERNAL, new Term[]{x, x}).dt(+1));
    }

    @Test
    void testConjEvents1a() throws Narsese.NarseseException {
        assertEquals(
                "(a &&+16 ((--,a)&|b))",
                conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(298L, $.$("a")),
                                pair(314L, $.$("b")),
                                pair(314L, $.$("(--,a)"))})
                ).toString()
        );
    }

    @Test
    void testConjEvents1b() throws Narsese.NarseseException {
        assertEquals(
                "((a&|b) &&+1 (--,a))",
                conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $.$("a")),
                                pair(1L, $.$("b")),
                                pair(2L, $.$("(--,a)"))})
                ).toString()
        );
    }

    @Test
    void testConjEvents2() throws Narsese.NarseseException {
        assertEquals(
                "((a &&+1 (&|,b1,b2,b3)) &&+1 (c &&+1 (d1&|d2)))",
                conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $.$("a")),
                                pair(2L, $.$("b1")),
                                pair(2L, $.$("b2")),
                                pair(2L, $.$("b3")),
                                pair(3L, $.$("c")),
                                pair(4L, $.$("d1")),
                                pair(4L, $.$("d2")),
                                pair(5L, Bool.True /* ignored */)
                        })).toString());
    }

    @Test
    void testConjEventsWithFalse() throws Narsese.NarseseException {
        assertEquals(
                False,
                conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $.$("a")),
                                pair(2L, $.$("b1")),
                                pair(2L, False)
                        })));
        assertEquals(
                False,
                conj(
                        new FasterList<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $.$("a")),
                                pair(1L, $.$("--a"))
                        })));
    }

    @Test
    void testConjPosNeg() throws Narsese.NarseseException {


        assertEquals(False, $.$("(x && --x)"));
        assertEquals(Bool.True, $.$("--(x && --x)"));
        assertEquals(Bool.True, $.$("(||, x, --x)"));

        assertEquals("y", $.$("(y && --(&&,x,--x))").toString());
    }

    @Test
    void testTrueFalseInXternal() {
        for (int i : new int[]{XTERNAL, 0, DTERNAL}) {
            assertEquals("x", CONJ.the(i, new Term[]{$.the("x"), Bool.True}).toString());
            assertEquals(False, CONJ.the(i, new Term[]{$.the("x"), False}));
            assertEquals(Null, CONJ.the(i, new Term[]{$.the("x"), Null}));
        }
    }

    @Test
    void testConegatedConjunctionTerms0() throws Narsese.NarseseException {
        assertEq("((--,#1) &&+- #1)", "(#1 &&+- (--,#1))");
        assertEq("(#1 &&+1 (--,#1))", "(#1 &&+1 (--,#1))");
        assertEq(False, "(#1 && (--,#1))");
        assertEq(False, "(#1 &| (--,#1))");
        assertEquals(False, parallel(varDep(1), varDep(1).neg()));

        assertEq(False, "(&&, #1, (--,#1), (x))");
        assertEq(False, "(&|, #1, (--,#1), (x))");

        assertEq("(x)", "(&&, --(#1 && (--,#1)), (x))");

        assertSame($("((x) &&+1 --(x))").op(), CONJ);
        assertSame($("(#1 &&+1 (--,#1))").op(), CONJ);


    }

    @Test
    void testCoNegatedJunction() {


        assertEq(False, "(&&,x,a:b,(--,a:b))");

        assertEq(False, "(&&, (a), (--,(a)), (b))");
        assertEq(False, "(&&, (a), (--,(a)), (b), (c))");


        assertEq(False, "(&&,x,y,a:b,(--,a:b))");
    }

    @Test
    void testCoNegatedDisjunction() {

        assertEq(Bool.True, "(||,x,a:b,(--,a:b))");

        assertEq(Bool.True, "(||,x,y,a:b,(--,a:b))");

        assertEq(Bool.True, "(||,((--,left) &&+120 (--,left)),left)");
        assertEq("x", "((||,((--,left) &&+120 (--,left)),left) ==> x)");
        assertEq("x", "((||,((--,left) &&+120 (--,left)),left) =|> x)");
        assertEq("(y==>x)", "((&&,(||,((--,left) &&+120 (--,left)),left),y) ==> (x&&y))");
    }

    @Test
    void testMergedDisjunction1() {
        //simplest case: merge one of the sequence
        Term a = $$("(x &&+1 y)");
        Term b = $$("(x &| y)");
        assertEq(
                "((--,((--,y) &&+1 (--,y)))&|x)",
                $.disj(a, b)
        );
    }

    @Test
    void testMergedDisjunction2() {
        //simplest case: merge one of the sequence
        Term a = $$("(x &&+10 y)");
        Term b = $$("(x &&-10 y)");
        assertEq(
                "((--,((--,y) &&+20 (--,y))) &&+10 x)",
                $.disj(a, b)
        );
    }

    @Test
    void testConegatedConjunctionTerms1() throws Narsese.NarseseException {
        assertEquals($("((--,(y&&z))&&x)"), $("(x && --(y && z))"));
    }

    @Test
    void testConegatedConjunctionTerms0not() {

        assertEq("((--,(y&|z))&&x)", "(x&&--(y &| z))");

        assertEq("((--,(y&|z))&|x)", "(x &| --(y && z))");
    }

    @Test
    void testConegatedConjunctionTerms1not() {

        assertEq("((--,(y &&+1 z))&&x)", "(x&&--(y &&+1 z))");

        assertEq("(x &&+1 (--,(y&|z)))", "(x &&+1 --(y && z))");
    }

    @Test
    void testConegatedConjunctionTerms2() {

        assertEq("((--,(robin-->swimmer))&&#1)", "(#1 && --(#1&&(robin-->swimmer)))");
    }

    @Test
    void testDemorgan1() {


        assertEq("(--,(p&&q))", "(||, --p, --q)");
    }

    @Test
    void testDemorgan2() {


        assertEq("((--,p)&&(--,q))", "(--p && --q)");
    }

    @Test
    void testConjDisjNeg() {
        assertEq("((--,x)&&y)", "(--x && (||,y,x))");
    }

    @Test
    void testConjParallelsMixture() {

        assertEq(False, "(((b &&+4 a)&|(--,b))&|((--,c) &&+6 a))");

        assertEq("((&|,a,b2,b3) &&+1 (c&|b1))",
                "(((a &&+1 b1)&|b2)&|(b3 &&+1 c))");
        assertEq("((a &&+1 (b1&|b2)) &&+1 c)", "((a &&+1 (b1&|b2)) &&+1 c)");


    }

    @Test
    void testConjParallelWithNegMix() {
        String x = "((--,(x &| y)) &| (--,y))";
        assertEquals($$(x).toString(), $$($$(x).toString()).toString());

        assertEq("(--,y)",
                x);

        assertEq("(--,y)",
                "((--,(x&|y))&|(--,y))");


        assertEquals("((--,(x&|y)) &&+1 (--,y))", $$("((--,(x &| y)) &&+1 (--,y))").toString());

    }

    @Test
    void testConjParallelWithNegMix2() {
        assertEquals("((--,(x &&+1 y))&|(--,y))", $$("((--,(x &&+1 y)) &| (--,y))").toString());
    }

    @Test
    void testFilterCommutedWithCoNegatedSubterms() throws Narsese.NarseseException {


        TermTestMisc.assertValidTermValidConceptInvalidTaskContent(("((--,x) && x)."));
        TermTestMisc.assertValidTermValidConceptInvalidTaskContent("((--,x) &| x).");
        assertValid($("((--,x) &&+1 x)"));
        assertValid($("(x &&+1 x)"));

        assertEquals($("x"), $("(x &| x)"));
        assertEquals($("x"), $("(x && x)"));
        assertNotEquals($("x"), $("(x &&+1 x)"));

        assertInvalidTerms("((--,x) || x)");
        assertInvalidTerms("(&|,(--,(score-->tetris)),(--,(height-->tetris)),(--,(density-->tetris)),(score-->tetris),(height-->tetris))");

    }

    @Test
    void testWrappingCommutiveConjunction() {


        Term xEternal = $$("((((--,angX) &&+4 x) &&+10244 angX) && y)");
        assertEquals(
                "((((--,angX) &&+4 x) &&+10244 angX)&&y)",
                //"((((--,angX)&|y) &&+4 (x&|y)) &&+10244 (y&|angX))",
                xEternal.toString());
    }

    @Test
    @Disabled
    void testWrappingCommutiveConjunctionX() {

        Term xFactored = $$("((x&&y) &&+1 (y&&z))");
        assertEquals("((x &&+1 z)&&y)", xFactored.toString());


        Term xAndContradict = $$("((x &&+1 x)&&--x)");
        assertEquals(False,
                xAndContradict);


        Term xAndRedundant = $$("((x &&+1 x)&&x)");
        assertEquals("(x &&+1 x)",
                xAndRedundant.toString());


        Term xAndRedundantParallel = $$("(((x &| y) &| z)&&x)");
        assertEquals("(&|,x,y,z)",
                xAndRedundantParallel.toString());


        Term xAndContradictParallel = $$("(((x &| y) &| z)&&--x)");
        assertEquals(False,
                xAndContradictParallel);


        Term xAndContradictParallelMultiple = $$("(&&,x,y,((x &| y) &| z))");
        assertEquals("(&|,x,y,z)",
                xAndContradictParallelMultiple.toString());


        Term xAndContradict2 = $$("((((--,angX) &&+4 x) &&+10244 angX) && --x)");
        assertEquals(False, xAndContradict2);


        Term xAndContradict3 = $$("((((--,angX) &&+4 x) &&+10244 angX) && angX)");
        assertEquals(False, xAndContradict3);


        Term xParallel = $$("((((--,angX) &&+4 x) &&+10244 angX) &| y)");
        assertEquals(False, xParallel);


        Term xParallelContradiction4 = $$("((((--,angX) &&+4 x) &&+10244 angX) &| angX)");
        assertEquals(False, xParallelContradiction4);


        Term x = $$("((((--,angX) &&+4 x) &&+10244 angX) &| angX)");
        Term y = $$("(angX &| (((--,angX) &&+4 x) &&+10244 angX))");
        assertEquals(x, y);

    }

    @Disabled
    @Test
    void testFactorFromEventSequence() {
        Term yParallel1 = $$("((((--,angX) &&+4 x) &&+10244 angX) &| y)");
        String yParallel2Str = "((((--,angX)&|y) &&+4 (x&|y)) &&+10244 (angX&|y))";
        Term yParallel2 = $$(yParallel2Str);
        assertEquals(yParallel1, yParallel2);
        assertEquals(yParallel2Str, yParallel1.toString());
    }

    @Disabled
    @Test
    void testFactorFromEventParallel() {
        Term yParallelOK = $$("(((a&&x) &| (b&&x)) &| (c&&x))");
        assertEquals("", yParallelOK.toString());


        Term yParallelContradict = $$("((a&&x) &| (b&&--x))");
        assertEquals(False, yParallelContradict);
    }

    @Test
    void testConjWithout() {

        assertEquals("(--,y)", Conj.diff($$("(--x && --y)"), $$("x"), true).toString());
        assertEquals("(--,y)", Conj.diff($$("(--x &&+1 --y)"), $$("x"), true).toString());
        assertEquals("(z &&+1 (--,y))", Conj.diff($$("((--x &&+1 z) &&+1 --y)"), $$("x"), true).toString());
        assertEquals("(z &&+1 (--,y))", Conj.diff($$("((x &&+1 z) &&+1 --y)"), $$("x"), true).toString());
        assertEquals("(z &&+1 (--,y))", Conj.diff($$("((--x &&+1 z) &&+1 --y)"), $$("--x"), true).toString());
        assertEquals("(z &&+1 (--,y))", Conj.diff($$("((x &&+1 z) &&+1 --y)"), $$("--x"), true).toString());
        assertEquals("((--,x)&&(--,y))", Conj.diff($$("(--x && --y)"), $$("x"), false).toString()); //unchanged
        assertEquals("((--,x) &&+1 (--,y))", Conj.diff($$("(--x &&+1 --y)"), $$("x"), false).toString()); //unchanged
        assertEquals("(((--,x) &&+1 z) &&+1 (--,y))", Conj.diff($$("((--x &&+1 z) &&+1 --y)"), $$("x"), false).toString());

        assertEquals("y", Conj.diff($$("(x && y)"), $$("x"), false).toString());

        assertEquals("(--,x)", Conj.diff($$("--x"), $$("x"), false).toString());
        assertEquals("x", Conj.diff($$("x"), $$("--x"), false).toString());
        assertEquals("true", Conj.diff($$("--x"), $$("x"), true).toString());
        assertEquals("true", Conj.diff($$("x"), $$("--x"), true).toString());


    }

    @Test
    void testConjWithoutAllParallel() {
        assertEquals("(a&&b)", Conj.diff(
                $$("(&&,a,b,c)"),
                $$("(&&,c,d,e)")).toString());

        //unchanged because negative
        assertEq("(--,(&&,a,b,c))", Conj.diff(
                $$("--(&&,a,b,c)"),
                $$("(&&,c,d,e)")));

        assertEq("(a&|b)", Conj.diff(
                $$("(&|,a,b,c)"),
                $$("(&|,c,d,e)")));


        assertEq("(a&&b)",
                Conj.diff(
                        $$("(&&,a,b,c)"),
                        $$("(&|,c,d,e)")));

        assertEq("(a&&b)", Conj.diff(
                $$("(&&,a,b,--c)"),
                $$("(&&,--c,d,e)")));
    }


    @Test
    void testConjWithoutAllParallel2() {
        assertEq("a", Conj.diff($$("(&&,a,b,c)"), $$("(b&&c)")));
        assertEq("b", Conj.diff($$("(&&,a,b,c)"), $$("(a&&c)")));
        assertEq("(b&&c)", Conj.diff($$("(&&,a,b,c)"), $$("a")));
        assertEq("(a&&c)", Conj.diff($$("(&&,a,b,c)"), $$("b")));


    }

    @Test
    void testConjLazyRemoveIf() {
        ConjLazy c = ConjLazy.events($$("((&|,c,f) &&+1 g)"));
        assertEquals(3, c.size());

        assertFalse(
                c.removeIf((when, what) -> when == 1 && what.toString().equals("c"))
        );
        assertEquals(3, c.size());

        assertTrue(
                c.removeIf((when, what) -> when == 0 && what.toString().equals("c"))
        );
        assertEquals(2, c.size());
        assertEquals(0, c.when(0));
        assertEq("f", c.get(0));
        assertEquals(1, c.when(1));
        assertEq("g", c.get(1));
        assertEq("(f &&+1 g)", c.term());
    }

    @Test
    void testConjWithoutAllParallel3() {
        assertEq("(f &&+1 g)", Conj.diff(
                $$("((&|,c,f) &&+1 g)"),
                $$("(&|,c,d,e)")));
    }

    @Test
    void testConjWithoutAllSequence() {
        assertEq("z", Conj.diff(
                $$("((x &&+1 y) &&+1 z)"),
                $$("(&&,x,y)")));

        assertEq("(y &&+1 z)", Conj.diff(
                $$("((x &&+1 y) &&+1 z)"),
                $$("(x &&+2 y)")));
    }

    @Test
    void testPromoteEternalToParallel3() {


        Term x = assertEq(//"((b&&c)&|(x&&y))",
                //"((b&&c)&|(x&&y))",
                "(&|,b,c,x,y)",
                "((b&&c)&|(x&&y))");

        Term y = $$("(&|,(b&&c),x)");
        assertEquals(

                "((b&&c)&|x)",
                //"(&|,b,c,x)",
                y.toString());

        assertEquals("y", Conj.diff(x, y).toString());

        //ConjCommutive.the(DTERNAL, $$("(a&|b)"), $$("(b&|c)"));

        assertEq("((a&|b)&&(b&|c))", "((a&|b)&&(b&|c))");
        assertEq("((a&|b)&&(c&|d))", "((a&|b)&&(c&|d))");
        assertEq("(&|,a,b,c,d)", "((a&&b)&|(c&&d))");
    }

    @Test
    void testConjCommutivity() {

        assertEquivalentTerm("(&&,a,b)", "(&&,b,a)");
        assertEquivalentTerm("(&&,(||,(b),(c)),(a))", "(&&,(a),(||,(b),(c)))");
        assertEquivalentTerm("(&&,(||,(c),(b)),(a))", "(&&,(a),(||,(b),(c)))");
        assertEquivalentTerm("(&&,(||,(c),(b)),(a))", "(&&,(a),(||,(c),(b)))");
    }

    @Test
    void testConjunction1Term() throws Narsese.NarseseException {

        assertEquals("a", $.$("(&&,a)").toString());
        assertEquals("x(a)", $.$("(&&,x(a))").toString());
        assertEquals("a", $.$("(&&,a, a)").toString());

        assertEquals("((before-->x) &&+10 (after-->x))",
                $.$("(x:after &&-10 x:before)").toString());
        assertEquals("((before-->x) &&+10 (after-->x))",
                $.$("(x:before &&+10 x:after)").toString());


    }

    @Test
    void testEmptyConjResultTerm() {
        ConjBuilder c = new Conj();
        assertEquals(Bool.True, c.term());
    }

    @Test
    void testEmptyConjTrueEternal() {
        ConjBuilder c = new Conj();
        c.add(ETERNAL, Bool.True);
        assertEquals(Bool.True, c.term());
    }

    @Test
    void testEmptyConjTrueTemporal() {
        ConjBuilder c = new Conj();
        c.add(0, Bool.True);
        assertEquals(Bool.True, c.term());
    }

    @Test
    void testEmptyConjFalseEternal() {
        ConjBuilder c = new Conj();
        c.add(ETERNAL, False);
        assertEquals(False, c.term());
    }

    @Test
    void testEmptyConjFalseTemporal() {
        ConjBuilder c = new Conj();
        c.add(0, False);
        assertEquals(False, c.term());
    }

    @Test
    void testEmptyConjFalseEternalShortCircuit() {
        ConjBuilder c = new Conj();
        c.add(ETERNAL, $$("x"));
        boolean addedFalse = c.add(ETERNAL, False);
        assertFalse(addedFalse);
        //boolean addedAfterFalse = c.addAt($$("y"), ETERNAL);
        assertEquals(False, c.term());
    }

    @Test
    void testEmptyConjFalseTemporalShortCircuit() {
        ConjBuilder c = new Conj();
        c.add(0, $$("x"));
        boolean addedFalse = c.add(0, False);
        assertFalse(addedFalse);
        //boolean addedAfterFalse = c.addAt($$("y"), 0);
        assertEquals(False, c.term());
    }

    @Test
    public void testReducibleDisjunctionConjunction0() {
        assertEq("x", $$("((x||y) && x)"));
    }

    @Test
    public void testReducibleDisjunctionConjunction1() {

        for (int dt : new int[]{DTERNAL, 0}) {
            String s0 = "(x||y)";
            Term x0 = $$(s0);
            Term x = CONJ.the(dt, x0, $$("x"));
            assertEquals("x", x.toString());
        }
    }

    @Test
    public void testReducibleDisjunctionConjunction2() {
        assertEq("(x&&y)", $$("((||,x,y,z)&&(x && y))").toString());
    }

    @Test
    public void testReducibleDisjunctionConjunction3() {
        assertEquals("(--,x)", $$("((||,--x,y)&& --x)").toString());
    }

    @Test
    public void testInvalidAfterXternalToNonXternalDT() {
        //String s = "((--,((||,dex(fz),reward(fz))&&dex(fz))) &&+- dex(fz))";
        String s = "((--x &&+1 y) &&+- x)";
        Term x = $$(s);
        assertEquals(False, x.dt(0));
        assertEquals(False, x.dt(DTERNAL));
//        assertThrows(TermException.class, ()->
//            x.dt(1)
//        );

    }


    @Test
    public void testInvalidSubsequenceComponent2() {
        Term s = $$("(--,((||,x,y)&&z))");
        assertEq("(&&,(--,x),(--,y),z)", CONJ.the(s, DTERNAL, $$("z")).toString()); //TODO check
    }

    @Test
    public void testSortingTemporalConj() {
        assertEquals(0, $$("(x &&+1 y)").compareTo($$("(x &&+1 y)")));

        assertEquals(-1, $$("(x &| y)").compareTo($$("(x &&+1 y)")));
        assertEquals(-1, $$("(x &&+1 y)").compareTo($$("(x &&+2 y)")));
        assertEquals(-1, $$("(x &&-1 y)").compareTo($$("(x &&+1 y)")));

        assertEquals(+1, $$("(x &&+2 y)").compareTo($$("(x &&+1 y)")));
        assertEquals(+1, $$("(x &&+10 y)").compareTo($$("(x &&-10 y)")));

        assertEquals(-1, $$("(x &&+1 y)").compareTo($$("(x &&+10 y)")));
    }


    @Test
    public void testConjConceptualizationWithFalse() {
        assertEquals(False, $$("((--,chronic(g))&&((--,up)&|false))"));
    }


    @Test
    public void testParallelFromEternalIfInXTERNAL() {
        assertEq("((a&&x) &&+- (a&&y))", "((a&&x) &&+- (a&&y))");
    }

    @Test
    public void testXternalInDternal() {
        assertEq(//"((a&&x) &&+- (a&&y))",
                "((x &&+- y)&&a)",
                "((x &&+- y) && a)");
    }

    @Test
    public void testXternalInSequence() {
        assertEq("((x &&+- y) &&+1 a)", "((x &&+- y) &&+1  a)");
        assertEq("(a &&+1 (x &&+- y))", "(a &&+1 (x &&+- y))");
        assertEq("((x &&+- y)&|a)", "((x &&+- y) &|  a)");

        assertEquals(0, $$("(x &&+- y)").eventRange());
        assertEquals(0, $$("((y &&+2 z) &&+- x)").eventRange());

        assertEq("(((y &&+2 z) &&+- x) &&+1 a)", "((x &&+- (y &&+2 z)) &&+1  a)");
        assertEq("(a &&+1 ((y &&+2 z) &&+- x))", "(a &&+1 ((y &&+2 z) &&+- x))");
        assertEq("(((w&&x) &&+- (y &&+2 z)) &&+1 a)", "(((x&&w) &&+- (y &&+2 z)) &&+1 a)");

    }

    @Test
    public void testFactorDternalComponentIntoTemporals3() {
        assertEquals(
                "((x&&y) &&+- x)"
                , $$("(((x && y) &&+- x)&&x)").toString());
    }

    @Test
    void testEventsWithRepeatParallel() throws Narsese.NarseseException {

        Term ab = $.$("(a&|b)");
        assertEquals("[0:a, 0:b]",
                ab.eventList().toString());

//        Term abc = $.$("((a&|b) &&+5 (b&|c))");
//        assertEquals(
//
//
//                "[0:a, 0:b, 5:b, 5:c]",
//                abc.eventList().toString());

    }



    @Test
    void testEventsWithXTERNAL() throws Narsese.NarseseException {

        assertEquals("[0:(x &&+- y)]", $.$("(x &&+- y)").eventList().toString());
        assertEquals("[0:(x &&+- y), 1:z]", $.$("((x &&+- y) &&+1 z)").eventList().toString());
    }

    @Test
    void testEventsWithDTERNAL() throws Narsese.NarseseException {

        assertEquals("[0:(x&&y)]", $.$("(x && y)").eventList().toString());
        assertEquals("[0:x, 0:y, 1:z]", $.$("((x && y) &&+1 z)").eventList().toString());
    }

    @Test
    void testConjSorting() throws Narsese.NarseseException {
        Term ea = $.$("(x&&$1)");
        assertEquals("(x&&$1)", ea.toString());
        Term eb = $.$("($1&&x)");
        assertEquals("(x&&$1)", eb.toString());
        Term pa = $.$("(x&|$1)");
        assertEquals("(x&|$1)", pa.toString());
        Term pb = $.$("($1&|x)");
        assertEquals("(x&|$1)", pb.toString());
        Term xa = $.$("($1 &&+- x)");
        assertEquals("(x &&+- $1)", xa.toString());
        Term xb = $.$("(x &&+- $1)");
        assertEquals("(x &&+- $1)", xb.toString());

        assertEquals(ea, eb);
        assertEquals(ea.dt(), eb.dt());
        assertEquals(ea.subterms(), eb.subterms());

        assertEquals(pa, pb);
        assertEquals(pa.dt(), pb.dt());
        assertEquals(ea.subterms(), pa.subterms());
        assertEquals(ea.subterms(), pb.subterms());

        assertEquals(xa, xb);
        assertEquals(xa.dt(), xb.dt());
        assertEquals(ea.subterms(), xa.subterms());
        assertEquals(ea.subterms(), xb.subterms());
    }


    @Test
    public void testIndepVarWTF() {
        assertEq("(x1&&$1)", new Conj().with(ETERNAL, $$("(&&,x1,$1)")).term());
        assertEq("(x1&|$1)", new Conj().with(0, $$("(&&,x1,$1)")).term());
    }

    @Test
    void testRetemporalization1() throws Narsese.NarseseException {

        assertEq(False /*"x"*/, $$("((--,(x &&+1 x))&&x)"));

        assertEq(
                "a(x,true)",
                $.$(
                        "a(x,(--,((--,(x &&+1 x)) &&+- x)))"
                ).temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL).toString()
        );
    }


    @Test
    void testCommutiveTemporalityConjEquiv() {


        testParse("((#1-->$2) &&-20 ({(row,3)}-->$2))", "(({(row,3)}-->$1) &&+20 (#2-->$1))");
    }

    @Test
    void testCommutiveTemporalityConjEquiv2() {
        testParse("(({(row,3)}-->$2) &&+20 (#1-->$2))", "(({(row,3)}-->$1) &&+20 (#2-->$1))");
    }

    @Test
    void testCommutiveTemporalityConj2() {
        testParse("(goto(a) &&+5 ((SELF,b)-->at))", "(goto(a) &&+5 at(SELF,b))");
    }

    @Test
    void testCommutiveConjTemporal() throws Narsese.NarseseException {
        Term x = $.$("(a &&+1 b)");
        assertEquals("a", x.sub(0).toString());
        assertEquals("b", x.sub(1).toString());
        assertEquals(+1, x.dt());
        assertEquals("(a &&+1 b)", x.toString());

        Term y = $.$("(a &&-1 b)");
        assertEquals("a", y.sub(0).toString());
        assertEquals("b", y.sub(1).toString());
        assertEquals(-1, y.dt());
        assertEquals("(b &&+1 a)", y.toString());

        Term z = $.$("(b &&+1 a)");
        assertEquals("a", z.sub(0).toString());
        assertEquals("b", z.sub(1).toString());
        assertEquals(-1, z.dt());
        assertEquals("(b &&+1 a)", z.toString());

        Term w = $.$("(b &&-1 a)");
        assertEquals("a", w.sub(0).toString());
        assertEquals("b", w.sub(1).toString());
        assertEquals(+1, w.dt());
        assertEquals("(a &&+1 b)", w.toString());

        assertEquals(y, z);
        assertEquals(x, w);

    }

    @Test
    void testCommutiveTemporality1() {
        testParse("(goto(a)&&((SELF,b)-->at))", "(at(SELF,b)&&goto(a))");
        testParse("(goto(a)&|((SELF,b)-->at))", "(at(SELF,b)&|goto(a))");
        testParse("(at(SELF,b) &&+5 goto(a))", "(at(SELF,b) &&+5 goto(a))");
    }

    @Test
    void testCommutiveTemporality2() {
        testParse("(at(SELF,b)&&goto(a))");
        testParse("(at(SELF,b)&|goto(a))");
        testParse("(at(SELF,b) &&+5 goto(a))");
        testParse("(goto(a) &&+5 at(SELF,b))");
    }

    @Test
    void testCommutiveTemporalityDepVar1() {
        testParse("(goto(#1) &&+5 at(SELF,#1))");
    }

    @Test
    void testCommutiveTemporalityDepVar2() {
        testParse("(goto(#1) &&+5 at(SELF,#1))", "(goto(#1) &&+5 at(SELF,#1))");
        testParse("(goto(#1) &&-5 at(SELF,#1))", "(at(SELF,#1) &&+5 goto(#1))");
    }



    @Test
    void testReversibilityOfCommutive() throws Narsese.NarseseException {
        for (String c : new String[]{"&&"/*, "<=>"*/}) {
            assertEquals("(a " + c + "+5 b)", $("(a " + c + "+5 b)").toString());
            assertEquals("(b " + c + "+5 a)", $("(b " + c + "+5 a)").toString());
            assertEquals("(a " + c + "+5 b)", $("(b " + c + "-5 a)").toString());
            assertEquals("(b " + c + "+5 a)", $("(a " + c + "-5 b)").toString());

            assertEquals($("(b " + c + "-5 a)"), $("(a " + c + "+5 b)"));
            assertEquals($("(b " + c + "+5 a)"), $("(a " + c + "-5 b)"));
            assertEquals($("(a " + c + "-5 b)"), $("(b " + c + "+5 a)"));
            assertEquals($("(a " + c + "+5 b)"), $("(b " + c + "-5 a)"));
        }
    }

    @Test
    void testCommutiveWithCompoundSubterm() throws Narsese.NarseseException {
        Term a = $("(((--,(b0)) &| (pre_1)) &&+10 (else_0))");
        Term b = $("((else_0) &&-10 ((--,(b0)) &| (pre_1)))");
        assertEquals(a, b);

        Term c = CONJ.the((Term)$("((--,(b0)) &| (pre_1))"), 10, $("(else_0)"));
        Term d = CONJ.the((Term)$("(else_0)"), -10, $("((--,(b0)) &| (pre_1))"));


        assertEquals(b, c);
        assertEquals(c, d);
        assertEquals(a, c);
        assertEquals(a, d);
    }



    @Test
    void testConjEarlyLate() throws Narsese.NarseseException {
        {
            Term yThenZ = $("(y &&+1 z)");
            assertEquals("y", yThenZ.sub(Conj.conjEarlyLate(yThenZ, true)).toString());
            assertEquals("z", yThenZ.sub(Conj.conjEarlyLate(yThenZ, false)).toString());
        }
        {
            Term yThenZ = $("(y &| z)");
            assertEquals("y", yThenZ.sub(Conj.conjEarlyLate(yThenZ, true)).toString());
            assertEquals("z", yThenZ.sub(Conj.conjEarlyLate(yThenZ, false)).toString());
        }

        {
            Term zThenY = $("(z &&+1 y)");
            assertEquals("z", zThenY.sub(Conj.conjEarlyLate(zThenY, true)).toString());
            assertEquals("y", zThenY.sub(Conj.conjEarlyLate(zThenY, false)).toString());
        }

    }


    @Test
    void testDTRange() throws Narsese.NarseseException {
        assertEquals(1, $("(z &&+1 y)").eventRange());
    }

    @Test
    void testDTRange2() throws Narsese.NarseseException {
        String x = "(x &&+1 (z &&+1 y))";
        Term t = $(x);
        assertEq("((x &&+1 z) &&+1 y)", t.toString());
        assertEquals(2, t.eventRange(), () -> t + " incorrect dtRange");
    }

    @Test
    void testDTRange3() throws Narsese.NarseseException {
        assertEquals(4, $("(x &&+1 (z &&+1 (y &&+2 w)))").eventRange());
        assertEquals(4, $("((z &&+1 (y &&+2 w)) &&+1 x)").eventRange());
    }

    @Test
    void testCommutivity() throws Narsese.NarseseException {

        assertTrue($("(b && a)").isCommutative());
        assertTrue($("(b &| a)").isCommutative());
        assertTrue($("(b &&+- a)").isCommutative());


        Term abc = $("((a &| b) &| c)");
        assertEq("(&|,a,b,c)", abc);
        assertTrue(abc.isCommutative());

//        assertFalse($("(b &&+1 a)").isCommutative());
    }

    @Test
    void testInvalidConjunction() throws Narsese.NarseseException {

        Compound x = $("(&&,(#1-->I),(#1-->{i141}),(#2-->{i141}))");
        assertNotNull(x);
        assertThrows(TermException.class, () -> x.dt(-1));
        assertThrows(TermException.class, () -> x.dt(+1));
        assertNotEquals(Null, x.dt(0));
        assertNotEquals(Null, x.dt(DTERNAL));
        assertNotEquals(Null, x.dt(XTERNAL));
    }

    @Test
    void testConjRoot() throws Narsese.NarseseException {


        Term a = $("(x && y)");

        Term b = $("(x &&+1 y)");
        assertEq("(x &&+- y)", b.root());

        Term c = $("(x &&+1 x)");
        assertEq("(x &&+- x)", c.root());

        Term cn = $("(x &&+1 --x)");
        assertEq("((--,x) &&+- x)", cn.root());
    }



    @Test
    void testRetermporalization1() throws Narsese.NarseseException {

        String st = "((--,(happy)) && (--,((--,(o))&&(happy))))";
        Compound t = $(st);
        assertEquals("(--,(happy))", t.toString());
        Term xe = t.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        assertEquals("(--,(happy))", xe.toString());


    }


    @Test
    void testXternalConjCommutiveAllowsPosNeg() {
        String s = "( &&+- ,(--,x),x,y)";
        assertEquals(s,
                Op.CONJ.the(XTERNAL, x, x.neg(), y).toString());
        assertEquals(s,
                Op.CONJ.the(XTERNAL, y, x, x.neg()).toString());
    }

    @Test
    void testConceptual2() throws Narsese.NarseseException {

        Term x = $("((--,(vy &&+- happy)) &&+- (happy &&+- vy))");
        assertTrue(x instanceof Compound);

//        Term y = assertEq(
//                "((--,(vy &&+84 happy))&&(vy&|happy))",
//                "((--,(vy &&+84 happy))&&(happy&|vy))");
//        assertEquals(
//
//                "( &&+- ,(--,(vy &&+- happy)),vy,happy)",
//                y.concept().toString());

    }

    @Test
    void testRetermporalization2() throws Narsese.NarseseException {
        String su = "((--,(happy)) &&+- (--,((--,(o))&&+-(happy))))";
        Compound u = $(su);
        assertEquals("((||+- ,(o),(--,(happy))) &&+- (--,(happy)))", u.toString());

        Term ye = u.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        assertEquals("(--,(happy))", ye.toString());

        Term yz = u.temporalize(Retemporalize.retemporalizeXTERNALToZero);
        assertEquals("(--,(happy))", yz.toString());

    }

    /**
     * conjunction and disjunction subterms which can occurr as a result
     * of variable substitution, etc which don't necessarily affect
     * the resulting truth of the compound although if the statements
     * were alone they would not form valid tasks themselves
     */
    @Test
    void testSingularStatementsInConjunction() throws Narsese.NarseseException {
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a<->a),c:d,e:f)"));

        assertEq($("(&&,c:d,e:f)"), "(&&,(a-->a),c:d,e:f)");
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a==>a),c:d,e:f)"));
        assertEq(False, "(&&,(--,(a==>a)),c:d,e:f)");

    }

    @Test
    void testSingularStatementsInDisjunction() {

        assertInvalidTerms("(||,(a<->a),c:d,e:f)");
    }

    @Test
    void testSingularStatementsInDisjunction2() {
        assertEq("(y-->x)", "(&&,(||,(a<->a),c:d,e:f),x:y)");
        assertEq(False, "(&&,(--,(||,(a<->a),c:d,e:f)),x:y)");


    }

    @Test
    void testEternalDisjunctionToParallel() {

        assertEq("(||,x,y,z)", "(||,(||,x,y),z)");
        assertEq("(||,x,y,z)", "(||,--(--x && --y), z)");
        assertEq("(--,(&|,(--,x),(--,y),(--,z)))", "(--,(((--,x)&|(--,y))&|(--,z)))");
        //assertEq("(--,(&|,(--,x),(--,y),(--,z)))", "(||,--(--x &| --y), z)"); //promoted
        assertEq("(||,(--,((--,x)&|(--,y))),z)", "(||,--(--x &| --y), z)"); //promoted

    }

    @Test
    void testDisjunctionStackOverflow() {
        assertEq("(||,x,y,z)", "((x || y) || z)");
        assertEq("((||,y,z)&&(--,x))", "((||,(x || y),z) && --x)");

        assertEq("((||,y,z)&&(--,x))", "((||,x,y,z) && --x)");
        assertEq("x", "((||,x,y,z) && x)");

        assertEq("((--,((--,y)&|(--,z)))&&(--,x))", "(--(&|, --x, --y, --z) && --x)");
        assertEq("x", "(--(&|, --x, --y, --z) && x)");

    }

    @Test
    void testDisjunctEqual() {
        Term pp = p(x);
        assertEquals(pp, disj(pp, pp));
    }

    @Test
    void testDisjReduction1() {

        Term x = $.the("x");
        assertEquals(x, $.disj(x, x));
        assertEquals(x, CONJ.the(DTERNAL, new Term[]{x.neg(), x.neg()}).neg());
    }

    @Disabled
    @Test
    void testRepeatConjunctionTaskSimplification() throws Narsese.NarseseException {

        assertEquals(
                "$.50 (x). 0⋈10 %1.0;.90%",
                Narsese.the().task("((x) &&+10 (x)). :|:", NARS.shell()).toString());
    }

    @Test
    void testConjParallelWithSeq() {
        assertEq("(a &&+5 b)", "((a &&+5 b)&|a)");

        assertEq(False, "((--a &&+5 b)&|a)");
    }

    @Test
    void testEmbeddedConjNormalizationN2() throws Narsese.NarseseException {
        Compound bad = $("(a &&+1 (b &&+1 c))");
        Compound good = $("((a &&+1 b) &&+1 c)");
        assertEquals(good, bad);
        assertEquals(good.toString(), bad.toString());
        assertEquals(good.dt(), bad.dt());
        assertEquals(good.subterms(), bad.subterms());
    }

    @Test
    void testEmbeddedConjNormalizationN2Neg() throws Narsese.NarseseException {
        Compound alreadyNormalized = $("((c &&+1 b) &&+1 a)");
        Compound needsNormalized = $("(a &&-1 (b &&-1 c))");
        assertEquals(alreadyNormalized, needsNormalized);
        assertEquals(alreadyNormalized.toString(), needsNormalized.toString());
        assertEquals(alreadyNormalized.dt(), needsNormalized.dt());
        assertEquals(alreadyNormalized.subterms(), needsNormalized.subterms());
    }

    @Test
    void testEmbeddedConjNormalizationN3() throws Narsese.NarseseException {

        String ns = "((a &&+1 b) &&+1 (c &&+1 d))";
        Compound normal = $(ns);

        assertEquals(3, normal.eventRange());
        assertEquals(ns, normal.toString());

        for (String unnormalized : new String[]{
                "(a &&+1 (b &&+1 (c &&+1 d)))",
                "(((a &&+1 b) &&+1 c) &&+1 d)"
        }) {
            Compound u = $(unnormalized);
            assertEquals(normal, u);
            assertEquals(normal.toString(), u.toString());
            assertEquals(normal.dt(), u.dt());
            assertEquals(normal.subterms(), u.subterms());
        }
    }

    @Test
    void testEmbeddedConjNormalizationWithNeg1() throws Narsese.NarseseException {
        String d = "(((d) &&+3 (a)) &&+1 (b))";

        String c = "((d) &&+3 ((a) &&+1 (b)))";
        Term cc = $(c);
        assertEquals(d, cc.toString());

        String a = "(((a) &&+1 (b)) &&-3 (d))";
        Term aa = $(a);
        assertEquals(d, aa.toString());


        assertTrue(aa.sub(0).subs() > aa.sub(1).subs());
        assertTrue(cc.sub(0).subs() > cc.sub(1).subs());

    }


    @Test
    void testEmbeddedConjNormalization2() {
        assertEq("((a &&+1 b) &&+3 (c &&+5 d))", "(a &&+1 (b &&+3 (c &&+5 d)))");
        assertEq("(((t2-->hold) &&+1 (t1-->at)) &&+3 ((t1-->[opened]) &&+5 open(t1)))", "(hold:t2 &&+1 (at:t1 &&+3 ((t1-->[opened]) &&+5 open(t1))))");
    }

    @Test
    void testConjMergeABCShift() throws Narsese.NarseseException {
        /* WRONG:
            $.23 ((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c))). 1⋈16 %1.0;.66% {171: 1;2;3;;} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
              $.50 (a &&+5 (--,a)). 1⋈6 %1.0;.90% {1: 1}
              $.47 ((b &&+5 (--,b)) &&+5 (--,c)). 6⋈16 %1.0;.73% {43: 2;3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
        */
        Term a = $.$("(a &&+5 (--,a))");
        Term b = $.$("((b &&+5 (--,b)) &&+5 (--,c))");
        assertEq("((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c)))", ConjSeq.sequence(a, 0, b, 5, Op.terms));
    }

    @Test
    void testConjunctionEqual() {
        assertEquals(x, CONJ.the(x, x));
    }

    @Test
    void testConjunctionNormal() throws Narsese.NarseseException {
        Term x = $("(&&, <#1 --> lock>, <#1 --> (/, open, #2, _)>, <#2 --> key>)");
        assertEquals(3, x.subs());
        assertEquals(CONJ, x.op());
    }

    @Test
    void testImpossibleSubtermWrong() throws Narsese.NarseseException {
        Term sooper = $("(cam(0,0) &&+3 ({(0,0)}-->#1))");
        Term sub = $("cam(0,0)");
        assertTrue(sooper.contains(sub));
        assertTrue(!sooper.impossibleSubTerm(sub));


    }

    @Test
    void testValidConjDoubleNegativeWTF() {
        assertEq("(x &&+1 x)", "((x &&+1 x) && x)");
        assertEquals(False, $.$$("((x &&+1 x) && --x)"));

        assertEq("((--,x) &&+1 x)", "((--x &&+1 x) &| --x)"); //matches at zero

        assertEquals(False, $.$$("((--x &&+1 x) && x)"));
        assertEquals(False, $.$$("((x &&+1 --x) && --x)"));

        assertEq("x", "(--(--x &&+1 x) &| x)");
        assertEq(False, "((--x &&+1 x) &| (--x &&+1 --x))");
    }

    @Test
    void testValidConjParallelContainingTermButSeparatedInTime0() {
        for (String s : new String[]{
                "(x &&+100 (--,(x&|y)))",
                "(x &&+100 ((--,(x&|y))&|a))"
        })
            assertStable(s);
    }

    @Test
    void testValidConjParallelContainingTermButSeparatedInTime() {
        for (String s : new String[]{
                "((--,(&|,(--,L),(--,R),(--,angVel)))&|(--,(x&|y)))",
                "(x &&+100 ((--,(&|,(--,L),(--,R),(--,angVel)))&|(--,(x&|y))))",
                "(x &&+100 ((--,(x&|y))&|(--,z)))",
                "(x &&+100 (--,(x&|y)))"
        })
            assertStable(s);


    }


    @Test
    void testCoNegatedSubtermTask() throws Narsese.NarseseException {


        assertNotNull(Narsese.the().task("(x &&+1 (--,x)).", n));


        assertInvalidTask("(x && (--,x)).");
        assertInvalidTask("(x &| (--,x)).");
    }


    @Test
    void testAtemporalization2() throws Narsese.NarseseException {

        assertEquals("((--,y) &&+- y)", $.<Compound>$("(y &&+3 (--,y))").temporalize(Retemporalize.retemporalizeAllToXTERNAL).toString());
    }

    @Test
    void testMoreAnonFails() {

    }

    @Test
    void testCommutizeRepeatingConjunctions() throws Narsese.NarseseException {
        assertEquals("a",
                $("(a &&+1 a)").dt(DTERNAL).toString());
        assertEquals(False,
                $("(a &&+1 --a)").dt(DTERNAL));

        assertEquals("a",
                $("(a &&+1 a)").dt(0).toString());
        assertEquals(False,
                $("(a &&+1 --a)").dt(0));

        assertEquals("(a &&+- a)",
                $("(a &&+1 a)").dt(XTERNAL).toString());
        assertEquals("((--,a) &&+- a)",
                $("(a &&+1 --a)").dt(XTERNAL).toString());

    }

    @Test
    void testDisjunctionInnerDTERNALConj() {


        Term x = $$("((x &&+1 --x) && --y)");
        Conj xc = Conj.from(x);
        assertEq(x, xc.term());

        assertEquals(1, xc.eventCount(0));
        assertEquals(1, xc.eventCount(1));
        assertEquals(1, xc.eventCount(ETERNAL));


        {
            assertTrue(xc.removeEventsByTerm($$("x"), true, false));
            assertEq("((--,x)&&(--,y))", xc.term());
        }

        Term xn = x.neg();
        assertEq("(||,(--,(x &&+1 (--,x))),y)", xn);

        assertEq(
                //"((||,(--,(x &&+1 (--,x))),y)&&x)",
                "x",
                CONJ.the(
                        xn,
                        $$("x")
                )
        );

    }

    private void assertInvalidTask(@NotNull String ss) {
        try {
            Narsese.task(ss, n);
            fail("");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * these are experimental cases involving contradictory or redundant events in a conjunction of
     * parallel and dternal sub-conjunctions
     * TODO TO BE DECIDED
     */
    @Disabled
    private class ConjReductionsTest {

        @Test
        void testConjParaEteReductionInvalid() {
            assertEquals(False,
                    $$("(((--,a)&&b)&|(--,b)))")
            );
        }

        @Test
        void testConjParaEteReductionInvalid2() {
            assertEquals(False,
                    $$("(((--,a)&&(--,b))&|b))")
            );
        }

        @Test
        void testConjParaEteReduction2simpler() throws Narsese.NarseseException {
            String o = "(((--,x)&|y) ==>+1 (((--,x)&&y)&|y))";
            String q = "(((--,x)&|y) ==>+1 ((--,x)&|y))";
            Term oo = $(o);
            assertEquals(q, oo.toString());
        }

        @Test
        void testConjParaEteReduction2() throws Narsese.NarseseException {
            String o = "(((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)) ==>-807 (((--,tetris(isRow,2,true))&&tetris(isRowClear,8,true))&|tetris(isRowClear,8,true)))";
            String q = "(((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)) ==>-807 ((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)))";
            Term oo = $(o);
            assertEquals(q, oo.toString());
        }

        @Test
        void testConjParaEteReduction() throws Narsese.NarseseException {
            String o = "(((--,a)&&b)&|b))";
            String q = "((--,a)&|b)";
            Term oo = $(o);
            assertEquals(q, oo.toString());
        }

        @Test
        void testConjEteParaReduction() throws Narsese.NarseseException {
            String o = "(((--,a)&|b)&&b))";
            String q = "((--,a)&|b)";
            Term oo = $(o);
            assertEquals(q, oo.toString());
        }

        @Test
        void testConjParallelOverrideEternal() {

            assertEq(
                    "(a&|b)",
                    "( (a&&b) &| (a&|b) )");

        }

        @Test
        void testConjNearIdentity() {
            assertEq(Bool.True, "( (a&&b) ==> (a&|b) )");

            assertEq(
                    "((X,x)&|#1)",
                    "( ((X,x)&&#1) &| ((X,x)&|#1) )");

            assertEq("((--,((X,x)&&#1))&|(--,((X,x)&|#1)))", "( (--,((X,x)&&#1)) &| (--,((X,x)&|#1)) )");
        }

    }

    @Test
    void testConjOneEllipsisDontRepeat() {
        assertEq("(&&,%1..+)", "(&&,%1..+)");
        assertEq("( &&+- ,%1..+)", $$("(&&,%1..+)").dt(XTERNAL));

    }

    @Test
    void testConjOneNonEllipsisDontRepeat() {
        assertEq("x", "(&&,x)");
        assertEq("x", "(&&+- , x)");
    }

    @Test
    void testConjRepeatXternalEllipsisDontCollapse() {

        assertEq("((%1..+ &&+- %1..+)&&(%2..+ &&+- %2..+))", $$("((%1..+ &&+- %1..+) && (%2..+ &&+- %2..+))"));

        //construct by changed dt to XTERNAL from DTERNAL
        assertEq("((%1..+ &&+- %1..+) &&+- (%2..+ &&+- %2..+))",
                $$("((%1..+ &&+- %1..+) && (%2..+ &&+- %2..+))").dt(XTERNAL));

        //construct directly
        assertEq("((%1..+ &&+- %1..+) &&+- (%2..+ &&+- %2..+))",
                "((%1..+ &&+- %1..+) &&+- (%2..+ &&+- %2..+))");


    }

    @Test
    void unifyXternalParallel() {
        assertUnifies("(&&+-, x, y, z)", "(&|, x, y, z)", true);
        assertUnifies("(&&+-, --x, y, z)", "(&|, --x, y, z)", true);
        assertUnifies("(&&+-, --x, y, z)", "(&|, x, y, z)", false);
        assertUnifies("(&&+-, x, y, z)", "(&|, --x, y, z)", false);
    }

    @Test
    void unifyXternalSequence2() {
        assertUnifies("(x &&+- y)", "(x &&+1 y)", true);
        assertUnifies("(x &&+- y)", "(x &&+1 --y)", false);
        assertUnifies("(--x &&+- y)", "(--x &&+1 y)", true);
    }

    @Test
    void unifyXternalSequence2Repeating() {
        assertUnifies("(x &&+- x)", "(x &&+1 x)", true);
        assertUnifies("(x &&+- --x)", "(x &&+1 --x)", true);
        assertUnifies("(x &&+- --x)", "(--x &&+1 x)", true);
    }

    @Test
    void unifyXternalSequence3() {
        assertUnifies("(&&+-, x, y, z)", "(x &&+1 (y &&+1 z))", true);
        assertUnifies("(&&+-, x, y, z)", "(z &&+1 (x &&+1 y))", true);
        assertUnifies("(&&+-, x, --y, z)", "(x &&+1 (--y &&+1 z))", true);
        assertUnifies("(&&+-, x, y, z)", "(x &&+1 (--y &&+1 z))", false);
    }

    @Test
    void testConjEternalConjEternalConj() {

        Term a = $$("((x &&+7130 --x)&&y)");
        Term b = $$("z");
        assertEq(
                "(&&,(x &&+7130 (--,x)),y,z)", //NOT: "(((x &&+7130 --x)&&y)&&z)",
                CONJ.the(DTERNAL, a, b)
        );
    }

    @Test
    void testFactorizeEternalConj1() {
        Conj c = new Conj();
        c.add(1, $$("(a&&x)"));
        c.add(2, $$("(b&&x)"));
        assertTrue(c.eventCount(ETERNAL) == 0);
        assertTrue(c.eventOccurrences() == 2);
        c.factor();
        assertTrue(c.eventCount(ETERNAL) == 1);
        assertTrue(c.eventOccurrences() == 3);
        assertEq("((a &&+1 b)&&x)", c.term());
        assertEquals(1, c.shift());
    }

    @Test
    void testFactorizeEternalConj2() {
        Conj c = new Conj();
        c.add(1, $$("(a&&(x&&y))"));
        c.add(2, $$("(b&&(x&&y))"));
        assertTrue(c.eventCount(ETERNAL) == 0);
        assertTrue(c.eventOccurrences() == 2);
        c.factor();
        assertTrue(c.eventCount(ETERNAL) == 2);
        assertTrue(c.eventOccurrences() == 3);
        assertEq("(x&&y)", c.term(ETERNAL));
        assertEq("a", c.term(1));
        assertEq("b", c.term(2));
        assertEq("(&&,(a &&+1 b),x,y)", c.term());
    }

    @Test
    void testFactorizeParallelConjETE() {
        Term x = $$("(a&|b)");
        Term y = $$("(b&|c)");
        Conj c = new Conj();
        c.add(ETERNAL, x);
        assertEquals(1, c.eventCount(ETERNAL));
        c.add(ETERNAL, y);
        assertEquals(2, c.eventCount(ETERNAL));
        assertEquals(1, c.eventOccurrences());
        c.factor();
        assertTrue(c.eventCount(ETERNAL) == 2); //unchanged
        assertTrue(c.eventOccurrences() == 1);
        assertEq("((a&|b)&&(b&|c))", c.term());
    }

    @Test
    void testConjEternalConj() {
        //"(&&,(((left-->g) &&+270 (--,(left-->g))) &&+1070 (right-->g)),(up-->g),(left-->g),(destroy-->g))"

        //construction method 1
        Term x = $$("(((left-->g) &&+270 (--,(left-->g))) &&+1070 (right-->g))");
        Term y = $$("(&&,(up-->g),(left-->g),(destroy-->g))");
        assertEq(False, CONJ.the(DTERNAL, x, y));

        //construction method 2
        ConjBuilder xy = new Conj();
        assertTrue(xy.add(ETERNAL, x));
        assertFalse(xy.add(ETERNAL, y));
        assertEquals(False, xy.term());


    }

    @Test
    void testConjEternalConj2Pre() {
        //(not ((not y)and x) and x) == x and y
        //https://www.wolframalpha.com/input/?i=(not+((not+y)and+x)+and+x)

        //construction method 1:
        Term a = $$("((--,((--,y)&|x))&&x)");
        assertEq("(x&&y)", a);

        {
            //construction method 2:
            ConjBuilder c = new Conj();
            c.add(ETERNAL, $$("(--,((--,y)&|x))"));
            c.add(ETERNAL, $$("x"));
            assertEq("(x&&y)", c.term());
        }
    }

    @Test
    void testSequenceAutoFactor() {
        Term xyz = $$("((x &| y) &&+2 (x &| z))");
        assertEq("((y &&+2 z)&&x)", xyz);

        assertEquals($$("(y &&+2 z)").eventRange(), xyz.eventRange());
        assertEquals(2, xyz.eventRange());
    }

    @Test
    void testSubtimeFirst_of_Sequence() {
        Term subEvent = $$("(((--,_3(_1,_2))&|_3(_4,_5)) &&+830 (--,_3(_8,_9)))").eventFirst();
        assertEq("((--,_3(_1,_2))&|_3(_4,_5))", subEvent);
        assertEquals(
                20,
                $$("((_3(_6,_7) &&+20 ((--,_3(_1,_2))&|_3(_4,_5))) &&+830 (--,_3(_8,_9)))").
                        subTimeFirst(subEvent)
        );

    }

    @Test
    void testAnotherComplexInvalidConj() {
        String a0 = "(((--,((--,_2(_1)) &&+710 (--,_2(_1))))&|(_3-->_1)) &&+710 _2(_1))";
        Term a = $$(a0);
        assertEq(a0, a);
        Term b = $$("(--,(_2(_1)&|(_3-->_1)))");
        assertEq("((_3-->_1) &&+710 _2(_1))", CONJ.the(0, a, b));

    }

    @Test
    void testAnotherComplexInvalidConj2() {
        //TODO check what this actually means
        String a0 = "((--,((--,_3) &&+190 (--,_3)))&|((_1,_2)&|_3))";
        Term a = $$(a0);
        assertEq("((_1,_2)&|_3)", a);
    }

    @Test
    void testDTChange() {
        assertEq("((a &&+3 b) &&+2 c)", $$("((a &&+3 b) &&+3 c)").dt(2));
    }

    @Test
    void testParallelDisjunctionAbsorb() {
        assertEq("((--,y)&|x)", CONJ.the(0, $$("--(x&&y)"), $$("x")));
    }

    @Test
    void testParallelDisjunctionInert() {
        assertEq("((--,(x&|y))&|z)", CONJ.the(0, $$("--(x&&y)"), $$("z")));
    }

    @Test
    void testSequentialDisjunctionAbsorb() {
        {
            assertEq("((--,R)&&(--,jump))", "(--R && (R || --jump))");
            assertEq("((--,R) &&+600 (--,jump))", "(--R && --(--R &&+600 jump))");
        }

        Term t = CONJ.the(0,
                $$("(--,((--,R) &&+600 jump))"),
                $$("(--,L)"),
                $$("(--,R)"));
        assertEq("(((--,L)&|(--,R)) &&+600 (--,jump))", t);
    }

    @Test
    void testSequentialDisjunctionContradict() {
        Term u = CONJ.the(0,
                $$("(--,((--,R) &&+600 jump))"),
                $$("(--,L)"),
                $$("R"));
        assertEq("((--,L)&|R)", u);
    }

    @Test
    void testDisjunctionParallelReduction() {
        assertEq("((--,y)&&x)",
                $$("(&&,(--,(y&&x)),x)")
        );
        assertEq("((--,y)&&x)", //<- maybe --y &| x
                $$("(&&,(--,(y&|x)),x)")
        );
        assertEq("((--,y)&|x)",
                $$("(&|,(--,(y&&x)),x)")
        );
        assertEq("((--,y)&|x)",
                $$("(&|,(--,(y&|x)),x)")
        );
        Term skdfjlsdfk = $$("(&|,(--,(y&|x)),z,x)");
        assertEq("(&|,(--,y),x,z)",
                skdfjlsdfk
        );

        assertEq("(&|,(--,curi(tetris)),(--,(height-->tetris)),(density-->tetris))",
                $$("(&|,(--,((height-->tetris)&|(density-->tetris))),(--,curi(tetris)),(density-->tetris))")
        );

    }

    @Test
    void testDisjunctionParallelReduction2() {

        assertEq("(&&,(--,y),x,z)",
                $$("(&&,(--,(y&&x)),z,x)")
        );

    }

    @Test
    void testDisjunctionParallelReduction3() {

        assertEq(False,
                $$("( &|, (--, (g, (1, 1),(18, 0))),(--, ((--, (right--> g))&|(down--> g))),(--, (right--> g)),(down--> g))")
        );
    }

    @Test
    void testCollapseEteParallel1() {

        assertEq("(&|,a,b,c)", "((&&,a,b)&|c)"); //collapse
        //assertEq("((a&&b)&|c)", "((&&,a,b)&|c)"); //NOT collapse

        assertEq("((a&|b)&&c)", "((&|,a,b)&&c)"); //NOT collapse

    }

    @Test
    void testCollapseEteParallel2() {
        assertEq("(&|,a,b,c,d)", "(&|,(&&,a,b),c,d)"); //collapse
//        assertEq("(&|,(a&&b),c,d)", "(&|,(&&,a,b),c,d)"); //NOT collapse
        assertEq("(&&,(a&|b),c,d)", "(&&,(&|,a,b),c,d)"); //NOT collapse
    }

    @Test
    void testCollapseEteParallel3() {

        {
            assertEq("((--,(c&|d))&&(a&|b))", ConjCommutive.the(DTERNAL, $$("(--,(c&|d))"), $$("(a&|b)")));
        }
        assertEq("((--,(c&|d))&&(a&|b))", "((&|,a,b) && --(&|,c,d))"); //NOT collapse

        assertEq("(&|,(--,(c&|d)),a,b)", "((&&,a,b) &| --(&|,c,d))");


    }

    @Test
    void testCollapseEteContainingEventParallel1() {

        assertEq("(a &&+1 (b&|c))", "(a &&+1 (b&&c))");
        assertEq("(a &&+1 (&|,b,c,d))", "(a &&+1 (b&|(c&&d)))");


    }

    @Test
    void testConjDistributeEteParallel1() {
        Term x = $$("((&|,_2(_1),_4(_3),_6(_5))&&(--,(_6(#1)&|_6(#2))))");
        {
            ConjBuilder c = Conj.from(x);
            assertEq(x, c.term());
            assertEquals(2, c.eventCount(ETERNAL));
        }
        {
            Conj c = Conj.from(x);
            c.distribute();
            assertEquals(2, c.eventCount(ETERNAL));
            assertEq(x, c.term());
        }
    }

    @Test
    void testDisj1() {

        assertEq("((||,x,y)&&a)", "(||,(a && x),(a && y))");
        assertEq("((||,x,y)&&a)", "--(--(a && x) && --(a && y))");
        assertEq("(--,((||,x,y)&&a))", "(--(a && x) && --(a && y))");

        assertEq("x", "(||,(a && x),(--a && x))");
        assertEq("x", "--(--(a && x) && --(--a && x))");
    }

    @Test
    void testConjOR() {
        Term c = $$("(((_1,_2)&|(_1,_3)) &&+2 ((_1,_2)&|(_1,_3)))");
        assertFalse(c.OR(x -> x instanceof Ellipsis));
    }

    @Test
    void testDisjuncSeq1() {


        //simple case of common event
        Term c = $$("(||,(a &&+1 b),(a &&+2 b))");
        assertEq("(a &&+1 (--,((--,b) &&+1 (--,b))))", c);
    }

    @Test
    void testBalancing() {

        //Term ct = Conj.conjSeqFinal(Op.terms, 590, $$("((--,(left &&+380 (--,left)))&|(--,left))"), $$("(--,(left &&+280 (--,left)))"));


        String as = "(((#1 &&+3080 #1) &&+16040 (#1 &&+1100 (--,((--,#1) &&+3580 (--,#1))))) &&+6540 (--,#1))";
        Term a = $$(as);
        String bs = "(((#1 &&+3080 #1) &&+16040 #1) &&+1100 ((--,((--,#1) &&+3580 (--,#1))) &&+6540 (--,#1)))";
        Term b = $$(bs);

        assertEq(a, b);
        a.printRecursive();
        assertEquals(bs, a.toString());
        assertEquals(bs, b.toString());
    }



//    @Test void testConjEternalConj2() {
//        Term a = $$("(--,((--,((--,y)&|x))&&x))"); //<-- should not be constructed
//        Term b = $$("(x &&+5480 (--,(z &&+5230 (--,x))))");
//        Term ab = CONJ.the(a, b);
//        assertTrue(ab.equals(Null) || ab.equals(False));
//    }

    static void assertUnifies(String x, String y, boolean unifies) {
        Random rng = new XoRoShiRo128PlusRandom(1);
        assertEquals(unifies, $$(x).unify($$(y), new SubUnify(rng)));
    }

    //TODO
//    @Test void testSubTimes() {
//        $$("(((--,_1) &&+22 _2) &&+2 (--,_2))").subTimes()
//    }

    static final Term x = $.the("x");
    static final Term y = $.the("y");
    static final Term z = $.the("z");
    static final Term a = $.the("a");
    static final Term b = $.the("b");
    private final Random rng = new XoRoShiRo128PlusRandom(1);
    private final NAR n = NARS.shell();

    @Deprecated
    static Term conj(FasterList<LongObjectPair<Term>> events) {
        int eventsSize = events.size();
        switch (eventsSize) {
            case 0:
                return True;
            case 1:
                return events.get(0).getTwo();
        }

        ConjBuilder ce = new Conj(eventsSize);

        for (LongObjectPair<Term> o : events) {
            if (!ce.add(o.getOne(), o.getTwo())) {
                break;
            }
        }

        return ce.term();
    }

    @Test
    public void factorizeInProductsTest() {
        /* https://github.com/Horazon1985/ExpressionBuilder/blob/master/test/logicalexpression/computationtests/GeneralLogicalTests.java#L109 */
        //LogicalExpression logExpr = LogicalExpression.build("(a|b)&(a|c)&x&(a|d)");
        //LogicalExpression expectedResult = LogicalExpression.build("(a|b&c&d)&x");
        assertEq("((||,(&&,b,c,d),a)&&x)", "((( (a||b) && (a||c)) && x) && (a||d))");
    }


    @Test void testFactorizeDNF() {
        //https://www.wolframalpha.com/input/?i=not+(a+and+b)++and+not+(a+and+c)
        assertEq("(--,((||,b,c)&&a))" , "(--(a && b) && --(a && c))");
    }

    @Test void test_Not_A_Sequence() {
        Term x = $$("(((--,((--,believe(z,rotate)) &&+4680 (--,believe(z,rotate))))&|(--,left))&&(right &&+200 (--,right)))");
        assertTrue(Conj.isSeq(x));
        assertEquals(1, Conj.seqEternalComponents(x.subterms()).cardinality());
    }

    @Test void notFactoredSequences() {
        assertEquals(1,
            $$("((--,(#1 &&+74 #1))&&(#1 &&+200 #1))").eventList().size());

        assertEquals(1,
            $$("((--,((_2-->_3) &&+6800 (_2-->_3)))&&(_1-->_2))").eventList().size());

        //TODO
        //                                                                                              ^         ^
        // (((--,(((--,(((--,x) &&+120 ((--,x)&|#1)) &&+9 #1))&|right) &&+300 (--,(#1 &&+90 (--,x)))))&|x)&&(((--,x)&|#2) &&+220 #2))
        // ((&|,(((--,(#1,#2))&(--,(#2,#2)))-->_1),_3(#1,#2,_2),(--,_4))&&((_4 &&+140 _3(#4,#5,_2)) &&+600 _3(#4,#5,_2)))
    }

}

