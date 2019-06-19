package nars.nal.nal3;

import nars.$;
import nars.subterm.util.SubtermCondition;
import nars.term.Term;
import nars.test.TestNAR;
import nars.unify.constraint.SubOfConstraint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NAL3DecomposeBeliefTest extends NAL3Test {


    @ValueSource(floats = {0, 0.25f, 0.5f, 0.75f, 0.9f /* TODO 1f should produce no output, add special test case */})
    @ParameterizedTest
    void compound_decomposition_two_premises_Negative_DiffIntensional(float freq) {
        String known = "<robin --> swimmer>";
        String composed = "<robin --> (mammal - swimmer)>";
        String unknown = "<robin --> mammal>";

        TestNAR test = testDecomposeNegDiff(freq, known, composed, unknown);

        //test.mustNotOutput(cycles, "<robin --> --swimmer>", BELIEF, 0, 1, 0, 1, ETERNAL);

        //test neqRCom
        test.mustNotOutput(cycles, "((mammal-swimmer)-->swimmer)", BELIEF, 0, 1, 0, 1, ETERNAL);
        test.mustNotOutput(cycles, "((mammal-swimmer)-->mammal)", BELIEF, 0, 1, 0, 1, ETERNAL);
    }

    @ValueSource(floats = {0, 0.25f, 0.5f, 0.75f, 0.9f /* TODO 1f should produce no output, add special test case */})
    @ParameterizedTest
    void compound_decomposition_two_premises_Negative_DiffExtensional(float freq) {
        String known = "<b-->x>";
        String composed = "<(a ~ b) --> x>";
        String unknown = "<a --> x>";

        TestNAR test = testDecomposeNegDiff(freq, known, composed, unknown);

//        test.mustNotOutput(cycles, "<--a --> x>", BELIEF, 0, 1, 0, 1, ETERNAL);
//        test.mustNotOutput(cycles, "<--b --> x>", BELIEF, 0, 1, 0, 1, ETERNAL);

        //test neqRCom
        //test.mustNotOutput(cycles, "(b --> (a-b))", BELIEF, 0, 1, 0, 1, ETERNAL);
    }

    private TestNAR testDecomposeNegDiff(float freq, String known, String composed, String unknown) {
        test
                .termVolMax(8)
                .confMin(0.5f)
                .believe(known, freq, 0.9f)
                .believe(composed, 0.0f, 0.9f)
        ;

        if (freq == 0 || freq == 1) {
            //0.81 conf
            test.mustBelieve(cycles, unknown, freq, 0.81f);
        } else {
            test.mustBelieve(cycles, unknown, freq, freq, 0, 0.81f); //up to 0.81 conf
        }

//        float confThresh = 0.15f;
//        if (freq > 0)
//            test.mustNotOutput(cycles, known, BELIEF, 0, freq - Param.truth.TRUTH_EPSILON, confThresh, 1, ETERNAL);
//        if (freq < 1)
//            test.mustNotOutput(cycles, known, BELIEF, freq + Param.truth.TRUTH_EPSILON, 1, confThresh, 1, ETERNAL);
        return test;
    }
    @Test
    void diff_compound_decomposition_single3_intersect() {
        test.termVolMax(8);
        test.believe("<(dinosaur && --ant) --> [strong]>", 0.9f, 0.9f);
        test.mustBelieve(cycles, "<dinosaur --> [strong]>", 0.90f, 0.73f);
        test.mustBelieve(cycles, "<ant --> [strong]>", 0.10f, 0.73f);
    }
    @Test
    void diff_compound_decomposition_single3_union() {
        test.termVolMax(8);
        test.believe("<(dinosaur || --ant) --> [strong]>", 0.9f, 0.9f);
        test.mustBelieve(cycles, "<dinosaur --> [strong]>", 0.90f, 0.73f);
        test.mustBelieve(cycles, "<ant --> [strong]>", 0.10f, 0.73f);
    }
//
//    @Test
//    void diff_compound_decomposition_low_dynamic() {
//        TestNAR tester = test;
//        tester.termVolMax(7);
//        tester.believe("<(ant && --spider) --> [strong]>", 0.1f, 0.9f);
//        tester.mustBelieve(cycles, "<spider --> [strong]>", 0.90f, 0.08f);
//        tester.mustBelieve(cycles, "<ant --> [strong]>", 0.10f, 0.08f);
//    }

    @Test
    void diff_compound_decomposition_single() {

        test.believe("(robin --> (bird && --swimmer))", 0.9f, 0.9f);
        test.mustBelieve(cycles, "<robin --> bird>", 0.90f, 0.73f);

    }


    @Test
    void sect_compound_decomposition_single2() {

        TestNAR tester = test;
        tester.believe("((dinosaur && ant) --> youth)", 0.9f, 0.9f);
        tester.mustBelieve(cycles, "(dinosaur --> youth)", 0.9f, 0.73f);

    }


//    @Test
//    void testDifference() {
//
//        TestNAR tester = test;
//        tester.believe("<swan --> bird>", 0.9f, 0.9f);
//        tester.believe("<dinosaur --> bird>", 0.7f, 0.9f);
//        tester.mustBelieve(cycles, "bird:(swan ~ dinosaur)", 0.27f, 0.81f);
//        tester.mustBelieve(cycles, "bird:(dinosaur ~ swan)", 0.07f, 0.81f);
//    }

    @Test
    void testArity1_Decomposition_IntersectExt() {
        test
                .believe("(a-->(b&&c))", 0f, 0.9f)
                .believe("(a-->b)")
                .mustBelieve(cycles, "(a-->c)", 0f, 0.81f);
    }

    @Test
    void testArity1_Decomposition_Intersect_3_2() {
        test
                .believe("(x-->(&&,a,b,c))", 0.9f, 0.9f)
                .believe("(x-->(&&,a,b))", 0.9f, 0.9f)
                .mustBelieve(cycles, "(x-->c)", 0.81f, 0.66f);
    }
    @Test
    void testArity1_Decomposition_Intersect_3_2_neg() {
        test
                .believe("(x-->(&&,--a,--b,c))", 0.9f, 0.9f)
                .believe("(x-->(&&,--a,--b))", 0.9f, 0.9f)
                .mustBelieve(cycles, "(x-->c)", 0.81f, 0.66f);
    }
    @Test
    void testArity1_Decomposition_Union_3_2() {
        test
                .termVolMax(10)
                .believe("(x-->(||,a,b,c))", 0.9f, 0.9f)
                .believe("(x-->(||,a,b))", 0.1f, 0.9f)
                .mustBelieve(cycles, "(x-->c)", 0.81f, 0.66f);
    }

    @Test
    void compound_decomposition_two_premises_union() {

        test.termVolMax(7);
        test.believe("<robin --> (bird || swimmer)>", 0.9f, 0.9f);
        test.believe("<robin --> swimmer>", 0.9f, 0.9f);
        test.mustBelieve(cycles, "<robin --> bird>", 0.09f, 0.07f);

    }

    @Test
    void testArity1_Decomposition_Union() {
        test
                .termVolMax(8)
                .believe("((b||c)-->a)", 0.25f, 0.9f)
                .believe("(b-->a)", 0.25f, 0.9f)
                .mustBelieve(cycles, "(c-->a)", 0.19f, 0.15f);
    }

    @Test
    void testArity1_Decomposition_Union2() {


        test
                .termVolMax(8)
                .believe("(a-->b)", 0.25f, 0.9f)
                .believe("(a-->(b||c))", 0.25f, 0.9f)
                .mustBelieve(cycles, "(a-->c)", 0.06f, 0.05f, ETERNAL);
    }

    @Test
    void testDisjoint2_int() {


        test
                .termVolMax(6)
                .believe("--(x-->(RealNumber&&ComplexNumber))")
                .believe("(x-->RealNumber)")
//                .mustBelieve(cycles, "(x-->ComplexNumber)", 0f, 0.45f) //single decomposition
                .mustBelieve(cycles, "(x-->ComplexNumber)", 0f, 0.81f); //double decomposition

    }
    @Test
    void testDisjoint2_ext() {


        test
                .termVolMax(6)
                .believe("--((RealNumber&&ComplexNumber)-->x)")
                .believe("(RealNumber-->x)")
//                .mustBelieve(cycles, "(ComplexNumber-->x)", 0f, 0.45f) //single decomposition
                .mustBelieve(cycles, "(ComplexNumber-->x)", 0f, 0.81f); //double decomposition

    }
    @Test
    void testDisjoint2Learned() {


        test
                .believe("--(x-->ComplexNumber)")
                .believe("(x-->RealNumber)")
                .mustBelieve(cycles, "(x-->(RealNumber-ComplexNumber))", 1f, 0.81f);

    }

    @Test
    void testDisjoint3() {

        test
                .termVolMax(8)
                .believe("--(x-->(&,RealNumber,ComplexNumber,Letter))")
                .believe("(x-->RealNumber)")
                .mustBelieve(cycles, "(x-->(ComplexNumber&&Letter))", 0f, 0.81f)
        ;

    }

    @Test
    void testDisjointWithVarPos() {


        test
                .termVolMax(8)
                .believe("(#1-->(RealNumber&ComplexNumber))")
                .believe("(x-->RealNumber)")
                .mustBelieve(cycles, "(x-->ComplexNumber)", 1f, 0.81f)
        ;

    }
    @Test
    void testDisjointWithVarNeg() {


        test
                .believe("--(#1-->(RealNumber&ComplexNumber))")
                .believe("(x-->RealNumber)")
                .mustBelieve(cycles, "(x-->ComplexNumber)", 0f, 0.81f)
        ;

    }

    @Test
    void testDifferenceQuestion() {
        test
                .termVolMax(8)
                .believe("((x&&y)-->a)")
                .mustQuestion(cycles, "((x&&--y)-->a)")
                .mustQuestion(cycles, "((y&&--x)-->a)")
        ;
    }

    @Test
    void testDifferenceQuest() {
        test
                .termVolMax(8)
                .goal("((x&&y)-->a)")
                .mustQuest(cycles, "((x&&--y)-->a)")
                .mustQuest(cycles, "((y&&--x)-->a)")
        ;
    }

    @Test
    void questPropagation() {

        test.nar.termVolMax.set(8);
        test
                .goal("x:a")
                .goal("x:b")
                .quest("x:a")
                .mustQuest(cycles, "x:(a&b)")
                .mustGoal(cycles, "x:(a&b)", 1f, 0.81f)
        ;
    }
    @Test void testDecomposeWTF() {
    /* wrong:
    $.05 (0-->x). 1 %1.0;.54% {419: 1;2;3©} (S --> M), X, is(S,"|"), subOf(S,X)   |-         (X --> M), (Belief:StructuralDeduction,Goal:StructuralDeduction)
        $.09 (((2-1)|(--,0))-->x). 1 %1.0;.60% {127: 1;2;3} (P --> M), (S --> M), notSetsOrDifferentSets(S,P), neq(S,P) |- ((polarizeTask(P) | polarizeBelief(S)) --> M), (Belief:IntersectionDepolarized)
        $.19 ((2-1)-->x). 1⋈2 %1.0;.76% {84: 2;3}
     */
        String ii = "(((a2-a1)|(--,a0))-->x)";
//        Term iii = $$(ii);
//        assertEquals("((a2-a1),(--,a0))", iii.sub(0).subterms().toString());
//        assertEquals(ii, iii.toString());

        Term cn = $$("((_2-_1)|(--,_3))");
//        Term cp = $$("((_2-_1)|_3)");
        Term xp = $$("_3");
//        Term xn = $$("(--,_3)");
//        assertFalse(cn.contains(xp));
//        assertFalse(cp.contains(xn));
//        assertTrue(cn.contains(xn));
//        assertTrue(cp.contains(xp));

        assertTrue(
                new SubOfConstraint($.varDep(1), $.varDep(2), SubtermCondition.Subterm)
                        .invalid(cn, xp, null)
        );
        test.termVolMax(9);
        test.believe(ii)
                .mustBelieve(cycles, "(a0-->x)", 0, 0.81f)
                .mustNotOutput(cycles, "(a0-->x)", BELIEF, 0.5f, 1f, 0, 0.99f, (t)->true)

        ;
    }

    @ParameterizedTest @ValueSource(strings={"||","&&"})
    void questionDecomposition0(String o) {
        test
                .termVolMax(8)
                .ask("((swan" + o + "swimmer) --> bird)")
                .mustQuestion(cycles, "(swimmer --> bird)")
                .mustQuestion(cycles, "(swan --> bird)")
        ;
    }


}
