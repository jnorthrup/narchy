package nars.nal.nal3;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class NAL3Extra extends NAL3Test {


    @Test
    void unionOfOppositesInt() {
        //Coincidentia oppositorum
        test
                .termVolMax(6)
                .believe("((  x&z)-->a)")
                .believe("((--x&y)-->a)")
                .mustBelieve(cycles, "((y&z)-->a)", 1f, 0.81f)
        ;
    }

    @Test
    void unionOfOppositesExt() {
        //Coincidentia oppositorum

        test
                .termVolMax(6)
                .believe("(a-->(  x|z))")
                .believe("(a-->(--x|y))")
                .mustBelieve(cycles, "(a-->(y|z))", 1f, 0.81f)
        ;
    }
}
