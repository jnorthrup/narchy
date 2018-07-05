package nars.nal.nal6;

import nars.test.NALTest;
import org.junit.jupiter.api.Test;

public class NAL6ToNAL3BridgeTest extends NALTest {


//        @BeforeEach
//        void setup() {
//            test.confTolerance(0.2f);
//        }

    //TODO temporal conjunction tests

    @Test public void test1() {
        test.input("((X-->A) && (Y-->A))!").mustGoal(4, "((X|Y)-->A)", 1.0f, 0.9f);
    }
    @Test public void test2() {
        test.input("((X-->A) || (Y-->A))!").mustGoal(4, "((X&Y)-->A)", 1.0f, 0.9f);
    }
    @Test public void test3() {
        test.input("((A-->X) && (A-->Y))!").mustGoal(4, "(A-->(X&Y))", 1.0f, 0.9f);
    }
    @Test public void test4() {
        test.input("((A-->X) || (A-->Y))!").mustGoal(4, "(A-->(X|Y))", 1.0f, 0.9f);
    }
    @Test public void test5() {
        test.input("((A-->X) - (A-->Y))!").mustGoal(4, "(A-->(X-Y))", 1.0f, 0.9f);
    }
    @Test public void test6() {
        test.input("((X-->A) - (Y-->A))!").mustGoal(4, "((X-Y)-->A)", 1.0f, 0.9f);
    }


}
