package nars.nal.nal6;

import nars.derive.Derivers;
import nars.derive.impl.BatchDeriver;
import nars.test.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class NAL6ToNAL3BridgeTest extends NALTest {
    public static final float CONF = 0.81f;
    final static int cycles = 256;


        @BeforeEach
        void setup() {
            test.confTolerance(0.2f);
        new BatchDeriver(Derivers.files(test.nar, "nal6.to.nal3.nal"));
        }

    @Test public void test1() { test.input("((X-->A) && (Y-->A)).")
            //.mustGoal(cycles, "((X|Y)-->A)", 1.0f, CONF); }
            .mustQuestion(cycles, "((X|Y)-->A)"); }
    @Test public void test2() {
        test.input("((X-->A) || (Y-->A)).").mustQuestion(cycles, "((X&Y)-->A)");
    }
    @Test public void test3() {
        test.input("((A-->X) && (A-->Y)).").mustQuestion(cycles, "(A-->(X&Y))");
    }
    @Test public void test4() {
        test.input("((A-->X) || (A-->Y)).")
                .mustQuestion(cycles, "(A-->(X|Y))");
    }
    @Disabled @Test public void test5() {
        test.input("((A-->X) ~ (A-->Y))!").mustGoal(cycles, "(A-->(X-Y))", 1.0f, CONF);
    }
    @Disabled
    @Test public void test6() {
        test.input("((X-->A) ~ (Y-->A))!").mustGoal(cycles, "((X~Y)-->A)", 1.0f, CONF);
    }

//    @Test public void testConjDiffernce() {
//        test.input("(x && a).").input("(x && b).")
//                .mustQuestion(cycles, "(a~b)")
//                .mustQuestion(cycles, "(b~a)")
//        ;
//    }

//    @Test public void testImplDiffernce() {
//        test.input("(a ==> x).").input("(b ==> x).").mustQuestion(cycles,"((a~b) ==>+- x)");
//    }
}
