package nars.nal.nal8;

import nars.nal.nal7.NAL7Test;
import nars.util.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** tests NAL8 interactions with &,|,~,-, etc.. */
public class NAL8SetTest extends NALTest {

    public static final int cycles = 230;


    @BeforeEach
    public void setTolerance() {
        test.confTolerance(NAL7Test.CONF_TOLERANCE_FOR_PROJECTIONS);
        test.nar.time.dur(3);
    }

    @Test
    public void testIntersectGoal1Pos() {
        test
            .input("((a|b)-->g)!")
            //.input("(a-->g).") //shouldnt matter
            .mustGoal(cycles,"(a-->g)", 1f, 0.81f)
            .mustGoal(cycles,"(b-->g)", 1f, 0.81f);
    }
    @Test
    public void testIntersectGoal1Neg() {
        test
                .input("--((a|b)-->g)!")
                //.input("(a-->g).") //shouldnt matter
                .mustGoal(cycles,"(a-->g)", 0f, 0.81f)
                .mustGoal(cycles,"(b-->g)", 0f, 0.81f);
    }

    @Test
    public void testDiffGoal1Pos1st() {
        test
                .log()
                .input("((a~b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles,"(b-->g)", 0f, 0.81f);
    }
    @Test
    public void testDiffGoal1Pos2nd() {
        test
                .log()
                .input("((a~b)-->g)!")
                .input("--(b-->g).")
                .mustGoal(cycles,"(a-->g)", 1f, 0.81f);
    }
    @Test
    public void testDiffGoal1Neg() {
        test
                .log()
                .input("--((a~b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles,"(b-->g)", 1f, 0.81f);
    }

}