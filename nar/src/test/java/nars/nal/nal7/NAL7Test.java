package nars.nal.nal7;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.task.TaskTest;
import nars.term.Term;
import nars.test.NALTest;
import nars.test.TestNAR;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.$.$;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class NAL7Test extends NALTest {

    public static final float CONF_TOLERANCE_FOR_PROJECTIONS = 2f; //200%
    private final static int cycles = 75;

    @Override
    protected NAR nar() {
        NAR n = NARS
                //.tmp();
                .tmp(1, 8);
        return n;
    }

    @BeforeEach
    void setTolerance() {
        test.termVolMax(13);
        test.confMin(0.3f);

        test.freqTolerance(0.1f);
        test.nar.freqResolution.set(0.1f);
        test.confTolerance(CONF_TOLERANCE_FOR_PROJECTIONS);
        test.nar.confResolution.set(0.05f);
    }


    @Test
    void induction_on_events_neg2() {

        test
                .inputAt(0, "x:before. |")
                .inputAt(1, "(--,x:after). |")
                .mustBelieve(cycles, "(x:before ==>+1 x:after)", 0.00f, 0.45f /*abductionConf*/, 0)
                .mustBelieve(cycles, "((--,x:after) ==>-1 x:before)", 1.00f, 0.45f /*inductionConf*/, 1)
                .mustBelieve(cycles, "(x:before &&+1 (--,x:after))", 1.00f, 0.81f /*intersectionConf*/, 0)
                .mustNotOutput(cycles, "(x:before &&-1 (--,x:after))", BELIEF,
                        (t -> t == 0 || t == 1));
    }

    @Test
    void temporal_explification() {

        TestNAR tester = test;
        test.termVolMax(11);
        tester.believe("(enter($x, room) ==>-5 open($x, door))", 0.9f, 0.9f);
        tester.believe("(open($y, door) ==>-5 hold($y, key))", 0.8f, 0.9f);

        tester.mustBelieve(cycles, "(hold($1,key) ==>+10 enter($1,room))", 1.00f, 0.37f);

    }


    @Test
    void temporal_analogy() {

        test
                .termVolMax(11)
                .believe("( open($x, door) ==>+5 enter($x, room) )", 0.95f, 0.9f)
                .believe("( enter($x, room) ==> leave($x, corridor_100) )", 1.0f, 0.9f)
                .believe("( leave($x, corridor_100) ==> enter($x, room) )", 1.0f, 0.9f)
                .mustBelieve(cycles, "( open($1, door) ==>+5 leave($1, corridor_100) )", 0.95f, 0.77f /*0.81f*/)
                .mustNotOutput(cycles, "( open($1, door) ==>-5 leave($1, corridor_100) )", BELIEF, ETERNAL);

    }

    /**
     * tests that although the task and belief do not temporally intersect, the belief can still be used to derive the projected result
     * adapted from: NAL1Test
     */
    @Test
    void temporalAnalogyNonIntersecting() {

        test.nar.believe("<gull <-> swan>", 1f, 0.9f, 0, 1);
        test.nar.believe("<swan --> swimmer>", 1f, 0.9f, 4, 5);
        test.mustBelieve(cycles, "<gull --> swimmer>", 1.0f, /*<*/0.45f, (s, e) -> s == 4 && e == 5);
    }

    @Test
    void testConjDecomposeAGAIN() {
        /*
          WRONG TIME
          $.20 (b &&+5 c). 11 %1.0;.73% {105: 2;3} ((%1,%2,task("."),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
            $.27 c. 11 %1.0;.81% {13: 3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief))))
              $.50 (c &&+5 d). 11 %1.0;.90% {11: 3} Narsese
            $.50 (b &&+5 c). 6 %1.0;.90% {6: 2} Narsese
         */
        test
                .inputAt(11, "c. :|:")
                .inputAt(6, "(b &&+5 c). :|:")
                .mustNotOutput(cycles, "(b &&+5 c)", BELIEF, 11);
    }

    @Test
    void testConjDecomposeWrongDirection() {
        /*
        $.13 b. -4 %1.0;.81% {399: 1;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
            $.50 (a &&+5 b). 1⋈6 %1.0;.90% {1: 1}
         */
        test
                .inputAt(1, "(a &&+5 b). |")
                .inputAt(6, "(b &&+5 #1). |")
                //.mustBelieve(cycles, "(a &&+10 #1)", 1.00f, 0.73f, 1)
                .mustBelieve(cycles, "a", 1.00f, 0.81f, 1)
                .mustBelieve(cycles, "b", 1.00f, 0.81f, 6)

                .mustNotOutput(cycles, "((a&&b) &&+5 (b && #1))", BELIEF, t -> true)
        ;
    }


    @Test
    void testConjDecomposeShift() {
        /*
        WRONG:
            $.02 (b &&+5 #1). 16⋈21 %1.0;.59% {410: 1;2;3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
                $.08 ((a &&+5 b) &&+5 #1). 1⋈11 %1.0;.66% {64: 1;2;3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
        */

        test
                .inputAt(1, "((a &&+5 b) &&+5 c). |")
                .mustBelieve(cycles, "(b &&+5 c)", 1.00f, 0.81f, 6)
                .mustNotOutput(cycles, "(b &&+5 c)", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "(b &&+5 c)", BELIEF, 16)
        ;
    }

    @Test
    void testConjDecomposeParallelBelief() {
        test
                .inputAt(1, "(a && b). :|:")
                .mustBelieve(cycles, "a", 1.00f, 0.81f, 1)
                .mustBelieve(cycles, "b", 1.00f, 0.81f, 1)
        ;
    }

    @Test
    void testConjDecomposeGoalPar() {
        test
                .inputAt(1, "(a && b)! :|:")
                .mustGoal(cycles, "a", 1.00f, 0.81f, 1)
                .mustGoal(cycles, "b", 1.00f, 0.81f, 1)
        ;
    }

    @Disabled
    @Test
    void testConjDecomposeGoalSeq() {
        test
                .inputAt(1, "(a &&+1 b)! :|:")
                .mustGoal(cycles, "a", 1.00f, 0.81f, 1)
                .mustNotOutput(cycles, "b", GOAL, t -> true)
        ;
    }

    @Test
    void testInduct3Events() {
        /*
          instability:
            $0.0 c. 7 %1.0;.73% {89: 2;3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
                $.18 (b &&+3 c). 2⋈5 %1.0;.81% {11: 2;3} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%2,belief) &&+- polarize(%1,task)),((IntersectionDepolarized-->Belief))))
                  $.50 b. 2 %1.0;.90% {2: 2}
                  $.50 c. 5 %1.0;.90% {5: 3}
         */

//        Param.DEBUG = true;
//        test.log();
        test
                .termVolMax(5)
                .confMin(0.5f)
                .inputAt(1, "a. |")
                .mustNotOutput(cycles, "a", BELIEF, (t) -> t != 1)
                .inputAt(5, "b. |")
                .mustNotOutput(cycles, "b", BELIEF, (t) -> t != 5)
                .inputAt(10, "c. |")
                .mustNotOutput(cycles, "c", BELIEF, (t) -> t != 10)
                .mustBelieve(cycles * 2, "(a &&+9 c)", 1.00f, 0.81f, (t) -> t == 1)
                .mustNotOutput(cycles * 2, "(a &&+9 c)", BELIEF, 0, 1.00f, 0, 1, (t) -> t != 1)
                .mustBelieve(cycles * 2, "((a &&+4 b) &&+5 c)", 1.00f, 0.73f, (t) -> t == 1)
                .mustNotOutput(cycles * 2, "((a &&+4 b) &&+5 c)", BELIEF, (t) -> t != 1)
                .mustBelieve(cycles, "(b &&+5 c)", 1.00f, 0.81f, (t) -> t == 5)
                .mustNotOutput(cycles, "(b &&+5 c)", BELIEF, (t) -> t != 5)
                .mustBelieve(cycles, "(a &&+4 b)", 1.00f, 0.81f, (t) -> t == 1)
                .mustNotOutput(cycles, "(a &&+4 b)", BELIEF, (t) -> t != 1)
                .mustNot(BELIEF,
                        t -> !t.hasVars() && (t.start() < 0 || t.start() > 10 || t.end() < 0 || t.end() > 10));
        ;
    }


    @Test
    void testShiftPlusDontEraseDT() {

        test
                .inputAt(1, "(z ==>+1 (x &&+1 y)).")
                .mustBelieve(cycles, "(z ==>+2 y)", 1f, 0.81f)
                .mustBelieve(cycles, "(z ==>+1 x)", 1f, 0.81f)
                .mustNotOutput(cycles, "(x&&y)", BELIEF)
                .mustNotOutput(cycles, "(z==>y)", BELIEF)
                .mustNotOutput(cycles, "(z==>x)", BELIEF)
        ;
    }

    @Test
    void testShiftPlus() {
        test
                .termVolMax(5)
                .inputAt(1, "((x &&+1 y) ==>+1 z).")
                .inputAt(3, "z. |")
                .mustBelieve(cycles, "(x &&+1 y)", 1, 0.81f, 1)
                .mustBelieve(cycles, "x", 1, 0.73f, 1)
                .mustBelieve(cycles, "y", 1, 0.73f, 2)
//                .must(BELIEF, x -> x.start() >= 1 && x.end() <= 3)
                .mustNotOutput(cycles, "(x &&+1 y)", BELIEF, (t) -> t != 1)
                .mustNotOutput(cycles, "x", BELIEF, (t) -> t != 1)
                .mustNotOutput(cycles, "y", BELIEF, (t) -> t != 2)
                .mustNotOutput(cycles, "z", BELIEF, (t) -> t != 3)
        ;
    }

    @Test
    void testDropAnyEventSimple2a() {
        test
                .inputAt(1, "((happy &&+4120 i) &&+1232 j). |")
                .mustBelieve(cycles, "(happy &&+4120 i)", 1f, 0.81f, 1)
                .mustNotOutput(cycles, "(i&&happy)", BELIEF, 1)
        ;
    }

    @Test
    void testDropAnyEventSimple2ba() {

        test
                .inputAt(1, "(happy &&+4120 (i &&+1232 (--,i))). |")
                .mustBelieve(cycles, "(happy &&+4120 i)", 1f, 0.81f, 1)
                .mustBelieve(cycles, "(happy &&+5352 --i)", 1f, 0.81f, 1)
                .mustNotOutput(cycles, "(happy &&+5352 (--,i))", BELIEF, -1231)
                .mustNotOutput(cycles, "(happy &&+4120 i)", BELIEF, 1233)
                .mustNotOutput(cycles, "(i&&happy)", BELIEF, 1)
        ;
    }

    @Test
    void testDropAnyEventSimple2bb() {
        test
                .inputAt(1, "((happy &&+4120 i) &&+1232 --i). :|:")
                .mustBelieve(cycles, "(happy &&+4120 i)", 1f, 0.81f, 1)
                .mustNotOutput(cycles, "(happy &&+4120 i)", BELIEF, t -> t != 1)
                .mustNotOutput(cycles, "(i && happy)", BELIEF, 1)
        ;
    }

    @Test
    void updating_and_revision() {
        testTemporalRevision(10, 0.50f, 0.7f, "hold(John,key)");
    }

    @Test
    void updating_and_revision2() {
        testTemporalRevision(1, 0.5f, 0.7f, "hold(John,key)");
    }

    private void testTemporalRevision(int delay, float freq, float conf, @NotNull String belief) {

        TestNAR tester = test;

        tester.input(belief + ". :|: %1.00;0.65%");
        tester.inputAt(delay, belief + ". :|: %0.5;0.70%");
        tester.inputAt(delay + 1, belief + "? :|:");
        tester.mustBelieve(delay + 50, belief, freq, conf, delay);
    }

    @Test
    void testSumNeg() {


        test
                .termVolMax(3)
                .believe("(x ==>+2 y)")
                .believe("(y ==>+3 z)")
                .mustBelieve(cycles, "(z ==>-5 x)", 1.00f, 0.45f);

    }


    @Test
    void testSum() {

        test
                .termVolMax(3)
                .believe("(x ==>+2 y)")
                .believe("(y ==>+3 z)")
                .mustBelieve(cycles, "(x ==>+5 z)", 1.00f, 0.81f);
    }

    @Test
    void testBminT() {


        test
                .termVolMax(3)
                .believe("(x ==>+2 y)")
                .believe("(z ==>+3 y)")
                .mustBelieve(cycles, "(z ==>+1 x)", 1.00f, 0.45f)
                .mustNotOutput(cycles, "(z ==>-2 x)", BELIEF, ETERNAL)
        ;
    }


    @Test
    void testSameDamnConjunction() {
        test
                .inputAt(6, "(b &&+5 c). :|: %1.0;0.66%")
                .inputAt(6, "(b &&+5 c). :|: %1.0;0.90%")

                .mustNotOutput(cycles, "((b &&+5 c) &&+5 (b &&+5 c))", BELIEF, x -> true)
                .mustNotOutput(cycles, "((b &&+5 c) &&+5 b)", BELIEF, x -> true);

    }

    @Test
    void testTminB() {


        test
                .termVolMax(3)
                .believe("(y ==>+3 x)")
                .believe("(y ==>+2 z)")
                .mustBelieve(cycles, "(z ==>+1 x)", 1.00f, 0.45f)
                .mustNotOutput(cycles, "(z ==>-1 x)", BELIEF, ETERNAL)
                .mustBelieve(cycles, "(x ==>-1 z)", 1.00f, 0.45f)
                .mustNotOutput(cycles, "(x ==>+1 z)", BELIEF, ETERNAL)


        ;

    }


    @Test
    void intervalPreserve_and_shift_occurence() {

        test


                .termVolMax(11)
                .inputAt(1, "(X:x &&+1 (Y:y &&+2 Z:z)). |")
                .mustBelieve(cycles, "X:x.", 1.00f, 0.73f, 1)
                .mustBelieve(cycles, "(Y:y &&+2 Z:z).", 1.00f, 0.81f, 2)
                .mustBelieve(cycles, "Y:y.", 1.00f, 0.73f, 2)
                .mustBelieve(cycles, "Z:z.", 1.00f, 0.73f, 4)
        ;

    }


    @Test
    void temporal_deduction() {

        test
                .termVolMax(11)
                .believe("(enter($x, room) ==>-3 open($x, door))", 0.9f, 0.9f)
                .believe("(open($y, door) ==>-4 hold($y, key))", 0.8f, 0.9f)
                .mustBelieve(cycles, "(enter($1,room) ==>-7 hold($1,key))", 0.7f, 0.58f)
                .mustNotOutput(cycles, "(enter($1,room) ==>-4 hold($1,key))", BELIEF, ETERNAL)
                .mustBelieve(cycles, "(hold($1,key) ==>+7 enter($1,room))", 1f, 0.37f);
    }

    @Test
    void temporal_induction_comparison() {

        test
                .termVolMax(11)

                .believe("((( $x, door) --> open) ==>+5 (( $x, room) --> enter))", 0.9f, 0.9f)
                .believe("((( $y, door) --> open) ==>-4 (( $y, key) --> hold))", 0.8f, 0.9f)


                .mustBelieve(cycles, "( hold($1,key) ==>+9 enter($1,room) )", 0.9f, 0.39f)
                .mustBelieve(cycles, "( enter($1,room) ==>-9 hold($1,key) )", 0.8f, 0.42f);


    }

    @Test
    void inference_on_tense() {

        test
                .input("(hold($x, key) ==>+1 enter($x, room)).")
                .input("hold(John, key). |")
                .mustBelieve(cycles, "enter(John,room)",
                        1.00f, 0.81f, 1);
    }

    @Test
    void inference_on_tense_reverse() {

        test
                .input("(hold($x, key) ==>+7 enter($x, room)).")
                .input("enter(John, room). :|:")
                .mustBelieve(cycles, "hold(John,key)",
                        1.00f, 0.45f, -7);
    }

    @Test
    void inference_on_tense_reverse_novar() {

        test

                .input("(hold(John, key) ==>+7 enter(John, room)).")
                .input("enter(John, room). :|:")
                .mustBelieve(cycles, "hold(John,key)",
                        1.00f, 0.45f, -7);
    }

    @Test
    void inference_on_tense_3() {

        test
                .inputAt(0, "hold(John,key). :|:")
                .believe("(hold(John,key) ==>+1 enter(John,room))", 1.0f, 0.9f)
                .mustBelieve(cycles, "enter(John,room)",
                        1.00f, 0.81f, 1)
        ;
    }

    @Test
    void inference_on_tense_4() {

        test
                .believe("(hold(John,key) ==>+3 enter(John,room))")
                .inputAt(0, "enter(John,room). |")
                .mustBelieve(cycles, "hold(John,key)", 1.00f, 0.45f, -3);
    }


    //public static class TemporalInductionTest2 {
        @Test
        void induction_on_events_0() {
            test.termVolMax(11);
            test

                    .inputAt(0, "open(John,door). :|:")
                    .inputAt(4, "enter(John,room). :|:")
                    .mustBelieve(cycles, "( enter(John, room) ==>-4 open(John, door) )",
                            1.00f, 0.45f, 4);
        }

        @Test
        void induction_on_events_0_neg() {

            test
                    .termVolMax(12)
                    .input("(--,open(John,door)). :|:")
                    .inputAt(4, "enter(John,room). :|:")
                    .mustBelieve(cycles, "( (--,open(John, door)) ==>+4 enter(John, room) )",
                            1.00f, 0.45f, 0)
                    .mustBelieve(cycles, "( (--,open(John, door)) &&+4 enter(John, room) )",
                            1f, 0.81f, 0)
            ;
        }

        @Test
        void induction_on_events2() {
            test.termVolMax(11);

            test
                    .inputAt(0, "<(John,door) --> open>. |")
                    .inputAt(4, "<(John,room) --> enter>. |")
                    .mustBelieve(cycles, "(((John, door) --> open) ==>+4 ((John, room) --> enter))",
                            1.00f, 0.45f, 0);

        }

        @Test
        void induction_on_events3() {
            test.termVolMax(11);

            test
                    .input("open(John,door). :|:")
                    .inputAt(4, "enter(John,room). :|:")
                    .mustBelieve(cycles, "(open(John, door) ==>+4 enter(John, room))",
                            1.00f, 0.45f,
                            0)
                    .mustBelieve(cycles, "(enter(John, room) ==>-4 open(John, door))",
                            1.00f, 0.45f,
                            4);

        }

        @Test
        void induction_on_events3_simple() {

            TestNAR tester = test;

            tester.inputAt(1, "<door --> open>. |");
            tester.inputAt(2, "<room --> enter>. |");

            tester.mustBelieve(cycles, "(<door --> open> ==>+1 <room --> enter>)",
                    1.00f, 0.45f,
                    1);
        }

        @Test
        void induction_on_events_pos_neg() {

            test
                    .termVolMax(4)
                    .inputAt(1, "a. |")
                    .inputAt(2, "--b. |")
                    .mustBelieve(cycles, "(a &&+1 --b)", 1.00f, 0.81f, 1)
                    .mustBelieve(cycles, "(--b ==>-1 a)", 1.00f, 0.45f, 2)
                    .mustBelieve(cycles, "(a ==>+1 b)", 0.00f, 0.45f, 1)
            ;
        }

        @Test
        void induction_on_events_conj_pos_neg() {
            test
                    .inputAt(1, "(a &&+5 (--,a)). :|:")
                    .inputAt(6, "(b &&+5 (--,b)). :|:")
                    .mustBelieve(cycles, "((a &&+5 ((--,a)&&b)) &&+5 (--,b))", 1.00f, 0.81f, 1)
            ;
        }

        @Test
        void induction_on_events_neg_pos() {

            test
                    .termVolMax(4)
                    .inputAt(1, "--b. :|:")
                    .inputAt(2, "a. :|:")
                    .mustBelieve(cycles, "(--b &&+1 a)", 1.00f, 0.81f, 1)
                    .mustBelieve(cycles, "(--b ==>+1 a)", 1.00f, 0.45f, 1)

            ;
        }

        @Test
        void induction_on_events_neg_neg() {

            test
                    .termVolMax(5)
                    .inputAt(1, "--a. :|:")
                    .inputAt(2, "--b. :|:")
                    .mustBelieve(cycles, "(--a &&+1 --b)", 1.00f, 0.81f, 1)
                    .mustBelieve(cycles, "(--a ==>+1 b)", 0.00f, 0.45f, t -> t == 1)
                    .mustBelieve(cycles, "(--b ==>-1 a)", 0.00f, 0.45f, t -> t == 2);
        }



    @Test
    void conjuction_on_events_with_variable_introduction() {
        test.termVolMax(11).inputAt(0, "open(John, door). |")
                .inputAt(2, "enter(John, room). |")
                .mustBelieve(cycles,
                        "(open(#1,door) &&+2 enter(#1,room))",
                        1.00f, 0.81f, 0
                )
                .mustNotOutput(cycles,
                        "(enter(#1,room) &&+2 open(#1,door))", BELIEF, (t) -> true
                )
        ;
    }

    @Test
    void conjuction_on_events_with_variable_introduction_pos_neg() {
        test.termVolMax(12)
                .inputAt(0, "open(John, door). |").inputAt(2, "--enter(John, room). |")
                .mustBelieve(cycles,
                        "(open(#1,door) &&+2 --enter(#1,room))",
                        1.00f, 0.81f, 0
                );
    }

    @Test
    void abduction_on_events_with_variable_introduction() {

        TestNAR tester = test;
        test.termVolMax(11);
        tester.input("open(John,door). :|:");
        tester.inputAt(2, "enter(John, room). :|:");


        tester.mustBelieve(cycles,
                "(enter($1,room) ==>-2 open($1,door))",
                1.00f, 0.45f,
                2
        );

    }


    @Test
    void induction_on_events_with_variable_introduction() {

        TestNAR tester = test;

        test.termVolMax(11);
        tester.input("open(John, door). :|:");
        tester.inputAt(2, "enter(John, room). :|:");


        tester.mustBelieve(cycles,
                "(open($1,door) ==>+2 enter($1, room))",
                1.00f,
                0.45f /* 0.45f */,
                0
        );


    }

    @Test
    void induction_on_events_composition_pre() {

        test
                .termVolMax(17)
                .confMin(0.4f)
                .input("hold(John,key). :|:")
                .input("(open(John,door) <-> enter(John,room)). :|:")
                .mustBelieve(cycles, "(hold(John,key) && (open(John,door) <-> enter(John,room)))",
                        1.00f, 0.81f,
                        0);
    }

    @Test
    void induction_on_events_composition1() {
        test.nar.confMin.set(0.1f);
        compositionTest(1, 5);
    }

    @Test
    void induction_on_events_composition2() {
        test.nar.confMin.set(0.1f);
        compositionTest(1, 7);
    }

    @Test
    void induction_on_events_composition3() {
        compositionTest(4, 3);
    }


    @ValueSource(ints = {0, 1, 2, 3})
    @ParameterizedTest
    void induction_on_events_composition_post(int dt) {
        TestNAR tester = test;

        test.termVolMax(17);

        int t = 0;
        String component = "(open(John,door) && hold(John,key))";
        tester.inputAt(t, component + ". |");
        tester.inputAt(t + dt, "enter(John,room). |");

        tester.mustBelieve((6 * (t + Math.max(3, dt)) + Math.max(3, dt) + 1) /** approx */,
                '(' + component + " ==>+" + dt + " enter(John,room))",
                1.00f, 0.45f,
                t);
    }

    private void compositionTest(int t, int dt) {

        test.nar.termVolMax.set(11);
        test.inputAt(t, "hold(John,key). |");
        test.inputAt(t, "(open(John,door) ==>+" + dt + " enter(John,room)).");


        String component = "(open(John,door) && hold(John,key))";
        test.inputAt(t, component + ". |");


        test.mustBelieve(cycles, "open(John,door)",
                1.00f, 0.81f,
                t);
        test.mustBelieve(cycles, "enter(John,room)",
                1.00f, 0.81f,
                t + dt);

        test.mustBelieve(cycles, component,
                1.00f, 0.34f,
                t);

    }

    @Test
    void variable_introduction_on_events() {

        test.termVolMax(14);
        test.input("at(SELF,{t003}). |");
        test.inputAt(4, "on({t002},{t003}). |");
        test.mustBelieve(cycles,
                "(at(SELF,{t003}) &&+4 on({t002},{t003}))",
                1.0f, 0.81f,
                0);
        test.mustBelieve(cycles,
                "(at(SELF,{#1}) &&+4 on({t002},{#1}))",
                1.0f, 0.81f,
                0);
        test.mustBelieve(cycles,
                "(at(SELF,#1) &&+4 on({t002},#1))",
                1.0f, 0.81f,
                0);
        test.mustBelieve(cycles,
                "(at(SELF,{$1}) ==>+4 on({t002},{$1}))",
                1.0f, 0.45f,
                0);
        test.mustBelieve(cycles,
                "(on({t002},{$1}) ==>-4 at(SELF,{$1}))",
                1.0f, 0.45f,
                4);
    }

    @Test
    void variable_introduction_on_events_with_negation() {

        test
                .input("a:x. | %0.1;0.8% ")
                .inputAt(2, "b:x. | %0.8;0.9% ")

                .mustBelieve(cycles,
                        "(b:x ==>-2 a:x)",
                        0.1f, 0.37f,
                        2)


        ;

    }

    @Test
    void variable_elimination_on_temporal_statements_simpler() {

        test
                .termVolMax(11)
                .inputAt(0, "((y,#1) && (x,#1)). |")
                .inputAt(1, "((($1,#2) && (x,#2)) ==> (x,$1)).")
                .mustBelieve(cycles, "(x,y)",
                        1.0f, 0.81f, 0);

    }

    @Test
    void variable_elimination_on_temporal_statements() {

        test
                .termVolMax(17)
                .inputAt(0, "(on({t002},#1) && at(SELF,#1)). |")
                .inputAt(1, "((on($1,#2) && at(SELF,#2)) ==> reachable(SELF,$1)).")
                .mustBelieve(cycles, "reachable(SELF,{t002})",
                        1.0f, 0.81f, 0);

    }

    @Test
    void testTemporalImplicationDecompositionIsntEternal() {

        /*
        Test that this eternal derivation does not happen, and that it is temporal with the right occ time

        $.08;.01;.23$ (--,(p3)). :1: %1.0;.40% {1: 5;7} ((%1,(%2==>%3),time(decomposeBelief)),(substituteIfUnifies(%3,"$",%2,%1),((Deduction-->Belief),(Induction-->Desire),(Anticipate-->Event))))
            $.50;.50;.95$ (p2). 0+0 %1.0;.90% {0+0: 5} Input
            $.75;.06;.12$ ((p2) ==>+0 (--,(p3))). 1-1 %1.0;.45% {1-1: 5;7} ((%1,%2,time(dtAfterReverse),neq(%1,%2),notImplicationOrEquivalence(%1),notImplicationOrEquivalence(%2)),((%2==>%1),((Abduction-->Belief)))) */

        test
                .confTolerance(0.1f)
                .inputAt(0, "a. |")
                .inputAt(0, "(a ==>+1 b). |")
                .mustNotOutput(cycles, "b", BELIEF, ETERNAL)
                .mustBelieve(cycles, "b", 1f, 0.81f, 1 /* occ */);

    }


    @Test
    void testEternalImplicationDecompositionIsntEternal() {
        test
                .confTolerance(0.01f)
                .inputAt(0, "a. :|:")
                .inputAt(0, "(a ==>+1 b).")


                .mustBelieve(cycles, "b", 1f, 0.81f, 1 /* occ */);
    }

    @Test
    void testEternalImplicationDecompositionWithConj() {

        test

                .inputAt(1, "(a &&+1 b). :|:")
                .inputAt(1, "((a &&+1 b) ==>+4 c). :|:")
                .mustBelieve(cycles, "c", 1f, 0.81f, 6 /* occ */)
                .mustNotOutput(cycles, "c", BELIEF, ETERNAL)

        ;
    }

    @Test
    void testImplicationDecompositionContradictionFairness() {

        test
                .inputAt(0, "b. :|:")
                .inputAt(0, "(a ==>+1 b). :|:")
                .mustBelieve(cycles, "a", 1f, 0.45f, -1)
                .mustNotOutput(cycles, "a", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "a", BELIEF, 0)
        ;


    }


    @Test
    void testTemporalConjunctionWithDepVarIntroduction() {
        /* WRONG:
        $1.0;.05;.10$ ((#1-->a) &&-3 (#1-->d)). 7-5 %1.0;.40% {7-5: 1;2;3} (((%1-->%2),(%1-->%3),neq(%2,%3),time(dtIfEvent)),((($4-->%2)==>($4-->%3)),((Induction-->Belief)),(($4-->%3)==>($4-->%2)),((Abduction-->Belief)),(($4-->%2)<=>($4-->%3)),((Comparison-->Belief)),((#5-->%2)&&(#5-->%3)),((Intersection-->Belief))))
            $.50;.50;.95$ (c-->d). 5+0 %1.0;.90% {5+0: 3} Input
            $1.0;.13;.24$ (c-->a). 3-1 %1.0;.45% {3-1: 1;2} (((%1-->%2),(%3-->%1),neq(%2,%3)),((%2-->%3),((Exemplification-->Belief),(Weak-->Desire),(AllowBackward-->Derive))))
        */

        test

                .inputAt(2, "a:x. | %1.0;0.45%")
                .inputAt(5, "b:x. | %1.0;0.90%")
                .mustBelieve(cycles, "(a:#1 &&+3 b:#1)", 1f, 0.40f, 2)
                .mustNotOutput(cycles, "(a:#1 &&-3 b:#1)", BELIEF, 0f, 1, 0f, 1, 2);

    }

    @Disabled @Test
    void testProjectedQuestion() {
        /*
        Since the question asks about a future time, the belief should
        be projected to it */
        test
                .inputAt(0, "(--, a:b). :|:")
                .inputAt(4, "a:b? :|:")
                .mustBelieve(cycles, "a:b", 0f, 0.58f /* some smaller conf since it is a prediction */, 4);
    }

    @Test
    void testConjInductionEternalTemporal() {
        test
                .input("a:x.")
                .input("a:y. |")
                .mustBelieve(cycles,
                        //"(a:x &| a:y)",
                        "(a:x && a:y)",
                        1f, 0.81f, 0);


    }

    @Test
    void testImplInductionEternalTemporal() {
        test
                .input("a:x.")
                .input("a:y. |")
                .mustBelieve(cycles, "(a:x ==> a:y)", 1f, 0.45f, 0)
                .mustNotOutput(cycles, "(a:x ==> a:y)", BELIEF, 0f, 1, 0f, 1, (t) -> t!=0);
    }

    @Test
    void testComparison1_Eternal() {
        /* (P ==> M), (S ==> M), neq(S,P) |- (S <=> P), (Belief:Comparison, Derive:AllowBackward)
           (M ==> P), (M ==> S), neq(S,P) |- (S <=> P), (Belief:Comparison, Derive:AllowBackward) */

        test
                .input("(p ==>+1 m).")
                .input("(s ==>+4 m).")
                .mustBelieve(cycles, "(s ==>+3 p)", 1f, 0.45f);
    }


    @Test
    void testComparison2() {

        test
                .input("(m ==>+1 p).")
                .input("(m ==>+4 s).")
                .mustBelieve(cycles, "(p ==>+3 s).", 1f, 0.45f);
    }


    @Test
    void testDTTaskEnd() {

        test
                .inputAt(2, "x. :|:")
                .inputAt(5, "y. :|:")
                .mustBelieve(cycles, "(y ==>-3 x)", 1f, 0.45f, 5)
                .mustBelieve(cycles, "(x &&+3 y)", 1f, 0.81f, 2)
                .mustNotOutput(cycles, "(x &&+3 y)", BELIEF, 5)
                .mustNotOutput(cycles, "x", BELIEF, -1)
                .mustNotOutput(cycles, "x", BELIEF, 5)
                .mustNotOutput(cycles, "y", BELIEF, 2)
        ;
    }

    @Test
    void testDecomposeConjunctionTemporal() {

        test
                .input("(x && y). :|:")
                .mustBelieve(cycles, "x", 1f, 0.81f, 0)
                .mustBelieve(cycles, "y", 1f, 0.81f, 0);
    }

    @Test
    void testDecomposeConjunctionEmbedded() {

        test
                .input("((x &&+1 y) &&+1 z). :|:")
                .mustBelieve(cycles, "(x &&+1 y)", 1f, 0.81f, 0)
                .mustBelieve(cycles, "(y &&+1 z)", 1f, 0.81f, 1)
                .mustBelieve(cycles, "(x &&+2 z)", 1f, 0.81f, 0);
    }

    @Test
    void testDecomposeConjunctionEmbedded2() {

        test
                .input("(z &&+1 (x &&+1 y)). :|:")
                .mustBelieve(cycles, "(x &&+1 y)", 1f, 0.81f, t -> t == 1)
                .mustBelieve(cycles, "(z &&+2 y)", 1f, 0.81f, t -> t == 0)
                .mustBelieve(cycles, "(z &&+1 x)", 1f, 0.81f, t -> t == 0);
    }

    @Test
    void testDecomposeConjunctionEmbeddedInnerCommute() {

        test
                .termVolMax(6)
                .input("((&&, a,b,c) &&+1 z). |")
                .mustBelieve(cycles, "(a &&+1 z)", 1f, 0.73f, 0)
                .mustBelieve(cycles, "(b &&+1 z)", 1f, 0.73f, 0)
                .mustBelieve(cycles, "(c &&+1 z)", 1f, 0.73f, 0);
    }


    @Test
    void decomposeConjunctionDTERNAL() {

        test
                .input("(x&&y). :|:")
                .mustBelieve(cycles, "x", 1f, 0.81f, 0)
                .mustBelieve(cycles, "y", 1f, 0.81f, 0);


    }


    @Disabled
    @Test
    void testDecomposeConjunctionQuestion() {

        test

                .input("(x &&+5 y)? :|:")
                .mustOutput(cycles, "x", QUESTION, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 0)
                .mustOutput(cycles, "y", QUESTION, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 5)
        ;
    }

    @Disabled
    @Test
    void testDecomposeConjunctionQuest() {

        test

                .input("(x &&+5 y)@ :|:")
                .mustOutput(cycles, "x", QUEST, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 0)
                .mustOutput(cycles, "y", QUEST, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 5)
        ;
    }

    @Test
    void testWTFDontDecomposeConjunction() {


        test

                .inputAt(0, "((I-->happy) && (I-->neutral)). | %0.06;0.90%")
                .inputAt(0, "(I-->sad). | %0.0;0.90%")


                .mustNotOutput(cycles, "(I-->sad)", BELIEF, 0.5f, 1f, 0.1f, 1f, 0)
                .mustNotOutput(cycles, "(I-->sad)", BELIEF, 0.5f, 1f, 0.1f, 1f, ETERNAL)


        ;
    }


    /**
     * conj subset decomposition
     */
    @Test
    void testConjSubsetDecomposition() throws Narsese.NarseseException {

        test


                .nar.believe($.parallel($("x"), $("y"), $("z")), 3, 1f, 0.9f);

        test
                .mustBelieve(cycles, "(x && y)", 1f, 0.81f, 3)
                .mustBelieve(cycles, "(y && z)", 1f, 0.81f, 3)
                .mustBelieve(cycles, "(x && z)", 1f, 0.81f, 3)
                .mustNotOutput(cycles, "(x && z)", BELIEF, t -> t != 3);

    }

    @Disabled
    @Test
    void testIntersectionTemporalSimultaneous() {

        test
                .inputAt(0, "(x --> a). :|:")
                .inputAt(0, "(y --> a). :|:")
                .mustBelieve(cycles, "((x&y)-->a)", 1f, 0.81f, 0)
        ;
    }

    /**
     * less confident than testIntersectionTemporalNear due to further distance between task and belief
     */
    @Test
    void testIntersectionTemporalFar() {

        test
                .inputAt(0, "(x --> a). :|:")
                .inputAt(3, "(y --> a). :|:")
                .mustNotOutput(cycles, "((x&y)-->a)", BELIEF,
                        t -> !((t >= 0) && (t <= 3)))
        ;
    }

    @Disabled
    @Test
    void testPrediction1() throws Narsese.NarseseException {


        int eventDT = 16;
        int cycles = 10;

        TestNAR t = test;


        t.dur(eventDT / 2);


        int x = 0;
        for (int i = 0; i < cycles; i++) {

            if (i == cycles - 1) {
                TaskTest.task($("y"), QUESTION, null).time(0, x, x + 2 * eventDT).withPri(1f).apply(t.nar);
            }

            t
                    .inputAt(x, "x. :|:")
                    .inputAt(x + eventDT, "y. :|:");


            x += 2 * eventDT;
        }


        int xx = x;
        t.mustBelieve(x - 1, "y", 1f, 0.73f,
                y -> y >= xx && y <= (xx + eventDT));
    }

    @Test
    void multiConditionSyllogismPrePre() {
        test
                .termVolMax(14)
                .input("((open(door)&&hold(key))==>enter(room)). |")
                .mustBelieve(cycles, "(open(door)==>enter(room))", 1f, 0.73f, 0);
    }

    @Test
    void multiConditionSyllogismPre() {


        test.nar.questionPriDefault.pri(0.01f);
        test.nar.termVolMax.set(14);
        test
                .input("hold(key). |")
                .input("((hold(#x) && open(door)) ==> enter(room)). |")
                .mustBelieve(cycles, "(open(door) ==> enter(room))",
                        1.00f, 0.81f,
                        0)
                .mustBelieve(cycles, "(hold(key) ==> enter(room))",
                        1.00f, 0.73f,
                        0)
        ;
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " |"})
    void multiConditionSyllogismPost(String implSuffix) {

//        long implTime = implSuffix.isEmpty() ? ETERNAL : 0;
        String a = "(goto(door) ==> open(door))";
        String b = "(goto(door) ==> hold(key))";
        test
                .termVolMax(14)
                .input("hold(key). |")
                .input("(goto(door) ==> (hold(key) && open(door))). " + implSuffix)
                //temporal via conditional (double premise)
                .mustBelieve(cycles, a, 1f, 0.45f, 0)
                //temporal via conditional (double premise)
                .mustBelieve(cycles, b, 1f, 0.45f, 0)

                //implication belief's time, via structural decomposition
                .mustBelieve(cycles, a, 1f, 0.81f, implSuffix.isEmpty() ? ETERNAL : 0)
                //implication belief's time, via structural decomposition
                .mustBelieve(cycles, b, 1f, 0.81f, implSuffix.isEmpty() ? ETERNAL : 0)
        ;


    }

    @Test
    void preconImplyConjPre() throws Narsese.NarseseException {


        assertEquals("((x &&+2 y) ==>+3 z)",

                $.$("(x ==>+2 (y ==>+3 z))").toString()
        );

        test
                .input("(x ==>+2 a). |")
                .input("(y ==>+3 a). |")
                .mustBelieve(cycles, "((y &&+1 x) ==>+2 a)", 1.00f, 0.81f, 0)
                .mustNotOutput(cycles, "((x &&+1 y) ==>+2 a)", BELIEF, (t) -> t == 0 || t == ETERNAL)
                .mustNotOutput(cycles, "((x && y) ==>+2 a)", BELIEF, (t) -> t == 0 || t == ETERNAL)
                .mustNotOutput(cycles, "((x && y) ==>+3 a)", BELIEF, (t) -> t == 0 || t == ETERNAL);
    }

    @Test
    void preconImplyConjPost() {


        test
                .input("(a ==>+2 x). |")
                .input("(a ==>+3 y). |")
                .mustBelieve(cycles, "(a ==>+2 (x &&+1 y))", 1.00f, 0.81f, 0)
                .mustNotOutput(cycles, "(a ==>+2 (y &&+1 x))", BELIEF, (t) -> t == 0 || t == ETERNAL)
                .mustNotOutput(cycles, "(a ==>+2 (x && y))", BELIEF, (t) -> t == 0 || t == ETERNAL)
                .mustNotOutput(cycles, "(a ==>+3 (x && y))", BELIEF, (t) -> t == 0 || t == ETERNAL);
    }

    @Test
    void preconImplyConjPostB() {


        test.nar.termVolMax.set(10);
        test
                .input("(a ==>+3 x). |")
                .input("(a ==>+2 y). |")
                .mustBelieve(cycles, "(a ==>+2 (y &&+1 x))", 1.00f, 0.81f, 0);
    }

    @Test
    void preconImplyConjPost2() {

        test
                .input("(a ==>+2 x). |")
                .input("(a ==>-3 y). |")
                .mustBelieve(cycles, "(a ==>-3 (y &&+5 x))", 1.00f, 0.81f, 0)
        ;
    }


    @Test
    void testPreConditionCombine() {


        test
                .termVolMax(6)
                .believe("(x ==>+5 z)")
                .believe("(y ==>+5 z)")
                .mustBelieve(cycles, "( (x && y) ==>+5 z)", 1f, 0.81f)
                .mustBelieve(cycles, "( x ==> y)", 1f, 0.81f)
                .mustBelieve(cycles, "( y ==> x)", 1f, 0.81f)
//                .mustQuestion(cycles, "( (x-y) ==>+5 z)")
//                .mustQuestion(cycles, "( (y-x) ==>+5 z)")
                .mustNotOutput(cycles, "( (x && y) ==> z)", BELIEF, 0, 1f, 0, 0.81f, t -> true)
                .mustNotOutput(cycles, "( (x && y) ==> z)", BELIEF, 0, 1f, 0, 0.81f, t -> true)
        //.mustBelieve(cycles, "( --(--x &| --y) ==>+5 z)", 1f, 0.81f)
        ;
    }

    @Test
    void testPostConditionCombine() {

        test.nar.termVolMax.set(5);
        test
                .believe("(z ==>+5 x)")
                .believe("(z ==>+5 y)")
                .mustBelieve(cycles, "( z ==>+5 (x && y))", 1f, 0.81f)
//                .mustBelieve(cycles, "( z ==>+5 --(--x &| --y))", 1f, 0.81f)
                .mustNotOutput(cycles, "( z ==> x )", BELIEF, t -> true) //lost timing
                .mustNotOutput(cycles, "( z ==> y )", BELIEF, t -> true) //lost timing
        ;
    }

    @Test
    void testPreconditionCombineVarying() {

        test
                .believe("(x ==>+5 z)")
                .believe("(y ==>+3 z)")

                .mustBelieve(cycles, "( (x &&+2 y) ==>+3 z)", 1f, 0.81f)
                .mustNotOutput(cycles, "( y ==>+2 x )", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "( y ==>-2 z )", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "( (x &&+2 y) ==>+5 z)", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "( (x &&+2 y) ==>+1 z)", BELIEF, ETERNAL)
        ;
    }

    @Test
    void testPreconditionCombineVaryingNeg() {

        test
                .believe("(--x ==>+5 z)")
                .believe("(--y ==>+3 z)")

                .mustBelieve(cycles, "( (--x &&+2 --y) ==>+3 z)", 1f, 0.81f)
                .mustNotOutput(cycles, "( --y ==>+2 x )", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "( --y ==>-2 z )", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "( (--x &&+2 --y) ==>+5 z)", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "( (--x &&+2 --y) ==>+1 z)", BELIEF, ETERNAL)
        ;
    }

    @Test
    void testPreconditionCombineNeg() {

        test
            .termVolMax(6)
                .believe("(x ==>+5 z)")
                .believe("(--y ==>+5 z)")
                .mustBelieve(cycles, "( (x && --y) ==>+5 z)", 1f, 0.81f);
    }

    @Test
    void testPropositionalDecompositionPositive() {

        test
            .termVolMax(3)
                .believe("s")
                .believe("(s && a)")
                .mustBelieve(cycles, "a", 1f, 0.81f);
    }


    @Test
    void testDTTMinB() {


        test
                .termVolMax(3)
                .believe("(b ==>+10 c)")
                .believe("(e ==>-20 c)")
                .mustBelieve(cycles, "(e ==>-30 b)", 1f, 0.45f)
                .mustBelieve(cycles, "(b ==>+30 e)", 1f, 0.45f)

                .mustNotOutput(cycles, "(b ==>+20 e)", BELIEF, ETERNAL);

    }

    @Test
    void testInductionInterval() {
        /*
        $.02;.69$ (((a-->b) &&+4 (c==>#1)) &&+9 (#1-->e)). 1⋈14 %1.0;.73% {1⋈14: 3;5;8} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(dtAfterOrEternal),neqAndCom(%1,%2)),(varIntro((%1 &&+- %2)),((Intersection-->Belief))))
            $.50;.90$ (d-->e). 10 %1.0;.90% {10: 8} Scheduled
            $.11;.81$ ((a-->b) &&+4 (c==>d)). 1⋈5 %1.0;.81% {1⋈5: 3;5} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(dtAfter)),((%1 &&+- %2),((Intersection-->Belief))))
        */
        test.termVolMax(11);
        test
                .inputAt(1, "((a-->b) &&+4 (c-->d)). :|:")
                .inputAt(10, "(d-->e). :|:")
                .mustBelieve(cycles, "(((a-->b) &&+4 (c-->#1)) &&+5 (#1-->e))", 1f, 0.81f, 1)
                .mustNotOutput(cycles, "(((a-->b) &&+4 (c-->#1)) &&+9 (#1-->e))", BELIEF, t -> t == ETERNAL || t == 1);

    }

    @Test
    void testInductionInterval2() {
        /*
        $.02;.69$ ((a==>b) &&+4 ((b-->#1) &&+3 (#1-->d))). 1⋈8 %1.0;.73% {1⋈8: 1;6;7} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(dtAfter)),((%1 &&+- %2),((Intersection-->Belief))))
            $.48;.90$ (a==>b). 1 %1.0;.90% {1: 1}
            $.12;.81$ ((b-->#1) &&+3 (#1-->d)). 2⋈5 %1.0;.81% {2⋈5: 6;7} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(dtAfterOrEternal),neqAndCom(%1,%2)),(varIntro((%1 &&+- %2)),((Intersection-->Belief))))
        */

        test
                .inputAt(1, "a. :|:")
                .inputAt(2, "(b &&+3 d). :|:")
                .mustBelieve(cycles, "((a &&+1 b) &&+3 d)", 1f, 0.81f, 1)
        ;

    }

    @Test
    void testInductionIntervalMerge1() {

        test.termVolMax(5)
            /*
            1: a
            3: b
            5: c
            */
            .inputAt(1, "(a &&+4 c). |")
            .inputAt(3, "b. |")
            .mustBelieveAtOnly(cycles, "(a &&+4 c)", 1f, 0.81f, 1)
            .mustBelieveAtOnly(cycles, "(b &&+2 c)", 1f, 0.81f, 3)
            .mustBelieveAtOnly(cycles, "((a &&+2 b) &&+2 c)", 1f, 0.81f, 1)
            .mustNotBelieve(cycles, "(b &&+3 (a &&+4 c))",  (t,e) -> t == 1 || t == ETERNAL);
    }

    @Test
    void testInductionIntervalMerge2() throws Narsese.NarseseException {


        assertEquals("(a &&+2 (&&,b,c,d))",
                $("(a &&+2 (&&,b,c,d) )").toString());


        test
                .termVolMax(6)
                .inputAt(1, "(a &&+2 c). |")
                .inputAt(3, "(b && d). |")
                .mustBelieve(cycles,
                        "(a &&+2 (&&,b,c,d) )", 1f, 0.81f, 1);
    }

    @Test
    void testInductionIntervalMerge3() {


        test
                .inputAt(1, "((a &&+3 c) &&+4 e). |")
                .inputAt(4, "b. |")
                .mustBelieve(cycles,
                        "((a &&+3 (b && c)) &&+4 e)",
                        1f, 0.81f, 1);
    }

    @Test
    void testInductionIntervalMerge3Neg() throws Narsese.NarseseException {

//        List m = $.newArrayList();
        Term d = $("--(a &&+3 --c)");
        assertEquals(0, d.eventRange());
//        d.events(m::addAt);

//        List l = $.newArrayList();
//        $("(--(a &&+3 --c) &&+4 (e))").events(l::addAt);

        test
                .inputAt(1, "(--(a &&+3 --c) &&+4 (e)). :|:")
                .inputAt(4, "b. :|:")
                .mustBelieve(cycles,
                        "(((--,(a &&+3 (--,c))) &&+3 b) &&+1 (e))",
                        1f, 0.81f, 1)
                .mustNotOutput(cycles, "(((--,(a &&+3 (--,c))) &&+3 b) &&+4 (e))", BELIEF, t -> t == ETERNAL || t == 1);

    }

    @Test
    void testDecomposeTaskSubset() {
        /*
        $0.0;.23$ (((d&&((a-->b) &&+1 (b-->c))) ==>+8 e) &&+9 (d-->e)). 1⋈10 %1.0;.31% {1⋈10: 4;5;6;7;8} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(conjoin)),((%1 &&+- %2),((Intersection-->Belief))))
        $.03;.40$ ((d&&((a-->b) &&+1 (b-->c))) ==>+8 e). 1 %1.0;.42% {1: 4;5;6} ((%1,%2,belief(positive),task("."),time(raw),time(dtAfterReverse),notEqui(%1),notImpl(%2)),((%2 ==>+- %1),((Abduction-->Belief))))
            |- (d -->e) @ 10, not 19
        */

        test
                .input("(((a-->b) &&+1 (b-->c)) &&+1 (d-->e)). :|:")
                .input("((a-->b) &&+1 (b-->c)). :|:")
                .mustBelieve(cycles, "(d-->e)", 1f, 0.73f, 2)
                .mustNotOutput(cycles, "d", BELIEF, 0, 1, 0, 1, t -> true)
        ;
    }

    @Test
    void testDecomposeTaskDontDecomposeNonEvent() {
        test
                .input("((a-->b) &&+1 (b-->c)). |")
                .mustNotOutput(cycles, "d", BELIEF, 0, 1, 0, 1, t -> true)
        ;
    }

    @Test
    void testDecomposeTaskDontDecomposeNonEventNeg() {

        test
                .input("((a-->b) &&+1 (b-->c)). | %0.25%")
                .mustNotOutput(cycles, "d", BELIEF, 0, 1, 0, 1, t -> true)
        ;
    }

    @Test
    void testForwardImplChainDTUnion() {
        /** wrong direction: this should have been dt = +20
         $.29;.69$ ((reshape(I,$1)&&($1-->[heated])) ==>-20 ($1-->[hardened])). %1.0;.73% {3: 3;4;5} ((((%2&&%1073742337..+)==>%3),(%4==>%2),time(dtUnion),neq(%3,%2),notImpl(%2),notEqui(%3)),(((%4&&%1073742337..+) ==>+- %3),((Deduction-->Belief))))
         $.50;.90$ ((reshape(I,$1) &| ($1-->[pliable])) ==>+10 ($1-->[hardened])). %1.0;.90% {0: 5}
         $.41;.81$ (($1-->[heated]) ==>+10 ($1-->[pliable])). %1.0;.81% {1: 3;4} ((%1,(%2<=>%3),neqCom(%1,%3),neq(%1,%2),time(beliefDTSimultaneous)),(substitute(%1,%2,%3,strict),((Intersection-->Belief),(Strong-->Goal))))
         */

        test.nar.termVolMax.set(17);
        test.nar.confMin.set(0.5f);
        test

                .input("((reshape(I,$1) && ($1-->[pliable])) ==>+10 ($1-->[hardened])).")
                .input("(($1-->[heated]) ==>+10 ($1-->[pliable])).")


                .mustBelieve(cycles, "(($1-->[heated]) ==>+20 ($1-->[hardened]))", 1f, 0.73f, ETERNAL)
                .mustNotOutput(cycles, "(($1-->[heated]) ==>-20 ($1-->[hardened]))", BELIEF,
                        (t -> t == ETERNAL || t == 10 || t == 20 || t == 0));
    }

    @Test
    void testDecomposeImplPredNoVar() {

        test.nar.termVolMax.set(15);
        test
                .believe("( a ==> (&&, x, y, z ) )", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "( a ==> x )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( a ==> y )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( a ==> z )", 1f, 0.73f, 0)
        ;
    }

    @Test
    void testDecomposeImplPred_ParallelConj_with_InDepVar() {


        test
                .termVolMax(12)
                .believe("( (a,$1) ==> (&&, (x,$1), y, z ) )", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "( (a,$1) ==> (x,$1) )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( (a,#1) ==> y )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( (a,#1) ==> z )", 1f, 0.73f, 0)
        ;
    }

    @Test
    void testDecomposeImplPred_ParallelConj_with_DepVar() {

        test
                .termVolMax(12)
                .believe("( (a,#1) ==> (&&, (x,#1), y, z ) )", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "( (a,#1) ==> (x,#1) )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( (a,#1) ==> y )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( (a,#1) ==> z )", 1f, 0.73f, 0)
        ;
    }

    @Test
    void testDecomposeImplPredSimpler() {
        test.nar.termVolMax.set(11);

        test
                .believe("( a ==> (&&,x,y,z) )", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "( a ==> x )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( a ==> y )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( a ==> z )", 1f, 0.73f, 0)
        ;
    }


    @Test
    void nal5_conditional_induction0Simple() {
        test.termVolMax(5);
        test.believe("((x1 && a) ==>+2 c)");
        test.believe("((y1 && a) ==>+1 c)");
        test.mustBelieve(cycles, "(x1 ==>+1 y1)", 1.00f, 0.45f);
        test.mustBelieve(cycles, "(y1 ==>-1 x1)", 1.00f, 0.45f);
    }

    @Test
    public void occtestShiftWorkingRight() {

        test.termVolMax(7);
        test.inputAt(1, "(x &&+5 y). |");
        test.inputAt(1, "((x &&+5 y)==>(b &&+5 c)). |");
        test.mustBelieve(cycles, "(b &&+5 c)", 1.00f, 0.45f, (t) -> t == 6);
        test.mustNotOutput(cycles, "(b &&+5 c)", BELIEF, 0f, 1f, 0.5f, 1f,
                (t) -> t != 6);
    }

    @Test
    public void occtestShiftWorkingRight2() {

        test.nar.termVolMax.set(12);
        test.inputAt(1, "(a &&+5 (--,a)). |");
        test.inputAt(1, "((a &&+5 (--,a))==>(b &&+3 (--,b))). |");
        test.mustBelieve(cycles, "(b &&+3 --b)", 1.00f, 0.45f, (t) -> t == 6);
//        test.mustNotOutput(cycles , "(b &&+3 --b)", BELIEF, 0f, 1f, 0f, 1f,
//                (t) -> t!=6);
    }

    @Test
    void testDurationOfInductedImplication() {
        /*
        right start/end timing of result?
           $.21 ((--,(left-->trackXY)) ==>+384 (right-->trackXY)). 21516⋈21867 %0.0;.44% {22256: 1ÂS;1ÅÌ;1Åà;1Åõ} ((%1,%2,(--,is(%1,"==>"))),(((--,%2) ==>+- %1),((InductionPN-->Belief),(BeliefRelative-->Time),(VarIntro-->Also))))
                $.50 (right-->trackXY). 21900⋈22251 %0.0;.90% {21900: 1Åõ}
                $.50 (left-->trackXY). 21516⋈22251 %0.0;.86% {22256: 1Åà;1ÅÌ;1ÂS}
         */
        test.inputAt(1L, "x. |..+2"); //1..3
        test.inputAt(2L, "y. |..+2"); //2..4
        test.mustBelieve(cycles, "(x ==>+1 y)", 1f, 0.45f, 1, 3); //(s,e)->(s==1 && e==2));
    }

    @Test
    void testDurationOfInductedImplicationLimited() {
        test.inputAt(1L, "x. |..+2"); //1..3
        test.inputAt(2L, "y. |..+1"); //2..3
        test.mustBelieve(cycles, "(x ==>+1 y)", 1f, 0.45f, 1, 2); //(s,e)->(s==1 && e==2));
    }

    @Test
    void testBeliefShiftTiming() {
        test
                .inputAt(1, "(a-->c). |")
                .inputAt(2, "(($1-->c) ==> ((a-->$1) &&+4 (c-->d))). |")
                .mustBelieve(cycles, "(c-->d)", 1f, 0.5f, t -> t == 5)
                .mustNotOutput(cycles, "(c-->d)", BELIEF, 0f, 1f, 0f, 1f, t -> t < 2);
    }
}
