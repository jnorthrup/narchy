package nars.nal.nal6;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.TestNAR;
import nars.time.Tense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

public class NAL6Test extends NALTest {

    private static final int cycles = 200;

    @BeforeEach
    void setup() {
        test.confTolerance(0.25f);
    }

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(6, 8);
        //NAR n = NARS.tmp(6);

        n.termVolMax.set(16);
        //n.freqResolution.setAt(0.1f);
        n.confMin.set(0.3f);
        return n;
    }

    @Test
    void variable_unification_revision() {
        test
                .mustBelieve(cycles, "(($1 --> bird) ==> ($1 --> flyer))", 0.79f, 0.92f)
                .believe("(($x --> bird) ==> ($x --> flyer))")
                .believe("(($y --> bird) ==> ($y --> flyer))", 0.00f, 0.70f)
        ;
    }

    @Test
    void variable_unification2() {

        test
                .termVolMax(8)
                .believe("<($x --> bird) ==> <$x --> animal>>")
                .believe("<<$y --> robin> ==> <$y --> bird>>")
                .mustBelieve(cycles, "<<$1 --> robin> ==> <$1 --> animal>>", 1.00f, 0.81f)
                .mustBelieve(cycles, "<<$1 --> animal> ==> <$1 --> robin>>", 1.00f, 0.45f);

    }


    @Test
    void variable_unification3() {
        TestNAR tester = test;


        tester.believe("<<$x --> swan> ==> ($x --> bird)>", 1.00f, 0.80f);
        tester.believe("<<$y --> swan> ==> <$y --> swimmer>>", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "<<$1 --> swan> ==> (&&,($1 --> bird),($1 --> swimmer))>", 0.80f, 0.72f);
        tester.mustBelieve(cycles, "<($1 --> swimmer) ==> ($1 --> bird)>", 1f, 0.37f);
        tester.mustBelieve(cycles, "<($1 --> bird) ==> ($1 --> swimmer)>", 0.80f, 0.42f);


    }


    @Test
    void variable_unification4() {

        TestNAR tester = test;
        tester.termVolMax(13);

        tester.believe("<<bird --> $x> ==> <robin --> $x>>");
        tester.believe("<<swimmer --> $y> ==> <robin --> $y>>", 0.70f, 0.90f);
        tester.mustBelieve(cycles, "(((bird --> $1) && (swimmer --> $1)) ==> (robin --> $1))", 0.7f /*1f? */, 0.81f);

        tester.mustBelieve(cycles, "<(bird --> $1) ==> (swimmer --> $1)>", 1f, 0.36f /*0.73F*/);
        tester.mustBelieve(cycles, "<(swimmer --> $1) ==> (bird --> $1)>", 0.7f, 0.45f);


    }


    @Test
    void variable_unification5() {

        TestNAR tester = test;
        tester.nar.termVolMax.set(14);
        tester.believe("<(&&,($x --> flyer),($x --> [chirping])) ==> ($x --> bird)>");
        tester.believe("<($y --> [withWings]) ==> ($y --> flyer)>");
        tester.mustBelieve(cycles, "((($1 --> [chirping]) && ($1 --> [withWings])) ==> ($1 --> bird))",
                1.00f,
                0.81f
        );

    }

    @Test
    void variable_unification5_neg() {


        TestNAR tester = test;
        tester.termVolMax(16);
        tester.believe("<(&&,($x --> flyer),($x --> [chirping])) ==> --($x --> nonBird)>");
        tester.believe("<($y --> [withWings]) ==> ($y --> flyer)>");
        tester.mustBelieve(cycles, "((($1 --> [chirping]) && ($1 --> [withWings])) ==> --($1 --> nonBird))",
                1.00f,
                0.81f
        );

    }

    @Test
    void variable_unification6() {


        TestNAR tester = test;
        tester.termVolMax(17)
            .believe("((&&,($x --> flyer),($x --> [chirping]), food($x, worms)) ==> ($x --> bird))")
            .believe("((&&,($y --> [chirping]),($y --> [withWings])) ==> ($y --> bird))")
            .mustBelieve(cycles, "((($1 --> flyer) && food($1,worms)) ==> ($1 --> [withWings]))", 1.00f,
                0.45f
                /*0.45f*/)
            .mustBelieve(cycles, "(($1 --> [withWings]) ==> (($1 --> flyer) && food($1,worms)))", 1.00f,
                0.45f
                /*0.45f*/);


        /*
        <patham9> 
        first result:
            (&&,($1 --> flyer),<($1,worms) --> food>) ==> ($1 --> [withWings])>
        it comes from the rule
            ((&&,C,A_1..n) ==> Z), ((&&,C,B_1..m) ==> Z) |- ((&&,A_1..n) ==> (&&,B_1..m)), (Truth:Induction)
        which basically says: if two different precondition conjunctions, with a common element lead to the same conclusion,
        it might be that these different preconditions in the specific conjunctions imply each other
        (each other because the premises can be swapped for this rule and it is still valid)

        second result:
            <($1 --> [withWings]) ==> (&&,($1 --> flyer),<($1,worms) --> food>)>
        by the same rule:
            ((&&,C,A_1..n) ==> Z), ((&&,C,B_1..m) ==> Z) |- ((&&,B_1..m) ==> (&&,A_1..n)), (Truth:Induction)
        where this time the diffierent preconditions of the second conjunction imply the different preconditions of the first
        no, no additionally info needed
        now I will show you what I think went wrong in your system:
        you got:
            ((&&,(($1,worms)-->food),($1-->flyer),($1-->[chirping]))==>(($1-->[withWings])&&($1-->[chirping]))).
        abduction in progress ^^
        your result is coming from the induction rule
            (P ==> M), (S ==> M), not_equal(S,P) |- (S ==> P), (Truth:Induction, Derive:AllowBackward)
        there are two possibilities, either restrict not_equal further to no_common_subter,
        or make the constructor of ==> make sure that the elements which occur in predicate and subject as well are removed
        its less fatal than in the inheritance composition, the derivation isnt wrong fundamentally, but if you can do so efficiently, let it avoid it
        additionally make sure that the two
            ((&&,C,A_1..n) ==> Z), ((&&,C,B_1..m) ==> Z) |- ((&&,A_1..n) ==> (&&,B_1..m)), (Truth:Induction)
        rules work, they are demanded by this reasoning about preconditions
        *hypothetical reasoning about preconditions to be exact
         */
    }


    @Test
    void variable_unification7() {

        TestNAR tester = test;
        tester.believe("((&&,($x --> flyer),(($x,worms) --> food)) ==> ($x --> bird))");
        tester.believe("(($y --> flyer) ==> ($y --> [withWings]))");
        tester.mustBelieve(cycles, "<(&&,($1 --> [withWings]),<($1,worms) --> food>) ==> ($1 --> bird)>", 1.00f, 0.45f);

    }

    @Test
    void variable_unification7_neg() {

        TestNAR tester = test;
        tester.believe("((&&,($x --> flyer),(($x,worms) --> food)) ==> ($x --> bird))");
        tester.believe("(($y --> flyer) ==> ($y --> [withWings]))");
        tester.mustBelieve(cycles, "<(&&,($1 --> [withWings]),<($1,worms) --> food>) ==> ($1 --> bird)>", 1.00f, 0.45f);

    }


    @Test
    void variable_elimination_impl_fwd_pos_pos() {

        test
                .believe("<($x --> bird) ==> <$x --> animal>>")
                .believe("<robin --> bird>")
                .mustBelieve(cycles, "<robin --> animal>", 1.00f, 0.81f);

    }

    @Test
    void variable_elimination_impl_fwd_pos_neg() {

        test
                .believe("(($x --> bird) ==> --($x --> --animal))")
                .believe("(robin --> bird)")
                .mustBelieve(cycles, "(robin --> --animal)", 0.00f, 0.81f);

    }

    @Test
    void variable_elimination_impl_fwd_neg_pos() {

        test
                .believe("(--($x --> --bird) ==> ($x --> animal))")
                .believe("--(robin --> --bird)")
                .mustBelieve(cycles, "(robin --> animal)", 1.00f, 0.81f);

    }

    @Test
    void variable_elimination_impl_rev() {

        test.believe("<($x --> bird) ==> <$x --> animal>>");
        test.believe("<tiger --> animal>");
        test.mustBelieve(cycles * 2, "<tiger --> bird>", 1.00f, 0.45f);

    }
    @Test
    void variable_elimination_impl_pred_conj() {

        test.termVolMax(13);
        test.believe("(accessibleFromMenu($1, $2) ==> (($1-->Entity) && ($2-->ComputerMenu)))");
        test.believe("(GraphicalComputerMenu-->ComputerMenu)");
        test.mustBelieve(cycles, "(accessibleFromMenu($1, GraphicalComputerMenu) ==> (($1-->Entity) && (GraphicalComputerMenu-->ComputerMenu)))", 1f, 0.81f);
    }

    @Test
    void variable_elimination_conj() {
        test
            .termVolMax(7)
            .confMin(0.4f)
            .believe("((#x --> bird) && (#x --> swimmer))")
            .believe("(swan --> bird)", 0.90f, 0.9f)
            .mustBelieve(cycles, "(swan --> swimmer)", 0.90f, 0.43f);
    }



    @Test
    void variable_elimination5() {


        test

                .believe("({Tweety} --> [withWings])")
                .believe("((($x --> [chirping]) && ($x --> [withWings])) ==> ($x --> bird))")
                .mustBelieve(cycles,
                        //"((({Tweety}-->[chirping])&&({Tweety}-->[withWings]))==>({Tweety}-->bird))",
                        "(({Tweety}-->[chirping])==>({Tweety}-->bird))",
                        1.00f,
                        0.81f
                );
    }

    @Test
    void variable_elimination6() {
        test
            .termVolMax(17)
            .confMin(0.6f)
            .believe("flyer:Tweety")
            .believe("((&&, flyer:$x, ($x-->[chirping]), food($x, worms)) ==> bird:$x)")
            .mustBelieve(cycles, "(((Tweety-->[chirping]) && food(Tweety,worms)) ==> bird:Tweety)",
                    1.0f,
                    0.73f);
    }

    @Test
    void variable_elimination5_neg() {



        test.confMin(0.6f);
        test.termVolMax(16);
        test.believe("({Tweety} --> [withWings])");
        test.believe("((($x --> [chirping]) && ($x --> [withWings])) ==> --($x --> nonBird))");
        test.mustBelieve(cycles,
                "((({Tweety}-->[chirping])&&({Tweety}-->[withWings]))==>({Tweety}-->nonBird))",
                //"(({Tweety}-->[chirping])==>({Tweety}-->nonBird))",
                0.00f,
                //0.81f
                0.81f
        );

    }

    @Test
    void variable_elimination6_easier() {



        test.confMin(0.6f);
        test.termVolMax(14);

        test.believe("((&&, flyer:$x, chirping:$x, eatsWorms:$x) ==> bird:$x)");
        test.believe("flyer:Tweety");
        test.mustBelieve(cycles * 3,
                "((chirping:Tweety && eatsWorms:Tweety) ==> bird:Tweety)",
                1.0f, 0.73f);

    }

    @Test
    void variable_elimination6simpler() {

        test.termVolMax(14);
        test.confMin(0.6f);
        test
                .believe("((&&, flyer:$x, chirping:$x, food:worms) ==> bird:$x)")
                .believe("flyer:Tweety")
                .mustBelieve(cycles, "((chirping:Tweety && food:worms) ==> bird:Tweety)",
                        1.0f,
                        0.73f);


    }

    @Test
    void variable_elimination6simplerReverse() {


        test.termVolMax(14);
        test.nar.confMin.set(0.35f);
        test

                .believe("(bird:$x ==> (&&, flyer:$x, chirping:$x, food:worms))")
                .believe("flyer:Tweety")
                .mustBelieve(cycles, "(bird:Tweety ==> (chirping:Tweety && food:worms))",
                        1.0f,
                        0.73f
                        //0.42f
                        //0.81f
                );


    }


    @Test
    void multiple_variable_elimination() {

        test.termVolMax(16);
        test.believe("((($x --> key) && ($y --> lock)) ==> open($x, $y))");
        test.believe("({lock1} --> lock)");
        test.mustBelieve(cycles, "((({lock1}-->lock)&&($1-->key))==>open($1,{lock1}))", 1.00f, 0.81f);
        test.mustBelieve(cycles, "(($1 --> key) ==> open($1, {lock1}))", 1.00f, 0.73f);


    }


    @Test
    void multiple_variable_elimination2() {

        test.nar.termVolMax.set(16);
        TestNAR tester = test;
        tester.believe("(lock:$x ==> (key:#y && open(#y,$x)))");
        tester.believe("lock:{lock1}");
        tester.mustBelieve(cycles, "((#1-->key) && open(#1,{lock1}))", 1.00f, 0.81f);

    }


    @Test
    void multiple_variable_elimination3() {

        TestNAR tester = test;
        tester.believe("((#x --> lock) && (key:$y ==> open($y,#x)))");
        tester.believe("({lock1} --> lock)");
        tester.mustBelieve(cycles, "(($1 --> key) ==> open($1,{lock1}))", 1.00f,
                //0.43f
                //0.66f
                0.73f
        );

    }


    @Test
    void multiple_variable_elimination4() {


        test.termVolMax(12)
                .believe("(&&,open(#y,#x),(#x --> lock),<#y --> key>)")
                .believe("({lock1} --> lock)")
                .mustBelieve(cycles, "((#1-->key) && open(#1,{lock1}))", 1.00f,
                        0.81f
                        //0.66f
                        //0.43f
                );
    }


    @Test
    void variable_introduction() {

        test.termVolMax(7)
                .believe("(swan --> bird)")
                .believe("(swan --> swimmer)", 0.80f, 0.9f)
                .mustBelieve(cycles, "(($1 --> swimmer) ==> ($1 --> bird))", 1.00f, 0.39f)
                .mustBelieve(cycles, "(($1 --> bird) ==> ($1 --> swimmer))", 0.80f, 0.45f)
                .mustBelieve(cycles, "((#1 --> swimmer) && (#1 --> bird))", 0.80f, 0.81f);

    }

    @Test
    void variable_introduction_with_existing_vars() {


        TestNAR tester = test;
        tester.believe("<swan --> <#1 --> birdlike>>");
        tester.believe("(swan --> swimmer)", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "<<$1 --> <#2 --> birdlike>> ==> ($1 --> swimmer)>", 0.80f, 0.45f);
    }


    @Test
    void variable_introduction2() {
        /*
        originally: https:
        mustBelieve(cycles, "<<gull --> $1> ==> <swan --> $1>>", 0.80f, 0.45f); 
        mustBelieve(cycles, "<<swan --> $1> ==> <gull --> $1>>", 1.00f, 0.39f); 
        mustBelieve(cycles, "<<gull --> $1> <=> <swan --> $1>>", 0.80f, 0.45f); 
        mustBelieve(cycles, "(&&,<gull --> #1>,<swan --> #1>)", 0.80f, 0.81f); 
         */

        TestNAR tester = test;
        tester.nar.termVolMax.set(13);
        tester.believe("<gull --> swimmer>");
        tester.believe("(swan --> swimmer)", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "<<gull --> $1> ==> <swan --> $1>>", 0.80f, 0.45f);
        tester.mustBelieve(cycles, "<<swan --> $1> ==> <gull --> $1>>", 1.00f, 0.39f);

        tester.mustBelieve(cycles, "(&&,<gull --> #1>,<swan --> #1>)", 0.80f, 0.81f);
    }

    @Test
    @Disabled
    void variable_introduction3() {

        TestNAR tester = test;
        tester.believe("<gull --> swimmer>", 1f, 0.9f);
        tester.believe("(swan --> swimmer)", 0f, 0.9f);
        tester.mustBelieve(cycles, "(&&,<gull --> #1>,<swan --> #1>)", 0.0f, 0.81f);
        tester.mustBelieve(cycles, "(&&,<gull --> #1>,(--,<swan --> #1>))", 1.0f, 0.81f);


    }

    @Test
    void variable_introduction_with_existing_vars2() {


        TestNAR tester = test;

        tester.believe("(#1 --> swimmer)");
        tester.believe("(swan --> swimmer)", 0.80f, 0.9f);

        tester.mustBelieve(cycles, "<<#1 --> $2> ==> <swan --> $2>>", 0.80f, 0.45f);
        tester.mustBelieve(cycles, "<<swan --> $1> ==> <#2 --> $1>>", 1.00f, 0.39f);

        tester.mustBelieve(cycles, "(<#1 --> #2> && <swan --> #2>)", 0.80f, 0.81f);
    }

    @Test
    void variables_introduction() {

        test.termVolMax(12)
                .confMin(0.35f)
                .believe("open({key1},{lock1})")
                .believe("key:{key1}")
                //.mustBelieve(cycles, "(key:{$1} ==> open({$1},{lock1}))", 1.00f, 0.45f)
                .mustBelieve(cycles, "(key:{$1} ==> open({$1},{lock1}))", 1.00f, 0.42f)
                .mustBelieve(cycles, "(&&,open({#1},{lock1}),key:{#1})", 1.00f, 0.81f);


    }

    @Test
    void testConjunctionContradictionInduction() {

        test
                .believe("((x && y) ==> z)")
                .believe("((x && --y) ==> z)")
                .mustBelieve(cycles, "(x ==> z)", 1.00f,
                        0.66f);
        //0.45f);

    }

    @Test
    void multiple_variables_introduction() {

        TestNAR tester = test;

        tester.believe("(key:$x ==> open($x,lock1))");
        tester.believe("lock:lock1");

        tester.mustBelieve(cycles * 1, "((key:$1 && lock:$2) ==> open($1,$2))",
                1.00f, 0.45f /*0.81f*/);


    }


    @Test
    void multiple_variables_introduction2() {

        TestNAR tester = test;
        tester.termVolMax(15);
        tester.believe("(key:#x && open(#x,{lock1}))");
        tester.believe("lock:{lock1}");


        tester.mustBelieve(cycles, "(&&,open(#1,{lock1}),({lock1}-->lock),(#1-->key))", 1.00f, 0.81f);
        //tester.mustBelieve(cycles, "(&&,open(#1,{#2}),({#2}-->lock),(#1-->key))", 1.00f, 0.81f);
        //tester.mustBelieve(cycles, "(&&, key:#1, lock:#2, open(#1,#2))", 1.00f, 0.81f);


        tester.mustBelieve(cycles, "(lock:{$1} ==> (key:#2 && open(#2,{$1})))", 1.00f, 0.45f); //this is ok too
        //tester.mustBelieve(cycles, "(lock:$1 ==> (key:#2 && open(#2,$1)))", 1.00f, 0.45f);

    }

    @Test
    void second_level_variable_unificationNoImgAndAsPreconditionAllIndep() {

        test.confMin(0.7f);
        test.termVolMax(15);
        test.believe("((($1 --> lock)&&($2 --> key)) ==> open($1,$2))", 1.00f, 0.90f);
        test.believe("({key1} --> key)", 1.00f, 0.90f);
        test.mustBelieve(cycles * 2, "(($1-->lock)==>open($1,{key1}))", 1.00f,
                0.73f
                /*0.81f*/);
    }

    @Test
    void second_level_variable_unificationNoImgAndAsPrecondition() {
        test.nar.termVolMax.set(15);

        TestNAR tester = test;
        tester.believe("(((#1 --> lock)&&($2 --> key)) ==> open(#1,$2))", 1.00f, 0.90f);
        tester.believe("({key1} --> key)", 1.00f, 0.90f);
        tester.mustBelieve(cycles,
                //"((({key1}-->key)&&(#1-->lock))==>open(#1,{key1}))",
                "((#1-->lock)==>open(#1,{key1}))",
                1.00f, 0.81f);
    }

    @Test
    void second_level_variable_unification() {
        /* there is a lock which is opened by all keys */
        test
            .believe("(((#1 --> lock) && ($2 --> key)) ==> open($2, #1))", 1.00f, 0.90f)
            .believe("({key1} --> key)", 1.00f, 0.90f)
            .mustBelieve(cycles, "((#1 --> lock) && open({key1}, #1))", 1.00f, 0.81f);
    }


    @Test
    void second_level_variable_unification_neg() {
        /* there is a lock which is opened by all non-keys */
        test.believe("(((#1 --> lock) && --($2 --> key)) ==> open($2, #1))")
            .believe("--({nonKey1} --> key)")
            .mustBelieve(cycles, "((#1 --> lock) && open({nonKey1}, #1))", 1.00f, 0.81f);
    }


    @Test
    void second_level_variable_unification2() {
        test.nar.termVolMax.set(15);

        test.believe("<($1 --> lock) ==> (&&,<#2 --> key>,open(#2,$1))>", 1.00f, 0.90f);
        test.believe("({key1} --> key)", 1.00f, 0.90f);
        test.mustBelieve(cycles, "<($1 --> lock) ==> open({key1},$1)>", 1.00f,
                //0.81f
                0.4f
        );
        //0.73f
        //0.43f);

    }

    @Test
    void testSimpleIndepUnification() {

        test.input("(y:$x ==> z:$x).");
        test.input("y:x.");
        test.mustBelieve(cycles, "z:x", 1.0f, 0.81f);
    }


    @Test
    void second_variable_introduction_induction() {

        test.termVolMax(17);
        test.believe("(open($1,lock1) ==> key:$1)");
        test.believe("open(lock,lock1)");
        test.mustBelieve(cycles,
                "((open(lock,#1) && open($2,#1)) ==> key:$2)",
                1.00f, 0.45f /*0.81f*/);

    }

    @Test
    void deductionBeliefWithVariable() {
        test
                .termVolMax(9)
                .believe("(x($1)==>y($1))", 1.00f, 0.90f)
                .believe("x(a)", 1.00f, 0.90f)
                .mustBelieve(cycles, "y(a)", 1.00f, 0.81f);
    }

    @Test
    void deductionBeliefWithVariableNeg() {
        test
                .believe("--(x($1)==>y($1))", 1.00f, 0.90f)
                .believe("x(a)", 1.00f, 0.90f)
                .mustBelieve(cycles, "--y(a)", 1.00f, 0.81f);
    }
//    @Test
//    void deductionBeliefWeakPositiveButNotNegative() {
//        test
//                .believe("(a==>b)", 0.55f, 0.90f)
//                .believe("a", 0.55f, 0.90f)
//                .mustBelieve(cycles, "b", 0.55f, 0.25f);
//    }
//    @Test
//    void deductionBeliefWeakNegativeButNotPositive() {
//        test
//                .believe("(a==>b)", 0.45f, 0.90f)
//                .believe("a", 0.45f, 0.90f)
//                .mustBelieve(cycles, "b", 0.49f, 0.81f);
//    }

    @Test
    void abductionBeliefWeakPositivesButNotNegative() {

        test
                .believe("(a==>b)", 0.9f, 0.90f)
                .believe("b", 0.9f, 0.90f)
                .mustBelieve(cycles, "a",
                        0.82f, 0.40f
                );
    }

    @Test
    void abductionBeliefWeakNegativesButNotNegative() {

        test
                .termVolMax(3)
                .believe("(a==>b)", 0.1f, 0.90f)
                .believe("b", 0.1f, 0.90f)
                .mustBelieve(cycles, "a", 1, 0.37f);
    }


    @Test
    void abductionBeliefOffCenteredPositiveNegativeButNotTotalFail() {

        test.nar.confMin.set(0.1f);
        test
                .believe("(a==>b)", 0.9f, 0.95f)
                .believe("b", 0.45f, 0.95f)
                .mustBelieve(cycles, "a",
                        0.46f, 0.29f
                );
    }

    @Test
    void GoalMatchSubjOfImplWithVariable_Pos() {
        test
            .confMin(0f)
            .believe("(x($1)==>y($1))")
                .goal("x(a)", Tense.Eternal, 1.00f, 0.90f)
                .mustGoal(cycles, "y(a)", 1.00f, 0.45f)
                .mustNotOutput(cycles, "y(a)", GOAL, 0.00f, 0.5f, 0, 0.9f, t->true)
        ;
    }

    @Test
    void GoalMatchSubjOfImplWithVariable_PosWeak() {
        test
            .confMin(0f)
            .believe("(x($1)==>y($1))", 0.75f, 0.9f)
            .goal("x(a)", Tense.Eternal, 1.00f, 0.90f)
            .mustGoal(cycles, "y(a)", 0.75f, 0.45f)
//            .mustGoal(cycles, "y(a)", 0.25f, 0.1f)
        ;
    }

    @Test
    void GoalMatchNegatedSubjOfImplWithVariable() {
        test
            .confMin(0f)
            .believe("(--x($1)==>y($1))")
            .goal("x(a)", Tense.Eternal, 0.00f, 0.90f)
            .mustGoal(cycles, "y(a)", 1.00f, 0.45f)
            .mustNotOutput(cycles, "y(a)", GOAL, 0f, 0.5f, 0, 0.9f, t->true)
        ;
    }
    @Test
    void GoalMatchNegatedSubjOfImplWithVariable_Neg() {
        test
            .confMin(0)
            .believe("(--x($1) ==> --y($1))")
            .goal("x(a)", Tense.Eternal, 0.00f, 0.90f)
            .mustGoal(cycles, "y(a)", 0.00f, 0.45f)
            .mustNotOutput(cycles, "y(a)", GOAL, 0.5f, 1f, 0, 0.9f, t->true)
        ;
    }

    @Test
    void GoalMatchSubjOfImplWithVariableNeg() {
        test
            .confMin(0)
                .believe("--(x($1)==>y($1))")
                .goal("x(a)", Tense.Eternal, 1.00f, 0.90f)
                .mustGoal(cycles, "y(a)", 0.00f, 0.45f)
                .mustNotOutput(cycles, "y(a)", GOAL, 0.5f, 1f, 0, 0.9f, t->true)
        ;
    }

    @Test
    void GoalMatchPredOfImplWithVariable() {
        test
                .believe("(x($1)==>y($1))")
                .goal("y(a)", Tense.Eternal, 1.00f, 0.90f)
                .mustGoal(cycles, "x(a)", 1.00f, 0.81f);
    }

    @Test
    void variable_elimination_deduction() {

        test
                .termVolMax(13)
                .believe("((&&,(#1 --> lock),open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .believe("(lock1 --> lock)", 1.00f, 0.90f)
                .mustBelieve(cycles, "((open($1,lock1)&&(lock1-->lock))==>($1-->key))", 1.00f, 0.81f)
                .mustBelieve(cycles, "(open($1,lock1)==>($1-->key))", 1.00f, 0.73f)
        ;

    }
    @Test
    void variable_elimination_deduction_neg_cond() {

        test
                .termVolMax(14)
                .believe("((&&,--(#1 --> lock),open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .believe("(lock1 --> lock)", 0.00f, 0.90f)
                .mustBelieve(cycles, "(open($1,lock1)==>($1-->key))", 1.00f, 0.81f)
        ;

    }
    @Test
    void variable_elimination_deduction_neg_conc() {

        test
                .termVolMax(13)
                .believe("((&&,(#1 --> lock),open($2,#1)) ==> ($2 --> key))", 0.00f, 0.90f)
                .believe("(lock1 --> lock)", 1.00f, 0.90f)
                //.mustBelieve(cycles, "((open($1,lock1)&&(lock1-->lock))==>($1-->key))", 0.00f, 0.81f)
                .mustBelieve(cycles, "(open($1,lock1)==>($1-->key))", 0.00f, 0.81f)
        ;
    }

    @Test
    void variable_elimination_deduction_neg_condition() {

        test
                .termVolMax(14)
                .believe("((&&, --(#1 --> lock), open($2,#1)) ==> ($2 --> key))")
                .believe("--(lock1 --> lock)")
                //.mustBelieve(cycles, "((open($1,lock1)&&--(lock1-->lock))==>($1-->key))", 1.00f, 0.81f)
                .mustBelieve(cycles, "(open($1,lock1)==>($1-->key))", 1.00f, 0.81f)
        ;
    }

    @Test
    void abduction_without_variable_elimination() {

        test
                .termVolMax(13)
                .confMin(0.4f)
                .believe("(open(x,lock1) ==> (x --> key))", 1.00f, 0.90f)
                .believe("(((lock1 --> lock) && open(x,lock1)) ==> (x --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles, "lock:lock1", 1.00f, 0.45f)
        ;
    }

    @Test
    void abduction_neg_without_variable_elimination() {

        test
                .termVolMax(14)
                .believe("(open(x,lock1) ==> (x --> key))", 1.00f, 0.90f)
                .believe("((--(lock1 --> lock) && open(x,lock1)) ==> (x --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles, "lock:lock1", 0.00f, 0.45f)
        ;
    }


    /**
     * Conditional Abduction via Multi-conditional Syllogism
     */
    @Test
    void abduction_with_variable_elimination() {

        test
                .termVolMax(13)
                .believe("(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.90f)
                .believe("(((#1 --> lock) && open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles, "lock:lock1", 1.00f, 0.45f)
        ;
    }

    /**
     * Conditional Abduction via Multi-conditional Syllogism
     */
    @Test
    void abduction_with_variable_elimination_negated() {

        test
                .termVolMax(14)
                .believe("(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.90f)
                .believe("((--(#1 --> lock) && open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles, "lock:lock1", 0.00f, 0.45f)
                .mustNotOutput(cycles, "lock:lock1", BELIEF, 0.5f, 1f, 0, 1f, ETERNAL)
        ;
    }

    /**
     * Conditional Abduction via Multi-conditional Syllogism
     */
    @Test
    void abduction_positive_negative_mix_depolarized() {

        test
                .termVolMax(7).confMin(0.3f)
                .believe("(P ==> N)", 0.00f, 0.90f)
                .believe("((A && --N) ==> P)", 1.00f, 0.90f)
                .mustBelieve(cycles, "A", 1.00f, 0.45f)
        ;
    }

    @Test
    void strong_unification_simple() {

        test
                .believe("(pair($a,$b) ==> ($a --> $b))", 1.00f, 0.90f)
                .believe("pair(x,y)", 1.00f, 0.90f)
                .mustBelieve(cycles * 4, "(x --> y)", 1.00f, 0.81f);
    }

    @Test
    void strong_unification_dep_indep_pre() {
        test.termVolMax(7)
                .believe("(#x --> y)")
                .believe("(($x --> y) ==> ($x --> z))")
                .mustBelieve(cycles, "(#x-->z)", 1f, 0.81f);
    }

    @Test
    void strong_unification_dep_indep_post() {
        test.termVolMax(7)
                .believe("(#x --> z)")
                .believe("(($x --> y) ==> ($x --> z))")
                .mustBelieve(cycles, "(#x-->y)", 1f, 0.45f);
    }

    @Test
    void strong_unification_simple2() {

        test.termVolMax(9)
                .believe("<<($a,$b) --> pair> ==> {$a,$b}>", 1.00f, 0.90f)
                .believe("<(x,y) --> pair>", 1.00f, 0.90f)
                .mustBelieve(cycles, "{x,y}", 1.00f, 0.81f);
    }


    @Test
    void strong_unification_pos() {

        test.believe("(sentence($a,is,$b) ==> ($a --> $b))", 1.00f, 0.90f);
        test.believe("sentence(bmw,is,car)", 1.00f, 0.90f);
        test.mustBelieve(cycles, "car:bmw", 1.00f, 0.81f);

    }

    @Test
    void strong_unification_neg() {
        test.termVolMax(12);
        test.believe("( --sentence($a,is,$b) ==> ($a --> $b) )", 1.00f, 0.90f);
        test.believe("sentence(bmw,is,car)", 0.00f, 0.90f);
        test.mustBelieve(cycles, "car:bmw", 1.00f, 0.81f);
        test.mustNotOutput(cycles, "car:bmw", BELIEF, 0, 0.5f, 0, 1f);
    }

    @Test
    void strong_unification_neg_neg() {
        test.termVolMax(12);
        test.believe("( --sentence($a,is,$b) ==> ($a --> $b) )", 0.00f, 0.90f);
        test.believe("sentence(bmw,is,car)", 0.00f, 0.90f);
        test.mustBelieve(cycles, "car:bmw", 0.00f, 0.81f);
        test.mustNotOutput(cycles, "car:bmw", BELIEF, 0.5f, 1f, 0, 1f);
    }

    @Test
    void strong_elimination() {

        test.termVolMax(18);
        test.confMin(0.6f);
        test.believe("((test($a,is,cat) && sentence($a,is,$b)) ==> ($a --> $b))");
        test.believe("test(tim,is,cat)");
        test.mustBelieve(cycles, "(sentence(tim,is,$1) ==> (tim --> $1))", 1.00f, 0.73f);

    }

    @Test
    void impliesUnbelievedYet() {

        test.termVolMax(7);
        test.believe("(x:a ==> c:d).");
        test.believe("x:a.");
        test.mustBelieve(cycles, "c:d", 1.00f, 0.81f);
    }

    @Test
    void implVariableSubst() {

        test.termVolMax(7);
        test.believe("x:y.");
        test.believe("(x:$y==>$y:x).");
        test.mustBelieve(cycles, "y:x", 1.00f, 0.81f);
    }



    @Test
    void testDecomposeImplPredConjQuestion() {
        test
                .ask("( x ==> (y && z) )")
                .mustOutput(cycles, "( x ==>+- y )", QUESTION)
                .mustOutput(cycles, "( x ==>+- z )", QUESTION)
        ;
    }

    @Test
    void testDecomposeImplPredDisjQuestion() {
        test
                .ask("( x ==> (y || z) )")
                .mustOutput(cycles, "( x ==>+- y )", QUESTION)
                .mustOutput(cycles, "( x ==>+- z )", QUESTION)
        ;
    }

    @Test
    void recursionSmall() {

        test.termVolMax(8);
        test.confMin(0.5f);
        test.nar.freqResolution.set(0.25f);
        test.nar.confResolution.set(0.05f);
        test
                .believe("num:x", 1.0f, 0.9f)
                .believe("( num:$1 ==> num($1) )", 1.0f, 0.9f)
                .mustBelieve(cycles, "num(x)", 1.0f, 1.0f, 0.81f, 1.0f)
                .mustBelieve(cycles, "num((x))", 0.99f, 1.0f, 0.50f, 1.0f)
                .mustBelieve(cycles*2, "num(((x)))", 0.99f, 1.0f, 0.25f, 1.0f)


        ;
    }

    @Test
    void recursionSmall1() {


        test.termVolMax(10);
        test.confMin(0.5f);
        test.nar.freqResolution.set(0.2f);
        test
                .believe("num(x)", 1.0f, 0.9f)
                .believe("( num($1) ==> num(($1)) )", 1.0f, 0.9f)
                .ask("num(((x)))")
                .mustBelieve(cycles, "num((x))", 1.0f, 1.0f, 0.8f, 1.0f)
                .mustBelieve(cycles, "num(((x)))", 1.0f, 1.0f, 0.1f /*0.66f*/, 1.0f);


    }


//    @Test
//    void inductRawProductDifference() {
//        test
//                .believe("(x,0)", 1f, 0.9f)
//                .believe("(x,1)", 0.6f, 0.9f)
//                .mustBelieve(cycles, "((x,1)~(x,0))", 0.0f, 0.85f)
//                .mustBelieve(cycles, "((x,0)~(x,1))", 0.4f, 0.85f);
//    }
//    @Test
//    void inductRawProductDifference2() {
//        test
//                .believe("(x,0)", 1f, 0.9f)
//                .believe("(x,1)", 0.5f, 0.9f)
//                .mustQuestion(cycles, "((x,1)~(x,0))")
//                .mustQuestion(cycles, "((x,0)~(x,1))")
//                .mustBelieve(cycles, "((x,1)~(x,0))", 0.0f, 0.85f)
//                .mustBelieve(cycles, "((x,0)~(x,1))", 0.5f, 0.85f);
//    }

    @Test
    void testHypothesizeSubconditionIdentityPre() {
        test
                .believe("((f(x) && f($1)) ==> g($1))", 1f, 0.9f)
                .mustBelieve(cycles, "(f($1) ==> g($1))", 1f, 0.81f)
                .mustBelieve(cycles, "(f(x) ==> g(x))", 1f, 0.81f)
        ;
    }

    @Test
    void testHypothesizeSubconditionIdentityPost() {
        test
                .believe("(g($1) ==> (f(x) && f($1)))", 1f, 0.9f)
                .mustBelieve(cycles, "(g($1) ==> f($1))", 1f, 0.81f)
                .mustBelieve(cycles, "(g(x) ==> f(x))", 1f, 0.81f)
        ;
    }

    @Test
    void testHypothesizeSubconditionIdentityConj() {
        test.nar.termVolMax.set(17);
        test
                .believe("(&&,f(x),f(#1),g(#1))", 1f, 0.9f)
                .mustBelieve(cycles, "(&&,f(x),g(x))", 1f, 0.81f)
        ;
    }

    @Test
    void testHypothesizeSubconditionNeg_Conj_ShortCircuit() {
        test.nar.termVolMax.set(13);
        test
                .believe("(--(f(x) && --f(#1)) ==> a)", 1f, 0.9f)
                .mustBelieve(cycles, "a", 1f, 0.43f)
        ;
    }

//    @Test void ImplIntersectionPos() {
//        test
//                .believe("(A ==> M)", 0.6f, 0.9f)
//                .believe("(B ==> M)", 0.7f, 0.9f)
//                .mustBelieve(cycles, "((A && B) ==> M)", .54f, 0.81f) //some freq and conf, dunno
//        ;
//    }
//    @Test void ImplIntersectionPosNeg() {
//        test
//                .believe("(A ==> M)", 0.25f, 0.9f)
//                .believe("(B ==> M)", 0.75f, 0.9f)
//                .mustNotOutputAnything()
//        ;
//        test.run(10);
//    }
//    @Test void ImplIntersectionNeg() {
//        test
//                .believe("(A ==> M)", 0.4f, 0.9f)
//                .believe("(B ==> M)", 0.3f, 0.9f)
//                .mustBelieve(cycles, "((A && B) ==> M)", .46f, 0.81f) //some freq and conf, dunno
//        ;
//    }
//    @Test void ImplUnionPos() {
//        test
//                .believe("(A ==> M)", 0.6f, 0.9f)
//                .believe("(B ==> M)", 0.7f, 0.9f)
//                .mustBelieve(cycles, "((A || B) ==> M)", .76f, 0.81f)
//        ;
//    }
//    @Test void ImplUnionNeg() {
//        test
//                .believe("(A ==> M)", 0.2f, 0.9f)
//                .believe("(B ==> M)", 0.1f, 0.9f)
//                .mustBelieve(cycles, "((A || B) ==> M)", .04f, 0.81f)
//        ;
//    }


    @Test
    void testMutexAbduction() {
        test
            .termVolMax(6)
            .believe("(--(x && y) ==> z)")
            .believe("(x && z)")
            .mustBelieve(cycles, "y", 0f, 0.45f)
            .mustNotOutput(cycles, "y", BELIEF, 0.1f, 1f, 0, 1)
        ;
    }
    @Test
    void testMutexAbductionNeg() {
        test
            .termVolMax(6)
            .believe("(--(x && y) ==> --z)")
            .believe("(x && --z)")
            .mustBelieve(cycles, "y", 0f, 0.45f)
            .mustNotOutput(cycles, "y", BELIEF, 0.1f, 1, 0, 1)
        ;
    }
    @Test
    void testMutexBelief() {
        test
                .confMin(0.7f)
                .termVolMax(3)
                .believe("--(x && y)")
                .believe("x")
                .mustBelieve(cycles, "y", 0f, 0.81f)
//                .mustNotOutput(cycles, "x", BELIEF, 0f, 0.5f, 0, 1f, ETERNAL)
//                .mustNotOutput(cycles, "y", BELIEF, 0.5f, 1f, 0, 1f, ETERNAL)
        ;
    }

    @Disabled @Test
    void testMutexConjBeliefInduction() {
        test
                .termVolMax(8)
                .confMin(0.4f)
                .believe("(x && --y)")
                .believe("(--x && y)")
                .mustBelieve(cycles, "(x && y)", 0f, 0.81f)
                .mustBelieve(cycles, "(x ==> y)", 0f, 0.45f)
                .mustBelieve(cycles, "(y ==> x)", 0f, 0.45f)
        ;
    }

    @Test
    void testMutexConjBeliefInduction2() {
        test
                .termVolMax(8)
                .believe("(&&,x,--y,a)")
                .believe("(&&,--x,y,a)")
                .mustBelieve(cycles, "(--(x && y) && a)", 1f, 0.81f)
        ;
    }
//    @Test void MutexConjImplBeliefInduction() {
//        test.nar.termVolumeMax.setAt(12);
//        test
//                .believe("((x && --y) ==> z)")
//                .believe("((--x && y) ==> z)")
//                .mustBelieve(cycles, "(--(x && y) ==> z)", 1f, 0.81f)
//        ;
//    }


//        @Test
//        void testMutexDiffGoal1NegNAary() {
//            test
//                    .input("--((&,a,b,--c)-->g)!")
//                    .input("((a&b)-->g).")
//                    .mustGoal(cycles, "(c-->g)", 1f, 0.81f);
//        }

    @Test
    void testMutexDissociation() {
        test
                .confMin(0.4f)
                .believe("(&&, x, --y, a)")
                .believe("(&&, --x, y, b)")
                .mustBelieve(cycles, "(a && b)", 0f, 0.81f)
        ;
    }

    @Test
    void testImplSubjQuestion() {
        test
                .believe("(x ==> y)")
                .ask("x")
                .mustQuestion(cycles, "y")
        ;
    }
    @Test
    void testImplSubjDisjDecompose() {
        test
            .believe("((a||b) ==> y)", 0.75f, 0.9f)
            .mustBelieve(cycles, "(a ==> y)", 0.75f, 0.81f)
        ;
    }
    @Test
    void testImplSubjDisjDeduction() {
        test
            .termVolMax(7)
            .believe("((a||b) ==> y)")
            .believe("a")
            //.mustNotOutput(cycles, "(a==>y)", BELIEF,1,1, 0.45f,0.45f,ETERNAL)
            .mustNotOutput(cycles, "(a ==>+- (a==>y))", QUESTION)

            .mustNotOutput(cycles, "(((--,a) &&+- b) ==>+- y)", QUESTION)
            .mustQuestion(cycles, "(((--,a)&&b)==>y)")

            .mustNotOutput(cycles, "(a ==>+- (a==>y))", QUESTION)
            .mustBelieve(cycles, "y", 1, 0.81f)
        ;
    }

    @Test
    void testImplSubjNegQuestion() {
        test
                .confMin(0.9f)
                .believe("(--x ==> y)")
                .ask("x")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test
    void testImplPredQuestion() {
        test
                .confMin(0.9f)
                .believe("((x&&y)==>z)")
                .ask("z")
                .mustQuestion(cycles, "(x&&y)")
        ;
    }
    @Test
    void testImplPredQuestionUnify() {
        test
                .termVolMax(8)
                .confMin(0.9f)
                .believe("((x && $1)==>z($1))")
                .ask("z(y)")
                .mustQuestion(cycles, "(x && y)")
        ;
    }
    @Test
    void testImplPredQuest() {
        test
                .termVolMax(7)
                .confMin(0.9f)
                .believe("((x&&y)==>z)")
                .quest("z")
                .mustQuest(cycles, "(x&&y)")
        ;
    }

    @Disabled
    @Test
    void testImplConjPredQuestion() {
        test
                .termVolMax(7)
                .confMin(0.9f)
                .believe("((x&&y)==>z)")
                .ask("z")
                .mustQuestion(cycles, "x")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test
    void testImplSubjQuestionUnificationConst() {
        test
                .termVolMax(13)
                .confMin(0.9f)
                .believe("(Criminal($1) ==> (&&,Sells($1,#2,#3),z))")
                .ask("Criminal(x)")
                .mustQuestion(cycles, "(&&,Sells(x,#2,#3),z)")
        ;
    }

    @Test
    void testImplSubjQuestionUnificationQuery() {
        test
                .termVolMax(13)
                .confMin(0.9f)
                .believe("(Criminal($1) ==> (&&,Sells($1,#2,#3),z))")
                .ask("Criminal(?x)")
                .mustQuestion(cycles, "(&&,Sells(?1,#2,#3),z)")
        ;
    }

    @Test
    void testImplSubjNegQuestionUnificationConst() {
        test
                .termVolMax(14)
                .confMin(0.9f)
                .believe("(--Criminal($1) ==> (&&,Sells($1,#2,#3),z))")
                .ask("Criminal(x)")
                .mustQuestion(cycles, "(&&,Sells(x,#2,#3),z)")
        ;
    }

    @Test
    void testImplPredQuestionUnification() {
        test
                .confMin(0.9f)
                .believe("((Sells($1,#2,#3) && z) ==> Criminal($1))")
                .ask("Criminal(?x)")
                .mustQuestion(cycles, "(Sells(?1,#2,#3) && z)")
        ;
    }

    /** fair "variable" var-shifting */
    @Test void VarShifts_Belief() {
        test
            .termVolMax(9)
            .believe("((a)-->#1)")
            .believe("(x(#1) && y(#2))")
            .mustBelieve(cycles, "x(#1)", 1f, 0.81f)
            .mustBelieve(cycles, "y(#1)", 1f, 0.81f)
            .mustBelieve(cycles, "(x(a) && y(#1))", 1f, 0.43f) //anonymous analogy
            .mustBelieve(cycles, "(x(#1) && y(a))", 1f, 0.43f) //anonymous analogy
            .mustBelieve(cycles, "(x(a) && y(x))", 1f, 0.43f) //anonymous analogy
            .mustBelieve(cycles, "(y(a) && x(y))", 1f, 0.43f) //anonymous analogy
        ;
    }

    /** fair "variable" var-shifting */
    @Test void VarShifts_Question() {
        test
            .termVolMax(9)
            .confMin(0.9f)
            .believe("((a)-->#1)")
            .ask("(x(#1) && y(#2))")
            .mustQuestion(cycles, "x(#1)")
            .mustQuestion(cycles, "y(#1)")
            .mustQuestion(cycles, "(x(a) && y(#1))")
            .mustQuestion(cycles, "(x(#1) && y(a))")
        ;
    }
    @Test void ConjBelief_2DepVars_Decompose() {
        test
            .believe("(x(#1) && y(#2))")
            .mustBelieve(cycles, "x(#1)", 1f, 0.81f)
            .mustBelieve(cycles, "y(#1)", 1f, 0.81f)
        ;
    }
    @Test void ConjBelief_DepVar_Decompose2() {
		test.believe("(((#1,_1)-->(_1,#2))&&cmp(#1,#2,_2))").mustBelieve(cycles, "cmp(#1,#2,_2)", 1f, 0.81f);
	}

    @Test void ConjQuestion_2DepVars_Decompose() {
        test
            .ask("(x(#1) && y(#2))")
            .mustQuestion(cycles, "x(#1)")
            .mustQuestion(cycles, "y(#1)")
        ;
    }

    @Test void conjImplSubjNonsensical() {
        //n.believe("(good ==> reward)", 1, 0.9f);
        //n.believe("(bad ==> reward)", 0, 0.9f);
        test
            .believe("(good ==> reward)", 1, 0.9f)
            .believe("(bad ==> reward)", 0, 0.9f)
            .mustNotOutput(cycles, "((bad&&good)==>reward)", BELIEF,0, 1, 0, 1)
            .mustBelieve(cycles, "((--bad&&good)==>reward)", 1f, 0.81f)
        ;
    }
//    @Test
//    void testImplToDisj_Subj() {
//        //reverse of: https://github.com/opencog/opencog/blob/master/opencog/pln/rules/wip/or-transformation.scm
//        test
//                .believe("(--x ==> y)")
//                .mustBelieve(cycles, "(||,x,y)", 1f, 0.81f)
//        ;
//    }
//    @Test
//    void testImplToDisj_Pred() {
//        //reverse of: https://github.com/opencog/opencog/blob/master/opencog/pln/rules/wip/or-transformation.scm
//        test
//                .believe("(x ==> --y)")
//                .mustBelieve(cycles, "(||,x,y)", 1f, 0.81f)
//        ;
//    }

//    @Test void MutexSwapPos() {
//        test.nar.termVolumeMax.setAt(14);
//        test
//                .believe("--(x && y)")
//                .believe("its(x,a)")
//                .mustBelieve(cycles, "(its(x,a)<->its(--y,a))", 1f, 0.45f)
//                //.mustBelieve(cycles, "its(--y,a)", 1f, 0.81f)
//        ;
//    }
//
//    @Test void MutexSwapNeg() {
//        test
//                .believe("--(x && y)")
//                .believe("its(--x,a)")
//                .mustBelieve(cycles, "(its(--x,a)<->its(y,a))", 1f, 0.45f)
//                //.mustBelieve(cycles, "its(y,a)", 1f, 0.81f)
//        ;
//    }
}


