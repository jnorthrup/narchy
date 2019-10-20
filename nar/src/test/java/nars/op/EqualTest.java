package nars.op;

import nars.Narsese;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.op.ArithmeticTest.assertEval;
import static nars.op.ArithmeticTest.assertSolves;
import static nars.term.atom.theBool.*;

public class EqualTest {
    /**
     * same tautological assumptions should hold in equal(x,y) results
     */
    @Test
    void testEqualOperatorTautologies() {
        //TODO finish
//        NAR n = NARS.shell();
        Assertions.assertEquals(True, Equal.the(True, True));
        Assertions.assertEquals(False, Equal.the(True, False));
        Assertions.assertEquals(Null, Equal.the(True, Null));
        Assertions.assertEquals(Null, Equal.the(False, Null));
//        assertEq("(y-->x)", Equal.the($$("x:y"), True));
//        assertEq("(--,(y-->x))", Equal.the($$("x:y"), False));

//        assertEquals("[equal(true,true)]", Evaluation.eval($$("equal(true,true)"), n).toString());
//        assertEquals("[equal(false,false)]", Evaluation.eval($$("equal(false,false)"), n).toString());
        //assertEquals("[null]", Evaluation.eval($$("equal(null,null)"), n).toString());
    }

    @Test
    void testEqBackSubstitutionAdd() throws Narsese.NarseseException {
        assertSolves("(&&,(#1,add(#2,1)),equal(#2,3),(#1,#2))", "((#1,4)&&(#1,3))");

    }
    @Test
    void testEqBackSubstitutionAdd2() throws Narsese.NarseseException {
        assertSolves("(&&,(#1,add(#2,1)),(#1,#2),equal(#2,3))", "((#1,3)&&(#1,4))");
    }

    @Test public void testEqualSolutionAddInverse() {
        assertEval($$("x(0)"), "(x(#1) && equal(add(#1,1),1))");
        assertEval($$("x(0)"), "(x(#1) && equal(add(1,#1),1))");
        assertEval($$("x(0)"), "(x(#1) && equal(1,add(#1,1)))");
        assertEval($$("x(0)"), "(x(#1) && equal(1,add(1,#1)))");
    }

    @Test public void testEqualSolutionMulInverseA() {
        assertEval($$("x(-2)"), "(x(#1) && equal(mul(#1,-1),2))");
        assertEval($$("x(-2)"), "(x(#1) && equal(mul(-1,#1),2))");
        assertEval($$("x(-2)"), "(x(#1) && equal(2,mul(#1,-1)))");
        assertEval($$("x(-2)"), "(x(#1) && equal(2,mul(-1,#1)))");
    }
    @Test public void testEqualSolutionMulInverseB() {
        assertEval($$("x(1)"), "(x(#1) && equal(mul(2,#1),2))");
        assertEval($$("x(1)"), "(x(#1) && equal(mul(#1,2),2))");
        assertEval($$("x(1)"), "(x(#1) && equal(2,mul(#1,2)))");
        assertEval($$("x(1)"), "(x(#1) && equal(2,mul(2,#1)))");
    }

    @Test public void testEqualSolutionComplex() {
        /*

        (&&,(--,(g(add(#1,1),0,(0,add(add(#1,1),9)))&&equal(add(#1,1),1))),(--,chronic(add(#1,1))),(--,add(#1,1)),(--,down))

        "equal(add(#1,1),1)" ===> (1 == 1+x) ===> (0 == x)

        drastic simplification:
            (&&,(--,(g(add(#1,1),0,(0,add(#1,10))&&equal(#1, 0)),(--,chronic(add(#1,1))),(--,add(#1,1)),(--,down))
            etc (&&,(--,(g(add(#1,1),0,(0,10)),(--,chronic(1)),(--,0),(--,down))
         */

        String t = "(&&,(--,(g(add(#1,1),0,(0,add(add(#1,1),9)))&&equal(add(#1,1),1))),(--,c(add(#1,1))),(--,add(#1,1)),(--,down))";

        assertEval($$("(&&,(--,g(1,0,(0,10))),(--,c(1)),(--,down),(--,1))"), t);
    }




    @Test
    void testAddEqualIdentity() {
        assertEval($$("answer(0)"), "(equal(#x,add(#x,$y))==>answer($y))");
    }
}
