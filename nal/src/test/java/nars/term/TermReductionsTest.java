package nars.term;

import nars.*;
import nars.io.NarseseTest;
import nars.task.util.TaskException;
import nars.term.atom.Atomic;
import nars.term.util.Conj;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.*;
import static nars.term.TermTest.*;
import static nars.time.Tense.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 12/10/15.
 */
public class TermReductionsTest extends NarseseTest {

    @Nullable
    private final static Term p = Atomic.the("P");
    @Nullable
    private final static Term q = Atomic.the("Q");
    @Nullable
    private final static Term r = Atomic.the("R");
    @Nullable
    private final static Term s = Atomic.the("S");


    @Test
    void testIntersectExtReduction1() {

        assertEquals("(&,P,Q,R)", SECTe.the(r, SECTe.the(p, q)).toString());
        assertEq("(&,P,Q,R)", "(&,R,(&,P,Q))");
    }

    @Test
    void testIntersectExtReduction2() {

        assertEquals("(&,P,Q,R,S)", SECTe.the(SECTe.the(p, q), SECTe.the(r, s)).toString());
        assertEq("(&,P,Q,R,S)", "(&,(&,P,Q),(&,R,S))");
    }

    @Test
    void testIntersectExtReduction3() {

        assertEq("(&,P,Q,R,S,T,U)", "(&,(&,P,Q),(&,R,S), (&,T,U))");
    }

    @Test
    void testIntersectExtReduction2_1() {

        assertEq("(&,P,Q,R)", "(&,R,(&,P,Q))");
    }

    @Test
    void testIntersectExtReduction4() {

        assertEquals("{P,Q,R,S}", SECTe.the(SETe.the(p, q), SETe.the(r, s)).toString());
        assertEq("{P,Q,R,S}", "(&,{P,Q},{R,S})");
    }

    @Test
    void testIntersectExtReduction5() {
        assertEquals(Null /* emptyset */, SECTe.the(SETi.the(p, q), SETi.the(r, s)));
    }

    @Test
    void testIntersectIntReduction1() {

        assertEquals("(|,P,Q,R)", SECTi.the(r, SECTi.the(p, q)).toString());
        assertEq("(|,P,Q,R)", "(|,R,(|,P,Q))");
    }

    @Test
    void testIntersectIntReduction2() {

        assertEquals("(|,P,Q,R,S)", SECTi.the(SECTi.the(p, q), SECTi.the(r, s)).toString());
        assertEq("(|,P,Q,R,S)", "(|,(|,P,Q),(|,R,S))");
    }

    @Test
    void testIntersectIntReduction3() {

        assertEq("(|,P,Q,R)", "(|,R,(|,P,Q))");
    }

    @Test
    void testIntersectIntReduction4() {

        assertEquals("[P,Q,R,S]", SECTi.the(SETi.the(p, q), SETi.the(r, s)).toString());
        assertEq("[P,Q,R,S]", "(|,[P,Q],[R,S])");

    }

    @Test
    void testCyclicalNAL1_and_NAL2() {

        assertInvalidTerms("((#1~swan)-->#1)");
        assertInvalidTerms(
                "((swimmer~swan)-->swimmer)",
                "((x|y)-->x)",
                "(y<->(x|y))",
                "(#1<->(#1|y))"
        );
    }

    @Test
    void testIntersectIntReductionToZero() {
        assertInvalidTerms("(|,{P,Q},{R,S})");
    }

    @Test
    void testIntersectIntReduction_to_one() {
        assertEq("(robin-->bird)", "<robin-->(|,bird)>");
        assertEq("(robin-->bird)", "<(|,robin)-->(|,bird)>");
    }

    @Test
    void testFunctionRecursion() throws Narsese.NarseseException {

        assertTrue($("task((polarize(%1,task) ==>+- polarize(%2,belief)))").subs() > 0);
    }


    @Test
    void testSimilarityNegatedSubtermsDoubleNeg() {
        assertEq("((--,(P))<->(--,(Q)))", "((--,(P))<->(--,(Q)))");
        /*
        <patham9> <-> is a relation in meaning not in truth
        <patham9> so negation can't enforce any equivalence here
        */
    }

    @Test
    void testSimilarityNegatedSubterms() {
        assertEq("((--,(Q))<->(P))", "((P)<->(--,(Q)))");
        assertEq("((--,(P))<->(Q))", "((--,(P))<->(Q))");
    }


    @Test
    void testImplicationTrue2() {
        assertEq(True, "((&&,a,b) ==> a)");
    }
    @Test
    void testImplicationTrue3() {



        assertEq(True, "((&&,x1,$1) ==> $1)");
    }

    @Test
    void testImplicationNegatedPredicate() {
        assertEq("(--,((P)==>(Q)))", "((P)==>(--,(Q)))");
        assertEq("((--,(P))==>(Q))", "((--,(P))==>(Q))");
    }

    @Test
    void testConjInhReflexive() {
        assertEq("((a &&+5 x)-->a)", "((a &&+5 x)-->a)");
        assertEq("(x-->(a &&+5 x))", "(x-->(a &&+5 x))");
        assertEq("((a&&b)-->(a&&c))", "((a&&b)-->(a&&c))");
    }



    @Test
    void testIntExtEqual() {
        assertEquals(p, SECTe.the(p, p));
        assertEquals(p, SECTi.the(p, p));
    }

    @Test void testInvalidDiff_Concept_involving_ConjSeq() {
        assertEq(Null, "(((y &&+1 x) &&+1 y)~(x~(y &&+- x)))");
    }
    @Test
    void testDiffIntEqual() {

        assertEquals(False, DIFFi.the(p, p));
    }

    @Test
    void testDiffExtEqual() {

        assertEquals(False, DIFFe.the(p, p));
    }


    @Test
    void testDifferenceSorted() {


        assertArrayEquals(
                new Term[]{r, s},
                Op.differenceSet(Op.SETe, SETe.the(r, p, q, s), SETe.the(p, q)).arrayClone()
        );
    }

    @Test
    void testDifferenceSortedEmpty() {


        assertEquals(
                Null,
                Op.differenceSet(Op.SETe, SETe.the(p, q), SETe.the(p, q))
        );
    }


    @Test
    void testDifference() throws Narsese.NarseseException {


        assertEquals(
                $("{Mars,Venus}"),
                Op.differenceSet(Op.SETe, $("{Mars,Pluto,Venus}"), $.<Compound>$("{Pluto,Saturn}"))
        );
        assertEquals(
                $("{Saturn}"),
                Op.differenceSet(Op.SETe, $("{Pluto,Saturn}"), $.<Compound>$("{Mars,Pluto,Venus}"))
        );


    }


    @Test
    void testDifferenceImmediate() throws Narsese.NarseseException {

        Term d = DIFFi.the(SETi.the($("a"), $("b"), $("c")), SETi.the($("d"), $("b")));
        assertEquals(Op.SETi, d.op());
        assertEquals(2, d.subs());
        assertEquals("[a,c]", d.toString());
    }

    @Test
    void testDifferenceImmediate2() throws Narsese.NarseseException {


        Term a = SETe.the($("a"), $("b"), $("c"));
        Term b = SETe.the($("d"), $("b"));
        Term d = DIFFe.the(a, b);
        assertEquals(Op.SETe, d.op());
        assertEquals(2, d.subs());
        assertEquals("{a,c}", d.toString());

    }

    @Test
    void testDisjunctionReduction() {

        assertEq("(||,(a-->x),(b-->x),(c-->x),(d-->x))", "(||,(||,x:a,x:b),(||,x:c,x:d))");
        assertEq("(||,(b-->x),(c-->x),(d-->x))", "(||,x:b,(||,x:c,x:d))");
    }

    @Test
    void testConjunctionReduction() {
        assertEq("(&&,a,b,c,d)", "(&&,(&&,a,b),(&&,c,d))");
        assertEq("(&&,b,c,d)", "(&&,b,(&&,c,d))");
    }

    @Test
    void testTemporalConjunctionReduction1() throws Narsese.NarseseException {
        assertEq("(a&|b)", "(a &&+0 b)");
        assertEquals(
                $("((--,(ball_left)) &&-270 (ball_right))"),
                $("((ball_right) &&+270 (--,(ball_left)))"));

    }

    @Test
    void testConjunctionParallelWithConjunctionParallel() {
        assertEq("(&|,nario(13,27),nario(21,27),nario(24,27))", "((nario(21,27)&|nario(24,27))&|nario(13,27))");
    }

    @Test
    void testTemporalConjunctionReduction2() {
        assertEq("((a&|b) &&+1 c)", "(a &&+0 (b &&+1 c))");
    }

    @Test
    void testTemporalConjunctionReduction3() {
        assertEq("(a&|b)", "( (a &&+0 b) && (a &&+0 b) )");
    }

    @Test
    void testTemporalConjunctionReduction4() {
        assertEq("(a&|b)", "( a &&+0 (b && b) )");
    }


    @Test
    void testTemporalNTermConjunctionParallel() {


        assertEq("(&|,a,b,c)", "( a &&+0 (b &&+0 c) )");
    }

    @Disabled
    @Test
    void testTemporalNTermEquivalenceParallel() {

        assertEq("(<|>, a, b, c)", "( a <|> (b <|> c) )");
    }


    @Test
    void testMultireduction() {

    }

    @Test
    void testConjunctionMultipleAndEmbedded() {

        assertEq("(&&,a,b,c,d)", "(&&,(&&,a,b),(&&,c,d))");
        assertEq("(&&,a,b,c,d,e,f)", "(&&,(&&,a,b),(&&,c,d), (&&, e, f))");
        assertEq("(&&,a,b,c,d,e,f,g,h)", "(&&,(&&,a,b, (&&, g, h)),(&&,c,d), (&&, e, f))");
    }

    @Test
    void testConjunctionEquality() throws Narsese.NarseseException {

        assertEquals(
                $("(&&,r,s)"),
                $("(&&,s,r)"));


    }

    @Test
    void testImplicationTrue() {
        assertEq(False, "(--x==>x)");
        assertEq(True, "(x==>x)");
        assertEq(True, "((x)==>(x))");
        assertEq(False, "(--(x)==>(x))");
    }

    @Test
    void testImplicationInequality() throws Narsese.NarseseException {

        assertNotEquals(
                $("<r ==> s>"),
                $("<s ==> r>"));


    }

    @Test
    void testDisjunctionMultipleAndEmbedded() {

        assertEq("(||,(a),(b),(c),(d))", "(||,(||,(a),(b)),(||,(c),(d)))");
        assertEq("(||,(a),(b),(c),(d),(e),(f))", "(||,(||,(a),(b)),(||,(c),(d)), (||,(e),(f)))");
        assertEq("(||,(a),(b),(c),(d),(e),(f),(g),(h))", "(||,(||,(a),(b), (||,(g),(h))),(||,(c),(d)), (||,(e),(f)))");

    }

    @Test
    void testImplicationConjCommonSubterms() {
        assertEq("((&&,a,b,c)==>d)", "((&&, a, b, c) ==> (&&, a, d))");
        assertEq("((a&&d)==>(b&&c))", "((&&, a, d) ==> (&&, a, b, c))");
        assertInvalidTerms("((&&, a, b, c) ==> (&&, a, b))");
        assertEq("((a&&b)==>c)", "((&&, a, b) ==> (&&, a, b, c))");
        assertEq(True, "((&&, a, b, c) ==> a)");

        assertEq("(a==>(b&&c))", "(a ==> (&&, a, b, c))");
    }


    @Test
    void testInvalidStatementIndepVarTask() {
        NAR t = NARS.shell();
        try {
            t.inputTask("at($1,$2,$3).");
            fail("");
        } catch (Narsese.NarseseException | TaskException e) {
            assertTrue(true);
        }
    }


    @Test
    void testRepeatInverseEquivalent() throws Narsese.NarseseException {
        assertEquals($("(x &&-1 x)"), $("(x &&+1 x)"));
        assertEquals($("(x =|> x)"), $("(x =|> x)"));
        assertEquals(True, $("(x =|> x)"));
        assertEquals($("x"), $("(x &| x)"));
    }


    @Test
    void testDisallowInhAndSimBetweenTemporallySimilarButInequalTerms() {


        assertEq(Null, "((x &&+1 y)<->(x &&+10 y))");
        assertEq(Null, "((y &&+10 x)<->(x &&+1 y))");
        assertEq(Null, "((x=|>y)-->(x ==>-10 y))");
    }

    @Test
    void testAllowInhNegationStatements() throws Narsese.NarseseException {
        assertEq(True, "(a-->a)");

        assertEq("((--,a)-->b)", "((--,a) --> b)");
        assertNotEquals("(a-->b)", $("((--,a) --> b)").toString());
        assertEq("(b-->(--,a))", "(b --> (--,a))");
        assertNotEquals("(a-->b)", $("(b --> (--,a))").toString());
        assertEq("((--,a)-->(--,b))", "(--a --> --b)");

        assertEq("((--,a)-->a)", "((--,a)-->a)");
        assertEq("(a-->(--,a))", "(a-->(--,a))");

    }

    @Test
    void testAllowSimNegationStatements() throws Narsese.NarseseException {
        assertEq(True, "(a<->a)");

        assertNotEquals($("(--a <-> b)"), $("(a <-> --b)"));

        assertEq("((--,a)<->b)", "((--,a) <-> b)");
        assertNotEquals("(a<->b)", $("((--,a) <-> b)").toString());
        assertEq("((--,a)<->b)", "(b <-> (--,a))");
        assertNotEquals("(a<->b)", $("(b <-> (--,a))").toString());
        assertEq("((--,a)<->(--,b))", "(--a <-> --b)");

        assertEq("((--,a)<->a)", "((--,a)<->a)");

    }


    @Test
    void testCoNegatedImpl() {
        assertValidTermValidConceptInvalidTaskContent(("(--x ==> x)."));
        assertValidTermValidConceptInvalidTaskContent(("(--x =|> x)."));
    }


    @Test
    void testImplCommonSubterms() {

        assertEq("(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))==>((y-->x)))", "(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))==>((--,(($1,xyz)-->$2))&&(x:y)))");
    }




    @Test
    void testConjInImplicationTautology() {
        Term x0 = $.$$("((x &&+2 x) ==>-2 x)");
        assertEquals(True, x0);

        Term x = $.$$("((((_1,_2)&|(_1,_3)) &&+2 ((_1,_2)&|(_1,_3))) ==>-2 ((_1,_2)&|(_1,_3)))");
        assertEquals(True, x);
    }

    @Test
    void testCommutizeRepeatingImpl() throws Narsese.NarseseException {

        assertEquals(True,
                $("(a ==>+1 a)").dt(DTERNAL));
        assertEquals(False,
                $("(--a ==>+1 a)").dt(DTERNAL));

        assertEquals(True,
                $("(a ==>+1 a)").dt(0));
        assertEquals(False,
                $("(--a ==>+1 a)").dt(0));


        assertEquals("(a ==>+- a)",
                $("(a ==>+1 a)").dt(XTERNAL).toString());
        assertEquals("((--,a) ==>+- a)",
                $("(--a ==>+1 a)").dt(XTERNAL).toString());
    }

    @Test
    void testImplXternalPredicateImpl() {
        assertEq("((x &&+- y) ==>+1 z)",
                "(x ==>+- (y ==>+1 z))");
        assertEq("(((x &&+1 y) &&+- z) ==>+1 w)",
                "((x &&+1 y) ==>+- (z ==>+1 w))");
    }


    @Test
    void testImplCommonSubterms2() {
        assertEq(True, "((tetris(isRowClear,7,true)&&tetris(7,14))==>tetris(7,14))");


        assertEq(True, "((tetris(isRowClear,7,true)&&tetris(7,14))=|>tetris(7,14))");

        assertEq("((tetris(isRowClear,7,true)&&tetris(7,14)) ==>+10 tetris(7,14))", "((tetris(isRowClear,7,true)&&tetris(7,14)) ==>+10 tetris(7,14))");
    }

    @Test
    void testImplCommonSubterms3() {

        assertEq(True, "((x(intValue,(),0)&&x(set,0))==>x(intValue,(),0))");
        assertEq("x(set,0)", "((x(intValue,(),0)==>x(intValue,(),0)) && x(set,0))");
        assertEq(

                "((x(set,0)==>x(intValue,(),0))&&x(intValue,(),0))",
                "((x(set,0)==>x(intValue,(),0)) && x(intValue,(),0))");

    }

    @Test
    void testCoNegatedImplOK() throws Narsese.NarseseException {
        assertValid($("((--,(a)) ==>+1 (a))"));
        assertValid($("((--,a) ==>+1 a)"));
    }


    @Test
    void testRepeatEvent() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        for (String x : new String[]{
                "((a) ==>+1 (a))",
                "((a) &&+1 (a))",

                /*"((a) &&+1 (a))",*/
        }) {
            Term t = $(x);
            assertTrue(t instanceof Compound, x + " :: " + t);
            assertTrue(t.dt() != DTERNAL);

            Task y = task(t, Op.BELIEF, t(1f, 0.9f)).apply(n);

            y.term().printRecursive();
            assertEquals(x, y.term().toString());

        }


    }


    @Test
    void testCoNegatedDifference() throws Narsese.NarseseException {


        {
            NAR n = NARS.shell();
            n.believe("X", 1.0f, 0.9f);
            n.believe("Y", 0.5f, 0.9f);
            tryDiff(n, "(X~Y)", "%.50;.81%");
            tryDiff(n, "((--,Y)~(--,X))", "%.50;.81%");
            tryDiff(n, "(Y~X)", "%0.0;.81%");
            tryDiff(n, "((--,X)~(--,Y))", "%0.0;.81%");
        }
        {
            NAR n = NARS.shell();
            n.believe("X", 1.0f, 0.9f);
            n.believe("Y", 0.75f, 0.9f);
            tryDiff(n, "(X~Y)", "%.25;.81%");

            tryDiff(n, "((--,Y)~(--,X))", "%.25;.81%");

            tryDiff(n, "(Y~X)", "%0.0;.81%");
            tryDiff(n, "((--,X)~(--,Y))", "%0.0;.81%");
        }
        assertEq("(Y~X)", "((--,X)~(--,Y))");
        assertEq("(X~Y)", "((--,Y)~(--,X))");
        assertEq("((Y~X)-->A)", "(((--,X)~(--,Y))-->A)");
        assertEq("(A-->(Y-X))", "(A-->((--,X)-(--,Y)))");

    }

    private void tryDiff(NAR n, String term, String truthExpected) throws Narsese.NarseseException {
        assertEquals(truthExpected, n.beliefTruth(term, ETERNAL).toString(), term::toString);

    }





    @Test
    void testOneArgIntersection() throws Narsese.NarseseException {
        Term x = $.p($.the("x"));
        assertEquals(x, $("(|,(x))"));
        assertEquals(x, $("(|,(x),(x))"));
        assertEquals(x, $("(&,(x))"));
        assertEquals(x, $("(&,(x),(x))"));
    }

    @Test
    void testCoNegatedIntersectionAndDiffs() {
        assertInvalidTerms("(|,(x),(--,(x))");
        assertInvalidTerms("(&,(x),(--,(x))");
        assertInvalidTerms("(-,(x),(--,(x))");
        assertInvalidTerms("(~,(x),(--,(x))");
        assertInvalidTerms("(-,(x),(x))");
    }




    @Test
    void taskWithFlattenedConunctions() throws Narsese.NarseseException {


        @NotNull Term x = $("((hear(what)&&(hear(is)&&(hear(is)&&(hear(what)&&(hear(is)&&(hear(is)&&(hear(what)&&(hear(is)&&(hear(is)&&(hear(is)&&hear(what))))))))))) ==>+153 hear(is)).");
        assertEquals("((hear(what)&&hear(is)) ==>+153 hear(is))", x.toString());

    }


    /**
     * TODO decide if it should not apply this reduction to eternal
     */
    @Test
    void testConjImplReduction0() {
        assertEq(

                "((inside(john,playground)==>inside(bob,kitchen))&&inside(bob,office))",
                "(inside(bob,office) && (inside(john,playground)==>inside(bob,kitchen)))");
    }

    @Test
    void testConjImplReduction() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(4, new Term[]{a, b});

        assertEquals(

                "(((a,b) ==>+1 (b,c)) &&+4 (c,d))",
                x.toString());
    }

    @Test
    void testConjImplNonReductionNegConj() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(-4, new Term[]{a, b});

        assertEquals(
                "((c,d) &&+4 ((a,b) ==>+1 (b,c)))",

                x.toString());
    }

    @Test
    void testConjImplReductionNegConj2() throws Narsese.NarseseException {
        Term b = $("(c,d)");
        Term a = $("((a,b) ==>+1 (b,c))");
        Term x = Op.CONJ.the(4, new Term[]{b, a});

        assertEquals(

                "((c,d) &&+4 ((a,b) ==>+1 (b,c)))",
                x.toString());
    }

    @Test
    void testConjImplNonReductionNegConj2() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c &&+1 d)");
        Term x = Op.CONJ.the(-4, new Term[]{a, b});

        assertEquals(

                "((c &&+1 d) &&+4 ((a,b) ==>+1 (b,c)))",
                x.toString());
    }

    @Test
    void testConjImplNonReductionNegConj3() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c &&+1 d)");
        {
            Term x = Op.CONJ.the(+4, new Term[]{a, b});

            assertEquals(

                    "((((a,b) ==>+1 (b,c)) &&+4 c) &&+1 d)",
                    x.toString());

            Term x2 = Conj.conjMerge(a, b, 4);
            assertEquals(x, x2);
        }
    }

    @Test
    void testConjImplReductionNegConj2b() throws Narsese.NarseseException {
        Term b = $("(c,d)");
        Term a = $("((a,b) ==>-1 (b,c))");
        Term x = Op.CONJ.the(4, new Term[]{b, a});

        assertEquals(

                "((c,d) &&+4 ((a,b) ==>-1 (b,c)))",
                x.toString());
    }

    @Test
    void testConjImplReductionNegImpl() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>-1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(4, new Term[]{a, b});

        assertEquals(

                "(((a,b) ==>-1 (b,c)) &&+4 (c,d))",
                x.toString());
    }

    @Test
    void testConjImplReductionWithVars() throws Narsese.NarseseException {
        Term a = $("((a,#1) ==>+1 (#1,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(4, new Term[]{a, b});

        assertEquals(

                "(((a,#1) ==>+1 (#1,c)) &&+4 (c,d))",
                x.toString());
    }

    @Test
    void testConjImplReduction1() {
        assertEq(

                "((inside(john,playground)==>inside(bob,kitchen))&|inside(bob,office))",
                "(inside(bob,office)&|(inside(john,playground)==>inside(bob,kitchen)))");
    }

    @Test
    void testConjImplReduction2() throws Narsese.NarseseException {


        Term t = $("(inside(bob,office) &&+1 (inside(john,playground) ==>+1 inside(bob,kitchen)))");

        assertEquals(

                "(inside(bob,office) &&+1 (inside(john,playground) ==>+1 inside(bob,kitchen)))",
                t.toString()
        );
    }

    @Test
    void testConjImplReductionNeg2() {

        assertEq(

                "(inside(bob,office) &&+1 ((--,inside(john,playground)) ==>+1 inside(bob,kitchen)))",
                "(inside(bob,office) &&+1 (--inside(john,playground) ==>+1 inside(bob,kitchen)))");
    }

    @Test
    void testConjImplReduction3() {

        assertEq(

                "((j ==>-1 k) &&+1 b)",
                "((j ==>-1 k) &&+1 b)");

        assertEq(

                "((j ==>-1 k) &&+1 b)",
                "(b &&-1 (j ==>-1 k))");
    }


}
