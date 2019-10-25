package nars.term;

import nars.$;
import nars.Narsese;
import nars.term.atom.IdempotentBool;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.$.*;
import static nars.term.atom.IdempotentBool.False;
import static nars.term.atom.IdempotentBool.Null;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * tests specific to implication compounds TODO
 */
class ImplTest {
    @Test
    void testInvalidImpl1() {
        assertEq(False, "(--y =|> y)");
    }

    @Test
    void testInvalidImpl2() {
        assertEq(False, "(--(x &| y) =|> y)");
    }

    @Test
    void testInvalidImpl3() {
        assertEq(False, "(--(--x &| y) =|> y)");
    }

    @Test
    void testReducibleImplFactored() {
        assertEq("((x&&y)==>z)", "((x &| y) ==> (y &| z))");
    }

//    @Test
//    void toomuchReduction() {
//        /** this took some thought but it really is consistent with the system */
//        assertEq("((b &&+60000 c)=|>(#1 &&+60000 (b&|#1)))",
//                "((b &&+60000 c)=|>((#1 &&+60000 b)&&(c &&+60000 #1)))");
//    }


    @Test
    void testReducibleImplFactored2() {
        assertEq("((x&&y)==>z)", "((y && x) ==> (y && z))");
        assertEq("((&&,a,x,y)==>z)", "((&&, x, y, a) ==> (y && z))");
        assertEq("((y &&+1 x)==>(z &&+1 y))", "((y &&+1 x)==>(z &&+1 y))");
    }

    @Test
    void testReducibleImplFactoredPredShouldRemainIntact() {

        for (String cp : new String[]{ "&&", " &&+- "}) {
            String ccp = cp;//cp.equals(" &&+- ") ? cp : "&|";
            assertEq("((x&&y) ==>+1 (y" + ccp + "z))", "((y&&x) ==>+1 (y" + cp + "z))");
            assertEq("(a ==>+1 (b &&+1 (y" + ccp + "z)))", "(a ==>+1 (b &&+1 (y" + cp + "z)))");
        }


    }

    /** the + and - versions have distinct meanings that must be maintained */
    @Test void TemporalRepeatDoesNotNormalization() {
      assertEq("(x ==>-2 x)", "(x ==>-2 x)");
      assertEq("(x ==>+2 x)", "(x ==>+2 x)");
    }

    @Test
    void testReducibleImpl() {

        assertEq("(--,((--,x)==>y))", "(--x ==> (--y && --x))");

        assertEq("(x==>y)", "(x ==> (y &| x))");
        assertEq(IdempotentBool.True, "((y &| x) ==> x)");
        assertEq("(--,((--,$1)==>#2))", "((--,$1)==>((--,$1)&|(--,#2)))");
    }

    @Test
    void testReducibleImplConjCoNeg() {
        assertEq(False, "((y && --x) ==> x)");

        for (String i : new String[]{"==>"/*, "=|>"*/}) {
            for (String c : new String[]{"&&"}) {
                assertEq(False, "(x " + i + " (y " + c + " --x))");
                assertEq(False, "(--x " + i + " (y " + c + " x))");
                assertEq(False, "((y " + c + " --x) " + i + " x)");
                assertEq(False, "((y " + c + " x) " + i + " --x)");
            }
        }
    }


    @Test
    void testReducibleImplParallelNeg() {
        assertEq("(--,((--,x)==>y))", "(--x ==> (--y && --x))");
    }
    @Test
    void testReducibleImplParallelNeg2() {
        assertEq(IdempotentBool.True, "((--y && --x) ==> --x)");
    }

    @Test
    void testInvalidCircularImpl() throws Narsese.NarseseException {
        assertNotEquals(Null, INSTANCE.$("(x(intValue,(),1) ==>+10 ((--,x(intValue,(),0)) &| x(intValue,(),1)))"));
        assertEq("(--,(x(intValue,(),1)==>x(intValue,(),0)))", "(x(intValue,(),1) ==> ((--,x(intValue,(),0)) &| x(intValue,(),1)))");
    }

    @Test
    void testInvalidCircularImpl2() {
        assertEq("(--,(x(intValue,(),1)==>x(intValue,(),0)))", "(x(intValue,(),1) ==> ((--,x(intValue,(),0)) &| x(intValue,(),1)))");
    }

    @Test
    void testImplInImplDTernal() {
        assertEq("(((--,(in))&&(happy))==>(out))", "((--,(in)) ==> ((happy)  ==> (out)))");
    }

    @Test
    void testImplInImplDTemporal() {
        assertEq("(((--,(in)) &&+1 (happy)) ==>+2 (out))", "((--,(in)) ==>+1 ((happy) ==>+2 (out)))");
    }

    @Test
    void testImplInConjPos() throws Narsese.NarseseException {
        String s = "((c==>a)&&a)";
        assertEquals(


                s,
                $.INSTANCE.$(s).toString());
    }

    @Test
    void testImplInConjNeg() throws Narsese.NarseseException {
        String s = "((--,(c==>a))&&(--,a))";
        assertEquals(

                s,
                $.INSTANCE.$(s).toString());
    }

    @Test
    void testImplInConj2xPos() throws Narsese.NarseseException {
        String s = "((c==>a)&&(d==>a))";
        assertEquals(

                s,
                $.INSTANCE.$(s).toString());
    }

    @Test
    void testImplInConj2xNeg() throws Narsese.NarseseException {
        String s = "((--,(c==>a))&&(--,(d==>a)))";

        assertEquals(

                s,
                $.INSTANCE.$(s).toString());
    }


    @Test
    void implSubjSimultaneousWithTemporalPred() {
        Term x = INSTANCE.$$("((--,(tetris-->happy))==>(tetris(isRow,(2,true),true) &&+5 (tetris-->happy)))");
        assertEquals(
                "((--,(tetris-->happy))==>(tetris(isRow,(2,true),true) &&+5 (tetris-->happy)))",
                x.toString());
    }

    @Test void implicatoinInSubj() {
        assertEq("((R==>P)==>Q)", "((R==>P)==>Q)"); //unchanged
        assertEq("((--,(R==>P))==>Q)", "(--(R==>P)==>Q)"); //unchanged
    }

    @Test void implicationInPred() {
        assertEq("((P&&R)==>Q)", "(R==>(P==>Q))");
        assertEq("((R &&+2 P) ==>+1 Q)", "(R ==>+2 (P ==>+1 Q))");
        assertEq("(((S &&+1 R) &&+2 P) ==>+1 Q)", "((S &&+1 R) ==>+2 (P ==>+1 Q))");
        assertEq("((x&&y) ==>+1 z)", "(x==>(y ==>+1 z))");
        assertEq("((x &&+1 y)==>z)", "(x ==>+1 (y==>z))");
        assertEq("((x &&+1 y) ==>+1 z)", "(x ==>+1 (y ==>+1 z))");
    }
    @Test void implicationInPred_xternal() {
        assertEq("((P&&R) ==>+- Q)", "(R==>(P ==>+- Q))");
        assertEq("(R ==>+- (P==>Q))", "(R ==>+- (P==>Q))"); //unchanged
    }
    @Test
    void testImplXternalDternalPredicateImpl() {

        assertEq("((x &&+1 y) ==>+- z)", "(x ==>+1 (y ==>+- z))");
        assertEq("((x &&+- y) ==>+- z)", "(x ==>+- (y ==>+- z))");
        assertEq("((x &&+1 (y&&z)) ==>+1 w)", "((x &&+1 y) ==> (z ==>+1 w))");

        assertEq("(((x &&+1 y) &&+1 z) ==>+1 w)", "((x &&+1 y) ==>+1 (z ==>+1 w))");

        //assertEq("((x &&+- y) ==>+1 z)", "(x ==>+- (y ==>+1 z))");
        //assertEq("(((x &&+1 y) &&+- z) ==>+1 w)", "((x &&+1 y) ==>+- (z ==>+1 w))");
    }

    @Test
    void implicationInPred_Collapse() {
        assertEq(IdempotentBool.True, "(R==>(P==>R))");
    }

    @Test
    void implicationInPred_Reduce() {
        assertEq("(R==>P)", "(R==>(R==>P))");
    }

    @Test
    void testDepvarWTF() {

        /*
            $.03 (((--,#1)&&(--,#1))==>b). %1.0;.45% {2: 1;3} ((%1,%2,(--,is(%1,"==>"))),(((--,%1) ==>+- %2),((AbductionN-->Belief),(TaskRelative-->Time),(VarIntro-->Also))))
              $.25 a. %0.0;.90% {0: 3}
              $.25 ((--,a)==>b). %1.0;.90% {0: 1}
         */
        assertEq("((--,#1)==>x)", "(((--,#1)&&(--,#1))==>x)");
    }

    @Test
    void testImplicit_DTERNAL_to_Parallel() {
        assertEq("((x&&y)==>z)", "((x&&y)==>z)"); //unchanged
        assertEq("((x&&y) ==>+- z)", "((x&&y) ==>+- z)"); //unchanged

        assertEq("((x&&y)==>z)", "((x&&y)=|>z)");  //temporal now
        assertEq("((x&&y) ==>+1 z)", "((x&&y) ==>+1 z)");
        assertEq("(z==>(x&&y))", "(z=|>(x&&y))");
    }


    @Test
    void testElimination1() {
        assertEq(
                "(--,((left &&+60 left) ==>+5080 left))",
                "((left &&+60 left) ==>-60 (left &&+5140 (--,left)))"
        );
    }

    @Test
    void testElimination2() {
        assertEq(
               False,
                "((--,(left &&+2518 left))==>left)"
        );
    }

    @Test
    void testFactoredElimination() {
        //TODO
        //test that the eternal component is not eliminated while its dependent temporal component remains
        //may need Conj.distribute() method for exhaustive, unfactored comparison
        //test that implication construction returns the same result whether conj-containing input is factored or not

        assertEq("((c &&+1 d)&&x)", "((x&|c) &&+1 (x&|d))"); //sanity pre-test
        assertEquals(INSTANCE.$$("((c &&+1 d),x)").volume() + 2, INSTANCE.$$("((x&|c),(x&|d))").volume()); //factored form results in 2 volume savings


        //the (c&&x) case reduces to 'c' because it occurs at the same time point as (b&&x)
        assertEq("(((a &&+1 b)&&x)==>(c &&+1 (d&&x)))",
                "((x&&(a &&+1 b)) ==> (x&&(c &&+1 d)))"
        );


        assertEq("(((a &&+1 b)&&x)==>(c &&+1 (d&&x)))",
                "((x&&(a &&+1 b)) ==> ((x&&c) &&+1 (x&&d)))");
    }



    @Test
    void testElimination3() {


        assertEq("(b ==>+1 (a&&x))", INSTANCE.$$("(b ==>+1 (a&&x))"));

        Compound x1 = INSTANCE.$$("((a &&+5 b) ==>+- (b &&+5 c))");
        Term y1 = x1.dt(0);
        assertEq("((a &&+5 b) ==>+5 c)", y1);
    }
    @Test
    void testElimination4() {
        Compound x2 = INSTANCE.$$("((a &&+5 b) ==>+1 (b &&+5 c))");
        assertEq("((a &&+5 b) ==>+5 c)", x2.dt(0));
        assertEq("((a &&+5 b) ==>+5 c)", x2.dt(DTERNAL));
        assertEq("((a &&+5 b) ==>+- (b &&+5 c))", x2.dt(XTERNAL));

    }


    /** test repeat that may appear in a Mapped subterms */
    @Test void ValidRepeatImplWithIndep() {
        {
            String x = "(($1 &&+5 b) ==>+1 ($1 &&+5 b))";
            assertEquals(x, INSTANCE.$$(x).toString());
        }

        {
            String x = "(($1 &&+5 b),($1 &&+5 b))";
            assertEquals(x, INSTANCE.$$(x).toString());
        }
    }









        /*
            (&,(&,P,Q),R) = (&,P,Q,R)
            (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)


            if (term1.op(Op.SET_INT) && term2.op(Op.SET_INT)) {


            if (term1.op(Op.SET_EXT) && term2.op(Op.SET_EXT)) {

         */




}
