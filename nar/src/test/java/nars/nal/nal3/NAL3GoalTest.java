package nars.nal.nal3;

import nars.NAR;
import nars.NARS;
import nars.nal.nal8.DecomposeTest;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import static nars.nal.nal3.NAL3Test.cycles;

class NAL3GoalTest {


    private final static String X = ("X");
    private final static String Xe = ("(X-->A)");
    private final static String Xi = ("(A-->X)");
    private final static String Y = ("Y");
    private final static String Ye = ("(Y-->A)");
    private final static String Yi = ("(A-->Y)");

    @Test
    void testNoDifference() {
        for (boolean subjOrPred : new boolean[] { true, false }) {
            for (boolean beliefPos : new boolean[]{true, false}) {
                float f = beliefPos ? 1f : 0f;
                for (boolean fwd : new boolean[]{true, false}) {
                    if (fwd) f = 1-f;
                    testGoalDiff(false, beliefPos, true, fwd, f, 0.81f, subjOrPred);
                }
            }
        }
    }

    @Test
    void testDifference() {
        for (boolean subjOrPred : new boolean[] { true, false }) {
            testGoalDiff(true, true, true, true, 0, 0.81f, subjOrPred);
            testGoalDiff(true, false, true, false, 1, 0.81f, subjOrPred);
        }
        //TODO more cases
    }

    private void testGoalDiff(boolean goalPolarity, boolean beliefPolarity, boolean diffIsGoal, boolean diffIsFwd, float f, float c, boolean subjOrPred) {

        String goalTerm, beliefTerm;
        String first, second;
        if (diffIsFwd) {
            first = X;
            second = Y;
        } else {
            first = Y;
            second = X;
        }
        String diff = (subjOrPred) ?
                "((" + first + '~' + second + ")-->A)" :
                "(A-->(" + first + '-' + second + "))";
        String XX = subjOrPred ? Xe : Xi;
        String YY = subjOrPred ? Ye : Yi;
        if (diffIsGoal) {
            goalTerm = diff;
            beliefTerm = XX;
        } else {
            beliefTerm = diff;
            goalTerm = XX;
        }
        if (!goalPolarity)
            goalTerm = "(--," + goalTerm + ')';
        if (!beliefPolarity)
            beliefTerm = "(--," + beliefTerm + ')';

        String goalTask = goalTerm + '!';
        String beliefTask = beliefTerm + '.';

        String expectedTask = YY + '!';
        if (f < 0.5f) expectedTask = "(--," + expectedTask + ')';

        System.out.println(goalTask + '\t' + beliefTask + "\t=>\t" + expectedTask);


        new TestNAR(NARS.tmp(3,3))
                .termVolMax(8)
                .input(goalTask)
                .input(beliefTask)
                .mustGoal(cycles, YY, f, c)
                .run(0);

    }

//    @Test void testGoalDiffRaw1() {
//        new TestNAR(NARS.tmp(3))
//                .input("X!")
//                .input("(X ~ Y).")
//                .mustGoal(GoalDecompositionTest.cycles, "Y", 0, 0.81f)
//                .run(16);
//    }
//    @Test void testGoalDiffRaw2() {
//        new TestNAR(NARS.tmp(3))
//                .input("X!")
//                .input("--(X ~ Y).")
//                .mustGoal(GoalDecompositionTest.cycles, "Y", 1, 0.81f)
//                .run(16);
//        new TestNAR(NARS.tmp(3))
//                .input("--X!")
//                .input("--(X ~ Y).")
//                .mustGoal(GoalDecompositionTest.cycles, "Y", 0, 0.81f)
//                .run(16);
//    }
//
//    @Test void testGoalDiffRaw3() {
//
//        //belief version
//        new TestNAR(NARS.tmp(3))
//                .input("Y.")
//                .input("--(X ~ Y).")
//                .mustBelieve(GoalDecompositionTest.cycles, "X", 1, 0.81f)
//                .run(16);
//
//        //goal version
//        new TestNAR(NARS.tmp(3))
//                .input("Y!")
//                .input("--(X ~ Y).")
//                .mustGoal(GoalDecompositionTest.cycles, "X", 1, 0.81f)
//                .run(16);
//        new TestNAR(NARS.tmp(3))
//                .input("--Y!")
//                .input("--(X ~ Y).")
//                .mustGoal(GoalDecompositionTest.cycles, "X", 0, 0.81f)
//                .run(16);
//    }
//
//    @Test void testGoalDiffRaw4() {
//        new TestNAR(NARS.tmp(3))
//                .input("--Y!")
//                .input("(X ~ Y).")
//                .mustGoal(GoalDecompositionTest.cycles, "X", 1, 0.81f)
//                .run(16);
//    }



    @Test
    void intersectionGoalDecomposition() {

        new TestNAR(NARS.tmp(3))
                .termVolMax(8)
                .input("(Z-->(X&&Y))!")
//                .input("(X --> Z).")
                .mustGoal(DecomposeTest.cycles, "(Z-->Y)", 1, 0.81f) //via structural decomposition of intersection, at least
                .run(0);

    }
    @Test
    void intersectionGoalDecomposition2() {

        new TestNAR(NARS.tmp(3))
                .termVolMax(8)
                .input("((X|Y) --> Z)!")
//                .input("(X --> Z).")
                .mustGoal(DecomposeTest.cycles, "(Y --> Z)", 1, 0.81f) //via structural decomposition of union, at least
                .run(0);

    }

//    @Test
//    void intersectionGoalDecomposition3() {
//        new TestNAR(NARS.tmp(3))
//                .input("((X&Y) --> Z)!")
//                .input("--(X --> Z).")
//                .mustGoal(GoalDecompositionTest.cycles, "(Y --> Z)", 1, 0.81f)
//                .run(16);
//    }

    static class DecompositionTest extends NALTest {

        public static final int cycles = 1050;

        @Override
        protected NAR nar() {
            NAR n = NARS.tmp(3,3);
            n.termVolMax.set(12);
            return n;
        }

        @Test
        void testUnionSinglePremiseDecomposeGoal1Pos() {
            test
                    .input("((a||b)-->g)!")
                    .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                    .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
        }

        @Test
        void testIntersectionSinglePremiseDecomposeGoal1Pos() {
            test
                    .input("((a&&b)-->g)!")
                    .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                    .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
        }

        @Test
        void testIntersectionConditionalDecomposeGoalPos() {
            test
                    .input("((a&&b)-->g)!")
                    .input("(a-->g). %0.8%")
                    .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
        }
        @Test
        void testIntersectionConditionalDecomposeGoalPosNeg() {
            test
                    .input("((b && --a)-->g)!")
                    .input("--(a-->g).")
                    .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
        }
        @Test
        void testSubjUnionConditionalDecomposeGoalPosNeg() {
            test
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
                    .mustGoal(cycles, "(b-->g)", 0f, 0.81f)
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
}
