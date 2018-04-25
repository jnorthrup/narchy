package nars.nal.nal8;

import nars.nal.nal7.NAL7Test;
import nars.util.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** tests NAL8 interactions with &,|,~,-, etc.. */
public class NAL8SetTest extends NALTest {

    public static final int cycles = 1530;


    @BeforeEach
    public void setTolerance() {
        test.confTolerance(NAL7Test.CONF_TOLERANCE_FOR_PROJECTIONS);
        test.nar.time.dur(3);
    }

    @Test
    public void testIntersectGoal1Pos() {
        test
            .input("((a|b)-->g)!")
            //.input("(a-->g). %0.75;0.90%")
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
    public void testMutexNegConj() {
        test
                .input("(||, --(a&|b), a, b,(--a &| --b))!")
                .input("a.")
                .mustGoal(cycles,"--b", 1f, 0.81f);
    }

    @Disabled
    @Test
    public void testMutexDiffGoal1Neg() {
        test
                .input("--((a~b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles,"(b-->g)", 1f, 0.81f);
    }

    @Disabled
    @Test
    public void testIntersectGoal1Neg() {
        test
                .input("(--,((a|b)-->g))!")
                //.input("(a-->g).")
                .mustGoal(cycles,"(a-->g)", 0f, 0.81f)
                .mustGoal(cycles,"(b-->g)", 0f, 0.81f);
    }

    @Disabled
    @Test
    public void testDiffGoal1Pos1st() {
        test
                .input("((a~b)-->g)! %1.00;0.90%")
                .input("(a-->g).")
                .mustGoal(cycles,"(b-->g)", 0f, 0.81f);
    }

}