package nars.nal.nal8;

import nars.test.TestNAR;
import nars.util.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;

@Disabled
public class NAL8EquivalenceTest extends NALTest {

    static final int cycles = NAL8Test.cycles;

    @Test
    public void testPosGoalEquivalenceSpreading() {

        test
                .input("(R)!")
                .input("((G) <=> (R)).")
                .mustGoal(cycles, "(G)", 1.0f, 0.81f);
    }

    @Test
    public void testNegatedGoalEquivalenceSpreading() {

        test
                .input("--(R)!")
                .input("((G) <=> (R)).")
                .mustGoal(cycles, "(G)", 0.0f, 0.81f);
    }

    @Test
    public void testGoalEquivComponent() {

        test
                .input("(happy)!")
                .input("((happy) <=>+0 ((--,(x)) &| (--,(out)))).")
                .mustGoal(cycles, "((--,(x)) &| (--,(out)))", 1f, 0.81f);
    }

    @Test
    public void testGoalEquivComponentNeg() {

        test
                .input("(happy)!")
                .input("(--(happy) <=>+0 ((--,(x))&&(--,(out)))).")
                .mustGoal(cycles, "((--,(x))&&(--,(out)))", 0f, 0.81f);
    }

    @Test
    public void testPredictiveEquivalenceTemporalTemporal() {

        test
                .inputAt(0, "((out) <=>-3 (happy)). :|:")
                .inputAt(13, "(happy)! :|:")
                .mustGoal(cycles, "(out)", 1f, 0.81f, 16)
                .mustNotOutput(cycles, "(out)", GOAL, 3);
    }

       @Test
    public void goal_deduction_equi_pos_pospos() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto(z) <=>+5 x:y).");
        tester.mustGoal(cycles, "goto(z)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "goto(z)", GOAL, ETERNAL);
    }
    @Test
    public void goal_deduction_equi_neg_pospos() {

        TestNAR tester = test;
        tester.input("--x:y! :|:");
        tester.input("(goto(z) <=>+5 x:y).");
        tester.mustGoal(cycles, "goto(z)", 0.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "goto(z)", GOAL, 0.9f, 1f, 0f, 0.81f, -5);
    }

    @Test
    public void goal_deduction_equi_pos_posneg() {

        test
                .input("(R)! :|:")
                .input("((S) <=>+5 --(R)).") //internally, this reduces to --(S ==> R)
                .mustGoal(cycles, "(S)", 0.0f, 0.81f, 0);

    }

    @Test
    public void goal_deduction_equi_pos_posneg_var() {

        test

                .input("g(x)! :|:")
                .input("(f($1) <=>+5 --g($1)).") //internally, this reduces to --(S ==> R)
                .mustGoal(cycles, "f(x)", 0.0f, 0.81f, 0)
                .mustNotOutput(cycles, "goto({t003})", GOAL, 0);

    }

    @Test
    public void goal_deduction_equi_neg_posneg() {

        test
                .input("--(R)! :|:")
                .input("((S) <=>+5 --(R)).") //internally, this reduces to --(S <=> R)
                .mustGoal(cycles, "(S)", 1.0f, 0.81f, 0 /* shifted to present */)
                .mustNotOutput(cycles, "(S)", GOAL, 0f, 0.5f, 0f, 1f, 0)
                .mustNotOutput(cycles, "(S)", GOAL, 0, 0.5f, 0f, 1f, -5)
        ;
    }

    @Test
    public void goal_deduction_equi_subst() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto($1) <=>+5 $1:y).");
        tester.mustGoal(cycles, "goto(x)", 1.0f, 0.81f, 0);
    }
    @Test
    public void goalInferredFromEquivAndImplEternalAndPresent() {

        TestNAR tester = test;

        tester.input("(a:b<=>c:d)."); //ETERNAL
        tester.input("(c:d &&+0 e:f). :|:"); //PRESENT
        tester.input("e:f! :|:"); //PRESENT
        tester.mustGoal(cycles, "a:b", 1.0f, 0.73f, 0);
        tester.mustNotOutput(cycles, "a:b", GOAL, ETERNAL);
    }

    @Test
    public void conjunctionSubstitutionViaEquiv() {

        TestNAR tester = test;

        tester.input("(a:b<=>c:d)."); //ETERNAL
        tester.input("(c:d &| e:f). :|:"); //PRESENT
        tester.mustBelieve(cycles, "(a:b &| e:f)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "(a:b &| e:f)", BELIEF, ETERNAL);
    }

    @Test
    public void conjunctionGoalSubstitutionViaEquiv() {

        TestNAR tester = test;

        tester.input("(a:b<=>c:d)."); //ETERNAL
        tester.input("(c:d &&+0 e:f)! :|:"); //PRESENT
        tester.mustGoal(cycles, "(a:b &&+0 e:f)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "(a:b &&+0 e:f)", BELIEF, ETERNAL);
    }

    @Test
    public void conjunctionSubstitutionViaEquivSimultaneous() {

        TestNAR tester = test;

        tester.input("(a:b <=>+0 c:d)."); //ETERNAL or Zero, for now dont allow time relation
        tester.input("(c:d &&+0 e:f). :|:"); //PRESENT
        tester.mustBelieve(cycles, "(a:b &&+0 e:f)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "(a:b &&+0 e:f)", BELIEF, ETERNAL);
    }


    @Test
    public void conjunctionSubstitutionViaEquivTemporal() {

        TestNAR tester = test;

        tester.input("(a:b <=>+1 c:d)."); //ETERNAL or Zero, for now dont allow time relation
        tester.input("(x:y <=>+0 c:d)."); //ETERNAL or Zero, for now dont allow time relation
        tester.input("(c:d &&+0 e:f). :|:"); //PRESENT
        tester.mustBelieve(cycles, "(x:y &&+0 e:f)", 1.0f, 0.81f, 0);
        //tester.mustNotOutput(cycles, "(a:b &&+0 e:f)", BELIEF, 0, ETERNAL);
    }
  @Test
    public void equiSubstitutionViaEquivalence() {

        test
                .input("(a:b<->c:d).") //ETERNAL
                .input("(e:f <=>+1 c:d). :|:") //PRESENT
                .mustBelieve(cycles, "(e:f <=>+1 a:b)", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "(e:f <=>+1 a:b)", BELIEF, ETERNAL);
    }
    @Test
    public void testPredictiveEquivalenceTemporalEternal() {

        //Param.TRACE = true;
//        test.nar.onCycle(()->{
//            nar.exe.print(System.out);
//        });
        test
                //.log()
                .inputAt(0, "((out) <=>-3 (happy)). :|:")
                .inputAt(5, "(happy)!")
                //.mustDesire(cycles, "(out)", 1f, 0.04f, 17)
                .mustGoal(16, "(out)", 1f, 0.81f, 3)
        //.mustNotOutput(cycles, "(out)", GOAL, 13, 0)
        ;
    }

}
