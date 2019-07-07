package nars.nal.nal3;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.GOAL;

public class NAL3DecomposeGoalTest extends NAL3Test {

    public static final int cycles = 900;


    @Test
    void goal_decomposition_1_union() {
        test.termVolMax(8);

        test.input("(x --> (bird || swimmer))! %0.9;0.9%");
        test.input("(x --> swimmer). %0.1;0.9%");
        test.mustGoal(cycles, "(x --> bird)", 0.81f, 0.66f);
    }
//    @Test
//    void goal_decomposition_1b() {
//        test.termVolMax(8);
//        test.input("(x --> (bird & swimmer)). %0.9;0.9%");
//        test.input("(x --> swimmer)! %0.9;0.9%");
//        test.mustGoal(cycles, "(x --> bird)", 0.81f, WEAK);
//    }

    @Test
    @Disabled /* TODO check */
    void goal_decomposition_1_intersection_subj() {
        test.termVolMax(8);
        test.input("((bird && swimmer) --> x)! %0.9;0.9%");
        test.input("(swimmer --> x). %0.9;0.9%");
        test.mustGoal(cycles, "(bird --> x)", 0.81f, 0.66f);
    }

    @Test
    void goal_decomposition_1_intersection_pred_pos_pos() {
        test.termVolMax(8);
        test.input("(x --> (bird && swimmer))! %0.9;0.9%");
        test.input("(x --> swimmer). %0.9;0.9%");
        test.mustGoal(cycles, "(x --> bird)", 0.81f, 0.66f);
    }
    @Test
    void goal_decomposition_1_intersection_pred_pos_neg() {
        test.termVolMax(8);
        test.input("(x --> (bird && --swimmer))! %0.9;0.9%");
        test.input("(x --> swimmer). %0.1;0.9%");
        test.mustGoal(cycles, "(x --> bird)", 0.81f, 0.66f);
    }
    @Test
    void goal_decomposition_1_intersection_pred_neg_pos() {
        test.termVolMax(8);
        test.input("(x --> (bird && swimmer))! %0.1;0.9%");
        test.input("(x --> swimmer). %0.9;0.9%");
        test.mustGoal(cycles, "(x --> bird)", 0.19f, 0.66f);
    }
    @Test
    void goal_decomposition_1_intersection_pred_neg_neg() {
        test.termVolMax(8);
        test.input("(x --> (bird && --swimmer))! %0.1;0.9%");
        test.input("(x --> swimmer). %0.1;0.9%");
        test.mustGoal(cycles, "(x --> bird)", 0.19f, 0.66f);
    }
    @Test
    void testUnionSinglePremiseDecomposeGoal1Pos() {
        test
                .termVolMax(7)
                .confMin(0.75f)
                .input("((a||b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    void testIntersectionSinglePremiseDecomposeGoal1Pos() {
        test
                .termVolMax(6)
                .confMin(0.75f)
                .input("((a&&b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    void testIntersectionConditionalDecomposeGoalPos() {
        test
                .termVolMax(6)
                .confMin(0.75f)
                .input("((a&&b)-->g)!")
                .input("(a-->g). %0.8%")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }
    @Test
    void testIntersectionConditionalDecomposeGoalPosNeg() {
        test
                .termVolMax(6)
                .confMin(0.75f)
                .input("((b && --a)-->g)!")
                .input("--(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }
    @Test
    void testSubjUnionConditionalDecomposeGoalPosNeg() {
        test
                .termVolMax(7)
                .confMin(0.75f)
                .input("((a||b)-->g)!")
                .input("--(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    void testPredUnionConditionalDecomposeGoalPosNeg() {
        test
                .input("(g-->(a||b))!")
                .input("--(g-->a).")
                .mustGoal(cycles, "(g-->b)", 1f, 0.81f);
    }


//    @Test
//    public void testUnionConditionalDecomposeGoalPosPos() {
//        test
//                .input("((a&b)-->g)!")
//                .input("(a-->g).")
//                .mustNotGoal(cycles, "(b-->g)", 1f, 0.81f);
//    }


    @Test
    void testIntersectionPosIntersectionSubGoalSinglePremiseDecompose() {
        test
                .input("((a&&b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f)
        ;
    }
    @Test
    void testIntersectionPosIntersectionPredGoalSinglePremiseDecompose() {
        test
                .input("(g-->(a&&b))!")
                .mustGoal(cycles, "(g-->a)", 1f, 0.81f)
                .mustGoal(cycles, "(g-->b)", 1f, 0.81f)
        ;
    }

    @Test
    void testNegIntersectionBeliefSinglePremiseDecompose() {
        test
                .input("--((a&&b)-->g).")
                .mustBelieve(cycles, "(a-->g)", 0f, 0.81f)
                .mustBelieve(cycles, "(b-->g)", 0f, 0.81f)
        ;
    }
    @Test
    void testNegIntersectionGoalSinglePremiseDecompose() {

        test
                .input("--((a&&b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 0f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f)
        ;
    }
    @Test
    void testNegUnionBeliefSinglePremiseDecompose() {
        test
                .input("--((a||b)-->g).")
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

    @Test
    void subj_intersectionConditionalDecomposeGoalNeg() {
        test
                .input("--((a&&b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
    }
    @Test
    void subj_intersectionConditionalDecomposeGoalNeg_Weaker() {
        test
                .input("--((a&&b)-->g)!")
                .input("(a-->g). %0.9%")
                .mustGoal(cycles, "(b-->g)", 0.1f, 0.73f);
    }
    @Test
    void pred_intersectionConditionalDecomposeGoalNeg() {
        test
                .input("--(g-->(a&&b))!")
                .input("(g-->a).")
                .mustGoal(cycles, "(g-->b)", 0f, 0.81f);
    }
    @Test
    void pred_intersectionConditionalDecomposeGoalNeg_Weaker() {
        test
                .input("--(g-->(a&&b))!")
                .input("(g-->a). %0.9%")
                .mustGoal(cycles, "(g-->b)", 0.1f, 0.73f);
    }



    @Test
    void testDiffGoal1SemiPos1st() {
        test
                .input("((a && --b)-->g)! %0.50;0.90%")
                .input("(a-->g). %1.00;0.90%")
                .mustGoal(cycles, "(b-->g)", 0.5f, 0.4f);
    }


    @Test
    void testMutexAbduction() {
        test
                .termVolMax(6)
                .believe("(--(x && y) ==> z)")
                .believe("(x && z)")
                .mustBelieve(cycles, "y", 0f, 0.45f)
        ;
    }

    @Test
    void testMutexDiffGoal1Neg() {
        test
                .input("--((a && --b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
    }

    @Test
    void testDiffGoal1Pos1st() {
        test
                .input("((a && --b)-->g)! %1.00;0.90%")
                .input("(a-->g).")
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
    }


}
