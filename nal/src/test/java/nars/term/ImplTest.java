package nars.term;

import nars.$;
import nars.Narsese;
import nars.term.atom.Bool;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.io.NarseseTest.assertInvalidTerms;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * tests specific to implication compounds TODO
 */
public class ImplTest {
    @Test
    public void testInvalidImpl1() {
        assertEq(Bool.False, "(--y =|> y)");
    }

    @Test
    public void testInvalidImpl2() {
        assertEq(Bool.False, "(--(x &| y) =|> y)");
    }

    @Test
    public void testInvalidImpl3() {
        assertEq(Bool.False, "(--(--x &| y) =|> y)");
    }

    @Test
    void testReducibleImplFactored() {
        assertEq("((x&|y)=|>z)", "((x &| y) =|> (y &| z))");
        assertEq("((x&|y)==>z)", "((x &| y) ==> (y &| z))");
    }

    @Test
    void toomuchReduction() {
        /** this took some thought but it really is consistent with the system */
        assertEq("((b &&+60000 c)=|>(#1 &&+60000 (b&|#1)))",
                "((b &&+60000 c)=|>((#1 &&+60000 b)&&(c &&+60000 #1)))");
    }

    @Test
    void conceptualizability() {
        assertEq("(( &&+- ,b,c,d) ==>+- b)", $$("((c &&+5 (b&|d)) ==>-10 b)").concept());
    }


    @Test
    void testReducibleImplFactored2() {
        assertEq("((x&&y)==>z)", "((y && x) ==> (y && z))");
        assertEq("((&&,a,x,y)==>z)", "((&&, x, y, a) ==> (y && z))");
        assertEq("((y &&+1 x)=|>(z &&+1 y))", "((y &&+1 x)=|>(z &&+1 y))");
    }

    @Test
    void testReducibleImplFactoredPredShouldRemainIntact() {

        for (String cp : new String[]{"&&", "&|", " &&+- "}) {
            String xternal = cp.equals(" &&+- ") ? cp : "&|";
            assertEq("((x&&y) ==>+1 (y" + xternal + "z))", "((y&&x) ==>+1 (y" + cp + "z))");
            assertEq("(a ==>+1 (b &&+1 (y" + xternal + "z)))", "(a ==>+1 (b &&+1 (y" + cp + "z)))");
        }


    }

    @Test
    void testReducibleImpl() {

        assertEq("(--,((--,x)==>y))", "(--x ==> (--y && --x))");

        assertEq("(x=|>y)", "(x ==>+0 (y &| x))");
        assertEq(Bool.True, "((y &| x) =|> x)");
        assertEq("(--,((--,$1)=|>#2))", "((--,$1)=|>((--,$1)&|(--,#2)))");
    }

    @Test
    void testReducibleImplConjCoNeg() {
        assertEq(Bool.False, "((y &| --x) ==> x)");

        for (String i : new String[]{"==>", "=|>"}) {
            for (String c : new String[]{"&&", "&|"}) {
                assertEq(Bool.False, "(x " + i + " (y " + c + " --x))");
                assertEq(Bool.False, "(--x " + i + " (y " + c + " x))");
                assertEq(Bool.False, "((y " + c + " --x) " + i + " x)");
                assertEq(Bool.False, "((y " + c + " x) " + i + " --x)");
            }
        }
    }


    @Test
    void testReducibleImplParallelNeg() {
        assertEq("(--,((--,x)=|>y))", "(--x =|> (--y &| --x))");
        assertEq(Bool.True, "((--y &| --x) =|> --x)");

    }

    @Test
    void testInvalidCircularImpl() throws Narsese.NarseseException {
        assertNotEquals(Bool.Null, $("(x(intValue,(),1) ==>+10 ((--,x(intValue,(),0)) &| x(intValue,(),1)))"));
        assertEq("(--,(x(intValue,(),1)=|>x(intValue,(),0)))", "(x(intValue,(),1) =|> ((--,x(intValue,(),0)) &| x(intValue,(),1)))");
    }

    @Test
    void testInvalidCircularImpl2() throws Narsese.NarseseException {
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
                $.$(s).toString());
    }

    @Test
    void testImplInConjNeg() throws Narsese.NarseseException {
        String s = "((--,(c==>a))&&(--,a))";
        assertEquals(

                s,
                $.$(s).toString());
    }

    @Test
    void testImplInConj2xPos() throws Narsese.NarseseException {
        String s = "((c==>a)&&(d==>a))";
        assertEquals(

                s,
                $.$(s).toString());
    }

    @Test
    void testImplInConj2xNeg() throws Narsese.NarseseException {
        String s = "((--,(c==>a))&&(--,(d==>a)))";

        assertEquals(

                s,
                $.$(s).toString());
    }


    @Test
    void implSubjSimultaneousWithTemporalPred() {
        Term x = $$("((--,(tetris-->happy))=|>(tetris(isRow,(2,true),true) &&+5 (tetris-->happy)))");
        assertEquals(
                "((--,(tetris-->happy))=|>(tetris(isRow,(2,true),true) &&+5 (tetris-->happy)))",
                x.toString());
    }


    @Disabled
    @Test
    void testReducedAndInvalidImplications5() {

        assertInvalidTerms("((P==>Q) ==> R)");
    }


    @Test
    void testReducedAndInvalidImplications2() {
        assertEq("((P&&R)==>Q)", "(R==>(P==>Q))");
        assertEq("((R &&+2 P) ==>+1 Q)", "(R ==>+2 (P ==>+1 Q))");
        assertEq("(((S &&+1 R) &&+2 P) ==>+1 Q)", "((S &&+1 R) ==>+2 (P ==>+1 Q))");
    }


    @Test
    void testReducedAndInvalidImplications3() {
        assertEq(Bool.True, "(R==>(P==>R))");
    }

    @Test
    void testReducedAndInvalidImplications4() {
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

        assertEq("((x&&y)=|>z)", "((x&&y)=|>z)");  //temporal now
        assertEq("((x&&y) ==>+1 z)", "((x&&y) ==>+1 z)");
        assertEq("(z=|>(x&&y))", "(z=|>(x&&y))");
    }


    @Test
    void testElimination1() {
        assertEq(
                "(--,((left &&+60 left) ==>+5080 left))",
                "((left &&+60 left) ==>-60 (left &&+5140 (--,left)))"
        );
    }
    @Test void testElimination2() {
        assertEq(
                "TODO", //False?
                "((--,(left &&+2518 left))==>left)"
        );

    }

    @Test void testFactoredElimination() {
        //TODO
        //test that the eternal component is not eliminated while its dependent temporal component remains
        //may need Conj.distribute() method for exhaustive, unfactored comparison
        //test that implication construction returns the same result whether conj-containing input is factored or not

        assertEq("((c &&+1 d)&&x)","((x&|c) &&+1 (x&|d))"); //sanity pre-test
        assertEquals($$("((c &&+1 d),x)").volume()+2,$$("((x&|c),(x&|d))").volume()); //factored form results in 2 volume savings

        assertEq("(((a &&+1 b)&&x)==>((c &&+1 d)&&x))",
                "((x&&(a &&+1 b)) ==> (x&&(c &&+1 d)))"); //same


        assertEq("(((a &&+1 b)&&x)==>((c &&+1 d)&&x))",
                "((x&&(a &&+1 b)) ==> ((x&|c) &&+1 (x&|d)))"); //same
    }
















        /*
            (&,(&,P,Q),R) = (&,P,Q,R)
            (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)


            if (term1.op(Op.SET_INT) && term2.op(Op.SET_INT)) {


            if (term1.op(Op.SET_EXT) && term2.op(Op.SET_EXT)) {

         */


}
