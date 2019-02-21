package nars.nal.nal6;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.TestNAR;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import nars.truth.func.TruthFunctions;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NAL6DecomposeTest extends NALTest {
    private static final int cycles = 350;

    @BeforeEach
    void setup() {
        test.confTolerance(0.1f);
    }

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(6);
        n.confMin.set(0.01f);
        return n;
    }

    private void assertDecomposeSubjConjAandB(float fAB, float fA, float fB, float cB) {
        assertDecomposeSubjAB(fAB, fA, 0.9f, fB, cB, true);
    }
    private void assertDecomposeSubjDisjAandB(float fAB, float fA, float fB, float cB) {
        assertDecomposeSubjAB(fAB, fA, 0.9f, fB, cB, false);
    }

    private void assertDecomposeSubjAB(float fAB, float fA, float c, float fB, float cB, boolean conjOrDisj) {

        if (conjOrDisj) {
            PreciseTruth tA = $.t(fA, c);
            PreciseTruth tAB = $.t(fAB, c);
            Truth tAB_decompose_tA = TruthFunctions.decompose(tA, tAB, true, true, true, 0);
            System.out.println(tAB + " <= " + tA + " * " + tAB_decompose_tA);

            assertEquals(fB, tAB_decompose_tA.freq(), 0.01f);

            //reverse, to check:
            @Nullable Truth tAB_check = TruthFunctions.intersection(tA, tAB_decompose_tA, 0);
            assertEquals(tAB.freq(), tAB_check.freq(), 0.01f);
        }

        //tests:
        // (S ==> M), (C ==> M), eventOf(C,S) |- (conjWithout(C,S) ==> M), (Belief:DecomposeNegativePositivePositive)
        String AB = conjOrDisj ? "(A && B)" : "(A || B)";
        test
                .termVolMax(conjOrDisj ? 5 : 8)
                .believe("(" + AB + " ==> X)", fAB, c)
                .believe("(A ==> X)", fA, c)
                .mustBelieve(cycles, "(B ==> X)", fB, cB)
        ;
    }

    @Test void testDecomposeSubjConjPosPos() {
        assertDecomposeSubjConjAandB(1f, 1f, 1f, 0.81f);
    }
    @Test void testDecomposeSubjConjPosWeakPos() {
        assertDecomposeSubjConjAandB(0.9f, 1f, 0.9f, 0.73f);
    }

    @Test void testDecomposeSubjDisjPosPos() {
        assertDecomposeSubjDisjAandB(1f, 0.9f, 0.1f, 0.08f);
    }
    @Test void testDecomposeSubjDisjPosPosWeak() {
        assertDecomposeSubjDisjAandB(1f, 0.5f, 0.1f, 0.08f);
    }
    @Test void testDecomposeSubjDisjPosPosOpposite() {
        assertDecomposeSubjDisjAandB(1f, 0.1f, 1f, 0.73f);
    }
    @Test void testDecomposeSubjDisjPosPosOppositeWeaker() {
        assertDecomposeSubjDisjAandB(0.75f, 0.1f, 0.78f, 0.66f);
    }


    @Test void testDecomposeSubjConjPosWeakPosWeak() {
        assertDecomposeSubjConjAandB(0.7f, 0.6f, .42f, 0.3f);
    }
    @Test void testDecomposeSubjConjNegNeg() {
        assertDecomposeSubjConjAandB(0f, 1f, 0f, 0.81f);
    }
//    @Test void testDecomposeSubjConjNegWeakNeg() {
//        test.logDebug();
//        assertDecomposeSubjConjAandB(0.1f, 0.75f, 0.63f, 0.3f);
//    }
    @Test void testDecomposeSubjConjNegWeakNeg2() {
        assertDecomposeSubjConjAandB(0.25f, 0.5f, 0.63f, 0.3f);
    }

    @Disabled @Test
    void testDecomposeImplSubj1Conj() {
        test
                .believe("( (y && z) ==> x )")
                .mustBelieve(cycles, "( y ==> x )", 1f, 0.81f)
                .mustBelieve(cycles, "( z ==> x )", 1f, 0.81f)
        ;
    }

    @Test
    void testDecomposeImplSubj1Disj() {
        test
                .believe("( (y || z) ==> x )")
                .mustBelieve(cycles, "( y ==> x )", 1f, 0.81f)
                .mustBelieve(cycles, "( z ==> x )", 1f, 0.81f)
        ;
    }

    @Test
    void testDecomposeImplPredDisjBelief() {
        test
                .believe("( x ==> (y || z))")
                .mustBelieve(cycles, "( x ==> y )", 1f, 0.81f)
                .mustBelieve(cycles, "( x ==> z )", 1f, 0.81f)
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
    void testDecomposeImplPred1b() {
        test.confTolerance(0.03f)
                .believe("( x ==> (&&, y, z, w) )")
                .mustBelieve(cycles, "( x ==> y )", 1f, 0.73f)
                .mustBelieve(cycles, "( x ==> z )", 1f, 0.73f)
                .mustBelieve(cycles, "( x ==> w )", 1f, 0.73f)
        ;
    }


    @Disabled @Test
    void testDecomposeImplPred2() {
        test.nar.termVolumeMax.set(11);
        test.nar.confMin.set(0.6f);
        test
                .believe("( (a,#b) ==> (&&, (x,#b), y, z ) )")
                .mustBelieve(cycles, "( (a,#b) ==> (x,#b) )", 1f, 0.73f)
                .mustBelieve(cycles, "( (a,#b) ==> y )", 1f, 0.73f)
                .mustBelieve(cycles, "( (a,#b) ==> z )", 1f, 0.73f)
        ;
    }

    @Disabled
    @Test
    void testDecomposeImplsubjNeg() {
        test
                .believe("( (&&, --y, --z ) ==> x )")
                .mustBelieve(cycles, "( --y ==> x )", 1f, 0.81f)
                .mustBelieve(cycles, "( --z ==> x )", 1f, 0.81f)
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
    @Test
    void impl_conjunction_subj_decompose_conditional() {
        test
                .believe("((a && b) ==> x)")
                .input("--(b ==> x). %0.75;0.9%")
                .mustBelieve(cycles, "(a ==> x)", 0.75f, 0.61f) //via decompose
        ;
    }
    @Test
    void impl_conjunction_pred_decompose_conditional() {
        test
                .believe("(x ==> --(a && b))")
                .input("(x ==> b). %0.75;0.9%")
                .mustBelieve(cycles, "(x ==> a)", 0.25f, 0.61f) //via decompose
        ;
    }
    @Test
    void impl_conjunction_predneg_decompose_conditional() {
        test
                .believe("(x ==> --(a && --b))")
                .input("--(x ==> b). %0.75;0.9%")
                .mustBelieve(cycles, "(x ==> a)", 0.25f, 0.61f) //via decompose
        ;
    }
    @Test
    void impl_disjunction_subj_decompose_conditional() {
        test
                .believe("((a || b) ==> x)")
                .input("--(b ==> x). %0.75;0.9%")
                .mustBelieve(cycles, "(a ==> x)", 0.75f, 0.61f) //via decompose
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
    void compound_decomposition_two_premises1() {

        TestNAR tester = test;
        tester.believe("(bird:robin ==> --(animal:robin && (robin-->[flying])))", 1.0f, 0.9f);
        tester.believe("          (bird:robin ==> (robin-->[flying]))");
        tester.mustBelieve(cycles, "--(bird:robin ==> animal:robin)", 1.00f, 0.81f);

    }

    @Test
    void disjunction_decompose_two_premises3() {
        TestNAR tester = test;
        tester.believe("((robin --> [flying]) || (robin --> swimmer))");
        tester.believe("(robin --> swimmer)", 0.0f, 0.9f);
        tester.mustBelieve(cycles, "(robin --> [flying])", 1.00f, 0.81f);
    }
    @Test
    void disjunction_impl_decompose_two_premises3() {
        TestNAR tester = test;
        tester.believe("(((robin --> [flying]) || (robin --> swimmer)) ==> x)");
        tester.believe("((robin --> swimmer) ==> x)", 0.0f, 0.9f);
        tester.mustBelieve(cycles, "((robin --> [flying]) ==> x)", 1.00f, 0.81f);
    }
    @Test
    void disjunction_impl_decompose_two_premises_neg() {
        TestNAR tester = test;
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

}
