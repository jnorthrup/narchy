package nars.nal.nal8;

import jcog.Util;
import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * tests goals involving &,|,~,-, etc..
 */
abstract public class DecomposeTest extends NALTest {

    public static final int cycles = 1500;


    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(5,6);
        n.termVolMax.set(12);
        return n;
    }

    @BeforeEach
    void setTolerance() {
        //test.confTolerance(NAL7Test.CONF_TOLERANCE_FOR_PROJECTIONS);
        test.nar.time.dur(3);
    }

    static class ConjBelief extends DecomposeTest {

        @Test
        void testConjBeliefWeak() {
            test
                    .termVolMax(3)
                    .input("(a && b). %0.75;0.9%")
                    .input("a. %0.80;0.9%")
                    .mustBelieve(cycles, "b", 0.60f, 0.49f)
                    .must(BELIEF, true, (t)->{
                        if (t.term().toString().equals("b")) {
                            float f = t.freq();
                            return !Util.equals(f, 0.60f, 0.05f) || t.conf() < 0.2f;
                        }
                        return true;
                    });
        }
        @Test
        void testConjBeliefWeakNeg() {
            test
                    .termVolMax(4)
                    .input("(--a && b). %0.75;0.9%")
                    .input("a. %0.20;0.9%")
                    .mustBelieve(cycles, "b", 0.60f, 0.49f)
                    .must(BELIEF, true, (t)->{
                        if (t.term().toString().equals("b")) {
                            float f = t.freq();
                            return !Util.equals(f, 0.60f, 0.05f) || t.conf() < 0.2f;
                        }
                        return true;
                    });
        }
        @Test
        void testConjBeliefNeg() {
            test
                    .termVolMax(5)
                    .input("(&&,--a,b).")
                    .input("--a.")
                    .mustBelieve(cycles, "b", 1f, 0.81f);
        }
    }

    static class DisjBelief extends DecomposeTest {

        @Test
        void testDisjBeliefPos() {

            test
                    .termVolMax(6)
                    .input("(||,a,b). %0.9;0.9%")
                    .input("--a. %0.9;0.9%")
                    .mustBelieve(cycles, "b", 0.81f, 0.59f);
        }

        @Test
        void testDisjBeliefPos2() {

            test
                    .termVolMax(7)
                    .input("(||,a,b,c). %0.9;0.9%")
                    .input("--(||,a,b). %0.9;0.9%")
                    .mustBelieve(cycles, "c", 0.81f, 0.59f);
        }

        @Test
        void testDisjBeliefPosMix() {

            test
                    .termVolMax(7)
                    .input("(||,--a,b,c). %0.9;0.9%")
                    .input("--(||,--a,b). %0.9;0.9%")
                    .mustBelieve(cycles, "c", 0.81f, 0.59f);
        }

        @Test
        void testDisjBeliefNeg() {

            test
                    .termVolMax(6)
                    .input("(||,--a,b).  %0.9;0.9%")
                    .input("a.  %0.9;0.9%")
                    .mustBelieve(cycles, "b", 0.81f, 0.59f);
        }
    }
    static class DisjGoal extends DecomposeTest {
        @Test
        void testDisjConditionalDecompose() {
            test
                    .termVolMax(6)
                    .input("(||,a,b)!")
                    .input("--a.")
                    .mustGoal(cycles, "b", 1f, 0.81f)
                    .mustNotOutput(cycles, "b", GOAL, 0f, 0.5f, 0f, 1f, t -> true)
            ;
        }

        @Test
        void testDisjOpposite() {

            //produces output from structural deduction
            test
                    .termVolMax(4)
                    .input("(||,a,--b)!")
                    .input("a.")
                    .mustNotOutput(cycles, "b", GOAL, 0f, 1f, 0f, 1f, t -> true)
            ;
        }

        @Test
        void testDisjNeg() {
            test
                    .termVolMax(4)
                    .input("(||,a,--b)!")
                    .input("--a.")
                    .mustGoal(cycles, "b", 0f, 0.81f)
                    .mustNotOutput(cycles, "b", GOAL, 0.5f, 1f, 0f, 1f, t -> true)

            ;
        }
        @Test
        void testDisjNeg2() {
            test
                    .termVolMax(4)
                    .input("(||,--a, b)!")
                    .input("a.")
                    .mustGoal(cycles, "b", 1f, 0.81f);
        }

        @Test
        void testDisjNeg3() {
            test
                    .termVolMax(5)
                    .input("(||,--a,--b)!")
                    .input("a.")
                    .mustGoal(cycles, "b", 0f, 0.81f);
        }

    }
    static class ConjGoal extends DecomposeTest {
        @Test
        void testConjPos1() {
            test
                    .termVolMax(6)
                    .input("(&&, a, --b)! %0.9%")
                    .input("a. %0.9%")
                    .mustGoal(cycles, "b", 0.19f, 0.66f);
        }

    }

//    @Test
//    void test_Pos_GoalInDisj2_AlternateSuppression_1() {
//        test
//                .input("(||,x,y).")
//                .input("x!")
//                .mustGoal(cycles, "y", 0f, 0.45f);
//    }
//    @Test
//    void test_Pos_AntiGoalInDisj2_AlternateSuppression_1() {
//        test
//                .input("(||,--x,y).")
//                .input("x!")
//                .mustGoal(cycles, "y", 1f, 0.45f);
//    }
//    @Test
//    void test_Neg_GoalInDisj2_AlternateSuppression_1() {
//        test
//                .input("(||,--x,y).")
//                .input("--x!")
//                .mustGoal(cycles, "y", 0f, 0.45f);
//    }
//    @Test
//    void test_Pos_GoalInDisj3_AlternateSuppression_1() {
//        test
//                .input("(||,x,y,z).")
//                .input("x!")
//                .mustGoal(cycles, "(||,y,z)" /* == (&&,--y,--z) */, 0f, 0.15f);
//    }



    public static class DoublePremiseDecompose extends NALTest {

        @Test
        void decompose_Conj_BeliefPosPos() {
            test
                    .termVolMax(3)
                    .input("(a && b). %0.9;0.9%")
                    .input("b. %0.9;0.9%")
                    .mustBelieve(cycles, "a", 0.81f, 0.66f);
        }
        @Test
        void decompose_Conj_BeliefPosNeg() {
            test
                    .termVolMax(4)
                    .input("(a && --b). %0.9;0.9%")
                    .input("b. %0.1;0.9%")
                    .mustBelieve(cycles, "a", 0.81f, 0.66f);
        }
        @Test
        void decompose_Conj_BeliefNegPos() {
            test
                    .termVolMax(3)
                    .input("(a && b). %0.1;0.9%")
                    .input("b. %0.9;0.9%")
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
            test.input("(--a || b)! %0.9;0.9%");
            test.input("b. %0.1;0.9%");
            test.mustGoal(cycles, "a", 0.19f, 0.66f);
        }

    }
}