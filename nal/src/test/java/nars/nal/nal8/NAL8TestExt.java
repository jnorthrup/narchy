package nars.nal.nal8;

import nars.test.TestNAR;
import nars.util.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/**
 * Additional experimental tests,
 */
@Disabled
public class NAL8TestExt extends NALTest {

        final int cycles = 96; //150 worked for most of the initial NAL8 tests converted





//    @Test
//    public void subsent_1_simpler()  {
//        test()
//
//                //.log()
//
//                .input("hold:t2. :|:") //@ 0
//                //.inputAt(5, "at:t1. :|:")
//                .inputAt(10, "(hold:t2 &&+5 (at:t1 &&+5 (open(t1) &&+5 [opened]:t1))).")
//                //.inputAt(15, "[opened]:t1. :|:")
//
//                .mustBelieve(cycles*8, "open(t1)",
//                        //1.0f, 0.81f,
//                        //-5);
//                        1.0f, 0.34f,
//                        10);
//
//    }

    @Test public void subsent_1_even_simpler()  {
        int time = cycles * 4;

        test
                //.log()
                .input("at:t1. :|:") //@ 0
                .inputAt(10, "(at:t1 &&+5 (open(t1) &&+5 [opened]:t1)).")
                //.mustBelieve(time, "open(t1)", 1.0f, 0.73f, 5)
                .mustBelieve(time, "(open(t1) &&+5 [opened]:t1)", 1.0f, 0.81f, 5)
                .mustNotOutput(time, "open(t1)", BELIEF, 1f, 1f, 0.59f, 0.59f, 5) //detect cyclic decomposition
                .mustNotOutput(time, "open(t1)", BELIEF, 1f, 1f, 0.32f, 0.32f, 5) //detect cyclic decomposition
        ;
    }
    @Test public void subsent_1_even_simplerGoal()  {
        int time = cycles * 16;

        test
                //.log()
                .input("at:t1. :|:") //@ 0
                .inputAt(10, "(at:t1 &&+5 (open(t1) &&+5 [opened]:t1))!")
                .mustGoal(time, "(open(t1) &&+5 [opened]:t1)", 1.0f, 0.81f, 5)
                //.mustDesire(time, "open(t1)", 1.0f, 0.73f, 5)
        ;
    }

    @Test
    public void subsent_simultaneous()  {

        TestNAR tester = test;

        //TODO decide correct parentheses ordering

        //tester.nar.log();
        tester.input("[opened]:t1. :|:");
        tester.inputAt(10, "(hold:t2 &&+0 (at:t1 &&+0 (open(t1) &&+0 [opened]:t1))).");

        //TODO Narsese parser for this:
        //tester.mustBelieve(cycles, "( &&+0 ,(t1-->at),(t2-->hold),(t1-->[opened]),open(t1))",
        tester.mustBelieve(cycles, "( && ,(t1-->at),(t2-->hold),(t1-->[opened]),open(t1))",
                1.0f, 0.43f,
                0);

        tester.mustBelieve(cycles, "(&&, hold:t2, at:t1, open(t1)).",
                1.0f, 0.81f,
                ETERNAL);


    }


}
