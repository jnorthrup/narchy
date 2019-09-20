package nars.nal.nal3;

import org.junit.jupiter.api.Test;

import static nars.Op.GOAL;

public class NAL3DecomposeGoalTest extends NAL3Test {

    public static final int cycles = 900;



    @Test
    void testUnionSinglePremiseDecomposeGoal1Pos() {
        test
                .termVolMax(7)
                .confMin(0.75f)
                .input("((a|b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    void testIntersectionSinglePremiseDecomposeGoal1Pos() {
        test
                .termVolMax(6)
                .confMin(0.75f)
                .input("((a&b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }


    @Test
    void testIntersectionPosIntersectionSubGoalSinglePremiseDecompose() {
        test
                .input("((a&b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f)
        ;
    }
    @Test
    void testIntersectionPosIntersectionPredGoalSinglePremiseDecompose() {
        test
                .input("(g-->(a&b))!")
                .mustGoal(cycles, "(g-->a)", 1f, 0.81f)
                .mustGoal(cycles, "(g-->b)", 1f, 0.81f)
        ;
    }

    @Test
    void testNegIntersectionBeliefSinglePremiseDecompose() {
        test
                .input("--((a&b)-->g).")
                .mustBelieve(cycles, "(a-->g)", 0f, 0.81f)
                .mustBelieve(cycles, "(b-->g)", 0f, 0.81f)
        ;
    }
    @Test
    void testNegIntersectionGoalSinglePremiseDecompose() {

        test
                .input("--((a&b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 0f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f)
        ;
    }
    @Test
    void testNegUnionBeliefSinglePremiseDecompose() {
        test
                .input("--((a|b)-->g).")
                .mustBelieve(cycles, "(a-->g)", 0f, 0.81f)
                .mustBelieve(cycles, "(b-->g)", 0f, 0.81f)
        ;
    }


    @Test
    void testNegUnionGoalSinglePremiseDecompose() {
        test
                .input("--((a||b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 0f, 0.81f)
                .mustNotOutput(cycles, "(a-->g)", GOAL, 0.1f, 1f, 0, 1)
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f)
                .mustNotOutput(cycles, "(b-->g)", GOAL, 0.1f, 1f, 0, 1)
        ;
    }

//        @Test
//        void testIntersectionConditionalDecomposeGoalNeg() {
//            test
//                    .input("--((a|b)-->g)!")
//                    .input("--(a-->g).")
//                    .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
//        }



}
