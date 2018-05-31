package nars.nal.nal8;

import nars.nal.nal7.NAL7Test;
import nars.util.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * tests goals involving &,|,~,-, etc..
 */
public class NAL8DecomposedGoalTest extends NALTest {

    public static final int cycles = 50;


    @BeforeEach
    public void setTolerance() {
        test.confTolerance(NAL7Test.CONF_TOLERANCE_FOR_PROJECTIONS);
        test.nar.time.dur(3);
    }

    @Test
    public void testIntersectionSinglePremiseDecomposeGoal1Pos() {
        test
                .input("((a|b)-->g)!")

                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    public void testIntersectionConditionalDecomposeGoalPos() {
        test
                .input("((a|b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    public void testUnionConditionalDecomposeGoalPosPos() {
        test
                .input("((a&b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    public void testUnionConditionalDecomposeGoalPosNeg() {
        test
                .input("((a&b)-->g)!")
                .input("--(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    public void testIntersectionConditionalDecomposeGoalNeg() {
        test
                .input("--((a|b)-->g)!")
                .input("--(a-->g).")
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
    }

    @Test
    public void testIntersectionConditionalDecomposeGoalConfused() {
        test
                .input("--((a|b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
    }

    @Test
    public void testDiffGoal1SemiPos1st() {
        test
                .input("((a~b)-->g)! %0.50;0.90%")
                .input("(a-->g). %1.00;0.90%")
                .mustGoal(cycles, "(b-->g)", 0.5f, 0.81f);
    }

    @Test
    public void testMutexDiffGoal1Pos2nd() {
        test
                .input("((a~b)-->g)!")
                .input("--(b-->g).")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f);
    }

    @Test
    public void testDisj() {
        test
                .input("(||,a,b)!")
                .input("--a.")
                .mustGoal(cycles, "b", 1f, 0.81f);
    }

    @Test
    public void testDisjNeg() {
        test
                .input("(||,a,--b)!")
                .input("--a.")
                .mustGoal(cycles, "b", 0f, 0.81f);
    }

    @Test
    public void testDisjNeg2() {
        test
                .input("(||,--a, b)!")
                .input("a.")
                .mustGoal(cycles, "b", 1f, 0.81f);
    }

    @Test
    public void testDisjNeg3() {
        test
                .input("(||,--a,--b)!")
                .input("a.")
                .mustGoal(cycles, "b", 0f, 0.81f);
    }

    @Test
    public void testDisjOpposite() {
        test
                .input("(||,a,--b)!")
                .input("a.")
                .mustGoal(cycles, "b", 1f, 0.81f);
    }

    @Test
    public void testAndConj() {
        test
                .input("(&&,a,b)!")
                .input("a.")
                .mustGoal(cycles, "b", 1f, 0.81f);
    }


    @Test
    public void testMutexDiffGoal1Neg() {
        test
                .input("--((a~b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    public void testIntersectSinglePremiseGoal1Neg() {
        test
                .input("(--,((a|b)-->g))!")

                .mustGoal(cycles, "(a-->g)", 0f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
    }

    @Test
    public void testDiffGoal1Pos1st() {
        test
                .input("((a~b)-->g)! %1.00;0.90%")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
    }


}