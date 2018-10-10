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
    void testIntersectionSinglePremiseDecomposeGoal1Pos() {
        test
                .input("((a|b)-->g)!")

                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    void testIntersectionConditionalDecomposeGoalPos() {
        test
                .input("((a|b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

//    @Test
//    public void testUnionConditionalDecomposeGoalPosPos() {
//        test
//                .input("((a&b)-->g)!")
//                .input("(a-->g).")
//                .mustNotGoal(cycles, "(b-->g)", 1f, 0.81f);
//    }

    @Test
    void testUnionConditionalDecomposeGoalPosNeg() {
        test
                .input("((a&b)-->g)!")
                .input("--(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    void testIntersectionPosGoalSinglePremiseDecompose() {
        test
                .input("((a|b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f)
        ;
    }
    @Test
    void testIntersectionNegGoalSinglePremiseDecompose() {
        test
                .input("--((a|b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 0f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f)
        ;
    }
    @Test
    void testIntersectionConditionalDecomposeGoalNeg() {
        test
                .input("--((a|b)-->g)!")
                .input("--(a-->g).")
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
    }

    @Test
    void testIntersectionConditionalDecomposeGoalConfused() {
        test
                .input("--((a|b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
    }

    @Test
    void testDiffGoal1SemiPos1st() {
        test
                .input("((a~b)-->g)! %0.50;0.90%")
                .input("(a-->g). %1.00;0.90%")
                .mustGoal(cycles, "(b-->g)", 0.5f, 0.81f);
    }

    @Test
    void testMutexDiffGoal1Pos2nd() {
        test
                .input("((a~b)-->g)!")
                .input("--(b-->g).")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f);
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
    void testDisj() {
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
    void testAndConj() {
        test
                .input("(&&,a,b)!")
                .input("a.")
                .mustGoal(cycles, "b", 1f, 0.81f);
    }


    @Test
    void testMutexDiffGoal1Neg() {
        test
                .input("--((a~b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    void testIntersectSinglePremiseGoal1Neg() {
        test
                .input("--((a|b)-->g)!")

                .mustGoal(cycles, "(a-->g)", 0f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
    }

    @Test
    void testDiffGoal1Pos1st() {
        test
                .input("((a~b)-->g)! %1.00;0.90%")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
    }


}