package nars.term;

import nars.$;
import nars.NAL;
import nars.Narsese;
import nars.Op;
import nars.io.NarseseTest;
import nars.term.atom.Atomic;
import nars.term.atom.IdempotentBool;
import nars.term.util.SetSectDiff;
import nars.term.util.TermTest;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.*;
import static nars.term.atom.IdempotentBool.False;
import static nars.term.atom.IdempotentBool.Null;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 12/10/15.
 */
public class TermReductionsTest extends NarseseTest {

    private static final @Nullable Term p = Atomic.the("P");
    private static final @Nullable Term q = Atomic.the("Q");
    private static final @Nullable Term r = Atomic.the("R");
    private static final @Nullable Term s = Atomic.the("S");


    @Test
    void testInterCONJxtReduction1() {

        assertEquals("(&&,P,Q,R)", CONJ.the(r, CONJ.the(p, q)).toString());
        assertEq("(&&,P,Q,R)", "(&&,R,(&&,P,Q))");
    }

    @Test
    void testInterCONJxtReduction2() {

        assertEquals("(&&,P,Q,R,S)", CONJ.the(CONJ.the(p, q), CONJ.the(r, s)).toString());
        assertEq("(&&,P,Q,R,S)", "(&&,(&&,P,Q),(&&,R,S))");
    }

    @Test
    void testInterCONJxtReduction3() {

        assertEq("(&&,P,Q,R,S,T,U)", "(&&,(&&,P,Q),(&&,R,S),(&&,T,U))");
    }

    @Test
    void testInterCONJxtReduction2_1() {

        assertEq("(&&,P,Q,R)", "(&&,R,(&&,P,Q))");
    }

    @Test void embeddedSetDontFlatten() {
        Term a = INSTANCE.$$("1"), b = INSTANCE.$$("2"), c = INSTANCE.$$("3");
        assertEq("{{1,2},3}", SETe.the(SETe.the(a, b), c));
    }

    @Test
    void testInterCONJntReduction1() {

        //assertEquals("(||,P,Q,R)", CONJ.the(r, CONJ.the(p, q)).toString());
        assertEq("(||,P,Q,R)", "(||,R,(||,P,Q))");
    }

    @Test
    void testInterCONJntReduction2() {

        //assertEquals("(||,P,Q,R,S)", CONJ.the(CONJ.the(p, q), CONJ.the(r, s)).toString());
        assertEq("(||,P,Q,R,S)", "(||,(||,P,Q),(||,R,S))");
    }

    @Test
    void testInterCONJntReduction3() {

        assertEq("(||,P,Q,R)", "(||,R,(||,P,Q))");
    }



    @Test void testAllowEternalImplRecursive() {
        assertEq("(x ==>+1 (x-->y))", "(x ==>+1 (x-->y))"); //OK, temporal

        assertEq("(x==>(x-->y))", "(x ==> (x-->y))"); //OK, eternal/parallel
    }

    @Test
    void testCyclicalNAL1_and_NAL2() {


        TermTest.assertInvalidTerms("((#1~swan)-->#1)");
        TermTest.assertInvalidTerms(
                "((swimmer~swan)-->swimmer)",
                "((x|y)-->x)",
                "((x&y)-->x)",
                "(x-->(x|y))",
                "(x-->(x&y))",
                "(y<->(x|y))",
                "(y<->(x&y))",
                "(#1<->(#1|y))"
        );
    }


    @Test
    void testInterCONJntReduction_to_one() {
        for (String o : new String[] { "||", "&&" }) {
            assertEq("(robin-->bird)", "(robin-->(" + o + ",bird))");
            assertEq("(robin-->bird)", String.join(o, "((", ",robin)-->(", ",bird))"));
        }
    }

    @Test
    void testFunctionRecursion() throws Narsese.NarseseException {

        assertTrue(INSTANCE.$("task((polarize(%1,task) ==>+- polarize(%2,belief)))").subs() > 0);
    }




    @Test
    void testImplicationTrue2() {
        assertEq(IdempotentBool.True, "((&&,a,b) ==> a)");
    }
    @Test
    void testImplicationTrue3() {



        assertEq(IdempotentBool.True, "((&&,x1,$1) ==> $1)");
    }

    @Test
    void testImplicationNegatedPredicate() {
        assertEq("(--,(P==>Q))", "(P==>(--,Q))");
        assertEq("((--,P)==>Q)", "((--,P)==>Q)");
    }

    @Test
    void testIntExtEqual() {
        assertEquals(p, CONJ.the(p, p));
    }

    @Test
    void testDiffEqual() {

        assertEquals(False, $.INSTANCE.diff(p, p));
        assertEquals(False, $.INSTANCE.diff(p.neg(), p.neg()));
    }


    @Test
    void testDifferenceSorted() {


        assertArrayEquals(
                new Term[]{r, s},
                ((Compound)SetSectDiff.differenceSet(Op.SETe, SETe.the(r, p, q, s), SETe.the(p, q)))
                    .arrayClone()
        );
    }

    @Test
    void testDifferenceSortedEmpty() {


        assertEquals(
                Null,
                SetSectDiff.differenceSet(Op.SETe, SETe.the(p, q), SETe.the(p, q))
        );
    }


    @Test
    void testDifference() throws Narsese.NarseseException {


        assertEquals(
                INSTANCE.$("{Mars,Venus}"),
                SetSectDiff.differenceSet(Op.SETe, INSTANCE.$("{Mars,Pluto,Venus}"), $.INSTANCE.<Compound>$("{Pluto,Saturn}"))
        );
        assertEquals(
                INSTANCE.$("{Saturn}"),
                SetSectDiff.differenceSet(Op.SETe, INSTANCE.$("{Pluto,Saturn}"), $.INSTANCE.<Compound>$("{Mars,Pluto,Venus}"))
        );


    }


    @Test
    void testDifferenceImmediate() throws Narsese.NarseseException {

        Term d = $.INSTANCE.diff(SETi.the(new Term[] { INSTANCE.$("a"), INSTANCE.$("b"), INSTANCE.$("c") }),
                SETi.the(new Term[] { INSTANCE.$("d"), INSTANCE.$("b")}));
        assertEquals(Op.SETi, d.op());
        assertEquals(2, d.subs());
        assertEquals("[a,c]", d.toString());
    }

    @Test
    void testDifferenceImmediate2() throws Narsese.NarseseException {


        Term a = SETe.the(new Term[] { INSTANCE.$("a"), INSTANCE.$("b"), INSTANCE.$("c") });
        Term b = SETe.the(new Term[] { INSTANCE.$("d"), INSTANCE.$("b") });
        Term d = $.INSTANCE.diff(a, b);
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
        assertEq("(a&&b)", "(a &&+0 b)");
        assertEquals(
                INSTANCE.$("((--,(ball_left)) &&-270 (ball_right))"),
                INSTANCE.$("((ball_right) &&+270 (--,(ball_left)))"));

    }

    @Test
    void testConjunctionParallelWithConjunctionParallel() {
        assertEq("(&&,nario(13,27),nario(21,27),nario(24,27))", "((nario(21,27)&|nario(24,27))&|nario(13,27))");
    }

    @Test
    void testTemporalConjunctionReduction2() {
        assertEq("((a&&b) &&+1 c)", "(a &&+0 (b &&+1 c))");
    }

    @Test
    void testTemporalConjunctionReduction3() {
        assertEq("(a&&b)", "( (a &&+0 b) && (a &&+0 b) )");
    }

    @Test
    void testTemporalConjunctionReduction4() {
        assertEq("(a&&b)", "( a &&+0 (b && b) )");
    }


    @Test
    void testTemporalNTermConjunctionParallel() {


        assertEq("(&&,a,b,c)", "( a &&+0 (b &&+0 c) )");
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
                INSTANCE.$("(&&,r,s)"),
                INSTANCE.$("(&&,s,r)"));


    }

    @Test
    void testImplicationTrue() {
        assertEq(False, "(--x==>x)");
        assertEq(False, "(--x=|>x)");
        assertEq(IdempotentBool.True, "(x==>x)");
        assertEq(IdempotentBool.True, "(x=|>x)");
        assertEq(IdempotentBool.True, "((x)==>(x))");
        assertEq(False, "(--(x)==>(x))");
    }

    @Test
    void testImplicationInequality() throws Narsese.NarseseException {

        assertNotEquals(
                INSTANCE.$("<r ==> s>"),
                INSTANCE.$("<s ==> r>"));


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
        TermTest.assertInvalidTerms("((&&, a, b, c) ==> (&&, a, b))");
        assertEq("((a&&b)==>c)", "((&&, a, b) ==> (&&, a, b, c))");
        assertEq(IdempotentBool.True, "((&&, a, b, c) ==> a)");

        assertEq("(a==>(b&&c))", "(a ==> (&&, a, b, c))");
    }




    @Test
    void testRepeatInverseEquivalent() throws Narsese.NarseseException {
        assertEquals(INSTANCE.$("(x &&-1 x)"), INSTANCE.$("(x &&+1 x)"));
        assertEquals(INSTANCE.$("(x =|> x)"), INSTANCE.$("(x =|> x)"));
        assertEquals(IdempotentBool.True, INSTANCE.$("(x =|> x)"));
        assertEquals(INSTANCE.$("x"), INSTANCE.$("(x &| x)"));
    }


    @Test
    void testDisallowInhAndSimBetweenTemporallySimilarButInequalTerms() {


        assertEq(Null, "((x &&+1 y)<->(x &&+10 y))");
        assertEq(Null, "((y &&+10 x)<->(x &&+1 y))");
        assertEq(Null, "((x=|>y)-->(x ==>-10 y))");
    }


        @Test
        void distinctSimNegationStatements() throws Narsese.NarseseException {
            if (!NAL.term.INH_CLOSED_BOOLEAN_DUALITY_MOBIUS_PARADIGM) {
                assertEq(IdempotentBool.True, "(a<->a)");

                assertNotEquals(INSTANCE.$("(--a <-> b)"), INSTANCE.$("(a <-> --b)"));

                assertEq("((--,a)<->b)", "((--,a) <-> b)");
                assertNotEquals("(a<->b)", INSTANCE.$("((--,a) <-> b)").toString());
                assertEq("((--,a)<->b)", "(b <-> (--,a))");
                assertNotEquals("(a<->b)", INSTANCE.$("(b <-> (--,a))").toString());
                assertEq("((--,a)<->(--,b))", "(--a <-> --b)");

//        assertEq("((--,a)<->a)", "((--,a)<->a)");
            }
        }

    @Disabled static class StructuralMobius {

        @Test
        void testAllowInhNegationStatements() throws Narsese.NarseseException {
            assertEq(IdempotentBool.True, "(a-->a)");

            assertEq("((--,a)-->b)", "((--,a) --> b)");
            assertNotEquals("(a-->b)", INSTANCE.$("((--,a) --> b)").toString());
            assertEq("(b-->(--,a))", "(b --> (--,a))");
            assertNotEquals("(a-->b)", INSTANCE.$("(b --> (--,a))").toString());
            assertEq("((--,a)-->(--,b))", "(--a --> --b)");

            assertEq(Null /*"((--,a)-->a)"*/, "((--,a)-->a)");
            assertEq(Null /*"(a-->(--,a))"*/, "(a-->(--,a))");

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
    }


    @Test
    void testImplCommonSubterms() {

        assertEq("(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))==>((y-->x)))", "(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))==>((--,(($1,xyz)-->$2))&&(x:y)))");
    }




    @Test
    void testConjInImplicationTautology() {

        assertEq(IdempotentBool.True, "((x &&+2 x) ==>-2 x)");
    }

    @Test
    void testConjInImplicationTautology2() {
        assertEq(IdempotentBool.True, "((((_1,_2)&|(_1,_3)) &&+2 ((_1,_2)&|(_1,_3))) ==>-2 ((_1,_2)&|(_1,_3)))");
    }






    @Test
    void testImplCommonSubterms2() {
        assertEq(IdempotentBool.True, "((tetris(isRowClear,7,true)&&tetris(7,14))==>tetris(7,14))");


        assertEq(IdempotentBool.True, "((tetris(isRowClear,7,true)&&tetris(7,14))=|>tetris(7,14))");

        assertEq("((tetris(isRowClear,7,true)&&tetris(7,14)) ==>+10 tetris(7,14))",
                "((tetris(isRowClear,7,true)&&tetris(7,14)) ==>+10 tetris(7,14))");
    }

    @Test
    void testImplCommonSubterms3() {

        assertEq(IdempotentBool.True, "((x(intValue,(),0)&&x(setAt,0))==>x(intValue,(),0))");
        assertEq("x(setAt,0)", "((x(intValue,(),0)==>x(intValue,(),0)) && x(setAt,0))");
        assertEq("((x(setAt,0)==>x(intValue,(),0))&&x(intValue,(),0))",
                "((x(setAt,0)==>x(intValue,(),0)) && x(intValue,(),0))");

    }



    @Test
    void testCoNegatedDifference() {


//        {
//            NAR n = NARS.shell();
//            n.believe("X", 1.0f, 0.9f);
//            n.believe("Y", 0.5f, 0.9f);
//            tryDiff(n, "(X~Y)", "%.50;.81%");
//            tryDiff(n, "((--,Y)~(--,X))", "%.50;.81%");
//            tryDiff(n, "(Y~X)", "%0.0;.81%");
//            tryDiff(n, "((--,X)~(--,Y))", "%0.0;.81%");
//        }
//        {
//            NAR n = NARS.shell();
//            n.believe("X", 1.0f, 0.9f);
//            n.believe("Y", 0.75f, 0.9f);
//            tryDiff(n, "(X~Y)", "%.25;.81%");
//
//            tryDiff(n, "((--,Y)~(--,X))", "%.25;.81%");
//
//            tryDiff(n, "(Y~X)", "%0.0;.81%");
//            tryDiff(n, "((--,X)~(--,Y))", "%0.0;.81%");
//        }

        assertEq("((--,Y)&&X)", "(X-Y)");
        assertEq("(X||(--,Y))", "(X~Y)");



//        assertEq("(Y~X)", "((--,X)~(--,Y))");
//        assertEq("(X~Y)", "((--,Y)~(--,X))");

//        assertEq("(A-->(Y-X))", "(A-->((--,X)~(--,Y)))");
//        assertEq("(A-->(Y-X))", "(A-->((--,X)-(--,Y)))");
//
//        assertEq("((Y-X)-->A)", "(((--,X)-(--,Y))-->A)");
//        assertEq("((Y~X)-->A)", "(((--,X)~(--,Y))-->A)");

    }

//    private void tryDiff(NAR n, String target, String truthExpected) throws Narsese.NarseseException {
//        assertEquals(truthExpected, n.beliefTruth(target, ETERNAL).toString(), target::toString);
//
//    }





    @Test
    void testOneArgInterCONJon() throws Narsese.NarseseException {
        Term x = $.INSTANCE.p($.INSTANCE.the("x"));
        assertEquals(x, INSTANCE.$("(||,(x))"));
        assertEquals(x, INSTANCE.$("(||,(x),(x))"));
        assertEquals(x, INSTANCE.$("(&&,(x))"));
        assertEquals(x, INSTANCE.$("(&&,(x),(x))"));
    }

    @Test
    void testCoNegatedInterCONJonAndDiffs() {
        TermTest.assertInvalidTerms("(||,(x),(--,(x))");
        TermTest.assertInvalidTerms("(&&,(x),(--,(x))");
        TermTest.assertInvalidTerms("(-,(x),(--,(x))");
        TermTest.assertInvalidTerms("(~,(x),(--,(x))");
        TermTest.assertInvalidTerms("(-,(x),(x))");
    }




    @Test
    void taskWithFlattenedConunctions() throws Narsese.NarseseException {


        Term x = INSTANCE.$("((hear(what)&&(hear(is)&&(hear(is)&&(hear(what)&&(hear(is)&&(hear(is)&&(hear(what)&&(hear(is)&&(hear(is)&&(hear(is)&&hear(what))))))))))) ==>+153 hear(is)).");
        assertEq("((hear(is)&&hear(what)) ==>+153 hear(is))",
                x.toString());

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
        Term a = INSTANCE.$("((a,b) ==>+1 (b,c))");
        Term b = INSTANCE.$("(c,d)");
        Term x = Op.CONJ.the(4, a, b);

        assertEquals(

                "(((a,b) ==>+1 (b,c)) &&+4 (c,d))",
                x.toString());
    }

    @Test
    void testConjImplNonReductionNegConj() throws Narsese.NarseseException {
        Term a = INSTANCE.$("((a,b) ==>+1 (b,c))");
        Term b = INSTANCE.$("(c,d)");
        Term x = Op.CONJ.the(-4, a, b);

        assertEquals(
                "((c,d) &&+4 ((a,b) ==>+1 (b,c)))",

                x.toString());
    }

    @Test
    void testConjImplReductionNegConj2() throws Narsese.NarseseException {
        Term b = INSTANCE.$("(c,d)");
        Term a = INSTANCE.$("((a,b) ==>+1 (b,c))");
        Term x = Op.CONJ.the(4, b, a);

        assertEquals(

                "((c,d) &&+4 ((a,b) ==>+1 (b,c)))",
                x.toString());
    }

    @Test
    void testConjImplNonReductionNegConj2() throws Narsese.NarseseException {
        Term a = INSTANCE.$("((a,b) ==>+1 (b,c))");
        Term b = INSTANCE.$("(c &&+1 d)");
        Term x = Op.CONJ.the(-4, a, b);

        assertEquals(

                "((c &&+1 d) &&+4 ((a,b) ==>+1 (b,c)))",
                x.toString());
    }

    @Test
    void testConjImplNonReductionNegConj3() throws Narsese.NarseseException {
        Term a = INSTANCE.$("((a,b) ==>+1 (b,c))");
        Term b = INSTANCE.$("(c &&+1 d)");
        Term x = Op.CONJ.the(+4, a, b);

        assertEquals(

                "((((a,b) ==>+1 (b,c)) &&+4 c) &&+1 d)",
                x.toString());

        Term x2 = Op.terms.conjAppend(a, 4, b);
        assertEquals(x, x2);
    }

    @Test
    void testConjImplReductionNegConj2b() throws Narsese.NarseseException {
        Term b = INSTANCE.$("(c,d)");
        Term a = INSTANCE.$("((a,b) ==>-1 (b,c))");
        Term x = Op.CONJ.the(4, b, a);

        assertEquals(

                "((c,d) &&+4 ((a,b) ==>-1 (b,c)))",
                x.toString());
    }

    @Test
    void testConjImplReductionNegImpl() throws Narsese.NarseseException {
        Term a = INSTANCE.$("((a,b) ==>-1 (b,c))");
        Term b = INSTANCE.$("(c,d)");
        Term x = Op.CONJ.the(4, a, b);

        assertEquals(

                "(((a,b) ==>-1 (b,c)) &&+4 (c,d))",
                x.toString());
    }

    @Test
    void testConjImplReductionWithVars() throws Narsese.NarseseException {
        Term a = INSTANCE.$("((a,#1) ==>+1 (#1,c))");
        Term b = INSTANCE.$("(c,d)");
        Term x = Op.CONJ.the(4, a, b);

        assertEquals(

                "(((a,#1) ==>+1 (#1,c)) &&+4 (c,d))",
                x.toString());
    }

    @Test
    void testConjImplReduction1() {
        assertEq(

                "((inside(john,playground)==>inside(bob,kitchen))&&inside(bob,office))",
                "(inside(bob,office)&&(inside(john,playground)==>inside(bob,kitchen)))");
    }

    @Test
    void testConjImplReduction2() throws Narsese.NarseseException {


        Term t = INSTANCE.$("(inside(bob,office) &&+1 (inside(john,playground) ==>+1 inside(bob,kitchen)))");

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
