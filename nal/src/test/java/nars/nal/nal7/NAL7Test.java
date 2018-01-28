package nars.nal.nal7;

import nars.$;
import nars.Narsese;
import nars.term.Term;
import nars.test.TestNAR;
import nars.time.Tense;
import nars.util.NALTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static nars.$.$;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class NAL7Test extends NALTest {

    public static final float CONF_TOLERANCE_FOR_PROJECTIONS = 0.85f;
    public int cycles = 350;

    @BeforeEach
    public void setTolerance() {
        test.confTolerance(CONF_TOLERANCE_FOR_PROJECTIONS);
    }

//    private static final Path tmpDir;
//    static {
//        try {
//            tmpDir
//                = new File("/tmp/" + NALTest.class.getSimpleName()).toPath();
//                // = Files.createTempDirectory(NALTest.class.getSimpleName());
//
//            if (!tmpDir.toFile().exists())
//                Files.createDirectory(tmpDir);
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    protected void afterTest(TestInfo testInfo) {
//        try {
//            File f = tmpDir.resolve(testInfo.getTestClass().get().getSimpleName() + "."
//                    + testInfo.getDisplayName().replace("()","") +
//                    ".nal").toFile();
//            nar.outputBinary(f, false);
//            logger.info("{} output {}", testInfo, f);
//        } catch (IOException e) {
//            logger.error("{} output error {}", test, e);
//        }
//
//    }

    @Test
    public void induction_on_events_neg2() {

        test
                .log()
                .input("x:before. :|:")
                .inputAt(1, "(--,x:after). :|:")
                .mustBelieve(cycles, "(x:before ==>+1 x:after)", 0.00f, 0.45f /*abductionConf*/, 0)
                .mustBelieve(cycles, "((--,x:after) ==>-1 x:before)", 1.00f, 0.45f /*inductionConf*/, 10)
                .mustBelieve(cycles, "(x:before &&+1 (--,x:after))", 1.00f, 0.81f /*intersectionConf*/, 0)
                .mustNotOutput(cycles, "(x:before &&-1 (--,x:after))", BELIEF,
                        (t -> t == 0 || t == 1));
//                .mustBelieve(cycles, "(x:after <=>-10 x:before)", 0.00f, 0.45f /*comparisonConf*/, 0, 10)
    }

    @Test
    public void temporal_explification() {

        TestNAR tester = test;
        tester.believe("(<($x, room) --> enter> ==>-5 <($x, door) --> open>)", 0.9f, 0.9f);
        tester.believe("(<($y, door) --> open> ==>-5 <($y, key) --> hold>)", 0.8f, 0.9f);

        tester.mustBelieve(cycles, "(<($1,key) --> hold> ==>+10 <($1,room) --> enter>)", 1.00f, 0.37f);

    }


    @Test
    public void temporal_analogy() {

        test

                .believe("( open($x, door) ==>+5 enter($x, room) )", 0.95f, 0.9f)
                //.believe("( enter($x, room) <=>+0 leave($x, corridor_100) )", 1.0f, 0.9f)
                .believe("( enter($x, room) =|> leave($x, corridor_100) )", 1.0f, 0.9f)
                .believe("( leave($x, corridor_100) =|> enter($x, room) )", 1.0f, 0.9f)
                .mustBelieve(cycles * 2, "( open($1, door) ==>+5 leave($1, corridor_100) )", 0.95f, 0.77f /*0.81f*/)
                .mustNotOutput(cycles * 2, "( open($1, door) ==>-5 leave($1, corridor_100) )", BELIEF, ETERNAL); //test correct dt polarity

    }

    @Test public void testConjDecomposeAGAIN() {
        /*
          WRONG TIME
          $.20 (b &&+5 c). 11 %1.0;.73% {105: 2;3} ((%1,%2,task("."),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
            $.27 c. 11 %1.0;.81% {13: 3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief))))
              $.50 (c &&+5 d). 11 %1.0;.90% {11: 3} Narsese
            $.50 (b &&+5 c). 6 %1.0;.90% {6: 2} Narsese
         */
          test
            .inputAt(11, "c. :|:")
            .inputAt(6, "(b &&+5 c). :|:") //should not unify in the same direction
            .mustNotOutput(cycles, "(b &&+5 c)", BELIEF, 11);
    }

    @Test
    public void testConjDecomposeWrongDirection() {
        /*
        $.13 b. -4 %1.0;.81% {399: 1;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
            $.50 (a &&+5 b). 1⋈6 %1.0;.90% {1: 1}
         */
        test
                .inputAt(1, "(a &&+5 b). :|:")
                .inputAt(6, "(b &&+5 #1). :|:") //should not unify in the same direction
                .mustBelieve(cycles, "(a &&+10 #1)", 1.00f, 0.73f, 1)
                .mustBelieve(cycles, "a", 1.00f, 0.81f, 1)
                .mustBelieve(cycles, "b", 1.00f, 0.81f, 6)
                .mustNotOutput(cycles, "b", BELIEF, -4)
                .mustNotOutput(cycles, "((a&|b) &&+5 (b&|#1))", BELIEF, t->true) //wtf
        ;
    }

    @Test
    public void testConjDecomposeWrongDirection2() {
        /*
        $.13 b. -4 %1.0;.81% {399: 1;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
            $.50 (a &&+5 b). 1⋈6 %1.0;.90% {1: 1}
         */
        test
                .inputAt(1, "((a &&+5 b) ==>+5 #1). :|:")
                .inputAt(1, "a. :|:")
                .mustNotOutput(cycles, "(a &&+5 b)", BELIEF, (t) -> t < 0 /* any negative number occ */)
        ;
    }

    @Test
    public void testConjDecomposeShift() {
        /*
        WRONG:
            $.02 (b &&+5 #1). 16⋈21 %1.0;.59% {410: 1;2;3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
                $.08 ((a &&+5 b) &&+5 #1). 1⋈11 %1.0;.66% {64: 1;2;3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
        */

        test
                .inputAt(1, "((a &&+5 b) &&+5 c). :|:")
                .mustBelieve(cycles, "(b &&+5 c)", 1.00f, 0.81f, 6)
                .mustNotOutput(cycles, "(b &&+5 c)", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "(b &&+5 c)", BELIEF, 16) //<- not caught here TODO fix this mustNotEmit stuff
        ;
    }

    @Test
    public void testDropAnyEventSimple() {
        /*
          instability:
            $0.0 (c). 7 %1.0;.73% {89: 2;3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
                $.18 ((b) &&+3 (c)). 2⋈5 %1.0;.81% {11: 2;3} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%2,belief) &&+- polarize(%1,task)),((IntersectionDepolarized-->Belief))))
                  $.50 (b). 2 %1.0;.90% {2: 2}
                  $.50 (c). 5 %1.0;.90% {5: 3}
         */
        test
                .inputAt(1, "(a). :|:") //try to ignore this
                .inputAt(2, "(b). :|:")
                .inputAt(5, "(c). :|:")
                .mustBelieve(cycles, "((b) &&+3 (c))", 1.00f, 0.81f, 2)
                .mustNotOutput(cycles, "(c)", BELIEF, (t) -> t != 5)
        ;
    }

    @Test
    public void testDropAnyEventSimple2() {
        test
                .inputAt(1, "((happy &&+4120 (i)) &&+1232 (--,(i)))! :|:")
                .mustGoal(cycles, "(happy &&+4120 (i))", 1f, 0.81f, 1)
                .mustNotOutput(cycles, "((i)&&happy)", GOAL, 1)
        ;
    }

    @Test
    public void updating_and_revision() {
        testTemporalRevision(10, 0.50f, 0.7f, "<(John,key) --> hold>");
    }

    @Test
    public void updating_and_revision2() {
        testTemporalRevision(1, 0.5f, 0.7f, "<(John,key) --> hold>");
    }

    void testTemporalRevision(int delay, float freq, float conf, @NotNull String belief) {

        TestNAR tester = test;

        tester.input(belief + ". :|: %1.00;0.65%");
        tester.inputAt(delay, belief + ". :|: %0.5;0.70%");
        tester.inputAt(delay + 1, belief + "? :|:");
        tester.mustBelieve(delay + 50, belief, freq, conf, delay);
    }

    @Test
    public void testSumNeg() {
        //(P ==> M), (M ==> S), neq(S,P), dt(sumNeg) |- (S ==> P), (Belief:Exemplification, Derive:AllowBackward)

        test

                .believe("(x ==>+2 y)")
                .believe("(y ==>+3 z)")
                .mustBelieve(cycles, "(z ==>-5 x)", 1.00f, 0.45f);

    }


    @Test
    public void testSum() {

        test
                .believe("(x ==>+2 y)")
                .believe("(y ==>+3 z)")
                .mustBelieve(cycles, "(x ==>+5 z)", 1.00f, 0.81f);
    }

    @Test
    public void testBminT() {
        //(P ==> M), (S ==> M), neq(S,P), dt(bmint) |- (S ==> P), (Belief:Induction, Derive:AllowBackward)

        test
                .believe("(x ==>+2 y)")
                .believe("(z ==>+3 y)")
                .mustBelieve(cycles, "(z ==>+1 x)", 1.00f, 0.45f)
                .mustNotOutput(cycles, "(z ==>-2 x)", BELIEF, ETERNAL)
        ;
    }

    @Test
    public void testDontStretchImplDerivation() {
        /*
        WRONG
          $0.0 (((a,b) &&+4 (#1,d)) ==>+1 (b,#1)). 1⋈5 %1.0;.40% {31: 1;2;3} ((%1,%2,task("."),time(raw),time(dtEventsOrEternals),neqAndCom(%1,%2)),(varIntro((polarize(%2,belief) &&+- polarize(%1,task))),((IntersectionDepolarized-->Belief))))
              $.01 ((a,b) ==>+1 (b,c)). 1 %1.0;.45% {2: 1;2} ((%1,%2,time(raw),belief(positive),task("."),time(dtEvents),neq(%1,%2),notImpl(%2)),((%2 ==>+- %1),((Induction-->Belief))))
              $.50 (c,d). 5 %1.0;.90% {5: 3}
        */

        test
                .inputAt(1, "((a,b) ==>+1 (b,c)). :|:")
                .inputAt(5, "(c, d). :|:")
                .mustBelieve(cycles, "(((a,b) &&+4 (c,d)) ==>-3 (b,c))", 1f, 0.81f, 1)
                .mustBelieve(cycles, "(((a,b) &&+4 ($1,d)) ==>-3 (b,$1))", 1f, 0.81f, 1)
        ;

    }

    @Disabled
    @Test
    public void justPlainWrongTiming() {
        /*
        WRONG
        $0.0 ((a&|b) &&+5 (b&|c)). -9⋈-4 %1.0;.11% {397: 1;2;3} ((%1,(%2==>%3),time(urgent),neq(%1,%2),notImpl(%1)),(subIfUnifiesAny(%2,%3,%1),((AbductionRecursivePB-->Belief),(DeciInduction-->Goal))))
            $.50 (c &&+5 d). 11⋈16 %1.0;.90% {11: 3}
            $0.0 (((a&|b) &&+5 (b&|c)) ==>+5 (c &&+5 d)). 6 %1.0;.29% {310: 1;2;3} ((((&&,%1073742337..+)==>%2),%3,neqRCom(%2,%3),notImpl(%3)),((((&&,%1073742337..+) &&+- %3) ==>+- %2),((Induction-->Belief))))
            */
        test
                .inputAt(11, "(c &&+5 d). :|:")
                .inputAt(6, "(((a&|b) &&+5 (b&|c)) ==>+5 (c &&+5 d)). :|:")
                .mustBelieve(cycles, "((a&|b) &&+5 (b&|c))", 1, 0.45f, 1)
                //.mustNotOutput(cycles, "((a&|b) &&+5 (b&|c))", BELIEF, (t) -> t!=1)
                .mustNotOutput(cycles, "(c &&+5 d)", BELIEF, (t) -> t != 11)
        ;
    }

    @Test
    public void testSameDamnConjunction() {
        test
                .inputAt(6, "(b &&+5 c). :|: %1.0;0.66%")
                .inputAt(6, "(b &&+5 c). :|: %1.0;0.90%")
                //.mustBelieve(cycles, "(b &&+5 c)", 1f, 0.59f)
                .mustNotOutput(cycles, "((b &&+5 c) &&+5 (b &&+5 c))", BELIEF, (x) -> true)
                .mustNotOutput(cycles, "((b &&+5 c) &&+5 b)", BELIEF, (x) -> true);

    }

    @Test
    public void testTminB() {
        //(M ==> P), (M ==> S), neq(S,P), dt(tminb) |- (S ==> P), (Belief:Abduction, Derive:AllowBackward)

        //y..x
        //y.z
        //  zx
        test
                .believe("(y ==>+3 x)")
                .believe("(y ==>+2 z)")
                .mustBelieve(cycles, "(z ==>+1 x)", 1.00f, 0.45f)
                .mustNotOutput(cycles, "(z ==>-1 x)", BELIEF, ETERNAL)
                .mustBelieve(cycles, "(x ==>-1 z)", 1.00f, 0.45f)
                .mustNotOutput(cycles, "(x ==>+1 z)", BELIEF, ETERNAL)
//                .mustBelieve(cycles, "(z <=>+1 x)", 1.00f, 0.45f)
//                .mustNotOutput(cycles, "(z <=>-1 x)", BELIEF, ETERNAL)
        ;

    }

//    @Test
//    public void testImplQuery() {
//        test()
//                .believe("(y ==>+3 x)")
//                .input("(y ==>+3 ?x)?")
//                .mustAnswer(15, "(y ==>+3 x)", 1.00f, 0.9f, Tense.Eternal);
//    }
//
//    @Test
//    public void testImplQueryTense() {
//        test()
//                .input("(y ==>+3 x). :|:")
//                .input("(y ==>+3 ?x)? :|:")
//                .mustAnswer(15, "(y ==>+3 x)", 1.00f, 0.90f, Tense.Present);
//    }
//
//    @Test
//    public void testImplQueryTenseFuture() {
//        test()
//                
//                .mustAnswer(cycles, "(y ==>+3 x)", 1.00f, 0.18f, 2)
//                .input("(y ==>+3 x). :\\:")
//                .inputAt(1, "(y ==>+3 ?z)? :/:");
//                //.mustAnswer(50, "(y ==>+3 x)", 1.00f, 0.74f, 15);
//    }

//    @Test public void testImplQuery2() {
//        TestNAR t = test();
//        t.nar;
//        t.believe("(y ==>+3 x)")
//        .input("(y ==> x)?")
//        .mustAnswer(15, "(y ==>+3 x)", 1.00f, 0.9f, Tense.Eternal);
//    }

//    @Test
//    public void intervalPreserve_and_shift_occurence_corner_case() {
//        TestNAR tester = test();
//        //tester;
//        tester.input("S:s.");
//        tester.inputAt(3, "(S:s &&+3 Z:z). :|:");
//        tester.mustBelieve(cycles, "S:s.", 1.00f, 0.81f /* 0.42? */, 3);
//        tester.mustBelieve(cycles, "Z:z.", 1.00f, 0.81f /* 0.42? */, 6);
//        tester.mustNotOutput(cycles,"Z:z",BELIEF,3);
//    }

    @Test
    public void intervalPreserve_and_shift_occurence() {

        test

                //.input("X:x.") //shouldnt be necessary
                .inputAt(1, "(X:x &&+1 (Y:y &&+2 Z:z)). :|:")
                .mustBelieve(cycles, "X:x.", 1.00f, 0.73f, 1)
                .mustBelieve(cycles, "(Y:y &&+2 Z:z).", 1.00f, 0.81f, 2)
                .mustBelieve(cycles, "Y:y.", 1.00f, 0.73f, 2)
                .mustBelieve(cycles, "Z:z.", 1.00f, 0.73f, 4)
        ;

    }


    @Test
    public void temporal_deduction() {

        test
                .believe("(enter($x, room) ==>-3 open($x, door))", 0.9f, 0.9f)
                .believe("(open($y, door) ==>-4 hold($y, key))", 0.8f, 0.9f)
                .mustBelieve(cycles, "(enter($1,room) ==>-7 hold($1,key))", 0.72f, 0.58f)
                .mustNotOutput(cycles, "(enter($1,room) ==>-4 hold($1,key))", BELIEF, ETERNAL) //avoid the substitutionIfUnifies result
                .mustBelieve(cycles, "(hold($1,key) ==>+7 enter($1,room))", 1f, 0.37f);
    }

    @Test
    public void temporal_induction_comparison() {

        test

                // hold ==>+4 open ==>+5 enter
                .believe("((( $x, door) --> open) ==>+5 (( $x, room) --> enter))", 0.9f, 0.9f)
                .believe("((( $y, door) --> open) ==>-4 (( $y, key) --> hold))", 0.8f, 0.9f)


                .mustBelieve(cycles, "( hold($1,key) ==>+9 enter($1,room) )", 0.9f, 0.39f)
                .mustBelieve(cycles, "( enter($1,room) ==>-9 hold($1,key) )", 0.8f, 0.42f);
//                .mustBelieve(cycles, "(hold($1,key) <=>+9 enter($1,room) )", 0.73f, 0.44f)
//                .mustNotOutput(cycles, "(hold($1,key) <=>-9 enter($1,room))", BELIEF, ETERNAL); //test correct dt polarity

    }

    @Test
    public void inference_on_tense() {

        test
                .input("(hold($x, key) ==>+1 enter($x, room)).")
                .input("hold(John, key). :|:")
                .mustBelieve(cycles, "enter(John,room)", 1.00f, 0.81f, 1);
    }

    @Test
    public void inference_on_tense_reverse() {

        test
                .input("(hold($x, key) ==>+7 enter($x, room)).")
                .input("enter(John, room). :|:")
                .mustBelieve(cycles, "hold(John,key)",
                        1.00f, 0.45f, -7);
    }

    @Test
    public void inference_on_tense_reverse_novar() {

        test

                .input("(hold(John, key) ==>+7 enter(John, room)).")
                .input("enter(John, room). :|:")
                .mustBelieve(cycles, "hold(John,key)",
                        1.00f, 0.45f, -7);
    }

    @Test
    public void inference_on_tense_3() {

        test
                .inputAt(0, "hold(John,key). :|:")
                .believe("(hold(John,key) ==>+1 enter(John,room))", 1.0f, 0.9f)
                .mustBelieve(cycles, "enter(John,room)",
                        1.00f, 0.81f, 1)
        ;
    }

    @Test
    public void inference_on_tense_4() {

        test
                .believe("(hold(John,key) ==>+3 enter(John,room))")
                .inputAt(0, "enter(John,room). :|:")
                .mustBelieve(cycles, "hold(John,key)", 1.00f, 0.45f, -3);
    }

    @Test
    public void induction_on_events_0() {

        test

                .inputAt(4, "enter(John,room). :|:")
                .input("open(John,door). :|:")
                .mustBelieve(cycles, "( enter(John, room) ==>-4 open(John, door) )",
                        1.00f, 0.45f, 4);
    }

    @Test
    public void induction_on_events_0_neg() {

        test
                .input("(--,open(John,door)). :|:")
                .inputAt(4, "enter(John,room). :|:")
                .mustBelieve(cycles, "( (--,open(John, door)) ==>+4 enter(John, room) )",
                        1.00f, 0.45f, 0)
                .mustBelieve(cycles, "( (--,open(John, door)) &&+4 enter(John, room) )",
                        1f, 0.81f, 0)
        ;
    }

    @Test
    public void induction_on_events2() {

        test
        .input("<(John,door) --> open>. :|:")
        .inputAt(4, "<(John,room) --> enter>. :|:")
        .mustBelieve(cycles, "(((John, door) --> open) ==>+4 ((John, room) --> enter))",
                1.00f, 0.45f, 0);

    }

    @Test
    public void induction_on_events3() {

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
    public void induction_on_events3_simple() {

        TestNAR tester = test;

        tester.inputAt(1, "<door --> open>. :|:");
        tester.inputAt(2, "<room --> enter>. :|:");

        tester.mustBelieve(cycles, "(<door --> open> ==>+1 <room --> enter>)",
                1.00f, 0.45f,
                1);
    }

    @Test
    public void induction_on_events_pos_neg() {

        test
                .inputAt(1, "a. :|:")
                .inputAt(2, "--b. :|:")
                .mustBelieve(cycles, "(a &&+1 --b)", 1.00f, 0.81f, 1)
                .mustBelieve(cycles, "(--b ==>-1 a)", 1.00f, 0.45f, 2)
                .mustBelieve(cycles, "(a ==>+1 b)", 0.00f, 0.45f, 1)
        ;
    }

    @Test
    public void induction_on_events_conj_pos_neg() {
        test
                .log()
                .inputAt(1, "(a &&+5 (--,a)). :|:")
                .inputAt(6, "(b &&+5 (--,b)). :|:")
                .mustBelieve(cycles, "((a &&+5 ((--,a)&|b)) &&+5 (--,b))", 1.00f, 0.81f, 1)
        ;
    }

    @Test
    public void induction_on_events_neg_pos() {

        test
                .inputAt(1, "--b. :|:")
                .inputAt(2, "a. :|:")
                .mustBelieve(cycles, "(--b &&+1 a)", 1.00f, 0.81f, 1) //i
                .mustBelieve(cycles, "(--b ==>+1 a)", 1.00f, 0.45f, 1) //in
        //.mustBelieve(cycles, "(a ==>-1 b)", 0.00f, 0.45f, 2) //ab
        ;
    }

    @Test
    public void induction_on_events_neg_neg() {

        test
                .log()
                .inputAt(1, "--a. :|:")
                .inputAt(2, "--b. :|:")
                .mustBelieve(cycles, "(--a &&+1 --b)", 1.00f, 0.81f, 1)
                .mustBelieve(cycles, "(--a ==>+1 b)", 0.00f, 0.45f, 1)
                .mustBelieve(cycles, "(--b ==>-1 a)", 0.00f, 0.45f, 2);
    }

    @Test
    public void induction_on_events_with_variable_introduction() {

        TestNAR tester = test;

        tester.input("open(John,door). :|:");
        tester.inputAt(2, "enter(John, room). :|:");

        //note: this result is reversed (pred equiv direction AND the occurrence time) from the original NAL7 test but its semantics are equivalent
        tester.mustBelieve(cycles,
                "(enter($1,room) ==>-2 open($1,door))",
                1.00f, 0.45f,
                2
        );

    }


    @Test
    public void induction_on_events_with_variable_introduction2() {

        TestNAR tester = test;

        //tester;
        tester.input("open(John, door). :|:");
        tester.inputAt(2, "enter(John, room). :|:");


        tester.mustBelieve(cycles * 2,
                "(open($1,door) ==>+2 enter($1, room))",
                1.00f,
                0.45f /* 0.45f */,
                0
        );

        //REVERSE:
//        tester.mustBelieve(cycles*4,
//                "(<$1 --> (/, open, _, door)> ==>+2 <$1 --> (/, enter, _, room)>)",
//                1.00f, 0.45f,
//                0
//        );

    }

    @Test
    public void induction_on_events_composition_pre() {

        test

                .input("hold(John,key). :|:")
                .input("(open(John,door) <-> enter(John,room)). :|:")
                .mustBelieve(cycles, "(hold(John,key) &&+0 (open(John,door) <-> enter(John,room)))",
                        1.00f, 0.81f,
                        0);
    }

    @Test
    public void induction_on_events_composition1() {
        compositionTest(1, 5);
    }

    @Test
    public void induction_on_events_composition2() {
        compositionTest(1, 7);
    }

    @Test
    public void induction_on_events_composition3() {
        compositionTest(4, 3);
    }

    @Test
    public void induction_on_events_composition_post() {
        TestNAR tester = test;

        int t = 1;
        int dt = 7;
        String component = "(open(John,door) &| hold(John,key))";
        tester.inputAt(t, component + ". :|:");
        tester.inputAt(t + dt, "enter(John,room). :|:");

        tester.mustBelieve((t + dt) + dt + 1 /** approx */,
                "(" + component + " ==>+" + dt + " enter(John,room))",
                1.00f, 0.45f,
                t);


    }

    private void compositionTest(int t, int dt) {


        test.inputAt(t, "hold(John,key). :|:");
        test.inputAt(t, "(open(John,door) ==>+" + dt + " enter(John,room)). :|:");

        //tester;

        String component = "(open(John,door) &&+0 hold(John,key))";

        //Given:
        test.mustBelieve(cycles * 2, "hold(John,key)",
                1.00f, 0.9f,
                t);

        //Result of 2nd Input's Decomposition
        test.mustBelieve(cycles * 2, "open(John,door)",
                1.00f, 0.81f,
                t);
        test.mustBelieve(cycles * 2, "enter(John,room)",
                1.00f, 0.81f,
                t + dt);

        test.mustBelieve(cycles * 2, component,
                1.00f, 0.73f,
                t);

        //this is probably prevented by stamp overlap:
//        tester.mustBelieve(cycles*12, "(" + component + " ==>+" + dt + " enter(John,room))",
//                1.00f, 0.45f,
//                t+dt);
    }

    @Test
    public void variable_introduction_on_events() {

        TestNAR tester = test;

        tester.input("at(SELF,{t003}). :|:");
        tester.inputAt(4, "on({t002},{t003}). :|:");

        tester.mustBelieve(cycles,
                "(at(SELF,{t003}) &&+4 on({t002},{t003}))",
                1.0f, 0.81f,
                0);
        tester.mustBelieve(cycles,
                "(at(SELF,#1) &&+4 on({t002},#1))",
                1.0f, 0.81f,
                0);
        tester.mustNotOutput(cycles, "(at(SELF,#1) &&-4 on({t002},#1))", BELIEF, ETERNAL);

    }

    @Test
    public void variable_introduction_on_events_with_negation() {

        test
                .input("(--,a:x). :|: %0.9;0.8% ")
                .inputAt(10, "b:x. :|: %0.8;0.9% ")

                .mustBelieve(cycles,
                        "(b:x ==>-10 a:x)",
                        0.1f, 0.37f,
                        10)
//                .mustBelieve(cycles,
//                        "(a:x <=>+10 b:x)", 0.27f, 0.41f, // and here, as a result of the comparison truth function's asymmetry
//                        0, 10)
//                .mustBelieve(cycles,
//                        "(($1 --> a) <=>+10 ($1 -->b))",
//                        0.27f, 0.41f,
//                        0, 10)
        ;

    }

    @Test
    public void variable_elimination_on_temporal_statements() {

        test
                .inputAt(0, "(on({t002},#1) &| at(SELF,#1)). :|:")
                .inputAt(1, "((on($1,#2) &| at(SELF,#2)) =|> reachable(SELF,$1)).")
                .mustBelieve(cycles, "reachable(SELF,{t002})",
                        1.0f, 0.81f, 0);

    }

    @Test
    public void testTemporalImplicationDecompositionIsntEternal() {

        /*
        Test that this eternal derivation does not happen, and that it is temporal with the right occ time

        $.08;.01;.23$ (--,(p3)). :1: %1.0;.40% {1: 5;7} ((%1,(%2==>%3),time(decomposeBelief)),(substituteIfUnifies(%3,"$",%2,%1),((Deduction-->Belief),(Induction-->Desire),(Anticipate-->Event))))
            $.50;.50;.95$ (p2). 0+0 %1.0;.90% {0+0: 5} Input
            $.75;.06;.12$ ((p2) ==>+0 (--,(p3))). 1-1 %1.0;.45% {1-1: 5;7} ((%1,%2,time(dtAfterReverse),neq(%1,%2),notImplicationOrEquivalence(%1),notImplicationOrEquivalence(%2)),((%2==>%1),((Abduction-->Belief)))) */

        test
                .confTolerance(0.1f)
                .inputAt(0, "(a). :|:")
                .inputAt(0, "((a) ==>+1 (b)). :|:")
                .mustNotOutput(cycles, "(b)", BELIEF, ETERNAL)
                .mustBelieve(cycles, "(b)", 1f, 0.81f, 1 /* occ */);

    }
//    //TODO
//    @Test public void testTemporalImplicationDecompositionIsntEternalAttenuatedConf() {
//
//        /*
//        since there is a conflict in the occurrence of the implication and the event it relates,
//        the resulting confidence should be reduced by some amount since another more relevantly ranked
//        temporal implication could disagree and that should be preferred.
//         */
//
//        test()
//                
//                .inputAt(0, "(a). :|:")
//                .inputAt(10, "((a) ==>+1 (b)). :|:")
//                .mustNotOutput(cycles, "(b)", BELIEF, ETERNAL)
//                .mustBelieve(cycles, "(b)", 1f, 0.81f, 1 /* occ */);
//
//    }

    @Test
    public void testEternalImplicationDecompositionIsntEternal() {

        test

                .inputAt(0, "(a). :|:")
                .inputAt(0, "((a) ==>+1 (b)).") //ETERNAL impl
                .mustNotOutput(cycles, "(b)", BELIEF, ETERNAL)
                .mustBelieve(cycles, "(b)", 1f, 0.81f, 1 /* occ */);
    }

    @Test
    public void testEternalImplicationDecompositionWithConj() {

        test

                .inputAt(1, "(a &&+1 b). :|:")
                .inputAt(1, "((a &&+1 b) ==>+4 c). :|:")
                .mustBelieve(cycles, "c", 1f, 0.81f, 6 /* occ */)
                .mustNotOutput(cycles, "c", BELIEF, ETERNAL)
        //.mustNotOutput(cycles, "c", BELIEF, 6)
        ;
    }

    @Test
    public void testImplicationDecompositionContradictionFairness() {

        test
                .inputAt(0, "(b). :|:")
                .inputAt(0, "((a) ==>+1 (b)). :|:")
                .mustBelieve(cycles, "(a)", 1f, 0.45f, -1)
                .mustNotOutput(cycles, "(a)", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "(a)", BELIEF, 0)
        ;
        //.mustBelieve(cycles, "(a)", 1f, 0.45f, 0 /* occ */);
        //        /** because the two temporal events create a contradiction when evaluating the
        //         * derivation's result time, this tests decomposition's
        //         * ability to randomly choose (by confidence weighting) the result
        //         * determined by either the task or the belief.
        //         */
    }

//    @Test
//    public void temporalOrder() {
//
//        test
//
//                .input("(<m --> M> ==>+5 <p --> P>).")
//                .inputAt(10, "(<s --> S> <=>+0 <m --> M>). %0.9;0.9%")
//                .mustBelieve(cycles, "(<s --> S> ==>+5 <p --> P>)", 0.90f, 0.73f);
//    }

    @Test
    public void testTemporalConjunctionWithDepVarIntroduction() {
        /* WRONG:
        $1.0;.05;.10$ ((#1-->a) &&-3 (#1-->d)). 7-5 %1.0;.40% {7-5: 1;2;3} (((%1-->%2),(%1-->%3),neq(%2,%3),time(dtIfEvent)),((($4-->%2)==>($4-->%3)),((Induction-->Belief)),(($4-->%3)==>($4-->%2)),((Abduction-->Belief)),(($4-->%2)<=>($4-->%3)),((Comparison-->Belief)),((#5-->%2)&&(#5-->%3)),((Intersection-->Belief))))
            $.50;.50;.95$ (c-->d). 5+0 %1.0;.90% {5+0: 3} Input
            $1.0;.13;.24$ (c-->a). 3-1 %1.0;.45% {3-1: 1;2} (((%1-->%2),(%3-->%1),neq(%2,%3)),((%2-->%3),((Exemplification-->Belief),(Weak-->Desire),(AllowBackward-->Derive))))
        */

        test

                .inputAt(2, "a:x. :|: %1.0;0.45%")
                .inputAt(5, "b:x. :|: %1.0;0.90%")
                .mustBelieve(cycles, "(a:#1 &&+3 b:#1)", 1f, 0.40f, 2)
                .mustNotOutput(cycles, "(a:#1 &&-3 b:#1)", BELIEF, 0f, 1, 0f, 1, 2);

    }

    @Test
    public void testProjectedQuestion() {
        /*
        Since the question asks about a future time, the belief should
        be projected to it, unlike this:

        $.16;.09;.36$ (p4). 2+20 %0.0;.90% {2+20: a;j} ((%1,(--,%1),task("?")),(%1,((BeliefNegation-->Belief),(Judgment-->Punctuation))))
            $0.0;.50;.50$ (p4)? 0+22 {0+22: a} FIFO Forgot
            $.26;.39;.95$ (--,(p4)). 1+0 %1.0;.90% {1+0: j} Input
        */


        test
                .inputAt(0, "(--, (x)). :|:")
                .inputAt(4, "(x)? :|:")
                .mustBelieve(cycles, "(x)", 0f, 0.58f /* some smaller conf since it is a prediction */, 4);
    }

    @Test
    public void testConjInductionEternalTemporal() {
        test
                .input("a:x.")
                .input("a:y. :|:")
                .mustBelieve(cycles, "(a:x && a:y)", 1f, 0.81f, 0)
                .mustNotOutput(cycles, "(a:x &| a:y)", BELIEF, 0f, 1, 0f, 1, (t) -> true);

    }

    @Disabled
    @Test
    public void testImplInductionEternalTemporal() {
        test
                .log()
                .input("a:x.")
                .input("a:y. :|:")
                .mustBelieve(cycles, "(a:x ==> a:y)", 1f, 0.45f, 0)
                .mustNotOutput(cycles, "(a:x =|> a:y)", BELIEF, 0f, 1, 0f, 1, (t) -> true);
    }

    @Test
    public void testComparison1_Eternal() {
        /* (P ==> M), (S ==> M), neq(S,P) |- (S <=> P), (Belief:Comparison, Derive:AllowBackward)
           (M ==> P), (M ==> S), neq(S,P) |- (S <=> P), (Belief:Comparison, Derive:AllowBackward) */

        test
                .input("(p ==>+1 m).")
                .input("(s ==>+4 m).")
                .mustBelieve(cycles, "(s ==>+3 p)", 1f, 0.45f);
    }


    @Test
    public void testComparison2() {

        test
                .input("(m ==>+1 p).")
                .input("(m ==>+4 s).")
                .mustBelieve(cycles, "(p ==>+3 s).", 1f, 0.45f);
    }

//    @Test public void testConditionalAbductionByDepVar() {
//        //TODO
//        //conditional abduction by dependent variable
//        //((X --> R) ==> Z), ((&&,(#Y --> B),(#Y --> R),A..*) ==> Z), time(dtTask) |- (X --> B), (Belief:Abduction)
//    }

//    @Test
//    public void testDTTaskEndPartial() {
//        test
//            .log()
//            .input("((d-->c) ==>-3 (a-->b)). :|:")
//            .mustNotOutput(cycles, "<d-->c>", BELIEF, 2, 5);
//    }

    @Test
    public void testDTTaskEnd() {
        /*
        $.06;.01;.23$ (c-->b). 220-221 %1.0;.40% {220-221: 2;3} (((%1==>%2),%3,time(dtTaskEnd)),(substituteIfUnifies(%2,"$",%1,%3),((StructuralDeduction-->Belief),(Induction-->Desire),(ForAllSame-->Order))))
            $1.0;.07;.10$ ((d-->c) ==>-3 (c-->b)). 6-4 %1.0;.45% {6-4: 2;3} ((%1,%2,time(dtAfterReverse),neq(%1,%2),notImplicationOrEquivalence(%1),notImplicationOrEquivalence(%2)),((%2==>%1),((Abduction-->Belief))))
                $.50;.50;.95$ (c-->b). 2+0 %1.0;.90% {2+0: 2} Input
                $.50;.50;.95$ (d-->c). 5+0 %1.0;.90% {5+0: 3} Input*/

        test

                .inputAt(2, "(a-->b). :|:")
                .inputAt(5, "(d-->c). :|:")
                .mustBelieve(cycles, "((d-->c) ==>-3 (a-->b))", 1f, 0.45f, 5)
                .mustNotOutput(cycles, "<a-->b>", BELIEF, -1)
                .mustNotOutput(cycles, "<a-->b>", BELIEF, 5)
                .mustNotOutput(cycles, "<d-->c>", BELIEF, 2)
        ;
    }

    @Test
    public void testDecomposeConjunctionTemporal() {

        test
                .input("((x) &&+0 (y)). :|:")
                .mustBelieve(cycles, "(x)", 1f, 0.81f, 0)
                .mustBelieve(cycles, "(y)", 1f, 0.81f, 0);
    }

    @Test
    public void testDecomposeConjunctionEmbedded() {

        test
                .input("(((x) &&+1 (y)) &&+1 (z)). :|:")
                .mustBelieve(cycles * 2, "((x) &&+1 (y))", 1f, 0.81f, 0)
                .mustBelieve(cycles * 2, "((y) &&+1 (z))", 1f, 0.81f, 1)
                .mustBelieve(cycles * 2, "((x) &&+2 (z))", 1f, 0.81f, 0);
    }

    @Test
    public void testDecomposeConjunctionEmbedded2() {

        test
                .input("((z) &&+1 ((x) &&+1 (y))). :|:")
                .mustBelieve(cycles, "((x) &&+1 (y))", 1f, 0.81f, 1)
                .mustBelieve(cycles, "((z) &&+2 (y))", 1f, 0.81f, 0)
                .mustBelieve(cycles, "((z) &&+1 (x))", 1f, 0.81f, 0);
    }

    @Test
    public void testDecomposeConjunctionEmbeddedInnerCommute() {

        test
                .input("((&|,a,b,c) &&+1 z). :|:")
                .mustBelieve(cycles, "(a &&+1 z)", 1f, 0.73f, 0)
                .mustBelieve(cycles, "(b &&+1 z)", 1f, 0.73f, 0)
                .mustBelieve(cycles, "(c &&+1 z)", 1f, 0.73f, 0);
    }


    @Test
    public void testWTFDontDecomposeConjunctionDTERNAL() {

        test
                .input("((x)&&(y)). :|:")
                .mustBelieve(cycles, "(x)", 1f, 0.81f, 0)
                .mustBelieve(cycles, "(y)", 1f, 0.81f, 0);
        //.mustNotOutput(cycles,"(x)",BELIEF,ETERNAL, 0)
        //.mustNotOutput(cycles,"(y)",BELIEF,ETERNAL, 0);
    }
//    @Test public void testWTFDontDecomposeConjunction1() {
//        test()
//                .input("((x) &&+0 (y)).")
//                .mustNotOutput(cycles,"(x)",BELIEF,0,ETERNAL)
//                .mustNotOutput(cycles,"(y)",BELIEF,0,ETERNAL);
//    }

    @Disabled
    @Test
    public void testDecomposeConjunctionQuestion() {

        test

                .input("((x) &&+5 (y))? :|:")
                .mustOutput(0, cycles, "(x)", QUESTION, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 0)
                .mustOutput(0, cycles, "(y)", QUESTION, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 5)
        ;
    }

    @Disabled
    @Test
    public void testDecomposeConjunctionQuest() {

        test
                //
                .input("((x) &&+5 (y))@ :|:")
                .mustOutput(0, cycles, "(x)", QUEST, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 0)
                .mustOutput(0, cycles, "(y)", QUEST, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 5)
        ;
    }

    @Test
    public void testWTFDontDecomposeConjunction() {
        //$.07;.23;.24$ ((I-->happy) &&+0 (I-->neutral)). 3-2 %.06;.81%
        //$.50;.50;.90$ (I-->sad). 1+0 %0.0;.90%
        //dont derive: (I-->sad). %.94;.04%  via rule: <(&&,(--,%X),%A..+), X, time(decomposeTask) |- (--,%X), (Belief:StructuralDeduction)>:Negated

        test

                .inputAt(0, "((I-->happy) &&+0 (I-->neutral)). :|: %0.06;0.90%")
                .inputAt(0, "(I-->sad). :|: %0.0;0.90%")

                //must be true, not false:
//                .mustNotOutput(cycles, "((--,(I-->sad)) <=>+0 ((I-->happy) &&+0 (I-->neutral)))", BELIEF, 0.5f, 1f, 0, 1f, 0)
//                .mustNotOutput(cycles, "((--,(I-->sad)) <=>+0 ((I-->happy) &&+0 (I-->neutral)))", BELIEF, 0.5f, 1f, 0, 1f, ETERNAL)
                //<A, B, task("."), time(dtAfter), neq(A,B), notImplicationOrEquivalence(A), notImplicationOrEquivalence(B) |- (A <=> B), (Belief:Comparison)>

                .mustNotOutput(cycles, "(I-->sad)", BELIEF, 0.5f, 1f, 0.1f, 1f, 0)
                .mustNotOutput(cycles, "(I-->sad)", BELIEF, 0.5f, 1f, 0.1f, 1f, ETERNAL)
        // '<(&&,X,%A..+), X, time(decomposeTask) |- (&&,%A..+), (Belief:StructuralDeduction)>:Negated'}


        ;
    }

//    @Test
//    public void testWTFDontDecomposeConjunction2() {
//        /*((--,(I-->sad)) <=>+0 ((I-->happy) &&+0 (I-->neutral))). 1-1 %.06;.45%
//                ((I-->happy) &&+0 (I-->neutral)). 0+0 %.06;.90% {0+0: 1} Narsese
//                NOT: (I-->sad). 5-5 %.94;.02%*/
//
//        test
//
//                .inputAt(0, "((--,(I-->sad)) <=>+0 (x)). :|: %0.06;0.90%")
//                .inputAt(0, "(x). :|: %0.06;0.90%")
//
//                //must be true, not false:
//                .mustNotOutput(cycles, "(I-->sad)", BELIEF, 0.5f, 1f, 0.1f, 1f, 0);
//
//    }
//   @Test
//    public void testComparison1_Temporal() {
//        /* (P ==> M), (S ==> M), neq(S,P) |- (S <=> P), (Belief:Comparison, Derive:AllowBackward)
//           (M ==> P), (M ==> S), neq(S,P) |- (S <=> P), (Belief:Comparison, Derive:AllowBackward) */
//
//        test
//
//                .input("(p ==>+1 m).")
//                .inputAt(5, "(s ==>+4 m). :|:")
//                .mustBelieve(cycles, "(s <=>+3 p)", 1f, 0.45f, 5)
//                .mustNotOutput(cycles, "(s <=>-3 p).", BELIEF, ETERNAL); //test correct dt polarity
//    }

    /**
     * conj subset decomposition
     */
    @Test
    public void testConjSubsetDecomposition() throws Narsese.NarseseException {

        test
                //.nar.input( $.task( (Compound)$.parallel( $("(x)"), $("(y)"), $("(z)")), BELIEF, 1f, 0.9f) )
                //
                .nar.believe($.parallel($("(x)"), $("(y)"), $("(z)")), 3, 1f, 0.9f);

        test
                .mustBelieve(cycles, "((x) &| (y))", 1f, 0.81f, 3)
                .mustBelieve(cycles, "((y) &| (z))", 1f, 0.81f, 3)
                .mustBelieve(cycles, "((x) &| (z))", 1f, 0.81f, 3)
                .mustNotOutput(cycles, "((x) && (z))", BELIEF, (t) -> t == 0 || t == 3 || t == ETERNAL) //the dternal (non-parallel) version of the term
                .mustNotOutput(cycles, "((x) &| (z))", BELIEF, (t) -> t == 0 || t == ETERNAL); //the correct form but at the wrong occurrence time

    }

    @Test
    public void testIntersectionTemporalSimultaneous() {

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
    public void testIntersectionTemporalFar() {

        test
                .dur(1)
                .inputAt(0, "(x --> a). :|:")
                .inputAt(3, "(y --> a). :|:")
                .mustNotOutput(cycles, "((x&y)-->a)", BELIEF,
                        t -> !((t >= 0) && (t <= 3)))
        ;
    }

    @Test
    public void testPrediction1() throws Narsese.NarseseException {


        int eventDT = 16;
        int cycles = 10;

        TestNAR t = test;
        //((FrameTime)t.nar.time).dur(eventDT);

        t.dur(eventDT / 2);

//        t.nar.onTask((tt)->{
//           if (tt.start() > t.nar.time()) {
//               System.out.println(tt.proof());
//           }
//        });

        int x = 0;
        for (int i = 0; i < cycles; i++) {

            if (i == cycles - 1) {
                $.task($("(y)"), QUESTION, null).time(0, x, x + 2 * eventDT).setPriThen(1f).apply(t.nar);
            }

            t
                    .inputAt(x, "(x). :|:")
                    .inputAt(x + eventDT, "(y). :|:");


            x += 2 * eventDT;
        }


        int xx = x;
        t.mustBelieve(x - 1, "(y)", 1f, 0.73f,
                (y) -> y >= xx && y <= (xx + eventDT));
    }

    @Test
    public void multiConditionSyllogismPrePre() {
        test
            .input("((open(door)&|hold(key))=|>enter(room)). :|:")
            .mustBelieve(cycles,"(open(door)=|>enter(room))", 1f, 0.73f, 0);
    }

    @Test
    public void multiConditionSyllogismPre() {
        //    Y, ((&&,X,A..+) ==> B), time(dtBeliefExact), notImpl(A..+) |- subIfUnifiesAny(((&&,A..+) ==>+- B),X,Y), (Belief:Deduction)

        test
                .input("hold(key). :|:")
                .input("((hold(#x) &| open(door)) =|> enter(room)). :|:")
                .mustBelieve(cycles*2, "(open(door) =|> enter(room))",
                        1.00f, 0.81f,
                        0)
                .mustBelieve(cycles*2, "(hold(key) =|> enter(room))",
                        1.00f, 0.73f,
                        0)
        ;
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " :|:"}) //both: ETERNAL and NOW
    public void multiConditionSyllogismPost(String implSuffix) {
        //    Y, ((&&,X,A..+) ==> B), time(dtBeliefExact), notImpl(A..+) |- subIfUnifiesAny(((&&,A..+) ==>+- B),X,Y), (Belief:Deduction)

        test
                .input("hold(key). :|:")
                .input("(goto(door) =|> (hold(#x) &| open(door)))." + implSuffix)
                .mustBelieve(cycles * 2, "(goto(door) =|> open(door))",
                        1f, 1.00f, 0.73f, 0.81f,
                        0)
                .mustBelieve(cycles * 2, "(goto(door) =|> hold(key))",
                        1f, 1.00f, 0.73f, 0.81f,
                        0);
    }

    @Test
    public void preconImplyConjPre() throws Narsese.NarseseException {
        //implication-based composition

        assertEquals("((x &&+2 y) ==>+3 z)",
                //$.$("((x ==>+2 y) ==>+3 z)")
                $.$("(x ==>+2 (y ==>+3 z))").toString()
        );

        test
                .input("(x ==>+2 a). :|:")
                .input("(y ==>+3 a). :|:")
                .mustBelieve(cycles, "((y &&+1 x) ==>+2 a)", 1.00f, 0.81f, 0) //correct conj sub-term DT
                .mustNotOutput(cycles, "((x &&+1 y) ==>+2 a)", BELIEF, (t) -> t == 0 || t == ETERNAL)
                .mustNotOutput(cycles, "((x && y) ==>+2 a)", BELIEF, (t) -> t == 0 || t == ETERNAL)
                .mustNotOutput(cycles, "((x && y) ==>+3 a)", BELIEF, (t) -> t == 0 || t == ETERNAL);
    }

    @Test
    public void preconImplyConjPost() {
        //implication-based composition

        test
                .input("(a ==>+2 x). :|:")
                .input("(a ==>+3 y). :|:")
                .mustBelieve(cycles, "(a ==>+2 (x &&+1 y))", 1.00f, 0.81f, 0) //correct conj sub-term DT
                .mustNotOutput(cycles, "(a ==>+2 (y &&+1 x))", BELIEF, (t) -> t == 0 || t == ETERNAL)
                .mustNotOutput(cycles, "(a ==>+2 (x && y))", BELIEF, (t) -> t == 0 || t == ETERNAL)
                .mustNotOutput(cycles, "(a ==>+3 (x && y))", BELIEF, (t) -> t == 0 || t == ETERNAL);
    }

    @Test
    public void preconImplyConjPost2() {
        //implication-based composition

        //  y -> +3 -> a --> +2 --> x
        test
                .input("(a ==>+2 x). :|:")
                .input("(a ==>-3 y). :|:")
                .mustBelieve(cycles, "(a ==>-3 (y &&+5 x))", 1.00f, 0.81f, 0) //correct conj sub-term DT
        ;
    }

//    @Test
//    public void testReverseImpl() {
//
//        test
//
//                .believe("((x) ==>+5 (y))")
//                .believe("((y) ==>-5 (x))")
//                .mustBelieve(cycles, "((x) <=>+5 (y))", 1f, 0.81f)
//                .mustNotOutput(cycles, "((y) <=>+5 (x))", BELIEF, ETERNAL);
//    }

    @Test
    public void testPreconditionCombine() {

        test
                .believe("((x) ==>+5 (z))")
                .believe("((y) ==>+5 (z))")
                .mustBelieve(cycles, "( ((x) &&+0 (y)) ==>+5 (z))", 1f, 0.81f);
    }

    @Test
    public void testPreconditionCombineVarying() { //may be equivalent to another case

        test
                .believe("(x ==>+5 z)")
                .believe("(y ==>+3 z)")
//                .mustBelieve(cycles, "( x <=>+2 y)", 1f, 0.45f)
                .mustBelieve(cycles, "( (x &&+2 y) ==>+3 z)", 1f, 0.81f)
                .mustNotOutput(cycles, "( y ==>+2 x )", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "( y ==>-2 z )", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "( (x &&+2 y) ==>+5 z)", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "( (x &&+2 y) ==>+1 z)", BELIEF, ETERNAL)
        ;
    }

    @Test
    public void testPreconditionCombineNeg() {

        test
                .believe("((x) ==>+5 (z))")
                .believe("(--(y) ==>+5 (z))")
                .mustBelieve(cycles, "( ((x) &| --(y)) ==>+5 (z))", 1f, 0.81f);
    }

    @Test
    public void testPropositionalDecompositionPositive() { //may be equivalent to another case

        test
                .believe("(s)")
                .believe("((s) && (a))")
                .mustBelieve(cycles, "(a)", 1f, 0.81f);
    }


//    @Test public void testBackwardImplicationGeneration() {
//        test()
//                .log()
//                .inputAt(0, "(a). :|:")
//                .inputAt(10, "(b). :|:")
//                .inputAt(20, "(c). :|:")
//                .inputAt(30, "(d). :|:")
//                .mustBelieve(cycles, "( ((TODO) &&+0 --(y)) ==>+5 (z))", 1f, 0.81f);
//
//
//    }

//    @Test public void testTruthDecayOverTime0() {
//        testTruthDecayOverTime(0, 0.81f, 0.005f);
//    }
//    @Test public void testTruthDecayOverTime1() {
//        testTruthDecayOverTime(9, 0.78f, 0.005f);
//    }
//
//    public void testTruthDecayOverTime(int dist, float conf, float toler) {
//        test()
//            
//            .inputAt(1, "(a-->b). :|:")
//            .inputAt(1+dist, "(b-->c). :|:")
//            .mustOutput(0, cycles, "(a-->c)", BELIEF, 1f, 1f, conf-toler, conf+toler, 1)
//            .mustOutput(0, cycles, "(a-->c)", BELIEF, 1f, 1f, conf-toler, conf+toler, 1+dist)
//        ;
//    }

    @Test
    public void testDTTMinB() {
        // x +20 e
        // b +10 x
        //   -> b +30 e

        //  b ==>+10 c ==>+20 e
        test

                .believe("(b ==>+10 c)")
                .believe("(e ==>-20 c)")
                .mustBelieve(cycles, "(e ==>-30 b)", 1f, 0.45f)
                .mustBelieve(cycles, "(b ==>+30 e)", 1f, 0.45f)
//                .mustBelieve(cycles, "(b <=>+30 e)", 1f, 0.45f)
                .mustNotOutput(cycles, "(b ==>+20 e)", BELIEF, ETERNAL);
//                .mustNotOutput(cycles, "(b <=>-30 e)", BELIEF, ETERNAL);
    }

    @Test
    public void testInductionInterval() {
        /*
        $.02;.69$ (((a-->b) &&+4 (c==>#1)) &&+9 (#1-->e)). 1⋈14 %1.0;.73% {1⋈14: 3;5;8} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(dtAfterOrEternal),neqAndCom(%1,%2)),(varIntro((%1 &&+- %2)),((Intersection-->Belief))))
            $.50;.90$ (d-->e). 10 %1.0;.90% {10: 8} Scheduled
            $.11;.81$ ((a-->b) &&+4 (c==>d)). 1⋈5 %1.0;.81% {1⋈5: 3;5} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(dtAfter)),((%1 &&+- %2),((Intersection-->Belief))))
        */

        test
                .inputAt(1, "((a-->b) &&+4 (c-->d)). :|:")
                .inputAt(10, "(d-->e). :|:")
                .mustBelieve(cycles, "(((a-->b) &&+4 (c-->#1)) &&+5 (#1-->e))", 1f, 0.81f, 1)
                .mustNotOutput(cycles, "(((a-->b) &&+4 (c-->#1)) &&+9 (#1-->e))", BELIEF, t -> t == ETERNAL || t == 1);

    }

    @Test
    public void testInductionInterval2() {
        /*
        $.02;.69$ ((a==>b) &&+4 ((b-->#1) &&+3 (#1-->d))). 1⋈8 %1.0;.73% {1⋈8: 1;6;7} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(dtAfter)),((%1 &&+- %2),((Intersection-->Belief))))
            $.48;.90$ (a==>b). 1 %1.0;.90% {1: 1}
            $.12;.81$ ((b-->#1) &&+3 (#1-->d)). 2⋈5 %1.0;.81% {2⋈5: 6;7} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(dtAfterOrEternal),neqAndCom(%1,%2)),(varIntro((%1 &&+- %2)),((Intersection-->Belief))))
        */

        test
                .log()
                .inputAt(1, "(a). :|:")
                .inputAt(2, "((b) &&+3 (d)). :|:")
                .mustBelieve(cycles, "(((a) &&+1 (b)) &&+3 (d))", 1f, 0.81f, 1)
        ;

    }

    @Test
    public void testInductionIntervalMerge1() {
        /*
        1: a
        3: b
        5: c
        */

        test
                .inputAt(1, "((a) &&+5 (c)). :|:")
                .inputAt(3, "(b). :|:")
                .mustBelieve(cycles * 2,
                        "(((a) &&+2 (b)) &&+3 (c))",
                        1f, 0.81f, 1)
                .mustNotOutput(cycles * 2,
                        "((b) &&+3 ((a) &&+5 (c)))", BELIEF, (t) -> t == 1 || t == ETERNAL);
    }

    @Test
    public void testInductionIntervalMerge2() throws Narsese.NarseseException {

        //1. test correct parse of the 3-ary parallel conj
        assertEquals("((a) &&+2 (&|,(b),(c),(d)))",
                $("((a) &&+2 (((b) &| (c)) &| (d)) )").toString());


        test
                .inputAt(1, "((a) &&+2 (c)). :|:")
                .inputAt(3, "((b) &| (d)). :|:")
                .mustBelieve(cycles,
                        //"((a) &&+2 (&|, (b), (c), (d)))", //HACK <- this should work
                        "((a) &&+2 (((b) &| (c)) &| (d)) )",
                        1f, 0.81f, 1);
    }

    @Test
    public void testInductionIntervalMerge3() {

        //a 1
        //c 4
        //e 8
        //b 4
        test
                .inputAt(1, "(((a) &&+3 (c)) &&+4 (e)). :|:")
                .inputAt(4, "(b). :|:")
                .mustBelieve(cycles,
                        "(((a) &&+3 ((b) &| (c))) &&+4 (e))",
                        1f, 0.81f, 1);
    }

    @Test
    public void testInductionIntervalMerge3Neg() throws Narsese.NarseseException {

        List m = $.newArrayList();
        Term d = $("--((a) &&+3 --(c))");
        assertEquals(0, d.dtRange());
        d.events(m::add);
        System.out.println(m);

        List l = $.newArrayList();
        $("(--((a) &&+3 --(c)) &&+4 (e))").events(l::add);
        System.out.println(l);
        System.out.println();

        test
                .inputAt(1, "(--((a) &&+3 --(c)) &&+4 (e)). :|:")
                .inputAt(4, "(b). :|:")
                .mustBelieve(cycles,
                        "(((--,((a) &&+3 (--,(c)))) &&+3 (b)) &&+1 (e))",
                        1f, 0.81f, 1)
                .mustNotOutput(cycles, "(((--,((a) &&+3 (--,(c)))) &&+3 (b)) &&+4 (e))", BELIEF, t -> t == ETERNAL || t == 1);

    }

    @Test
    public void testDecomposeTaskSubset() {
        /*
        $0.0;.23$ (((d&&((a-->b) &&+1 (b-->c))) ==>+8 e) &&+9 (d-->e)). 1⋈10 %1.0;.31% {1⋈10: 4;5;6;7;8} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(conjoin)),((%1 &&+- %2),((Intersection-->Belief))))
        $.03;.40$ ((d&&((a-->b) &&+1 (b-->c))) ==>+8 e). 1 %1.0;.42% {1: 4;5;6} ((%1,%2,belief(positive),task("."),time(raw),time(dtAfterReverse),notEqui(%1),notImpl(%2)),((%2 ==>+- %1),((Abduction-->Belief))))
            |- (d -->e) @ 10, not 19
        */

        test
                .input("(((a-->b) &&+1 (b-->c)) &&+9 (d-->e)). :|:")
                .input("((a-->b) &&+1 (b-->c)). :|:")
                .mustBelieve(cycles, "(d-->e)", 1f, 0.73f, 10 /* 10? */)
        ;
    }

//    @Test
//    public void testDecomposeTaskSubset2() {
//        /*
//        $0.0;.23$ (((d&&((a-->b) &&+1 (b-->c))) ==>+8 e) &&+9 (d-->e)). 1⋈10 %1.0;.31% {1⋈10: 4;5;6;7;8} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(conjoin)),((%1 &&+- %2),((Intersection-->Belief))))
//        $.03;.40$ ((d&&((a-->b) &&+1 (b-->c))) ==>+8 e). 1 %1.0;.42% {1: 4;5;6} ((%1,%2,belief(positive),task("."),time(raw),time(dtAfterReverse),notEqui(%1),notImpl(%2)),((%2 ==>+- %1),((Abduction-->Belief))))
//            |- (d -->e) @ 10, not 19
//        */
//
//        test
//                .inputAt(1, "(((((a-->b) &&+1 (b-->c))) ==>+8 e) &&+9 (d-->e)). :|:")
//                .inputAt(1, "((((a-->b) &&+1 (b-->c))) ==>+8 e). :|:")
//                .mustBelieve(cycles, "(d-->e)", 1f, 0.45f /*0.73f*/, 10 /* 10? */)
//                .mustNotOutput(cycles, "(d-->e)", BELIEF, (t) -> (t == ETERNAL || t == 1 || t == 19));
//    }

    @Test
    public void testForwardImplChainDTUnion() {
        /** wrong direction: this should have been dt = +20
         $.29;.69$ ((reshape(I,$1)&&($1-->[heated])) ==>-20 ($1-->[hardened])). %1.0;.73% {3: 3;4;5} ((((%2&&%1073742337..+)==>%3),(%4==>%2),time(dtUnion),neq(%3,%2),notImpl(%2),notEqui(%3)),(((%4&&%1073742337..+) ==>+- %3),((Deduction-->Belief))))
         $.50;.90$ ((reshape(I,$1) &&+0 ($1-->[pliable])) ==>+10 ($1-->[hardened])). %1.0;.90% {0: 5}
         $.41;.81$ (($1-->[heated]) ==>+10 ($1-->[pliable])). %1.0;.81% {1: 3;4} ((%1,(%2<=>%3),neqCom(%1,%3),neq(%1,%2),time(beliefDTSimultaneous)),(substitute(%1,%2,%3,strict),((Intersection-->Belief),(Strong-->Goal))))
         */

        test
                .input("((reshape(I,$1) &| ($1-->[pliable])) ==>+10 ($1-->[hardened])).")
                .input("(($1-->[heated]) ==>+10 ($1-->[pliable])).")

//            .mustBelieve(cycles, "((reshape(I,$1)&&($1-->[heated])) ==>+20 ($1-->[hardened]))", 1f, 0.81f, ETERNAL)
//            .mustNotOutput(cycles, "((reshape(I,$1)&&($1-->[heated])) ==>-20 ($1-->[hardened]))", BELIEF, ETERNAL, 10, 20, 0)

                .mustBelieve(cycles, "(($1-->[heated]) ==>+20 ($1-->[hardened]))", 1f, 0.73f, ETERNAL)
                .mustNotOutput(cycles, "(($1-->[heated]) ==>-20 ($1-->[hardened]))", BELIEF,
                        (t -> t == ETERNAL || t == 10 || t == 20 || t == 0));
    }

//    @Test public void testContrapositionWierdness() {
//        test
//                .log()
//                .inputAt(2, "(c ==>-1 a). :|:")
//                .inputAt(5, "(c==>d). :|:")
//                //.mustBelieve(cycles, "", 1f, 0.45f /*0.73f*/, 10 /* 10? */)
//                .mustNotOutput(cycles, "((--,b) ==>+4 ((--,a) &&+4 (--,d)))", BELIEF, -3, ETERNAL);
//
//    }

//    @Test public void inductNegativesConjunction() {
//        test()
//                .log()
//                .input("--(i).")
//                .input("--(j).")
//                .mustBelieve(cycles, "(--(i) && --(j))", 1f, 0.81f);
//
//    }
//    @Test public void dontUnifyNegated() {
//        /*
//        shouldn't happen:
//
//            (((--,(x-->j))&&(x-->i))==>(x-->(1,0))). %1.0;.90% {0: 3}
//            (x-->i). %1.0;.90% {0: 5}
//               |- (x-->(1,0)). %1.0;.73% {148: 3;5} ((%1,(%2==>%3),time(decomposeBeliefLate)),(subIfUnifiesAny(%3,%2,%1),((Deduction-->Belief),(Induction-->Goal))))
//        */
//        test()
//                .log()
//                .believe("(((--,(x-->j))&&(x-->i))<=>(x-->(1,0)))")
//                .believe("(x-->i)")
//            //balanced:
//                .mustBelieve(cycles, "(x-->(1,0))", 1f, 0.73f)
//                .mustBelieve(cycles, "(x-->(1,0))", 0f, 0.73f);
//
//    }

//    static {
//        Param.TRACE = true;
//    }

    @Test
    public void testDecomposeImplPred() {

        test
                .believe("( (a,#1) =|> ( ( (x,#1) &| y) &| z ) )", Tense.Present, 1f, 0.9f);

        //test.nar.run(1);
        //test.nar.concept($.$safe("( (a,#1) =|> ( ( (x,#1) &| y) &| z ) )")).print();

        test
                .mustBelieve(cycles, "( (a,#1) =|> (x,#1) )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( (a,#1) =|> y )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( (a,#1) =|> z )", 1f, 0.73f, 0)
        ;
    }

    @Test
    public void testDecomposeImplPredSimpler() {

        test
                .believe("( a =|> ( ( x &| y) &| z ) )", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "( a =|> x )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( a =|> y )", 1f, 0.73f, 0)
                .mustBelieve(cycles, "( a =|> z )", 1f, 0.73f, 0)
        ;
    }
    @Disabled
    @Test
    public void testImplInductionAndConjReduction() {
        /*
        test for 2 things:
            a) the inducted implication should not lose its temporal information in the result
            b) the conjunction with implication can be reduced to an implication of a conjunction precondition
        $.25 (inside(bob,office) &&+0 (inside(john,playground)==>inside(bob,kitchen))). 0 %1.0;.50% {6: 2;3;5} ((%1,%2,task(positive),belief(positive),task("."),time(raw),time(dtEvents)),((%1 &&+- %2),((Intersection-->Belief))))
          $.05 (inside(john,playground) ==>+0 inside(bob,kitchen)). 0 %1.0;.50% {1: 2;5} ((%1,%2,time(raw),belief(positive),task("."),time(dtEventsReverse),notImpl(%1),notImpl(%2)),((%2 ==>+- %1),((Abduction-->Belief))))
            $.50 inside(bob,kitchen). 0 %1.0;.90% {0: 5}
            $.50 inside(john,playground). 0 %1.0;.90% {0: 2}
          $.50 inside(bob,office). 0 %1.0;.90% {0: 3}

          instead the result should be:
            ((inside(bob,office) &&+0 inside(john,playground) ==>+0 inside(bob,kitchen))).
          */

    }

    @Test
    public void nal5_conditional_induction0Simple() {
        TestNAR tester = test;
        tester.believe("((x1 && a) ==>+2 c)");
        tester.believe("((y1 && a) ==>+1 c)");
        tester.mustBelieve(cycles, "(x1 ==>+1 y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==>-1 x1)", 1.00f, 0.45f);
    }

}
