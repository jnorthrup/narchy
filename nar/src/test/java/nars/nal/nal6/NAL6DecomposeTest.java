package nars.nal.nal6;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.QUESTION;

public abstract class NAL6DecomposeTest extends NALTest {
    private static final int cycles = 700;

    @BeforeEach
    void setup() {
        test.confTolerance(0.1f);
    }

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(6, 8);
        n.termVolMax.set(8);
        n.confMin.set(0.02f);
        return n;
    }

    static class Unsorted extends NAL6DecomposeTest {
        //        private void assertDecomposeSubjConjAandB(float fAB, float fA, float fB, float cB) {
//            assertDecomposeSubjAB(fAB, fA, 0.9f, fB, cB, true);
//        }
//
//        private void assertDecomposeSubjDisjAandB(float fAB, float fA, float fB, float cB) {
//            assertDecomposeSubjAB(fAB, fA, 0.9f, fB, cB, false);
//        }
        @Test
        void testPropositionalDecompositionConjPos() {
            test
                    .termVolMax(5)
                    .believe("--(&&,x,y,z)") //== (||,--x,--y,--z)
                    .believe("x")
                    .mustBelieve(cycles, "(y&&z)", 0f, 0.81f)
            ;
        }

        @Test
        void testPropositionalDecompositionConjNeg() {
            ////If S is the case, and (&&,S,A_1..n) is not the case, it can't be that (&&,A_1..n) is the case
            test
                    .termVolMax(5)
                    .believe("--(&&,--x,y,z)") // == (||, x, --y, --z)
                    .believe("--x")
                    .mustBelieve(cycles, "(y&&z)", 0f, 0.81f)
            ;
        }

        @Test
        void testPropositionalDecompositionDisjPos() {
            ////If S is the case, and (&&,S,A_1..n) is not the case, it can't be that (&&,A_1..n) is the case
            test
                    .termVolMax(9)
                    .believe("--(||,x,y,z)")
                    .believe("--x")
                    .mustBelieve(cycles, "(y||z)", 0f, 0.81f)
            ;
        }

        @Test
        void testPropositionalDecompositionDisjNeg() {
            ////If S is the case, and (&&,S,A_1..n) is not the case, it can't be that (&&,A_1..n) is the case
            test
                    .termVolMax(9)
                    .believe("--(||,--x,y,z)")
                    .believe("x")
                    .mustBelieve(cycles, "(y||z)", 0f, 0.81f)
            ;
        }

        @Test
        void testDecomposeDisj() {
            test
                    .termVolMax(7)
                    .believe("(||, x, z)")
                    .believe("--x")
                    .mustBelieve(cycles, "z", 1f, 0.81f)
            ;
        }
        @Test
        void testDecomposeDisj2() {
            test
                .termVolMax(16)
                .believe("(||, a,b,c,d,e)")
                .believe("--a")
                .mustBelieve(cycles, "(||,b,c,d,e)", 1f, 0.81f)
            ;
        }
        @Test
        void testDecomposeDisj3() {
            //test weak truth value to distinguish the result from structural deduction
            test
                .termVolMax(12)
                .believe("--((--a &&+1 --b) &&+1 --c)", 0.8f, 0.8f)
                .believe("--a", 0.9f, 0.9f)
                .mustBelieve(cycles, "(--b &&+1 --c)", 0.28f, 0.75f) //<--??
            ;
        }
        @Test
        void testDecomposeDisjNeg2() {
            test
                    .termVolMax(5)
                    .believe("(||, x, --z)")
                    .believe("--x")
                    .mustBelieve(cycles, "z", 0f, 0.81f)
            ;
        }


        @Test
        void testDecomposeConjNeg2() {
            test
                    .termVolMax(6)
                    .confMin(0.7f)
                    .believe("(&&, --y, --z)")
                    .mustBelieve(cycles, "y", 0f, 0.81f)
                    .mustBelieve(cycles, "z", 0f, 0.81f)
            ;
        }

        @Test
        void testDecomposeConjNeg3() {
            test
                    .believe("(&&, --y, --z, --w)")
                    .mustBelieve(cycles, "y", 0f, 0.81f)
                    .mustBelieve(cycles, "z", 0f, 0.81f)
                    .mustBelieve(cycles, "w", 0f, 0.81f)
            ;
        }


        @Test
        void disjunction_decompose_two_premises3() {
            TestNAR tester = test;
            tester.termVolMax(13);
            tester.believe("((robin --> [flying]) || (robin --> swimmer))");
            tester.believe("(robin --> swimmer)", 0.0f, 0.9f);
            tester.mustBelieve(cycles, "(robin --> [flying])", 1.00f, 0.81f);
        }


    }

    static class Unsorted_ImplSubjPred extends NAL6DecomposeTest {
//        private void assertDecomposeSubjAB(float fAB, float fA, float c, float fB, float cB, boolean conjOrDisj) {
//
////            if (conjOrDisj) {
////                PreciseTruth tA = $.t(fA, c);
////                PreciseTruth tAB = $.t(fAB, c);
////                Truth tAB_decompose_tA = TruthFunctions.decompose(tA, tAB, true, true, true, 0);
////                assertNotNull(tAB_decompose_tA);
////                System.out.println(tAB + " <= " + tA + " * " + tAB_decompose_tA);
////
////                assertEquals(fB, tAB_decompose_tA.freq(), 0.01f);
////
////                //reverse, to check:
////                @Nullable Truth tAB_check = TruthFunctions.intersection(tA, tAB_decompose_tA, 0);
////                assertEquals(tAB.freq(), tAB_check.freq(), 0.01f);
////            }
//
//            //tests:
//            // (S ==> M), (C ==> M), eventOf(C,S) |- (conjWithout(C,S) ==> M), ...
//            String AB = conjOrDisj ? "(A && B)" : "(A || B)";
//            test
//                    .termVolMax(conjOrDisj ? 5 : 8)
//                    .believe('(' + AB + " ==> X)", fAB, c)
//                    .believe("(A ==> X)", fA, c)
//                    .mustBelieve(cycles, "(B ==> X)", fB, cB);
//
//            float df = 0.02f;
//            if (fB < 1f-df)
//                    test.mustNotOutput(cycles, "(B ==> X)", BELIEF, fB + df, 1, 0, 1, (t)->true);
//            if (fB > df)
//                    test.mustNotOutput(cycles, "(B ==> X)", BELIEF, 0, fB - df, 0, 1, (t)->true);
//
//        }

//        @Test
//        void testDecomposeSubjConjPosPos() {
//            assertDecomposeSubjConjAandB(1f, 1f, 1f, 0.81f);
//        }
//
//        @Test
//        void testDecomposeSubjConjPosWeakPos() {
//            assertDecomposeSubjConjAandB(0.9f, 1f, 0.9f, 0.73f);
//        }
//
//        @Test
//        void testDecomposeSubjDisjPosPos() {
//            assertDecomposeSubjDisjAandB(1f, 0.9f, 0.1f, 0.08f);
//        }
//
//        @Test
//        void testDecomposeSubjDisjPosPosWeak() {
//            assertDecomposeSubjDisjAandB(1f, 0.5f, 0.5f /*0.1f*/, 0.08f);
//        }
//
//        @Test
//        void testDecomposeSubjDisjNegPos() {
//            assertDecomposeSubjDisjAandB(1f, 0f, 1f, 0.81f);
//        }
//
//        @Test
//        void testDecomposeSubjDisjPosPosOppositeWeaker() {
//            assertDecomposeSubjDisjAandB(0.75f, 0.1f, 0.78f, 0.66f);
//        }
//
//
//        @Test
//        void testDecomposeSubjConjPosWeakPosWeak() {
//            assertDecomposeSubjConjAandB(0.7f, 0.6f, .42f, 0.3f);
//        }
//
//        @Test
//        void testDecomposeSubjConjNegNeg() {
//            assertDecomposeSubjConjAandB(0f, 0f, 0f, 0.81f);
//        }

        //    @Test void DecomposeSubjConjNegWeakNeg() {
//        test.logDebug();
//        assertDecomposeSubjConjAandB(0.1f, 0.75f, 0.63f, 0.3f);
//    }
//        @Test
//        void testDecomposeSubjConjNegWeakNeg2() {
//            assertDecomposeSubjConjAandB(0.25f, 0.5f, 0.63f, 0.3f);
//        }
//


        @Test @Disabled
        void impl_conjunction_subj_conj_decompose_conditional_neg() {
            test

                    .termVolMax(6)
                    .input("((a && b) ==> --x).")
                    .input("(--b ==> --x).")
                    .mustBelieve(cycles, "(a ==> x)", 0f, 0.81f) //via decompose
//                    .mustNotOutput(cycles,"(a ==> x)", BELIEF, 0.1f, 1f, 0.1f, 1f)
//                    .mustNotOutput(cycles,"(--b ==> x)", BELIEF, 0.1f, 1f, 0.1f, 1f)
//                    .mustNotOutput(cycles, "a", BELIEF)
//                    .mustNotOutput(cycles, "b", BELIEF)
//                    .mustNotOutput(cycles, "x", BELIEF)
//                    .mustNot(BELIEF, (x)->{
//                        if (x.volume()==1) {
//                            return true;
//                        }
//                        return false;
//                    })
            ;
        }

        @Test
        void disjunction_impl_decompose_two_premises3() {
            TestNAR tester = test;
            tester.termVolMax(13);
            tester.believe("(((robin --> [flying]) || (robin --> swimmer)) ==> x)");
            tester.believe("((robin --> swimmer) ==> x)", 0.0f, 0.9f);
            tester.mustBelieve(cycles, "((robin --> [flying]) ==> x)", 1.00f, 0.81f);
        }

        @Test
        void disjunction_impl_decompose_two_premises_neg() {
            TestNAR tester = test;
            tester.termVolMax(13).confMin(0.8f);
            tester.believe("(((robin --> [flying]) || (robin --> swimmer)) ==> --x)");
            tester.believe("((robin --> swimmer) ==> --x)", 0.0f, 0.9f);
            tester.mustBelieve(cycles, "((robin --> [flying]) ==> --x)", 1.00f, 0.81f);
        }

//    @Test
//    void compound_decomposition_subj_posneg() {
//        test.nar.termVolumeMax.setAt(9);
//        test.believe("((b && --c)==>a)", 1.0f, 0.9f)
//                .mustBelieve(cycles, "(b==>a)", 1.00f, 0.81f)
//                .mustBelieve(cycles, "(--c==>a)", 1.00f, 0.81f);
//    }
//
//    @Test
//    void compound_decomposition_pred_posneg() {
//        test.nar.termVolumeMax.setAt(9);
//        test.believe("(a==>(b && --c))", 1.0f, 0.9f)
//                .mustBelieve(cycles, "(a==>b)", 1.00f, 0.81f)
//                .mustBelieve(cycles, "(a==>c)", 0.00f, 0.81f);
//    }


        @Test
        void impl_conjunction_subj_intersection_decompose_conditional_neg() {
            test
                    .believe("((a || b) ==> --x)")
                    .input("(b ==> --x).")
                    .mustBelieve(cycles, "(a ==> x)", 0f, 0.81f) //via decompose
            ;
        }

        @Test
        void impl_conjunction_subj_decompose_conditional2b() {
            test
                    .believe("((a && --b) ==> --x)")
                    .input("(--b  ==> --x).")
                    .mustBelieve(cycles, "(a ==> --x)", 1f, 0.81f) //via decompose
            ;
        }

        @Test
        void impl_conjunction_pred_decompose_conditional() {
            test
                    .termVolMax(6)
                    .believe("(x ==> --(a && b))")
                    .input("(x ==> b). %0.75;0.9%")
                    .mustBelieve(cycles, "(x ==> a)", 0.25f, 0.61f) //via decompose
            ;
        }

        @Test
        void impl_conjunction_predneg_decompose_conditional() {
            test
                    .believe("(x ==> --(a && --b))")
                    .input("(x ==> --b). %0.75;0.9%")
                    .mustBelieve(cycles, "(x ==> a)", 0.25f, 0.61f) //via decompose
            ;
        }

        @Disabled
        @Test
        void impl_disjunction_subj_decompose_conditional() {

            test
                    .termVolMax(8)
                    .believe("((a || b) ==> x)")
                    .input("(b ==> x). %0.1;0.9%")
                    .mustBelieve(cycles, "(a ==> x)", 0.9f, 0.73f) //via decompose
                    .mustNotOutput(cycles, "(b ==> x)", BELIEF, 0.15f, 1f, 0, 1f)
                    .mustNotOutput(cycles, "(a ==> x)", BELIEF, 0f, 0.7f, 0, 1f)
            ;
        }

        @Test
        void impl_disjunction_pred_decompose_conditional() {
            test
                    .termVolMax(8)
                    .believe("(x ==> (a || b))")
                    .input("(x ==> b). %0.25;0.9%")
                    .mustBelieve(cycles, "(x ==> a)", 0.75f, 0.61f) //via decompose
            ;
        }

        @Test
        void impl_disjunction_pred_decompose_conditional2() {
            test
                    .termVolMax(8)
                    .believe("(x ==> (a || b))")
                    .input("(x ==> b). %0.75;0.9%")
                    .mustBelieve(cycles, "(x ==> a)", 0.25f, 0.20f) //via decompose
            ;
        }

        @Test
        void compound_decomposition_two_premises1() {

            TestNAR tester = test;
            tester.termVolMax(12);
            tester.believe("(bird:robin ==> --(animal:robin && (robin-->[flying])))", 1.0f, 0.9f);
            tester.believe("          (bird:robin ==> (robin-->[flying]))");
            tester.mustBelieve(cycles, "(bird:robin ==> animal:robin)", 0.00f, 0.81f);

        }

        @Test
        void compound_decomposition_two_premises1_simpler() {

            test
                    .termVolMax(6)
                    .confMin(0.75f)
                    .believe("(b ==> --(a && r))", 1.0f, 0.9f)
                    .believe("          (b ==> r)")
                    .mustBelieve(cycles, "(b ==> a)", 0.00f, 0.81f);

        }

        @Test
        void testDecomposeImplSubjConjQuestion() {
            test
                    .ask("( (&&, y, z) ==> x )")
                    .mustOutput(cycles, "( y ==> x )", QUESTION)
                    .mustOutput(cycles, "( z ==> x )", QUESTION)
            ;
        }

        @Test
        void testDecomposeImplSubjDisjQuestion() {
            test
                    .ask("( (||, y, z) ==> x )")
                    .mustOutput(cycles, "( y ==> x )", QUESTION)
                    .mustOutput(cycles, "( z ==> x )", QUESTION)
            ;
        }

    }

    static class SinglePremiseConjPred extends NAL6DecomposeTest {

        @Test
        void testDecomposeImplPred1b() {
            test.confTolerance(0.03f)
                    .believe("( x ==> (&&, y, z, w) )")
                    .mustBelieve(cycles, "( x ==> y )", 1f, 0.73f)
                    .mustBelieve(cycles, "( x ==> z )", 1f, 0.73f)
                    .mustBelieve(cycles, "( x ==> w )", 1f, 0.73f)
            ;
        }

        @Test
        void testDecomposeImplPred2() {
            test.nar.termVolMax.set(13);
            test.nar.confMin.set(0.7f);
            test
                    .believe("( (a,#b) ==> (&&, (x,#b), y, z ) )")
                    .mustBelieve(cycles, "( (a,#b) ==> (x,#b) )", 1f, 0.73f)
                    .mustBelieve(cycles, "( (a,#b) ==> y )", 1f, 0.73f)
                    .mustBelieve(cycles, "( (a,#b) ==> z )", 1f, 0.73f)
            ;
        }


        @Test
        void testDecomposeImplPredNeg() {
            test
                    .believe("( x ==> (&&, --y, --z ) )")
                    .mustBelieve(cycles, "( x ==> --y )", 1f, 0.81f)
                    .mustBelieve(cycles, "( x ==> --z )", 1f, 0.81f)
            ;
        }

        @Test
        void testDecomposeImplPred1() {
            test
                    .believe("( x ==> (y && z) )")
                    .mustBelieve(cycles, "( x ==> y )", 1f, 0.81f)
                    .mustBelieve(cycles, "( x ==> z )", 1f, 0.81f)
            ;
        }
    }

    static class Impl_SinglePremise extends NAL6DecomposeTest {
        @Test
        void testDecomposeImplSubj1Conj() {
            test
                    .termVolMax(5)
                    .believe("( (y && z) ==> x )")
                    .mustBelieve(cycles, "( y ==> x )", 1f, 0.45f)
                    .mustBelieve(cycles, "( z ==> x )", 1f, 0.45f)
            ;
        }

        @Test
        void testDecomposeImplSubj1Disj() {
            test
                    .termVolMax(8)
                    .believe("( (y || z) ==> x )")
                    .mustBelieve(cycles, "( y ==> x )", 1f, 0.81f)
                    .mustBelieve(cycles, "( z ==> x )", 1f, 0.81f)
            ;
        }

        @Test
        void testDecomposeImplSubj1Disj_neg_pos() {
            test
                    .termVolMax(8)
                    .believe("( (--y || z) ==> x )")
                    .mustBelieve(cycles, "( --y ==> x )", 1f, 0.81f)
                    .mustBelieve(cycles, "( z ==> x )", 1f, 0.81f)
            ;
        }

        @Test
        void testDecomposeImplPredDisjBelief_pos_pos() {
            test
                .termVolMax(7)
                    .believe("( x ==> (y || z))")
                    .mustBelieve(cycles, "( x ==> y )", 1f, 0.45f)
                    .mustBelieve(cycles, "( x ==> z )", 1f, 0.45f)
            ;
        }


//    @Test
//    void testDecomposeImplSubj1b() {
//        test.confTolerance(0.03f)
//                .believe("( (&&, y, z, w) ==> x )")
//                .mustBelieve(cycles, "( y ==> x )", 1f, 0.73f)
//                .mustBelieve(cycles, "( z ==> x )", 1f, 0.73f)
//                .mustBelieve(cycles, "( w ==> x )", 1f, 0.73f)
//        ;
//    }

//    @Test
//    void testDecomposeImplSubj1bNeg() {
//        test.confTolerance(0.03f)
//                .believe("( (&&, --y, --z, --w) ==> x )")
//                .mustBelieve(cycles, "( --y ==> x )", 1f, 0.73f)
//                .mustBelieve(cycles, "( --z ==> x )", 1f, 0.73f)
//                .mustBelieve(cycles, "( --w ==> x )", 1f, 0.73f)
//        ;
//    }


        @Test
        void testDecomposeImplsubjNeg() {
            test
                    .believe("( (&&, --y, --z ) ==> x )")
                    .mustBelieve(cycles, "( --y ==> x )", 1f, 0.45f)
                    .mustBelieve(cycles, "( --z ==> x )", 1f, 0.45f)
            ;
        }


    }

}
