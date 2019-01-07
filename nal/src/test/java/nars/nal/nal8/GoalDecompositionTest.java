package nars.nal.nal8;

import nars.nal.nal7.NAL7Test;
import nars.test.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.GOAL;

/**
 * tests goals involving &,|,~,-, etc..
 */
public class GoalDecompositionTest extends NALTest {

    public static final int cycles = 150;


    @BeforeEach
    void setTolerance() {
        test.confTolerance(NAL7Test.CONF_TOLERANCE_FOR_PROJECTIONS);
        test.nar.time.dur(3);
    }



    @Test
    void testConjBeliefPos() {
//
//        test.log();
        test
                .input("(&&,a,b). %0.75;0.9%")
                .input("a. %0.80;0.9%")
                .mustBelieve(cycles, "b", 0.60f, 0.49f);
    }

    @Test
    void testConjBeliefNeg() {
        test
                .input("(&&,--a,b).")
                .input("--a.")
                .mustBelieve(cycles, "b", 1f, 0.81f);
    }

    @Test
    void testDisjBeliefPos() {

        test
                .input("(||,a,b). %0.9;0.9%")
                .input("--a. %0.9;0.9%")
                .mustBelieve(cycles, "b", 0.81f, 0.66f);
    }
    @Test
    void testDisjBeliefNeg() {

        test
                .input("(||,--a,b).  %0.9;0.9%")
                .input("a.  %0.9;0.9%")
                .mustBelieve(cycles, "b", 0.81f, 0.66f);
    }

    @Test
    void testDisjConditionalDecompose() {
        test
            .input("(||,a,b)!")
            .input("--a.")
            .mustGoal(cycles, "b", 1f, 0.81f)
            .mustNotOutput(cycles, "b", GOAL, 0f, 0.5f, 0f, 1f, t->true)
        ;
    }

    @Disabled
    @Test
    void testDisjOpposite() {
//
//        test.log();
        //produces output from structural deduction
        test
                .input("(||,a,--b)!")
                .input("a.")
                .mustNotOutput(cycles, "b", GOAL, 0f, 1f, 0f, 1f, t->true)
                ;
    }
    @Test
    void testDisjNeg() {
        test
                .input("(||,a,--b)!")
                .input("--a.")
                .mustGoal(cycles, "b", 0f, 0.81f)
                .mustNotOutput(cycles, "b", GOAL, 0.5f, 1f, 0f, 1f, t->true)

        ;
    }

    @Test
    void testDisjNeg2() {
        test
                .input("(||,--a, b)!")
                .input("a.")
                .mustGoal(cycles, "b", 1f, 0.81f);
    }

    @Test
    void testDisjNeg3() {
        test
                .input("(||,--a,--b)!")
                .input("a.")
                .mustGoal(cycles, "b", 0f, 0.81f);
    }

    @Test
    void test_Pos_GoalInDisj2_AlternateSuppression_1() {
        test
                .input("(||,x,y).")
                .input("x!")
                .mustGoal(cycles, "y", 0f, 0.45f);
    }
    @Test
    void test_Pos_AntiGoalInDisj2_AlternateSuppression_1() {
        test
                .input("(||,--x,y).")
                .input("x!")
                .mustGoal(cycles, "y", 1f, 0.45f);
    }
    @Test
    void test_Neg_GoalInDisj2_AlternateSuppression_1() {
        test
                .input("(||,--x,y).")
                .input("--x!")
                .mustGoal(cycles, "y", 0f, 0.45f);
    }
    @Test
    void test_Pos_GoalInDisj3_AlternateSuppression_1() {
        test
                .input("(||,x,y,z).")
                .input("x!")
                .mustGoal(cycles, "(||,y,z)" /* == (&&,--y,--z) */, 0f, 0.15f);
    }


    @Test
    void testAndConj() {
        test
                .input("(&&,a,b)!")
                .input("a.")
                .mustGoal(cycles, "b", 1f, 0.81f);
    }


}