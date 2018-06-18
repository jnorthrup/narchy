package nars.nal.nal8;

import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;

@Disabled
class NAL8EquivalenceTest extends NALTest {

    private static final int cycles = NAL8Test.cycles;

    @Test
    void testPosGoalEquivalenceSpreading() {

        test
                .input("(R)!")
                .input("((G) <=> (R)).")
                .mustGoal(cycles, "(G)", 1.0f, 0.81f);
    }

    @Test
    void testNegatedGoalEquivalenceSpreading() {

        test
                .input("--(R)!")
                .input("((G) <=> (R)).")
                .mustGoal(cycles, "(G)", 0.0f, 0.81f);
    }

    @Test
    void testGoalEquivComponent() {

        test
                .input("(happy)!")
                .input("((happy) <=>+0 ((--,(x)) &| (--,(out)))).")
                .mustGoal(cycles, "((--,(x)) &| (--,(out)))", 1f, 0.81f);
    }

    @Test
    void testGoalEquivComponentNeg() {

        test
                .input("(happy)!")
                .input("(--(happy) <=>+0 ((--,(x))&&(--,(out)))).")
                .mustGoal(cycles, "((--,(x))&&(--,(out)))", 0f, 0.81f);
    }

    @Test
    void testPredictiveEquivalenceTemporalTemporal() {

        test
                .inputAt(0, "((out) <=>-3 (happy)). :|:")
                .inputAt(13, "(happy)! :|:")
                .mustGoal(cycles, "(out)", 1f, 0.81f, 16)
                .mustNotOutput(cycles, "(out)", GOAL, 3);
    }

       @Test
       void goal_deduction_equi_pos_pospos() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto(z) <=>+5 x:y).");
        tester.mustGoal(cycles, "goto(z)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "goto(z)", GOAL, ETERNAL);
    }
    @Test
    void goal_deduction_equi_neg_pospos() {

        TestNAR tester = test;
        tester.input("--x:y! :|:");
        tester.input("(goto(z) <=>+5 x:y).");
        tester.mustGoal(cycles, "goto(z)", 0.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "goto(z)", GOAL, 0.9f, 1f, 0f, 0.81f, -5);
    }

    @Test
    void goal_deduction_equi_pos_posneg() {

        test
                .input("(R)! :|:")
                .input("((S) <=>+5 --(R)).") 
                .mustGoal(cycles, "(S)", 0.0f, 0.81f, 0);

    }

    @Test
    void goal_deduction_equi_pos_posneg_var() {

        test

                .input("g(x)! :|:")
                .input("(f($1) <=>+5 --g($1)).") 
                .mustGoal(cycles, "f(x)", 0.0f, 0.81f, 0)
                .mustNotOutput(cycles, "goto({t003})", GOAL, 0);

    }

    @Test
    void goal_deduction_equi_neg_posneg() {

        test
                .input("--(R)! :|:")
                .input("((S) <=>+5 --(R)).") 
                .mustGoal(cycles, "(S)", 1.0f, 0.81f, 0 /* shifted to present */)
                .mustNotOutput(cycles, "(S)", GOAL, 0f, 0.5f, 0f, 1f, 0)
                .mustNotOutput(cycles, "(S)", GOAL, 0, 0.5f, 0f, 1f, -5)
        ;
    }

    @Test
    void goal_deduction_equi_subst() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto($1) <=>+5 $1:y).");
        tester.mustGoal(cycles, "goto(x)", 1.0f, 0.81f, 0);
    }
    @Test
    void goalInferredFromEquivAndImplEternalAndPresent() {

        TestNAR tester = test;

        tester.input("(a:b<=>c:d)."); 
        tester.input("(c:d &&+0 e:f). :|:"); 
        tester.input("e:f! :|:"); 
        tester.mustGoal(cycles, "a:b", 1.0f, 0.73f, 0);
        tester.mustNotOutput(cycles, "a:b", GOAL, ETERNAL);
    }

    @Test
    void conjunctionSubstitutionViaEquiv() {

        TestNAR tester = test;

        tester.input("(a:b<=>c:d)."); 
        tester.input("(c:d &| e:f). :|:"); 
        tester.mustBelieve(cycles, "(a:b &| e:f)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "(a:b &| e:f)", BELIEF, ETERNAL);
    }

    @Test
    void conjunctionGoalSubstitutionViaEquiv() {

        TestNAR tester = test;

        tester.input("(a:b<=>c:d)."); 
        tester.input("(c:d &&+0 e:f)! :|:"); 
        tester.mustGoal(cycles, "(a:b &&+0 e:f)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "(a:b &&+0 e:f)", BELIEF, ETERNAL);
    }

    @Test
    void conjunctionSubstitutionViaEquivSimultaneous() {

        TestNAR tester = test;

        tester.input("(a:b <=>+0 c:d)."); 
        tester.input("(c:d &&+0 e:f). :|:"); 
        tester.mustBelieve(cycles, "(a:b &&+0 e:f)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "(a:b &&+0 e:f)", BELIEF, ETERNAL);
    }


    @Test
    void conjunctionSubstitutionViaEquivTemporal() {

        TestNAR tester = test;

        tester.input("(a:b <=>+1 c:d)."); 
        tester.input("(x:y <=>+0 c:d)."); 
        tester.input("(c:d &&+0 e:f). :|:"); 
        tester.mustBelieve(cycles, "(x:y &&+0 e:f)", 1.0f, 0.81f, 0);
        
    }
  @Test
  void equiSubstitutionViaEquivalence() {

        test
                .input("(a:b<->c:d).") 
                .input("(e:f <=>+1 c:d). :|:") 
                .mustBelieve(cycles, "(e:f <=>+1 a:b)", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "(e:f <=>+1 a:b)", BELIEF, ETERNAL);
    }
    @Test
    void testPredictiveEquivalenceTemporalEternal() {

        



        test
                
                .inputAt(0, "((out) <=>-3 (happy)). :|:")
                .inputAt(5, "(happy)!")
                
                .mustGoal(16, "(out)", 1f, 0.81f, 3)
        
        ;
    }

}
