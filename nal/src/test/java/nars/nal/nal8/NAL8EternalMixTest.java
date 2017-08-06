package nars.nal.nal8;

import nars.test.TestNAR;
import nars.time.Tense;
import nars.util.AbstractNALTest;
import org.junit.Ignore;
import org.junit.Test;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;

/**
 * NAL8 tests specifically involving one or more eternal input tasks
 */
public class NAL8EternalMixTest extends AbstractNALTest {

    final int cycles = 30;


    @Test
    public void subsent_1_even_simpler_simplerGoalEternal() {

        test

                .input("(open(t1) && [opened]:t1)!")
                .mustDesire(cycles, "open(t1)", 1.0f, 0.81f, ETERNAL)
                .mustNotOutput(cycles, "open(t1)", GOAL, 0)
        ;

    }

    @Test
    public void subsent_1() {

        TestNAR tester = test;


        //TODO decide correct parentheses ordering

        tester.input("[opened]:{t001}. :|:");
        tester.input("(((hold({t002}) &&+5 at({t001})) &&+5 open({t001})) &&+5 [opened]:{t001}).");

        // hold .. at .. open
        tester.mustBelieve(cycles, "((hold({t002}) &&+5 at({t001})) &&+5 open({t001}))",
                1.0f, 0.81f,
                -15, -5);


//        //the structurually inverted sequence
//        tester.mustBelieve(cycles,
//                "(hold({t002}) &&+5 (at({t001}) &&+5 (open({t001}) &&+5 [opened]:{t001})))",
//                1.0f, 0.90f
//                );


        //tester.inputAt(10, "(hold({t002}) &&+5 (at({t001}) &&+5 (open({t001}) &&+5 [opened]:{t001}))).");
////        tester.mustBelieve(cycles, "(hold({t002}) &&+5 (at({t001}) &&+5 open({t001})))",
////                1.0f, 0.45f,
////                -5);

//        tester.mustBelieve(cycles, "(hold({t002}) &&+5 (at({t001}) &&+5 open({t001})))",
//                1.0f, 0.45f,
//                -5);

    }

    @Test
    public void conditional_abduction_temporal_vs_eternal() { //maybe to nal7 lets see how we will split these in the future

        TestNAR tester = test;

        tester.input("at(SELF,{t003}). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)).");

        tester.mustBelieve(cycles, "goto({t003})", 1.0f, 0.45f, -5);

    }

    @Test
    public void ded_with_indep_var_temporal() {

        TestNAR tester = test;

        tester.input("goto({t003}). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)).");

        tester.mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5);

    }

    @Test
    public void ded_with_var_temporal() {

        test

                .input("goto({t003}). :|:")
                .inputAt(10, "(goto(#1) ==>+5 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5);
    }

    @Test
    public void ded_with_var_temporal2() {

        TestNAR tester = test;

        tester.input("goto({t003}). :|: ");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)). ");

        tester.mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5);

    }


    @Test
    public void goal_deduction_tensed_conseq() {

        TestNAR tester = test;

        tester.input("goto(x). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)).");

        tester.mustBelieve(cycles, "at(SELF,x)", 1.0f, 0.81f, 5);
    }


    @Test
    public void condition_goal_deductionWithVariableEliminationOpposite() {

        test
                //.log()
                .input("goto({t003}). :|:")
                .input("(goto(#1) &&+5 at(SELF,#1))!")
                .mustDesire(2 * cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5)
        ;
    }

    @Test
    public void goal_deduction_impl() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto(z) ==>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f, 0);
    }

    @Test
    public void goal_deduction_impl_after() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto(z) ==>-5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f, 0);
    }

    @Test
    public void goal_deduction_delayed_impl() {

        TestNAR tester = test;

        tester.input("x:y!");
        tester.inputAt(10, "(goto(z) ==>+5 x:y). :|:");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f, 10);
    }


    @Test
    public void goal_deduction_equi_pos_pospos() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto(z) <=>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 1.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "goto(z)", GOAL, ETERNAL);
    }


    @Test
    public void goal_deduction_equi_neg_pospos() {

        TestNAR tester = test;
        tester.input("--x:y! :|:");
        tester.input("(goto(z) <=>+5 x:y).");
        tester.mustDesire(cycles, "goto(z)", 0.0f, 0.81f, 0);
        tester.mustNotOutput(cycles, "goto(z)", GOAL, 0.9f, 1f, 0f, 0.81f, -5);
    }

    @Test
    public void goal_deduction_equi_pos_posneg() {

        test
                .input("(R)! :|:")
                .input("((S) <=>+5 --(R)).") //internally, this reduces to --(S ==> R)
                .mustDesire(cycles, "(S)", 0.0f, 0.81f, 0);

    }

    @Test
    public void goal_deduction_equi_pos_posneg_var() {

        test

                .input("g(x)! :|:")
                .input("(f($1) <=>+5 --g($1)).") //internally, this reduces to --(S ==> R)
                .mustDesire(cycles, "f(x)", 0.0f, 0.81f, 0)
                .mustNotOutput(cycles, "goto({t003})", GOAL, 0);

    }

    @Test
    public void goal_deduction_equi_neg_posneg() {

        test
                .log()
                .input("--(R)! :|:")
                .input("((S) <=>+5 --(R)).") //internally, this reduces to --(S <=> R)
                .mustDesire(cycles, "(S)", 1.0f, 0.81f, 0 /* shifted to present */)
                .mustNotOutput(cycles, "(S)", GOAL, 0f, 0.5f, 0f, 1f, 0)
                .mustNotOutput(cycles, "(S)", GOAL, 0, 0.5f, 0f, 1f, -5)
        ;
    }

    @Test
    public void goal_deduction_equi_subst() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto($1) <=>+5 $1:y).");
        tester.mustDesire(cycles, "goto(x)", 1.0f, 0.81f, 0);
    }

    @Test
    public void goal_deduction_tensed_conseq_noVar() {

        TestNAR tester = test;

        tester.inputAt(1, "goto(x). :|:");
        tester.inputAt(10, "(goto(x) ==>+5 at(SELF,x)).");

        tester.mustBelieve(cycles, "at(SELF,x)", 1.0f, 0.81f, 6);
    }

    @Test
    public void belief_deduction_by_condition() {

        TestNAR tester = test;

        tester.input("(open({t001}) ==>+5 [opened]:{t001}).");
        tester.inputAt(10, "open({t001}). :|:");

        tester.mustBelieve(cycles, "[opened]:{t001}", 1.0f, 0.81f, 15);

    }

    @Test
    public void condition_goal_deduction2() {

        test

                .input("a:b! :|:")
                .inputAt(10, "(( c:d &&+5 e:f ) ==>+0 a:b).")
                .mustDesire(cycles, "( c:d &&+5 e:f)", 1.0f, 0.81f, 0, 5)
                .mustNotOutput(cycles, "( c:d &&+5 e:f)", GOAL, ETERNAL)
        ;
    }

    @Test
    public void condition_goal_deduction_interval() {

        test

                .input("a:b! :|:")
                .input("(( c:d &&+5 e:f ) ==>+5 a:b).")
                .mustDesire(cycles, "( c:d &&+5 e:f)", 1.0f, 0.81f, 0, 5)
        ;
    }

    @Test
    public void condition_goal_deductionEternal() {

        test

                .input("a:b!")
                .inputAt(10, "(( c:d &&+5 e:f ) ==> a:b).")
                .mustDesire(cycles, "( c:d &&+5 e:f)", 1.0f, 0.81f, ETERNAL)
                .mustNotOutput(cycles, "( c:d &&+5 e:f)", GOAL, 0)
        ;
    }

    @Test
    public void further_detachment() {

        test
                .input("reachable(SELF,{t002}). :|:")
                .inputAt(10, "(reachable(SELF,{t002}) &&+5 pick({t002}))!")
                .mustDesire(cycles, "pick({t002})", 1.0f, 0.81f, 5);

    }

    @Test
    public void condition_goal_deduction_eternal_belief() {

        test
                .log()
                .input("reachable(SELF,{t002})! :|:")
                .inputAt(5, "((on($1,#2) &&+0 at(SELF,#2)) ==>+0 reachable(SELF,$1)).")
                .mustDesire(cycles, "(on({t002},#1) &&+0 at(SELF,#1))", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "(at(SELF,#1) &&+0 on({t002},#1))", GOAL, ETERNAL, 5);

    }

    @Test
    public void goal_ded_2() {

        TestNAR tester = test;

        tester.log();
        tester.inputAt(0, "at(SELF,{t001}). :|:");
        tester.inputAt(0, "(at(SELF,{t001}) &&+5 open({t001}))!");

        tester.mustDesire(cycles, "open({t001})", 1.0f, 0.81f, 5);

    }

    @Test
    public void condition_goal_deduction_3simplerReverse() {

        test
                //.log()
                .inputAt(1, "at:t003! :|:")
                .input("(at:$1 ==>+5 goto:$1).")

                .mustDesire(cycles, "goto:t003", 1.0f, 0.45f, 6)
                .mustNotOutput(cycles, "goto:t003", GOAL, 0f, 1f, 0.1f, 1f, 1L);

    }


    @Test
    public void temporal_deduction_1() {

        TestNAR tester = test;

        //tester.nar;
//        tester.input("pick({t002}). :\\:");
//        tester.inputAt(10, "(pick({t002}) ==>+5 hold({t002})). :\\:");
//
//        tester.mustBelieve(cycles, "hold({t002})", 1.0f, 0.81f, 0);

        tester.input("pick:t2. :|:");
        tester.inputAt(10, "(pick:t2 ==>+5 hold:t2).");
        tester.mustBelieve(cycles, "hold:t2", 1.0f, 0.81f, 5);

    }

    @Test
    public void further_detachment_2() {

        test
                .input("reachable(SELF,{t002}). :|:")
                .inputAt(3, "((reachable(SELF,{t002}) &&+5 pick({t002})) ==>+7 hold(SELF,{t002})).")
                .mustBelieve(cycles, "(pick({t002}) ==>+7 hold(SELF, {t002}))", 1.0f, 0.81f, 5)
        //.mustNotOutput(cycles, "(pick({t002}) ==>+7 hold(SELF, {t002}))", BELIEF, 0)
        ;

    }

    @Test
    public void goal_deduction_2() {

        TestNAR tester = test;

        tester.input("goto({t001}). :|: ");
        tester.inputAt(7, "(goto($1) ==>+2 at(SELF,$1)). ");

        tester.mustBelieve(cycles, "at(SELF,{t001})", 1.0f, 0.81f, 2);

    }
      @Test
    public void condition_goal_deduction_2() {

        test

                .input("on({t002},{t003}). :|:")
                .input("(on({t002},#1) &&+0 at(SELF,#1))!")
                .mustDesire(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 0);

        //tester.mustNotOutput(time, selfAtT3, GOAL, 0, 1f, 0, 1f, ETERNAL);
    }
        @Test
    public void condition_belief_deduction_2() {

        test
                .input("on({t002},{t003}). :|:")
                .inputAt(2, "(on({t002},#1) &&+0 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.43f, 0)
                .mustNotOutput(cycles, "at(SELF,{t003})", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }



    @Test
    public void condition_belief_deduction_2_neg() {

        test
                .input("(--,on({t002},{t003})). :|:")
                .inputAt(2, "((--,on({t002},#1)) &&+0 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.43f, 0)
                .mustNotOutput(cycles, "at(SELF,{t003})", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }

    @Test
    public void condition_belief_deduction_2_easier() {

        test
                .input("on(t002,t003). :|:")
                .inputAt(2, "(on(t002,#1) &&+0 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,t003)", 1.0f, 0.43f, 0)
                .mustNotOutput(cycles, "at(SELF,t003)", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }

    @Ignore
    @Test
    public void condition_belief_deduction_2_dternal() {


        test

                .input("on:(t002,t003). :|:")
                .inputAt(10, "(on:(t002,#1) && at(SELF,#1)).") //<-- DTERNAL
                .mustBelieve(cycles * 4, "at(SELF,t003)", 1.0f, 0.43f, 0)
        //TODO mustNotBelieve? ^ the DTERNAL conjunctoin maybe should not be decomposed
        ;
    }

        @Test
    public void temporal_goal_detachment_1() {

        test

                .input("(hold). :|:")
                .input("( (hold) &&+5 ((at) &&+5 (open)) )!")
                .mustDesire(cycles, "((at) &&+5 (open))", 1.0f, 0.81f, 5, 10)
                .mustNotOutput(cycles, "((at) &&+5 (open))", GOAL, ETERNAL)
        ;
    }

    @Test
    public void temporal_goal_detachment_2() {
        //this is the reverse case which should not be derived by decomposing the belief

        test
                .input("(hold)! :|:")
                .inputAt(2, "( (hold) &&+5 (eat) ).") //should not decomposed by the goal task
                .mustDesire(cycles, "(eat)", 1f, 0.81f, 5)
        ;
    }

    @Test
    public void temporal_goal_detachment_3_valid() {

        test
                .log()
                .input("(use)! :|:")
                .inputAt(2, "( (hold) &&+5 (use) ).") //should be decomposed by the goal task
                .mustDesire(cycles, "(hold)", 1f, 0.81f, 0)
                .mustNotOutput(cycles, "(use)", GOAL, ETERNAL) //not eternal, we have a temporal basis here
                .mustNotOutput(cycles, "(hold)", GOAL, ETERNAL)
        ;
    }

    @Test
    public void temporal_goal_detachment_3_valid_negate() {

        test
                .input("--(use)! :|:")
                .inputAt(1, "( (hold) &&+5 --(use) ).")
                .mustDesire(cycles, "(hold)", 1f, 0.81f, 0)
                .mustNotOutput(cycles, "(use)", GOAL, ETERNAL) //not eternal, we have a temporal basis here
        ;
    }

    @Test
    public void detaching_condition0() {

        TestNAR tester = test;

        tester.input("( ( hold:t2 &&+5 (att1 &&+5 open:t1)) ==>+5 opened:t1).");
        tester.inputAt(10, "hold:t2. :|:");

        tester.mustBelieve(cycles, "((att1 &&+5 open:t1) ==>+5 opened:t1)", 1.0f, 0.81f, 15);

    }

    @Test
    public void detaching_condition() {

        test
                .input("( ( hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001}))) ==>+5 [opened]:{t001}).")
                .inputAt(10, "hold(SELF,{t002}). :|:")
                .mustBelieve(cycles, "((at(SELF,{t001}) &&+5 open({t001})) ==>+5 [opened]:{t001})", 1.0f, 0.81f, 15);

    }

    @Test
    public void subgoal_1_abd() {

        TestNAR tester = test;

        tester.input("opened:{t001}. :|:");
        tester.input("((hold(SELF,{t002}) &&+5 ( at(SELF,{t001}) &&+5 open({t001}))) ==>+5 opened:{t001}).");

        tester.mustBelieve(cycles, "( hold(SELF,{t002}) &&+5 ( at(SELF,{t001}) &&+5 open({t001})))",
                1.0f, 0.45f,
                -15, -5);

    }

    @Test
    public void temporal_deduction_2() {

        TestNAR tester = test;

        tester.input("((hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001}))) ==>+5 [opened]:{t001}).");
        tester.inputAt(10, "hold(SELF,{t002}). :|: ");

        tester.mustBelieve(cycles, "((at(SELF,{t001}) &&+5 open({t001})) ==>+5 [opened]:{t001})", 1.0f, 0.81f, 15);

    }


    @Test
    public void goalInferredFromEquivAndImplEternalAndPresent() {

        TestNAR tester = test;

        tester.input("(a:b<=>c:d)."); //ETERNAL
        tester.input("(c:d &&+0 e:f). :|:"); //PRESENT
        tester.input("e:f! :|:"); //PRESENT
        tester.mustDesire(cycles, "a:b", 1.0f, 0.73f, 0);
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
        tester.mustDesire(cycles, "(a:b &&+0 e:f)", 1.0f, 0.81f, 0);
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
    public void implSubstitutionViaSimilarity() {

        test
                .input("(a:b<->c:d).") //ETERNAL
                .input("(c:d ==>+1 e:f). :|:") //PRESENT
                .mustBelieve(cycles, "(a:b ==>+1 e:f)", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "(a:b ==>+1 e:f)", BELIEF, ETERNAL);
    }

    @Test
    public void implSubstitutionViaSimilarityReverse() {

        test

                .input("(a:b<->c:d).") //ETERNAL
                .input("(e:f ==>+1 c:d). :|:") //PRESENT
                .mustBelieve(cycles, "(e:f ==>+1 a:b)", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "(e:f ==>+1 a:b)", BELIEF, ETERNAL);
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
    public void testDeiredConjDelayed() {

        test
                .believe("(x)", Tense.Present, 1f, 0.9f)
                .goal("((x) &&+3 (y))")
                .mustDesire(cycles, "(y)", 1f, 0.81f, 3)
                .mustNotOutput(cycles, "(y)", GOAL, ETERNAL);
    }

    @Test
    public void testDeiredConjDelayedNeg() {

        test
                .believe("(x)", Tense.Present, 0f, 0.9f)
                .goal("(--(x) &&+3 (y))")
                .mustDesire(cycles, "(y)", 1f, 0.81f, 3)
                .mustNotOutput(cycles, "(y)", GOAL, ETERNAL);
    }

    @Test
    public void testBelievedImplOfDesireDelayed() {

        test
                //t
                .goal("(x)", Tense.Present, 1f, 0.9f)
                .believe("((x)==>+3(y))")
                .mustDesire(cycles, "(y)", 1f, 0.45f, 3)
        //.mustDesire(cycles, "(y)", 1f, 0.66f, ETERNAL)
        ;
    }

    @Test
    public void testGoalConjunctionDecomposeSuffix() {

        test
                .goal("((x) &&+3 (y))", Tense.Eternal, 1f, 0.9f)
                .inputAt(4, "(x). :|:")
                .mustDesire(cycles, "(y)", 1f, 0.81f, (4 + 3))
                .mustNotOutput(cycles, "(y)", GOAL, 3)
        //.mustNotOutput(cycles, "(y)", GOAL, ETERNAL)
        ;
    }

        @Test
    public void testNegatedImplicationS() {

        test
                .goal("(R)")
                .input("((--,a:b) ==>+0 (R)). :|:")
                .mustDesire(cycles, "a:b", 0.0f, 0.81f, 0);
    }

    @Test
    public void testNegatedImplicationP() {

        test

                .input("(R)! :|:")
                .input("((S) ==>+0 --(R)).") //internally, this reduces to --(S ==> R)
                .mustDesire(cycles, "(S)", 0.0f, 0.81f, 0);
    }
        @Test
    public void testNegatedImplicationTerm2() {

        test
                .input("(R)! :|:")
                .input("((--,a:b) ==>+0 (R)).")
                .mustDesire(cycles, "a:b", 0.0f, 0.81f, 0);

    }

    @Test
    public void testNegatedImplicationTerm3() {

        test
                .input("(R). :|:")
                .input("((--,a:b) &&+0 (R))!")
                .mustDesire(cycles, "a:b", 0.0f, 0.81f, 0);
    }


    @Ignore
    @Test
    public void disjunctionBackwardsQuestionTemporal() {

        test
                .inputAt(0, "(||, (x), (y))?")
                .believe("(x)", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "(&&, (--,(x)), (--,(y)))", 0f, 0.81f, 0);
    }

    @Test
    public void testGoalImplComponentTemporal() {

        test
                .input("(happy)! :|:")
                .input("((--,(in)) ==>+1 ((happy) &&-1 (--,(out)))).")
                .mustDesire(cycles, "(in)", 0f, 0.73f, 0);
    }

    @Test
    public void testGoalImplComponentWithVar() {

        test
                .inputAt(0, "c(x)! :|:")
                .inputAt(1, "a(x). :|:")
                .input("((a($x) &&+4 b($x)) ==>-3 c($x)).")
                .mustDesire(cycles * 2, "b(x)", 1f, 0.73f, 3 /* early since c(x) is alrady active when this gets derived */);
    }
    @Test
    public void testPredictiveImplicationTemporalEternal() {

        test
                .inputAt(0, "((out) ==>-3 (happy)).")
                .inputAt(13, "(happy)! :|:")
                .mustDesire(cycles, "(out)", 1f, 0.81f, 16)
                .mustNotOutput(cycles, "(out)", GOAL, 3);
    }

    @Test
    public void testPredictiveImplicationEternalTemporal() {

        test
                .inputAt(0, "((out) ==>-3 (happy)). :|:")
                .inputAt(13, "(happy)!")
                .mustDesire(cycles, "(out)", 1f, 0.81f, 3)
                .mustNotOutput(cycles, "(out)", GOAL, 13);
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
                .mustDesire(16, "(out)", 1f, 0.81f, 3)
        //.mustNotOutput(cycles, "(out)", GOAL, 13, 0)
        ;
    }

    @Test
    public void deriveNegInhGoalTemporal() {

        test
                .input("b:a! :|:") //positive pair
                .input("c:b.")
                .input("--y:x!  :|:") //negative pair
                .input("z:y.")
                .mustDesire(cycles * 2, "c:a", 1f, 0.81f, 0)
                .mustDesire(cycles * 2, "z:x", 0f, 0.81f, 0);
    }

     @Test public void testStrongUnificationDeductionPN() {
        //((--,%Y)==>X),Z,task(".") |- subIfUnifiesAny(X,Y,Z), (Belief:DeductionPN)
        test
                .input("((--,Y) ==>+1 (X)).")
                .input("(--,Y). :|:")
                .mustBelieve(cycles, "(X)", 1f, 0.81f, 1)
                .mustNotOutput(cycles, "(X)", BELIEF, ETERNAL)
        ;
    }

    @Test public void testStrongUnificationAbductionPN() {
        //((--,%Y)==>X),Z,task(".") |- subIfUnifiesAny(X,Y,Z), (Belief:DeductionPN)
        test
                .input("((--,X) ==>+1 (Y)).")
                .input("(Y). :|:")
                .mustBelieve(cycles, "X", 0f, 0.45f, -1);
    }

}
