package nars.nal.nal8;

import nars.NAR;
import nars.NARS;
import nars.nal.nal7.NAL7Test;
import nars.test.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.GOAL;

/**
 * tests goals involving &,|,~,-, etc..
 */
public class DecompositionTest extends NALTest {

    public static final int cycles = 1000;


    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(5,6);
        n.termVolumeMax.set(12);
        return n;
    }

    @BeforeEach
    void setTolerance() {
        test.confTolerance(NAL7Test.CONF_TOLERANCE_FOR_PROJECTIONS);
        test.nar.time.dur(3);
    }


    @Test
    void testConjBeliefWeak() {
        test
                .termVolMax(5)
                .input("(&&,a,b). %0.75;0.9%")
                .input("a. %0.80;0.9%")
                .mustBelieve(cycles, "b", 0.60f, 0.49f);
    }

    @Test
    void testConjBeliefNeg() {
        test
                .termVolMax(5)
                .input("(&&,--a,b).")
                .input("--a.")
                .mustBelieve(cycles, "b", 1f, 0.81f);
    }

    @Test
    void testDisjBeliefPos() {

        test
                .termVolMax(5)
                .input("(||,a,b). %0.9;0.9%")
                .input("--a. %0.9;0.9%")
                .mustBelieve(cycles, "b", 0.81f, 0.66f);
    }
    @Test
    void testDisjBeliefNeg() {

        test
                .termVolMax(6)
                .input("(||,--a,b).  %0.9;0.9%")
                .input("a.  %0.9;0.9%")
                .mustBelieve(cycles, "b", 0.81f, 0.66f);
    }

    @Test
    void testDisjConditionalDecompose() {
        test
            .termVolMax(5)
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
                .termVolMax(5)
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



    public static class DoublePremiseDecompose extends NALTest {

        public static final int cycles = 2500;


        @Test
        void decompose_Conj_BeliefPosPos() {
            test
                    .termVolMax(5)
                    .input("(a && b). %0.9;0.9%")
                    .input("b. %0.9;0.9%")
                    .mustBelieve(cycles, "a", 0.81f, 0.66f);
        }
        @Test
        void decompose_Conj_BeliefPosNeg() {
            test
                    .termVolMax(5)
                    .input("(a && --b). %0.9;0.9%")
                    .input("b. %0.1;0.9%")
                    .mustBelieve(cycles, "a", 0.81f, 0.66f);
        }
        @Test
        void decompose_Conj_BeliefNegPos() {
            test
                    .termVolMax(5)
                    .input("(a && b). %0.1;0.9%")
                    .input("b. %0.9;0.9%")
                    .mustBelieve(cycles, "a", 0.19f, 0.66f);
        }
        @Test
        void decompose_Conj_BeliefNegNeg() {
            test
                    .termVolMax(5)
                    .input("(a && --b). %0.1;0.9%")
                    .input("b. %0.1;0.9%")
                    .mustBelieve(cycles, "a", 0.19f, 0.66f);
        }

        @Test
        void decompose_Conj_Goal_pos_decompose_pos() {
            //adapted form nal3 test
            test.termVolMax(6);
            test.input("(a && b)! %0.9;0.9%");
            test.input("b. %0.9;0.9%");
            test.mustGoal(cycles, "a", 0.81f, 0.66f);
        }

      
        @Test
        void decompose_Conj_Goal_pos_decompose_neg() {
            //adapted form nal3 test
            test.termVolMax(6);
            test.input("(a && --b)! %0.9;0.9%");
            test.input("b. %0.1;0.9%");
            test.mustGoal(cycles, "a", 0.81f, 0.66f);
        }


        @Test
        void decompose_Conj_Goal_neg_decompose_pos() {
            //adapted form nal3 test
            test.termVolMax(6);
            test.input("(a && b)! %0.1;0.9%");
            test.input("b. %0.9;0.9%");
            test.mustGoal(cycles, "a", 0.19f, 0.66f);
        }

        @Test
        void decompose_Conj_Goal_neg_decompose_neg() {
            //adapted form nal3 test
            test.termVolMax(6);
            test.input("(a && --b)! %0.1;0.9%");
            test.input("b. %0.1;0.9%");
            test.mustGoal(cycles, "a", 0.19f, 0.66f);
        }

    }
}