package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import org.junit.jupiter.api.Test;

public class NAL1GoalTest extends NALTest {


    private final int cycles = 120;

    @Override protected NAR nar() {
        return NARS.tmp(1);
    }


    @Test
    void testBeliefDeductionReverse() {
        test
                .input("(b-->c).")
                .input("(a-->b).")
                .mustBelieve(cycles, "(a-->c)", 1f, 0.81f)
                .mustBelieve(cycles, "(c-->a)", 1f, 0.45f)
        ;
    }

    @Test
    void deductionPositiveGoalNegativeBelief() {
        test
                .input("(a-->b)!")
                .input("(b-->c). %0.1%")
                .mustGoal(cycles, "(a-->c)", 1f, 0.1f)
        ;
    }


    @Test
    void deductionNegativeGoalPositiveBelief() {
        test
                .log()
                .input("--(nars --> stupid)!")
                .input("(stupid --> dangerous).")
                .mustGoal(cycles, "(nars-->dangerous)", 0f, 0.81f)
        ;
    }
    @Test
    void deductionNegativeGoalPositiveBeliefSwap() {
        //(B --> C), (A --> B), neqRCom(A,C)    |- (A --> C), (Belief:DeductionX)
        test
                .log()
                .input("--(nars --> stupid)!")
                .input("(derivation --> nars).")
                .mustGoal(cycles, "(derivation-->stupid)", 0f, 0.81f)
        ;
    }

    @Test
    void abductionNegativeGoalPositiveBelief()  {
        test
                .goal("--(nars --> stupid)")
                .believe("(human --> stupid)")
                .mustGoal(cycles, "(nars --> human)", 0f, 0.4f)
                .mustGoal(cycles, "(human --> nars)", 0f, 0.42f);
    }

}
