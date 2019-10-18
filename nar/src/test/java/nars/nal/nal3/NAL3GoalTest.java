package nars.nal.nal3;

import nars.NARS;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import static nars.Op.GOAL;
import static nars.Op.QUEST;
import static nars.nal.nal3.NAL3Test.cycles;

class NAL3GoalTest {


    private static final String X = ("X");
    private static final String Xe = ("(X-->A)");
    private static final String Xi = ("(A-->X)");
    private static final String Y = ("Y");
    private static final String Ye = ("(Y-->A)");
    private static final String Yi = ("(A-->Y)");

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

    private static void testGoalDiff(boolean goalPolarity, boolean beliefPolarity, boolean diffIsGoal, boolean diffIsFwd, float f, float c, boolean subjOrPred) {

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
        String beliefTerm;
        String goalTerm;
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

        //System.out.println(goalTask + '\t' + beliefTask + "\t=>\t" + expectedTask);


        new TestNAR(NARS.tmp(3,3))
                .termVolMax(8)
                .input(goalTask)
                .input(beliefTask)
                .mustGoal(cycles, YY, f, c)
                .run();
    }

//    @Test void GoalDiffRaw1() {
//        new TestNAR(NARS.tmp(3))
//                .input("X!")
//                .input("(X ~ Y).")
//                .mustGoal(GoalDecompositionTest.cycles, "Y", 0, 0.81f)
//                .run(16);
//    }
//    @Test void GoalDiffRaw2() {
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
//    @Test void GoalDiffRaw3() {
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
//    @Test void GoalDiffRaw4() {
//        new TestNAR(NARS.tmp(3))
//                .input("--Y!")
//                .input("(X ~ Y).")
//                .mustGoal(GoalDecompositionTest.cycles, "X", 1, 0.81f)
//                .run(16);
//    }





    @Test
    void dontFormUselessIntersection() {

        new TestNAR(NARS.tmp(3)).termVolMax(10)
            .input("(x-->a)!")
            .input("(y-->a)!")
            //.mustGoal(cycles, "((x | y)-->a)", 1f, 0.81f)
            .mustNotOutput(cycles, "(x-->a)", GOAL, 0, 0.5f, 0, 0.89f) //inverted
            .mustNotOutput(cycles, "(y-->a)", GOAL, 0, 0.5f, 0, 0.89f) //inverted
            .mustNotOutput(cycles, "(((--,x)&y)-->a)", QUEST)
            .mustNotOutput(cycles, "(((--,y)&x)-->a)", QUEST)
            .mustNotOutput(cycles, "((--x & y)-->a)", GOAL)
            .mustNotOutput(cycles, "((x & --y)-->a)", GOAL)
            .run();

    }
//    @Test
//    void intersectionGoalDecomposition3() {
//        new TestNAR(NARS.tmp(3))
//                .input("((X&Y) --> Z)!")
//                .input("--(X --> Z).")
//                .mustGoal(GoalDecompositionTest.cycles, "(Y --> Z)", 1, 0.81f)
//                .run(16);
//    }

}
