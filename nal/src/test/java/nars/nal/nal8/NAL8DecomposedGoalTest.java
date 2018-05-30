package nars.nal.nal8;

import nars.NARS;
import nars.nal.nal7.NAL7Test;
import nars.test.TestNAR;
import nars.util.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** tests goals involving &,|,~,-, etc.. */
public class NAL8DecomposedGoalTest extends NALTest {

    public static final int cycles = 50;


    @BeforeEach
    public void setTolerance() {
        test.confTolerance(NAL7Test.CONF_TOLERANCE_FOR_PROJECTIONS);
        test.nar.time.dur(3);
    }

    @Test
    public void testIntersectGoal1Pos() {
        test
            .input("((a|b)-->g)!")
            
            .mustGoal(cycles,"(a-->g)", 1f, 0.81f)
            .mustGoal(cycles,"(b-->g)", 1f, 0.81f);
    }


    @Test
    public void testDiffGoal1SemiPos1st() {
        test
                .input("((a~b)-->g)! %0.50;0.90%")
                .input("(a-->g). %1.00;0.90%")
                .mustGoal(cycles,"(b-->g)", 0.5f, 0.81f);
    }

    @Test
    public void testMutexDiffGoal1Pos2nd() {
        test
                .input("((a~b)-->g)!")
                .input("--(b-->g).")
                .mustGoal(cycles,"(a-->g)", 1f, 0.81f);
    }

    @Test
    public void testDisj() {
        test
                .input("(||,a,b)!")
                .input("--a.")
                .mustGoal(cycles,"b", 1f, 0.81f);
    }

    @Test
    public void testDisjNeg() {
        test
                .input("(||,a,--b)!")
                .input("--a.")
                .mustGoal(cycles,"b", 0f, 0.81f);
    }

    @Test
    public void testAndConj() {
        test
                .input("(&&,a,b)!")
                .input("a.")
                .mustGoal(cycles,"b", 1f, 0.81f);
    }









    @Test
    public void testMutexDiffGoal1Neg() {
        test
                .input("--((a~b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles,"(b-->g)", 1f, 0.81f);
    }

    @Test
    public void testIntersectGoal1Neg() {
        test
                .input("(--,((a|b)-->g))!")
                
                .mustGoal(cycles,"(a-->g)", 0f, 0.81f)
                .mustGoal(cycles,"(b-->g)", 0f, 0.81f);
    }

    @Test
    public void testDiffGoal1Pos1st() {
        test
                .input("((a~b)-->g)! %1.00;0.90%")
                .input("(a-->g).")
                .mustGoal(cycles,"(b-->g)", 0f, 0.81f);
    }




    static class TestGoalDiff {

        final static String X = ("X");
        final static String Xe = ("(X-->A)");
        final static String Xi = ("(A-->X)");
        final static String Y = ("Y");
        final static String Ye = ("(Y-->A)");
        final static String Yi = ("(A-->Y)");

        @Test public void test1() {
            for (boolean beliefPos : new boolean[] {true, false} ) {
                float f = beliefPos ? 1f : 0f;
                for (boolean fwd : new boolean[]{true, false}) {
                    testGoalDiff(false, beliefPos, true, fwd, f, 0.81f);
                }
            }
            testGoalDiff(true, true, true, true, 0, 0.81f);
            testGoalDiff(true, false, true, false, 1, 0.81f);
        }

        void testGoalDiff(boolean goalPolarity, boolean beliefPolarity, boolean diffIsGoal, boolean diffIsFwd, float f, float c) {
            
            testGoalDiff(goalPolarity, beliefPolarity, diffIsGoal, diffIsFwd,
            true, f, c);




        }

        TestNAR testGoalDiff(boolean goalPolarity, boolean beliefPolarity, boolean diffIsGoal, boolean diffIsFwd, boolean diffIsEx, float f, float c) {
            String goalTerm, beliefTerm;
            String first, second;
            if (diffIsFwd) {
                first = X;
                second = Y;
            } else {
                first = Y;
                second = X;
            }
            String diff = (diffIsEx) ?
                    "((" + first + "~" + second + ")-->A)" :
                    "(A-->(" + first + "-" + second + "))";
            String XX = diffIsEx ? Xe : Xi;
            String YY = diffIsEx ? Ye : Yi;
            if (diffIsGoal) {
                goalTerm = diff;
                beliefTerm = XX;
            } else {
                beliefTerm = diff;
                goalTerm = XX;
            }
            if (!goalPolarity)
                goalTerm = "(--," + goalTerm + ")";
            if (!beliefPolarity)
                beliefTerm = "(--," + beliefTerm + ")";

            String goalTask = goalTerm + "!";
            String beliefTask = beliefTerm + ".";

            String expectedTask = YY + "!";
            if (f < 0.5f) expectedTask = "(--," + expectedTask + ")";

            System.out.println(goalTask + "\t" + beliefTask + "\t=>\t" + expectedTask);

            return new TestNAR(NARS.tmp(3))
                    .input(goalTask)
                    .input(beliefTask) 
                    .mustGoal(cycles, YY, f, c)
                    .run(16);
        }
    }
}