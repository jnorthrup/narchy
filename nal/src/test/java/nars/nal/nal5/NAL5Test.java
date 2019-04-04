package nars.nal.nal5;

import nars.NAR;
import nars.NARS;
import nars.term.Term;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class NAL5Test extends NALTest {

    private final int cycles = 530;

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(6);
        n.termVolumeMax.set(10);
        n.confMin.set(0.1f);
        return n;
    }

    @BeforeEach
    void setup() {
        test.confTolerance(0.2f);
    }

    @Test
    void revision() {

        test
                .mustBelieve(cycles, "<(robin --> [flying]) ==> a>", 0.85f, 0.92f)
                .believe("<(robin --> [flying]) ==> a>")
                .believe("<(robin --> [flying]) ==> a>", 0.00f, 0.60f);

    }

    @Test
    void deduction() {

        TestNAR tester = test;
        tester.believe("(a ==> b)");
        tester.believe("((robin --> [flying]) ==> a)");
        tester.mustBelieve(cycles, "((robin --> [flying]) ==> b)", 1.00f, 0.81f);

    }

    @Test
    void deductionPosCommon() {
        test.nar.termVolumeMax.set(3);
        test
                .believe("(x ==> z)")
                .believe("(z ==> y)")
                .mustBelieve(cycles, "(x ==> y)", 1.00f, 0.81f);

    }

    @Test
    void deductionNegCommon() {
        test
                .believe("(x ==> --z)")
                .believe("(--z ==> y)")
                .mustBelieve(cycles, "(x ==> y)", 1.00f, 0.81f);

    }

    @Test
    void exemplification() {

        TestNAR tester = test;
        tester.believe("<(robin --> [flying]) ==> a>");
        tester.believe("(a ==> b)");
        tester.mustBelieve(cycles, "<b ==> (robin --> [flying])>.", 1.00f, 0.45f);

    }

    @Test
    void depVarUniqueness() {

        test
                .termVolMax(11)
                .believe("f(x,#1)")
                .believe("f(y,#1)")
                //both forms
                .mustBelieve(cycles, "(f(x,#1) ==> f(y,#2))", 1.00f, 0.45f)
                .mustBelieve(cycles, "(f(y,#1) ==> f(x,#2))", 1.00f, 0.45f)
                .mustBelieve(cycles, "(f(x,#1) ==> f(y,#1))", 1.00f, 0.45f)
        ;

    }

    @Test
    void induction() {
        /*
         (a ==> b).
         <(robin --> [flying]) ==> b>. %0.8%
         OUT: <a ==> (robin --> [flying])>. %1.00;0.39%
         OUT: <(robin --> [flying]) ==> a>. %0.80;0.45%
         */
        TestNAR tester = test;
        tester.nar.termVolumeMax.set(7);
        tester.believe("(a ==> b)", 1f, 0.9f);
        tester.believe("<(robin --> [flying]) ==> b>", 0.8f, 0.9f);
        tester.mustBelieve(cycles, "<a ==> (robin --> [flying])>", 1.00f, 0.39f);
        tester.mustBelieve(cycles, "<(robin --> [flying]) ==> a>", 0.80f, 0.45f);

    }


    @Test
    void abduction() {

        /*
        (a ==> b).         
        <a ==> (robin --> [flying])>. %0.80%  
        14
         OUT: <(robin --> [flying]) ==> b>. %1.00;0.39% 
         OUT: <b ==> (robin --> [flying])>. %0.80;0.45% 
         */
        TestNAR tester = test;
        tester.nar.termVolumeMax.set(10);
        tester.believe("(a ==> b)");
        tester.believe("(a ==> (robin --> [flying]))", 0.8f, 0.9f);
        tester.mustBelieve(cycles, "((robin --> [flying]) ==> b)", 1.00f, 0.39f);
        tester.mustBelieve(cycles, "(b ==> (robin --> [flying]))", 0.80f, 0.45f);


    }

    @Test
    void abductionSimple() {

        /*
        (a ==> b).         
        <a ==> (robin --> [flying])>. %0.80%  
        14
         OUT: <(robin --> [flying]) ==> b>. %1.00;0.39% 
         OUT: <b ==> (robin --> [flying])>. %0.80;0.45% 
         */

        test.nar.termVolumeMax.set(3);
        test
                .believe("(a ==> b)")
                .believe("(a ==> c)", 0.8f, 0.9f)
                .mustBelieve(cycles, "(c ==> b)", 1.00f, 0.39f)
                .mustBelieve(cycles, "(b ==> c)", 0.80f, 0.45f);
    }

    @Test
    void abductionSimpleNeg() {
        test.nar.termVolumeMax.set(4);
        test
                .believe("--(a ==> b)")
                .believe("--(a ==> c)", 0.8f, 0.9f)
                .mustBelieve(cycles, "(--c ==> b)", 0.00f, 0.39f)
                .mustBelieve(cycles, "(--b ==> c)", 0.20f, 0.45f);
    }

    @Test
    void testImplBeliefPosPos() {


        test

                .believe("b")
                .believe("(b==>c)", 1, 0.9f)
                .mustBelieve(cycles, "c", 1.00f, 0.81f);
    }


    @Test
    void testImplBeliefPosNeg() {


        test
                .believe("b")
                .believe("(b ==> --c)", 1, 0.9f)
                .mustBelieve(cycles, "c", 0.00f, 0.81f);
    }

    @Test
    void detachment() {

        test
                .believe("(a ==> b)")
                .believe("a")
                .mustBelieve(cycles, "b", 1.00f, 0.81f);

    }


    @Test
    void detachment2() {

        TestNAR tester = test;
        tester.believe("(a ==> b)", 0.70f, 0.90f);
        tester.believe("b");
        tester.mustBelieve(cycles, "a",
                0.7f, 0.45f);


    }


    @Test
    void anonymous_analogy1_depvar() {
        test.nar.termVolumeMax.set(5);
        test
                .believe("(a:#1 && y)")
                .believe("a:x", 0.80f, 0.9f)
                .mustBelieve(cycles, "y", 0.80f, 0.65f);
    }

    @Test
    void anonymous_analogy1_depvar_neg() {
        test.nar.termVolumeMax.set(8);
        test
                .believe("(--(#1-->a) && y)")
                .believe("(x-->a)", 0.20f, 0.9f)
                .mustBelieve(cycles, "y", 0.80f, 0.65f);
    }

    @Test
    void anonymous_analogy1_pos2() {
        test.termVolMax(3)
                .believe("(x && y)")
                .believe("x", 0.80f, 0.9f)
                .mustBelieve(cycles, "y",
                        0.80f, 0.58f);

    }

    @Test
    void anonymous_analogy1_pos3() {
        test
                .believe("(&&, x, y, z)")
                .believe("x", 0.80f, 0.9f)
                .mustBelieve(cycles, "(y && z)", 0.80f,
                        0.58f);

    }

    @Test
    void anonymous_analogy1_neg2() {
        test
                .believe("(&&, --x, y, z)")
                .believe("x", 0.20f, 0.9f)
                .mustBelieve(cycles, "(&&,y,z)", 0.80f,
                        0.65f /*0.43f*/);
    }

    @Test
    void compound_composition_Pred() {

        TestNAR tester = test;
        tester.believe("(a ==> b)");
        tester.believe("<a ==> (robin --> [flying])>", 0.9f, 0.9f);
        tester.mustBelieve(cycles, " <a ==> (&&,(robin --> [flying]),b)>",
                0.90f, 0.81f);

    }

    @Test
    void compound_composition_PredPosNeg() {

        TestNAR tester = test;
        tester.believe("(a ==> b)");
        tester.believe("--(a==>c)");
        tester.mustBelieve(cycles, "(a ==> (b && --c))",
                1f, 0.81f);

    }


    @Test
    void compound_composition_Subj() {
        TestNAR tester = test;
        test.termVolMax(14);
        tester.believe("(bird:robin ==> animal:robin)");
        tester.believe("((robin-->[flying]) ==> animal:robin)", 0.9f, 0.81f);
        tester.mustBelieve(cycles * 2, " ((bird:robin && (robin-->[flying])) ==> animal:robin)", 0.9f, 0.73f);
        //tester.mustBelieve(cycles*2, " ((bird:robin || (robin-->[flying])) ==> animal:robin)", 1f, 0.73f);
    }

    @Test
    void compound_composition_SubjNeg() {
        TestNAR tester = test;
        test.termVolMax(14).confMin(0.65f);
        tester.believe("--(bird:robin ==> animal:nonRobin)");
        tester.believe("--((robin-->[flying]) ==> animal:nonRobin)", 0.9f, 0.81f);
        tester.mustBelieve(cycles, " ((bird:robin && (robin-->[flying])) ==> animal:nonRobin)", 0.1f, 0.73f);
        //tester.mustBelieve(cycles, " ((bird:robin || (robin-->[flying])) ==> animal:nonRobin)", 0f, 0.73f);
    }


    @Test
    void compound_decomposition_one_premise_pos() {

        TestNAR tester = test;

        tester.believe("((robin --> [flying]) && (robin --> swimmer))", 1.0f, 0.9f);
        tester.mustBelieve(cycles, "(robin --> swimmer)", 1.00f, 0.81f);
        tester.mustBelieve(cycles, "(robin --> [flying])", 1.00f, 0.81f);
    }

    @Test
    void impl_disjunction_subj_decompose_one_premise() {
        test
                .termVolMax(8)
                .believe("((a || b) ==> x)")
                .mustBelieve(cycles, "(a ==> x)", 1f, 0.81f)
                .mustBelieve(cycles, "(b ==> x)", 1f, 0.81f)
        ;
    }

    @Test
    void impl_disjunction_pred_decompose_one_premise() {
        test
                .believe("(x ==> (a || b))")
                .mustBelieve(cycles, "(x ==> a)", 1f, 0.81f)
                .mustBelieve(cycles, "(x ==> b)", 1f, 0.81f)
        ;
    }

    @Disabled
    @Test
    void disjunction_decompose_one_premise() {
        test
                .believe("(||, a, b)")
                .mustBelieve(cycles, "a", 1f, 0.40f)
                .mustBelieve(cycles, "b", 1f, 0.40f)
        ;
    }

    @Disabled
    @Test
    void disjunction_decompose_one_premise_pos_neg() {
        test
                .believe("(||, a, --b)")
                .mustBelieve(cycles, "a", 1f, 0.40f)
                .mustBelieve(cycles, "b", 0f, 0.40f)
        ;
    }


    /**
     * not sure this one makes logical sense
     */
    @Disabled
    @Test
    void compound_composition_one_premises() {

        TestNAR tester = test;
        tester.believe("(robin --> [flying])");
        tester.ask("(||,(robin --> [flying]),(robin --> swimmer))");

        tester.mustBelieve(cycles, " ((robin --> swimmer) || (robin --> [flying]))", 1.00f, 0.81f);
    }


    @Test
    void conjunction_decomposition_one_premises() {

        test

                .believe("(&&,(robin --> swimmer),(robin --> [flying]))", 0.9f, 0.9f)
                .mustBelieve(cycles, "(robin --> swimmer)", 0.9f, 0.73f)
                .mustBelieve(cycles, "(robin --> [flying])", 0.9f, 0.73f);

    }

    @Test
    void conjunction_decomposition_one_premises_simple() {
        test
                .believe("(&&, a, b)")
                .mustBelieve(cycles, "a", 1f, 0.81f)
                .mustBelieve(cycles, "b", 1f, 0.81f)
        ;
    }

    @Test
    void negation0() {

        test
                .mustBelieve(cycles, "(robin --> [flying])", 0.10f, 0.90f)
                .believe("(--,(robin --> [flying]))", 0.9f, 0.9f);


    }

    @Test
    void negation1() {

        test
                .mustBelieve(cycles, "<robin <-> parakeet>", 0.10f, 0.90f)
                .believe("(--,<robin <-> parakeet>)", 0.9f, 0.9f);


    }


    @Test
    void contraposition() {
        TestNAR tester = test;
        tester.believe("(--(robin --> bird) ==> (robin --> [flying]))", 0.1f, 0.9f);
        tester.mustBelieve(cycles, " (--(robin --> [flying]) ==> (robin --> bird))",
                0.1f, 0.36f);
        //0f, 0.45f);
    }

    @Test
    void contrapositionPos() {
        test.nar.termVolumeMax.set(9);
        test
                .believe("(--B ==> A)", 0.9f, 0.9f)
                .mustBelieve(cycles, " (--A ==> B)",
                        0.9f, 0.36f);
        //0.1f, 0.36f);
        //0f, 0.08f);
    }

    @Test
    void contrapositionNeg() {
        test
                .believe("(--B ==> A)", 0.1f, 0.9f)
                .mustBelieve(cycles, " (--A ==> B)",
                        0.1f, 0.36f);

    }

    @Test
    void conditional_deduction_simple() {
        TestNAR tester = test;
        tester.believe("((x && y) ==> a)");
        tester.believe("x");
        tester.mustBelieve(cycles, "(y && a)", 1.00f, 0.81f);
        tester.mustBelieve(cycles, "(y ==> a)", 1.00f, 0.81f);
    }

    @Test
    void conditional_deduction_neg_simple() {
        TestNAR tester = test;
        tester.believe("((--x && y) ==> a)");
        tester.believe("--x");
        tester.mustBelieve(cycles, "(y && a)", 1.00f, 0.81f);
        tester.mustBelieve(cycles, "(y ==> a)", 1.00f, 0.81f);
    }

    @Test
    void conditional_deduction() {
        test
                .confMin(0.75f)
                .termVolMax(14)
                .believe("<(&&,(robin --> [flying]),(robin --> [withWings])) ==> a>")
                .believe("(robin --> [flying])")
                .mustBelieve(cycles, " <(robin --> [withWings]) ==> a>", 1.00f, 0.81f);
    }

    @Test
    void conditional_deduction_unification() {

        test
                .termVolMax(12)
                .believe("<(&&,(#x --> [flying]),(#x --> [withWings])) ==> a>")
                .believe("(robin --> [flying])")
                .mustBelieve(cycles, " ((robin-->[withWings])==>a)", 1.00f, 0.81f);
        //.mustBelieve(cycles, " (((robin-->[flying])&&(robin-->[withWings]))==>a)", 1.00f, 0.81f);

    }

    @Test
    void conditional_deduction_neg() {

        TestNAR tester = test;
        tester.nar.termVolumeMax.set(12);
        tester.believe("((--(robin-->[swimming]) && (robin --> [withWings])) ==> a)");
        tester.believe("--(robin-->[swimming])");
        tester.mustBelieve(cycles, "((robin --> [withWings]) ==> a)", 1.00f, 0.81f);

    }


    @Test
    void conditional_deduction2() {


        test
                .termVolMax(15)
                .believe("<(&&,(robin --> [chirping]),(robin --> [flying]),(robin --> [withWings])) ==> a>")
                .believe("(robin --> [flying])")
                .mustBelieve(cycles, " <(&&,(robin --> [chirping]),(robin --> [withWings])) ==> a>", 1.00f, 0.81f);

    }


    @Test
    void conditional_deduction3() {
        test.nar.termVolumeMax.set(11);

        TestNAR tester = test;
        tester.believe("<(&&,a,(robin --> [living])) ==> b>");
        tester.believe("<(robin --> [flying]) ==> a>");
        tester.mustBelieve(cycles, " <(&&,(robin --> [flying]),(robin --> [living])) ==> b>",
                1.00f, 0.81f);

    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogism_simple() {
        test.termVolMax(5)
            .believe("(x ==> y)")
            .believe("((z && x) ==> y)")
            .mustBelieve(cycles, "z", 1.00f, 0.45f);
    }
    @Test
    void conditional_antiAbduction_viaMultiConditionalSyllogism_simple_a() {
        test.termVolMax(6)
                .believe("(x ==> --y)")
                .believe("((z && x) ==> y)")
                .mustBelieve(cycles, "z", 0.00f, 0.45f);
    }
    @Test
    void conditional_antiAbduction_viaMultiConditionalSyllogism_simple_b() {
        test.termVolMax(6)
                .believe("(x ==> y)")
                .believe("((z && x) ==> --y)")
                .mustBelieve(cycles, "z", 0.00f, 0.45f);
    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogismSimple_NegPredicate() {
        test.termVolMax(6)
            .believe("(x ==> --y)")
            .believe("((z && x) ==> --y)")
            .mustBelieve(cycles, "z", 1.00f, 0.45f);
    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogismSimple_Predicate_Polarity_Mismatch() {
        test
                .believe("(x ==> y)")
                .believe("((z && x) ==> --y)")
                .mustNotBelieve(cycles, "z", 1.00f, 0.45f, (s,e)->true);
    }
    @Test
    void conditional_abduction_viaMultiConditionalSyllogismSimple_NegSubCondition_The() {
        test
                .termVolMax(8)
                .believe("(x ==> y)")
                .believe("((--z && x) ==> y)")
                .mustBelieve(cycles, "--z", 1.00f, 0.45f);
    }
    @Test
    void conditional_abduction_viaMultiConditionalSyllogismSimple_NegSubCondition_Other() {
        test
                .termVolMax(7)
                .believe("(--x ==> y)")
                .believe("((z && --x) ==> y)")
                .mustBelieve(cycles, "z", 1.00f, 0.45f);
    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogismSimple2() {

        test
                .termVolMax(9).confMin(0.4f)
                .believe("((&&,x1,x2) ==> y)")
                .believe("((&&,x1,x2,z) ==> y)")
                .mustBelieve(cycles, "z", 1.00f, 0.45f);

    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogism() {

        test
                .termVolMax(11).confMin(0.4f)
                .believe("(flying:robin ==> bird:robin)")
                .believe("((swimmer:robin && flying:robin) ==> bird:robin)")
                .mustBelieve(cycles, "swimmer:robin", 1.00f, 0.45f);

    }


    @Test
    void conditional_abduction2_viaMultiConditionalSyllogism() {

        test
                .termVolMax(15).confMin(0.4f)
                .believe("<(&&,(robin --> [withWings]),(robin --> [chirping])) ==> a>")
                .believe("<(&&,(robin --> [flying]),(robin --> [withWings]),(robin --> [chirping])) ==> a>")
                .mustBelieve(cycles, "(robin --> [flying])",
                        1.00f, 0.45f
                )
                .mustNotOutput(cycles, "(robin --> [flying])", BELIEF, 0f, 0.5f, 0, 1, ETERNAL);
    }


    @Test
    void conditional_abduction3_semigeneric2() {

        TestNAR tester = test;
        test.termVolMax(14);
        tester.confMin(0.4f);
        tester.believe("<(&&,<ro --> [f]>,<ro --> [w]>) ==> <ro --> [l]>>", 0.9f, 0.9f);
        tester.believe("<(&&,<ro --> [f]>,<ro --> b>) ==> <ro --> [l]>>");
        tester.mustBelieve(cycles, "<<ro --> b> ==> <ro --> [w]>>", 1.00f, 0.42f);
        tester.mustBelieve(cycles, "<<ro --> [w]> ==> <ro --> b>>", 0.90f, 0.45f);
    }


    @Test
    void conditional_abduction3_semigeneric3() {

        TestNAR tester = test;
        tester.termVolMax(14);
        tester.believe("((&&,(R --> [f]),(R --> [w])) ==> (R --> [l]))", 0.9f, 0.9f);
        tester.believe("((&&,(R --> [f]),(R --> b)) ==> (R --> [l]))");
        tester.mustBelieve(cycles, "((R --> b) ==> (R --> [w]))", 1f, 0.42f /*0.36f*/);
        tester.mustBelieve(cycles, "((R --> [w]) ==> (R --> b))", 0.90f, 0.45f);
    }

    @Test
    void conditional_abduction3() {

        TestNAR tester = test;
        test.termVolMax(14);
        tester.believe("((&&,(robin --> [flying]),(robin --> [withWings])) ==> (robin --> [living]))", 0.9f, 0.9f);
        tester.believe("<(&&,(robin --> [flying]),a) ==> (robin --> [living])>");
        tester.mustBelieve(cycles, "(a ==> (robin --> [withWings]))",

                1.00f, 0.42f);
        tester.mustBelieve(cycles, "<(robin --> [withWings]) ==> a>",
                0.90f, 0.42f /*0.45f*/);

    }

    @Test
    void conditional_abduction3_generic_simpler() {
        test.termVolMax(5);
        test
                .believe("((a && b) ==> d)", 0.9f, 0.9f)
                .believe("((a && c) ==> d)", 1f, 0.9f)
                .mustBelieve(cycles, "(c ==> b)", 1f, 0.42f)
                .mustBelieve(cycles, "(b ==> c)", 0.90f, 0.45f);
    }

    @Test
    void conditional_abduction3_generic() {

        TestNAR tester = test;
        test.termVolMax(14);

        tester.believe("((&&,(R --> [f]),(R --> [w])) ==> (R --> [l]))", 0.9f, 0.9f);
        tester.believe("((&&,(R --> [f]),(R --> b)) ==> (R --> [l]))");
        tester.mustBelieve(cycles, "((R --> b) ==> (R --> [w]))", 1f, 0.42f);
        tester.mustBelieve(cycles, "((R --> [w]) ==> (R --> b))", 0.90f, 0.45f);
    }

    @Test
    void conditional_induction_described() {

        TestNAR tester = test;
        test.termVolMax(14);
        tester.believe("<(&&,(robin --> [chirping]),(robin --> [flying])) ==> a>");
        tester.believe("<(robin --> [flying]) ==> (robin --> [withBeak])>", 0.9f, 0.9f);
        tester.mustBelieve(cycles, "<(&&,(robin --> [chirping]),(robin --> [withBeak])) ==> a>",
                1.00f, 0.45f);

    }

    @Test
    void conditional_induction() {
        test.nar.termVolumeMax.set(5);
        test
                .believe("((x&&y) ==> z)")
                .believe("(y ==> a)", 0.9f, 0.9f)
                .mustBelieve(cycles, "((a && x) ==> z)", 1.00f, 0.45f);
    }

    @Test
    void conditional_abduction() {
        test.nar.termVolumeMax.set(6);
        test
                .believe("((x&&y) ==> z)")
                .believe("(a ==> y)", 0.9f, 0.9f)
                .mustBelieve(cycles, "((a && x) ==> z)", 0.9f, 0.73f);
    }

    @Test
    void conditional_abduction2() {
        test.nar.confMin.set(0.3);
        test.termVolMax(5);
        test
                .believe("((x && y) ==> z)")
                .believe("(y ==> z)")
                .mustBelieve(cycles, "x", 1.00f, 0.45f);
    }

    @Test
    void conditional_abduction2_depvar_2() {
        test.nar.confMin.set(0.3);
        test.termVolMax(5);
        test
                .believe("((x && y) ==> #1)")
                .believe("(y ==> #1)")
                .mustBelieve(cycles, "x", 1.00f, 0.45f);
    }
    @Test
    void conditional_abduction2_depvar() {
        test.nar.confMin.set(0.3);
        test.termVolMax(5);
        test
                .believe("((x && y) ==> z)")
                .believe("(y ==> #1)")
                .mustBelieve(cycles, "x", 1.00f, 0.45f);
    }
    @Test
    void conditional_induction0Simple() {
        TestNAR tester = test;
        tester.nar.termVolumeMax.set(5);
        tester.believe("((x1 && a) ==> c)");
        tester.believe("((y1 && a) ==> c)");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0Simple_NegInner() {
        test.termVolMax(6)
                .believe("((x1 && a) ==> c)")
                .believe("((--y1 && a) ==> c)")
                .mustBelieve(cycles, "--(x1 ==> y1)", 1.00f, 0.45f)
                .mustBelieve(cycles, "(--y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleDepVar() {
        test
                .termVolMax(5)
                .believe("((x1 && #1) ==> c)")
                .believe("((y1 && #1) ==> c)")
                .mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f)
                .mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleDepVar2() {
        TestNAR tester = test;
        tester.termVolMax(7);
        tester.believe("((x1 && #1) ==> (a && #1))");
        tester.believe("((y1 && #1) ==> (a && #1))");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleDepVar3() {
        TestNAR tester = test;
        test.nar.termVolumeMax.set(8);
        tester.believe("((x1 && #1) ==> (a && #1))");
        tester.believe("((#1 && #2) ==> (a && #2))");
        tester.mustBelieve(cycles, "(x1 ==> #1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(#1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleIndepVar() {
        TestNAR tester = test;

        tester.believe("((x1 && $1) ==> (a,$1))");
        tester.believe("((y1 && $1) ==> (a,$1))");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction_3ary() {
        test.termVolMax(8)
            .believe("((&&,x1,x2,a) ==> c)")
            .believe("((&&,y1,y2,a) ==> c)")
            .mustBelieve(cycles, "((x1&&x2) ==> (y1&&y2))", 1.00f, 0.45f)
            .mustBelieve(cycles, "((y1&&y2) ==> (x1&&x2))", 1.00f, 0.45f);
    }
    @Test
    void conditional_induction_3ary_some_inner_Neg_other() {
        test.termVolMax(8)
                .believe("((&&,x1,--x2,a) ==> c)")
                .believe("((&&,y1,y2,a) ==> c)")
                .mustBelieve(cycles, "((x1&&--x2) ==> (y1&&y2))", 1.00f, 0.45f)
                .mustBelieve(cycles, "((y1&&y2) ==> (x1&&--x2))", 1.00f, 0.45f);
    }
    @Test
    void conditional_induction_3ary_some_inner_Neg_the() {
        test.termVolMax(8)
                .believe("((&&,x1,x2,--a) ==> c)")
                .believe("((&&,y1,y2,--a) ==> c)")
                .mustBelieve(cycles, "((x1&&x2) ==> (y1&&y2))", 1.00f, 0.45f)
                .mustBelieve(cycles, "((y1&&y2) ==> (x1&&x2))", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0NegBothSimple() {
        TestNAR tester = test;
        test.nar.termVolumeMax.set(9);
        tester.believe("--((x&&a) ==> c)");
        tester.believe("--((x&&b) ==> c)");
        tester.mustBelieve(cycles, "(a ==> b)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(b ==> a)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0NegBoth() {
        Term both = $$("(((x1&&x2) ==> (y1&&y2))&&((y1&&y2) ==> (x1&&x2)))");
        assertEquals("(((x1&&x2)==>(y1&&y2))&&((y1&&y2)==>(x1&&x2)))",
                both.toString());

        TestNAR tester = test;
        test.nar.termVolumeMax.set(7);

        tester.believe("--((&&,x1,x2,a) ==> c)");
        tester.believe("--((&&,y1,y2,a) ==> c)");
        tester.mustBelieve(cycles, "((x1&&x2) ==> (y1&&y2))", 1.00f, 0.4f);
        tester.mustBelieve(cycles, "((y1&&y2) ==> (x1&&x2))", 1.00f, 0.45f);
    }

//    @Test
//    void conditional_induction0NegInner() {
//        TestNAR tester = test;
//        test.nar.termVolumeMax.setAt(9);
//        tester.believe("((x&&a) ==> c)");
//        tester.believe("(--(x&&b) ==> c)");
//        tester.mustBelieve(cycles, "(a ==> --b)", 1.00f, 0.45f);
//        tester.mustBelieve(cycles, "(--b ==> a)", 1.00f, 0.45f);
//    }

    /* will be moved to NAL multistep test file!!
    
    
    @Test public void deriveFromConjunctionComponents() { 
        TestNAR tester = test();
        tester.believe("(&&,<a --> b>,<b-->a>)", Eternal, 1.0f, 0.9f);

        
        tester.mustBelieve(70, "<a --> b>", 1f, 0.81f);
        tester.mustBelieve(70, "<b --> a>", 1f, 0.81f);

        tester.mustBelieve(70, "<a <-> b>", 1.0f, 0.66f);
        tester.run();
    }*/


    @Test
    void testPosPosImplicationConc() {


        test
                .input("x. %1.0;0.90%")
                .input("(x ==> y).")
                .mustBelieve(cycles, "y", 1.0f, 0.81f)
                .mustNotOutput(cycles, "y", BELIEF, 0f, 0.5f, 0, 1, ETERNAL);

    }


    @Test
    void testImplNegPos() {

        test
                .input("--x.")
                .input("(--x ==> y).")
                .mustBelieve(cycles, "y", 1.0f, 0.81f)
        // .mustNotOutput(cycles, "((--,#1)==>y)", BELIEF, 0f, 0.5f, 0, 1, ETERNAL)
        //.mustNotOutput(cycles, "y", BELIEF, 0f, 0.5f, 0, 1, ETERNAL)
        ;
    }


    @Test
    void testImplNegNeg() {

        test

                .input("--x.")
                .input("(--x ==> --y).")
                .mustBelieve(cycles, "y", 0.0f, 0.81f)
        //.mustNotOutput(cycles, "y", BELIEF, 0.5f, 1f, 0.1f, 1, ETERNAL)
        ;
    }


    @Test
    void testAbductionNegPosImplicationPred() {
        test

                .input("y. %1.0;0.90%")
                .input("(--x ==> y).")
                .mustBelieve(cycles, "x", 0.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }


    @Disabled
    @Test
    void testAbductionPosNegImplicationPred() {

        test
                .input("y. %1.0;0.90%")
                .input("--(x ==> y).")
                .mustBelieve(cycles, "x", 0.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }

    @Disabled
    @Test
    void testAbductionNegNegImplicationPred() {

        /*
        via contraposition:
        $.32 x. %1.0;.30% {11: 1;2} ((%1,%2,time(raw),belief(positive),task("."),time(dtEvents),notImpl(%2)),((%2 ==>+- %1),((Induction-->Belief))))
            $.21 ((--,y)==>x). %0.0;.47% {1: 2;;} ((((--,%1)==>%2),%2),(((--,%2) ==>+- %1),((Contraposition-->Belief))))
              $.50 ((--,x)==>y). %0.0;.90% {0: 2}
            $.50 y. %1.0;.90% {0: 1}
         */
        test
                .input("y. %1.0;0.90%")
                .input("--(--x ==> y).")
                .mustBelieve(cycles, "x", 1.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.0f, 0.5f, 0, 1, ETERNAL)
        ;
    }

    @Test
    void testDeductionPosNegImplicationPred() {
        test
                .believe("y")
                .believe("(y ==> --x)")
                .mustBelieve(cycles, "x", 0.0f, 0.81f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }



    @Test
    void testConversion() {

        test
                .termVolMax(3)
                .input("(x==>y)?")
                .input("(y==>x).")
                .mustBelieve(cycles, "(x==>y).", 1.0f, 0.47f)
        ;
    }

    @Test
    void testConversionNeg() {

        test
                .termVolMax(4).confMin(0.4f)
                .input("(x ==> y)?")
                .input("(--y ==> x).")
                .mustBelieve(cycles, "(x ==> y).", 0.0f, 0.47f)
        ;
    }


    @Test
    void testConversionNeg3() {
        test
                .termVolMax(4).confMin(0.4f)
                .input("(--x ==> y)?")
                .input("(y ==> --x).")
                .mustBelieve(cycles, "(--x ==> y).", 1f, 0.47f)
        ;
    }

    @Test
    public void testImplSubj_Questioned() {
        test
            .termVolMax(3)
            .input("(x ==> y)?")
            .mustQuestion(cycles, "x")
            .mustQuestion(cycles, "y")
        ;
    }

    @Test
    public void testImplSubj_and_ConditionsQuestioned_fwd() {
        test.termVolMax(3)
                .ask("x")
                .input("(x ==> y).")
                .mustQuestion(cycles, "y")
        ;
    }
    @Test
    public void testImplSubj_and_ConditionsQuestioned_rev() {
        test.termVolMax(3)
                .ask("y")
                .input("(x ==> y).")
                .mustQuestion(cycles, "x")
        ;
    }

    @Test
    public void testImplNegSubjQuestioned() {
        test.termVolMax(4)
                .input("(--x ==> y)?")
                .mustQuestion(cycles, "x")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test
    public void testImplConjSubjQuestioned() {
        test
                .input("((a && b) ==> y)?")
                .mustQuestion(cycles, "(a && b)")
        ;
    }

    @Test
    public void testAnonymousAbduction() {
        test
                .believe("(x ==> #1)")
                .ask("x")
                .mustBelieve(cycles, "x", 1f, 0.45f)
        ;
    }

    @Test
    public void testAnonymousAbductionNeg() {
        test
                .believe("(--x ==> #1)")
                .ask("x")
                .mustBelieve(cycles, "x", 0f, 0.45f)
        ;
    }

    @Test
    public void testAnonymousDeduction() {
        test
                .believe("(#1 ==> x)")
                .ask("x")
                .mustBelieve(cycles, "x", 1f, 0.81f)
        ;
    }

    @Test
    public void testAnonymousDeductionNeg() {
        test
                .believe("(#1 ==> --x)")
                .ask("x")
                .mustBelieve(cycles, "x", 0f, 0.81f)
        ;
    }

    @Test
    public void conjPreconditionDecompositionToImpl_BackChaining_Question() {
        test
                .ask("((x&&y)==>z)")
                .mustQuestion(cycles, "(x&&y)")
                .mustQuestion(cycles, "(x ==>+- z)")
                .mustQuestion(cycles, "(y ==>+- z)")
//                .mustQuestion(cycles, "(x==>(y&&z))")
//                .mustQuestion(cycles, "(y==>(x&&z))")
        ;
    }

    @Test
    public void conjPostconditionDecompositionToImpl_BackChaining_Question() {
        test
                .ask("(z==>(x&&y))")
                .mustQuestion(cycles, "(x&&y)")
                .mustQuestion(cycles, "(z ==>+- x)")
                .mustQuestion(cycles, "(z ==>+- y)")
        ;
    }

    @Test
    public void implConjNeutralize() {
        test
                .believe("((x && y) ==> z)")
                .believe("((x && --y) ==> --z)")
                .mustBelieve(cycles, "((y ==> z) && (--y ==> --z))", 1f, 0.45f)
        ;
    }
}
