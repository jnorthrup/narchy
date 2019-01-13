package nars.nal.nal6;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.TestNAR;
import nars.time.Tense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.QUESTION;
import static nars.time.Tense.ETERNAL;

public class NAL6Test extends NALTest {

    private static final int cycles = 400;

    @BeforeEach
    void setup() {
        test.confTolerance(0.3f);
    }

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(6);
        n.termVolumeMax.set(18);
        //n.freqResolution.set(0.1f);
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
        tester.nar.termVolumeMax.set(14);

        tester.believe("<<bird --> $x> ==> <robin --> $x>>");
        tester.believe("<<swimmer --> $y> ==> <robin --> $y>>", 0.70f, 0.90f);
        tester.mustBelieve(cycles, "<(&&,(bird --> $1),(swimmer --> $1)) ==> (robin --> $1)>", 0.7f /*1f? */, 0.81f);

        tester.mustBelieve(cycles, "<(bird --> $1) ==> (swimmer --> $1)>", 1f, 0.36f /*0.73F*/);
        tester.mustBelieve(cycles, "<(swimmer --> $1) ==> (bird --> $1)>", 0.7f, 0.45f);


    }


    @Test
    void variable_unification5() {

        TestNAR tester = test;
        tester.nar.termVolumeMax.set(14);
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
        tester.nar.termVolumeMax.set(16);
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
        tester.believe("((&&,($x --> flyer),($x --> [chirping]), food($x, worms)) ==> ($x --> bird))");
        tester.believe("((&&,($y --> [chirping]),($y --> [withWings])) ==> ($y --> bird))");
        tester.mustBelieve(cycles, "((($1 --> flyer) && food($1,worms)) ==> ($1 --> [withWings]))", 1.00f,
                0.45f
                /*0.45f*/);
        tester.mustBelieve(cycles, "(($1 --> [withWings]) ==> (($1 --> flyer) && food($1,worms)))", 1.00f,
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

        TestNAR tester = test;

        tester.believe("<($x --> bird) ==> <$x --> animal>>");
        tester.believe("<tiger --> animal>");
        tester.mustBelieve(cycles * 2, "<tiger --> bird>", 1.00f, 0.45f);

    }


    @Test
    void variable_elimination_conj() {
        test.nar.termVolumeMax.set(7);

        TestNAR tester = test;

        tester.believe("((#x --> bird) && (#x --> swimmer))");
        tester.believe("(swan --> bird)", 0.90f, 0.9f);
        tester.mustBelieve(cycles, "(swan --> swimmer)", 0.90f,

                0.43f);
    }

    @Test
    void variable_elimination_sim_subj() {

        TestNAR tester = test;
        tester.believe("(($x --> bird) <-> ($x --> swimmer))");
        tester.believe("(swan --> bird)", 0.90f, 0.9f);
        tester.mustBelieve(cycles, "(swan --> swimmer)", 0.90f,
                0.81f);

    }

    @Test
    void variable_elimination_analogy_substIfUnify() {

        TestNAR tester = test;
        tester.believe("((bird --> $x) <-> (swimmer --> $x))");
        tester.believe("(bird --> swan)", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "(swimmer --> swan)", 0.80f,
                0.81f);

    }

    @Test
    void variable_elimination_analogy_substIfUnifyOther() {
        //same as variable_elimination_analogy_substIfUnify but with sanity test for commutive equivalence
        TestNAR tester = test;
        tester.believe("((bird --> $x) <-> (swimmer --> $x))");
        tester.believe("(swimmer --> swan)", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "(bird --> swan)", 0.80f,
                0.81f);

    }

    @Test
    void variable_elimination_analogy_substIfUnify_Neg() {
        TestNAR tester = test;
        tester.believe("(--(x --> $1) <-> (z --> $1))");
        tester.believe("(x --> y)", 0.10f, 0.9f);
        tester.mustBelieve(cycles, "(z --> y)", 0.9f,
                0.81f);

    }

    @Test
    void variable_elimination5() {
        test.nar.termVolumeMax.set(16);

        TestNAR tester = test;
        tester.believe("({Tweety} --> [withWings])");
        tester.believe("((($x --> [chirping]) && ($x --> [withWings])) ==> ($x --> bird))");
        tester.mustBelieve(cycles, "((({Tweety}-->[chirping])&&({Tweety}-->[withWings]))==>({Tweety}-->bird))",
                1.00f,
                0.81f
        );

    }
    @Test
    void variable_elimination5_neg() {
        test.nar.termVolumeMax.set(16);

        TestNAR tester = test;
        tester.believe("({Tweety} --> [withWings])");
        tester.believe("((($x --> [chirping]) && ($x --> [withWings])) ==> --($x --> nonBird))");
        tester.mustBelieve(cycles, "((({Tweety}-->[chirping])&&({Tweety}-->[withWings]))==>({Tweety}-->nonBird))",
                0.00f,
                //0.81f
                0.81f
        );

    }

    @Test
    void variable_elimination6_easier() {

        test.nar.termVolumeMax.set(14);

        TestNAR tester = test;

        tester.believe("((&&, flyer:$x, chirping:$x, eatsWorms:$x) ==> bird:$x)");
        tester.believe("flyer:Tweety");
        tester.mustBelieve(cycles * 3,
                "((chirping:Tweety && eatsWorms:Tweety) ==> bird:Tweety)",
                1.0f, 0.73f);

    }

    @Test
    void variable_elimination6simpler() {

        test.nar.termVolumeMax.set(14);

        test
                .believe("((&&, flyer:$x, chirping:$x, food:worms) ==> bird:$x)")
                .believe("flyer:Tweety")
                .mustBelieve(cycles, "((chirping:Tweety && food:worms) ==> bird:Tweety)",
                        1.0f,
                        0.73f);


    }

    @Test
    void variable_elimination6simplerReverse() {


        test.nar.termVolumeMax.set(14);
        test
                .believe("(bird:$x ==> (&&, flyer:$x, chirping:$x, food:worms))")
                .believe("flyer:Tweety")
                .mustBelieve(cycles, "(bird:Tweety ==> (chirping:Tweety && food:worms))",
                        1.0f,
                        0.42f
                        //0.81f
                );


    }

    @Test
    void variable_elimination6() {


        test
                .believe("((&&, flyer:$x, ($x-->[chirping]), food($x, worms)) ==> bird:$x)")
                .believe("flyer:Tweety")
                .mustBelieve(cycles, "(((Tweety-->[chirping]) && food(Tweety,worms)) ==> bird:Tweety)",
                        1.0f,
                        0.38f /*0.73f*/);


    }


    @Test
    void multiple_variable_elimination() {

        TestNAR tester = test;
        tester.nar.termVolumeMax.set(16);
        tester.believe("((($x --> key) && ($y --> lock)) ==> open($x, $y))");
        tester.believe("({lock1} --> lock)");
        tester.mustBelieve(cycles, "(($1 --> key) ==> open($1, {lock1}))", 1.00f,
                0.73f);


    }


    @Test
    void multiple_variable_elimination2() {

        test.nar.termVolumeMax.set(16);
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
                0.43f
                //0.66f
        );

    }


    @Test
    void multiple_variable_elimination4() {

        TestNAR tester = test;
        tester.believe("(&&,open(#y,#x),(#x --> lock),<#y --> key>)");
        tester.believe("({lock1} --> lock)");
        tester.mustBelieve(cycles, "((#1-->key) && open(#1,{lock1}))",
                1.00f,
                //0.66f
                0.43f
        );
    }


    @Test
    void variable_introduction() {


        test.termVolMax(7);
        test.believe("(swan --> bird)");
        test.believe("(swan --> swimmer)", 0.80f, 0.9f);
        test.mustBelieve(cycles, "(($1 --> swimmer) ==> ($1 --> bird))", 1.00f, 0.39f);
        test.mustBelieve(cycles, "(($1 --> bird) ==> ($1 --> swimmer))", 0.80f, 0.45f);
        test.mustBelieve(cycles, "((#1 --> swimmer) && (#1 --> bird))", 0.80f, 0.81f);

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
        tester.nar.termVolumeMax.set(13);
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

        test
                .believe("open({key1},{lock1})")
                .believe("key:{key1}")
                //.mustBelieve(cycles, "(key:{$1} ==> open({$1},{lock1}))", 1.00f, 0.45f)
                .mustBelieve(cycles, "(key:$1 ==> open({$1},{lock1}))", 1.00f, 0.42f)
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
        tester.believe("(key:#x && open(#x,{lock1}))");
        tester.believe("lock:{lock1}");


        tester.mustBelieve(cycles, "(&&,open(#1,{#2}),({#2}-->lock),(#1-->key))", 1.00f, 0.81f);
        //tester.mustBelieve(cycles, "(&&, key:#1, lock:#2, open(#1,#2))", 1.00f, 0.81f);


        tester.mustBelieve(cycles, "(lock:{$1} ==> (key:#2 && open(#2,{$1})))", 1.00f, 0.45f); //this is ok too
        //tester.mustBelieve(cycles, "(lock:$1 ==> (key:#2 && open(#2,$1)))", 1.00f, 0.45f);

    }
    @Test
    void second_level_variable_unificationNoImgAndAsPreconditionAllIndep() {
        test.nar.termVolumeMax.set(15);

        TestNAR tester = test;
        tester.believe("((($1 --> lock)&&($2 --> key)) ==> open($1,$2))", 1.00f, 0.90f);
        tester.believe("({key1} --> key)", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "(($1-->lock)==>open($1,{key1}))", 1.00f,
                0.73f
                /*0.81f*/);
    }

    @Test
    void second_level_variable_unificationNoImgAndAsPrecondition() {
        test.nar.termVolumeMax.set(15);

        TestNAR tester = test;
        tester.believe("(((#1 --> lock)&&($2 --> key)) ==> open(#1,$2))", 1.00f, 0.90f);
        tester.believe("({key1} --> key)", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "((({key1}-->key)&&(#1-->lock))==>open(#1,{key1}))", 1.00f, 0.81f);
    }

    @Test
    void second_level_variable_unification() {
//        test.nar.termVolumeMax.set(10);
        TestNAR tester = test;
        ////there is a lock which is opened by all keys
        tester.believe("(((#1 --> lock) && ($2 --> key)) ==> open($2, #1))", 1.00f, 0.90f);
        tester.believe("({key1} --> key)", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "((#1 --> lock) && open({key1}, #1))", 1.00f, 0.81f);
    }

    @Test
    void second_level_variable_unification_neg() {
//        test.nar.termVolumeMax.set(10);

        TestNAR tester = test;
        ////there is a lock which is opened by all non-keys
        tester.believe("(((#1 --> lock) && --($2 --> key)) ==> open($2, #1))");
        tester.believe("--({nonKey1} --> key)");
        tester.mustBelieve(cycles, "((#1 --> lock) && open({nonKey1}, #1))", 1.00f, 0.81f);
    }


    @Test
    void second_level_variable_unification2() {
        test.nar.termVolumeMax.set(15);

        TestNAR tester = test;
        tester.believe("<($1 --> lock) ==> (&&,<#2 --> key>,open(#2,$1))>", 1.00f, 0.90f);
        tester.believe("({key1} --> key)", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "<($1 --> lock) ==> open({key1},$1)>", 1.00f,
                //0.81f
                0.4f
        );
        //0.73f
        //0.43f);

    }

    @Test
    void testSimpleIndepUnification() {

        TestNAR t = test;
        t.input("(y:$x ==> z:$x).");
        t.input("y:x.");
        t.mustBelieve(cycles, "z:x", 1.0f, 0.81f);
    }


    @Test
    void second_variable_introduction_induction() {

        TestNAR tester = test;
        test.nar.termVolumeMax.set(17);

        tester.believe("(open($1,lock1) ==> key:$1)");
        tester.believe("open(lock,lock1)");
        tester.mustBelieve(cycles,
                "((open(lock,#1) && open($2,#1)) ==> key:$2)",
                1.00f, 0.45f /*0.81f*/);

    }

    @Test
    void deductionBeliefWithVariable() {
        test
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
                .believe("(a==>b)", 0.1f, 0.90f)
                .believe("b", 0.1f, 0.90f)
                .mustBelieve(cycles, "a",
                        0.82f, 0.40f
                );
    }

    @Test
    void abductionBeliefPositiveNegativeButNotTotalFail() {

        test.nar.confMin.set(0.1f);
        test
                .believe("(a==>b)", 0.55f, 0.90f)
                .believe("b", 0.45f, 0.90f)
                .mustBelieve(cycles, "a",
                        0.5f, 0.29f
                );
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
    void GoalMatchSubjOfImplWithVariable() {
        test
                .believe("(x($1)==>y($1))", 1.00f, 0.90f)
                .goal("x(a)", Tense.Eternal, 1.00f, 0.90f)
                .mustGoal(cycles, "y(a)", 1.00f, 0.45f);
    }
    @Test
    void GoalMatchSubjOfImplWithVariableNeg() {
        test
                .believe("--(x($1)==>y($1))", 1.00f, 0.90f)
                .goal("x(a)", Tense.Eternal, 1.00f, 0.90f)
                .mustGoal(cycles, "--y(a)", 1.00f, 0.45f);
    }

    @Test
    void GoalMatchPredOfImplWithVariable() {
        test
                .believe("(x($1)==>y($1))", 1.00f, 0.90f)
                .goal("y(a)", Tense.Eternal, 1.00f, 0.90f)
                .mustGoal(cycles, "x(a)", 1.00f, 0.81f);
    }

    @Test
    void variable_elimination_deduction() {

        test
                .termVolMax(14)
                .believe("((&&,(#1 --> lock),open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .believe("(lock1 --> lock)", 1.00f, 0.90f)
                .mustBelieve(cycles, "((open($1,lock1)&&(lock1-->lock))==>($1-->key))", 1.00f, 0.81f);
    }
    @Test
    void variable_elimination_deduction_neg_conc() {

        test
                .termVolMax(14)
                .believe("((&&,(#1 --> lock),open($2,#1)) ==> ($2 --> key))", 0.00f, 0.90f)
                .believe("(lock1 --> lock)", 1.00f, 0.90f)
                .mustBelieve(cycles, "((open($1,lock1)&&(lock1-->lock))==>($1-->key))", 0.00f, 0.81f);
    }

    @Test
    void variable_elimination_deduction_neg_condition() {

        test
                .termVolMax(14)
                .believe("((&&, --(#1 --> lock), open($2,#1)) ==> ($2 --> key))")
                .believe("--(lock1 --> lock)")
                .mustBelieve(cycles, "((open($1,lock1)&&--(lock1-->lock))==>($1-->key))", 1.00f, 0.81f);
    }


    @Test
    @Disabled
    void abduction_with_variable_elimination() {

        test
                .believe("(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.90f)

                .believe("(((#1 --> lock) && open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles * 2, "lock:lock1", 1.00f, 0.45f)
        ;
    }

    @Test
    /** TODO verify */
    @Disabled
    void abduction_with_variable_elimination_negated() {

        test

                .believe("(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.90f)

                .believe("(((--,(#1 --> lock)) && open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles * 2, "lock:lock1", 0.00f, 0.45f)
                .mustNotOutput(cycles * 2, "lock:lock1", BELIEF, 0.5f, 1f, 0, 1f, ETERNAL)
        ;
    }

    @Test
    void strong_unification_simple() {

        TestNAR tester = test;
        tester.believe("(pair($a,$b) ==> ($a --> $b))", 1.00f, 0.90f);
        tester.believe("pair(x,y)", 1.00f, 0.90f);
        tester.mustBelieve(cycles * 4, "(x --> y)", 1.00f, 0.81f);
    }

    @Test
    void strong_unification_simple2() {

        TestNAR tester = test;
        tester.believe("<<($a,$b) --> pair> ==> {$a,$b}>", 1.00f, 0.90f);
        tester.believe("<(x,y) --> pair>", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "{x,y}", 1.00f, 0.81f);
    }


    @Test
    void strong_unification_pos() {

        TestNAR tester = test;
        tester.believe("(sentence($a,is,$b) ==> ($a --> $b))", 1.00f, 0.90f);
        tester.believe("sentence(bmw,is,car)", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "car:bmw", 1.00f, 0.81f);

    }

    @Test
    void strong_unification_neg() {
        test.nar.termVolumeMax.set(12);

        TestNAR tester = test;
        tester.believe("( --sentence($a,is,$b) ==> ($a --> $b) )", 1.00f, 0.90f);
        tester.believe("sentence(bmw,is,car)", 0.00f, 0.90f);
        tester.mustBelieve(cycles, "car:bmw", 1.00f, 0.81f);

    }

    @Test
    void strong_elimination() {
        test.nar.termVolumeMax.set(18);
        test.nar.confMin.set(0.25f);
        TestNAR tester = test;
        tester.believe("((test($a,is,cat) && sentence($a,is,$b)) ==> ($a --> $b))");
        tester.believe("test(tim,is,cat)");
        tester.mustBelieve(cycles, "(sentence(tim,is,$1) ==> (tim --> $1))",
                1.00f, 0.73f);

    }

    @Test
    void impliesUnbelievedYet() {

        TestNAR tester = test;
        tester.believe("(x:a ==> c:d).");
        tester.believe("x:a.");
        tester.mustBelieve(cycles, "c:d", 1.00f, 0.81f);
    }

    @Test
    void implVariableSubst() {

        TestNAR tester = test;
        tester.believe("x:y.");
        tester.believe("(x:$y==>$y:x).");
        tester.mustBelieve(cycles, "y:x", 1.00f, 0.81f);
    }


    @Test
    void testDecomposeImplSubj1Conj() {
        test
                .believe("( (y && z) ==> x )")
                .mustBelieve(cycles, "( y ==> x )", 1f, 0.81f)
                .mustBelieve(cycles, "( z ==> x )", 1f, 0.81f)
        ;
    }

    @Test
    void testPropositionalDecompositionConjPos() {
        ////If S is the case, and (&&,S,A_1..n) is not the case, it can't be that (&&,A_1..n) is the case
        test
                .believe("--(&&,x,y,z)")
                .believe("x")
                .mustBelieve(cycles, "(y&&z)", 0f, 0.81f)
        ;
    }

    @Test
    void testPropositionalDecompositionConjNeg() {
        ////If S is the case, and (&&,S,A_1..n) is not the case, it can't be that (&&,A_1..n) is the case
        test
                .believe("--(&&,--x,y,z)")
                .believe("--x")
                .mustBelieve(cycles, "(y&&z)", 0f, 0.81f)
        ;
    }

    @Test
    void testPropositionalDecompositionDisjPos() {
        ////If S is the case, and (&&,S,A_1..n) is not the case, it can't be that (&&,A_1..n) is the case
        test
                .believe("--(||,x,y,z)")
                .believe("--x")
                .mustBelieve(cycles, "(y||z)", 0f, 0.81f)
        ;
    }

    @Test
    void testPropositionalDecompositionDisjNeg() {
        ////If S is the case, and (&&,S,A_1..n) is not the case, it can't be that (&&,A_1..n) is the case
        test
                .believe("--(||,--x,y,z)")
                .believe("x")
                .mustBelieve(cycles, "(y||z)", 0f, 0.81f)
        ;
    }

    @Test
    void testDecomposeDisj() {
        test
                .believe("(||, x, z)")
                .believe("--x")
                .mustBelieve(cycles, "z", 1f, 0.81f)
        ;
    }

    @Test
    void testDecomposeDisjNeg() {
        test
                .believe("(||, --x, z)")
                .believe("x")
                .mustBelieve(cycles, "z", 1f, 0.81f)
        ;
    }

    @Test
    void testDecomposeDisjNeg2() {
        test
                .believe("(||, x, --z)")
                .believe("--x")
                .mustBelieve(cycles, "z", 0f, 0.81f)
        ;
    }

//    @Test
//    void testDecomposeImplSubjDisjBelief() {
//        test
//                .believe("( (||, y, z) ==> x )")
//                .mustBelieve(cycles, "( y ==> x )", 1f, 0.81f)
//                .mustBelieve(cycles, "( z ==> x )", 1f, 0.81f)
//        ;
//    }

    @Test
    void testDecomposeImplSubjConjQuestion() {
        test
                .ask("( (&&, y, z) ==> x )")
                .mustOutput(cycles, "( y ==>+- x )", QUESTION)
                .mustOutput(cycles, "( z ==>+- x )", QUESTION)
        ;
    }

    @Test
    void testDecomposeImplSubjDisjQuestion() {
        test
                .ask("( (||, y, z) ==> x )")
                .mustOutput(cycles, "( y ==>+- x )", QUESTION)
                .mustOutput(cycles, "( z ==>+- x )", QUESTION)
        ;
    }

    @Test
    void testDecomposeImplPredDisjBelief() {
        test
                .believe("( x ==> (||, y, z))")
                .mustBelieve(cycles, "( x ==> y )", 1f, 0.81f)
                .mustBelieve(cycles, "( x ==> z )", 1f, 0.81f)
        ;
    }

    @Test
    void testDecomposeImplPredConjQuestion()  {
        test
                .ask("( x ==> (&&, y, z) )")
                .mustOutput(cycles, "( x ==>+- y )", QUESTION)
                .mustOutput(cycles, "( x ==>+- z )", QUESTION)
        ;
    }

    @Test
    void testDecomposeImplPredDisjQuestion()  {
        test
                .ask("( x ==> (||, y, z) )")
                .mustOutput(cycles, "( x ==>+- y )", QUESTION)
                .mustOutput(cycles, "( x ==>+- z )", QUESTION)
        ;
    }


    @Test
    void testDecomposeConjNeg2() {
        test
                .believe("(&&, --y, --z)")
                .mustBelieve(cycles, "y", 0f, 0.81f)
                .mustBelieve(cycles, "z", 0f, 0.81f)
        ;
    }

    @Test
    void testDecomposeConjNeg3() {
        test
                .believe("(&&, --y, --z, --w)")
                .mustBelieve(cycles, "y", 0f, 0.73f)
                .mustBelieve(cycles, "z", 0f, 0.73f)
                .mustBelieve(cycles, "w", 0f, 0.73f)
        ;
    }


    @Test
    void recursionSmall() {

        test.nar.termVolumeMax.set(10);

        test
                .believe("num:x", 1.0f, 0.9f)
                .believe("( num:$1 ==> num($1) )", 1.0f, 0.9f)
                //.ask("num(((x)))")
                .mustBelieve(cycles, "num(x)", 1.0f, 1.0f, 0.81f, 1.0f)
                .mustBelieve(cycles, "num((x))", 0.99f, 1.0f, 0.50f, 1.0f)
                .mustBelieve(cycles * 2, "num(((x)))", 0.99f, 1.0f, 0.25f, 1.0f)


        ;
    }

    @Test
    void recursionSmall1()  {


        test.nar.termVolumeMax.set(13);
        test.nar.freqResolution.set(0.1f);
        test
                .believe("num(x)", 1.0f, 0.9f)
                .believe("( num($1) ==> num(($1)) )", 1.0f, 0.9f)
                .ask("num(((x)))")
                .mustBelieve(cycles * 2, "num((x))", 1.0f, 1.0f, 0.8f, 1.0f)
                .mustBelieve(cycles * 3, "num(((x)))", 1.0f, 1.0f, 0.1f /*0.66f*/, 1.0f);


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
                .mustBelieve(cycles, "(f(x) ==> g(x))", 1f, 0.81f)
        ;
    }

    @Test
    void testHypothesizeSubconditionIdentityPost() {
        test
                .believe("(g($1) ==> (f(x) && f($1)))", 1f, 0.9f)
                .mustBelieve(cycles, "(g(x) ==> f(x))", 1f, 0.81f)
        ;
    }

    @Test
    void testHypothesizeSubconditionIdentityConj() {
        test.nar.termVolumeMax.set(17);
        test
                .believe("(&&,f(x),f(#1),g(#1))", 1f, 0.9f)
                .mustBelieve(cycles, "(&&,f(x),g(x))", 1f, 0.81f)
        ;
    }
    @Test
    void testHypothesizeSubconditionNeg_Conj_ShortCircuit() {
        test.nar.termVolumeMax.set(13);
        test
                .believe("(--(f(x) && --f(#1)) ==> a)", 1f, 0.9f)
                .mustBelieve(cycles, "a", 1f, 0.43f)
        ;
    }

//    @Test void testImplIntersectionPos() {
//        test
//                .believe("(A ==> M)", 0.6f, 0.9f)
//                .believe("(B ==> M)", 0.7f, 0.9f)
//                .mustBelieve(cycles, "((A && B) ==> M)", .54f, 0.81f) //some freq and conf, dunno
//        ;
//    }
//    @Test void testImplIntersectionPosNeg() {
//        test
//                .believe("(A ==> M)", 0.25f, 0.9f)
//                .believe("(B ==> M)", 0.75f, 0.9f)
//                .mustNotOutputAnything()
//        ;
//        test.run(10);
//    }
//    @Test void testImplIntersectionNeg() {
//        test
//                .believe("(A ==> M)", 0.4f, 0.9f)
//                .believe("(B ==> M)", 0.3f, 0.9f)
//                .mustBelieve(cycles, "((A && B) ==> M)", .46f, 0.81f) //some freq and conf, dunno
//        ;
//    }
//    @Test void testImplUnionPos() {
//        test
//                .believe("(A ==> M)", 0.6f, 0.9f)
//                .believe("(B ==> M)", 0.7f, 0.9f)
//                .mustBelieve(cycles, "((A || B) ==> M)", .76f, 0.81f)
//        ;
//    }
//    @Test void testImplUnionNeg() {
//        test
//                .believe("(A ==> M)", 0.2f, 0.9f)
//                .believe("(B ==> M)", 0.1f, 0.9f)
//                .mustBelieve(cycles, "((A || B) ==> M)", .04f, 0.81f)
//        ;
//    }

    @Test void testDecomposePositiveImplicationCommonConjunctionSubterm() {
        //tests:
        // (S ==> M), (C ==> M), eventOf(C,S) |- (conjWithout(C,S) ==> M), (Belief:DecomposeNegativePositivePositive)
        test
                .termVolMax(8)
                .believe("(S ==> M)", 0.6f, 0.9f)
                .believe("((X && S) ==> M)", 0.7f, 0.81f)
                .mustBelieve(cycles, "(X ==> M)", .6f, 0.36f) //some freq and conf, dunno
        ;
    }
    @Test void testDecomposeNegativeImplicationCommonConjunctionSubterm() {
        //tests:
        // (S ==> M), (C ==> M), eventOf(C,S) |- (conjWithout(C,S) ==> M), (Belief:DecomposeNegativePositivePositive)
        test
                .termVolMax(8)
                .believe("(       S ==> --M)")
                .believe("((X && S) ==> --M)")
                .mustBelieve(cycles, "(X ==> M)", 0f, 0.81f)
        ;
    }

//    @Test
//    void testDecomposeImplSubj1b() {
//        test.confTolerance(0.03f)
//                .believe("( (&&, y, z, w) ==> x )")
//                .mustBelieve(cycles, "( y ==> x )", 1f, 0.73f)
//                .mustBelieve(cycles, "( z ==> x )", 1f, 0.73f)
//                .mustBelieve(cycles, "( w ==> x )", 1f, 0.73f)
//        ;
//    }

//    @Test
//    void testDecomposeImplSubj1bNeg() {
//        test.confTolerance(0.03f)
//                .believe("( (&&, --y, --z, --w) ==> x )")
//                .mustBelieve(cycles, "( --y ==> x )", 1f, 0.73f)
//                .mustBelieve(cycles, "( --z ==> x )", 1f, 0.73f)
//                .mustBelieve(cycles, "( --w ==> x )", 1f, 0.73f)
//        ;
//    }

    @Test
    void testDecomposeImplPred1b() {
        test.confTolerance(0.03f)
                .believe("( x ==> (&&, y, z, w) )")
                .mustBelieve(cycles, "( x ==> y )", 1f, 0.73f)
                .mustBelieve(cycles, "( x ==> z )", 1f, 0.73f)
                .mustBelieve(cycles, "( x ==> w )", 1f, 0.73f)
        ;
    }


    @Test
    void testDecomposeImplPred2() {
        test.nar.termVolumeMax.set(12);
        test
                .believe("( (a,#b) ==> (&&, (x,#b), y, z ) )")
                .mustBelieve(cycles, "( (a,#b) ==> (x,#b) )", 1f, 0.73f)
                .mustBelieve(cycles, "( (a,#b) ==> y )", 1f, 0.73f)
                .mustBelieve(cycles, "( (a,#b) ==> z )", 1f, 0.73f)
        ;
    }

    @Test
    void testDecomposeImplsubjNeg() {
        test
                .believe("( (&&, --y, --z ) ==> x )")
                .mustBelieve(cycles, "( --y ==> x )", 1f, 0.81f)
                .mustBelieve(cycles, "( --z ==> x )", 1f, 0.81f)
        ;
    }

    @Test
    void testDecomposeImplPredNeg() {
        test
                .believe("( x ==> (&&, --y, --z ) )")
                .mustBelieve(cycles, "( x ==> --y )", 1f, 0.81f)
                .mustBelieve(cycles, "( x ==> --z )", 1f, 0.81f)
        ;
    }

    @Test
    void testDecomposeImplPred1() {
        test
                .believe("( x ==> (y && z) )")
                .mustBelieve(cycles, "( x ==> y )", 1f, 0.81f)
                .mustBelieve(cycles, "( x ==> z )", 1f, 0.81f)
        ;
    }

    @Test void testMutexBelief() {
        test
                .believe("--(x && y)")
                .believe("x")
                .mustBelieve(cycles, "y", 0f, 0.81f)
                .mustNotOutput(cycles, "x", BELIEF, 0f, 0.5f, 0, 1f, ETERNAL)
                .mustNotOutput(cycles, "y", BELIEF, 0.5f, 1f, 0, 1f, ETERNAL)
        ;
    }
    @Test void testMutexConjBeliefInduction() {
        test
                .believe("(x && --y)")
                .believe("(--x && y)")
                .mustBelieve(cycles, "(x && y)", 0f, 0.81f)
                .mustBelieve(cycles, "(x ==> y)", 0f, 0.45f)
                .mustBelieve(cycles, "(y ==> x)", 0f, 0.45f)
        ;
    }
    @Test void testMutexConjBeliefInduction2() {
        test
                .believe("(&&,x,--y,a)")
                .believe("(&&,--x,y,a))")
                .mustBelieve(cycles, "(--(x && y) && a)", 1f, 0.81f)
        ;
    }
//    @Test void testMutexConjImplBeliefInduction() {
//        test.nar.termVolumeMax.set(12);
//        test
//                .believe("((x && --y) ==> z)")
//                .believe("((--x && y) ==> z)")
//                .mustBelieve(cycles, "(--(x && y) ==> z)", 1f, 0.81f)
//        ;
//    }

    @Test void testMutexAbduction() {
        test
                .believe("(--(x && y) ==> z)")
                .believe("(x && z)")
                .mustBelieve(cycles, "y", 0f, 0.45f)
        ;
    }
    @Test void testMutexDissociation() {
        test
                .believe("(&&, x, --y, a)")
                .believe("(&&, --x, y, b)")
                .mustBelieve(cycles, "(a && b)", 0f, 0.81f)
        ;
    }

    @Test void testImplSubjQuestion() {
        test
                .believe("(x ==> y)")
                .ask("x")
                .mustQuestion(cycles, "y")
        ;
    }
    @Test void testImplSubjNegQuestion() {
        test
                .believe("(--x ==> y)")
                .ask("x")
                .mustQuestion(cycles, "y")
        ;
    }
    @Test void testImplPredQuestion() {
        test
                .believe("((x&&y)==>z)")
                .ask("z")
                .mustQuestion(cycles, "(x&&y)")
        ;
    }
    @Test void testImplPredQuest()  {
        test
                .believe("((x&&y)==>z)")
                .quest("z")
                .mustQuest(cycles, "(x&&y)")
        ;
    }
    @Disabled @Test void testImplConjPredQuestion() {
        test
                .believe("((x&&y)==>z)")
                .ask("z")
                .mustQuestion(cycles, "x")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test void testImplSubjQuestionUnificationConst() {
        test
                .believe("(Criminal($1) ==> (&&,Sells($1,#2,#3),z))")
                .ask("Criminal(x)")
                .mustQuestion(cycles, "(&&,Sells(x,#2,#3),z)")
        ;
    }
    @Test void testImplSubjNegQuestionUnificationConst() {
        test
                .believe("(--Criminal($1) ==> (&&,Sells($1,#2,#3),z))")
                .ask("Criminal(x)")
                .mustQuestion(cycles, "(&&,Sells(x,#2,#3),z)")
        ;
    }
    @Test void testImplSubjQuestionUnificationQuery() {
        test
                .believe("(Criminal($1) ==> (&&,Sells($1,#2,#3),z))")
                .ask("Criminal(?x)")
                .mustQuestion(cycles, "(&&,Sells(?1,#2,#3),z)")
        ;
    }

    @Test void testImplPredQuestionUnification() {
        test
                .believe("((&&,Sells($1,#2,#3),z)==>Criminal($1))")
                .ask("Criminal(?x)")
                .mustQuestion(cycles, "(&&,Sells(?1,#2,#3),z)")
        ;
    }

//    @Test void testMutexSwapPos() {
//        test.nar.termVolumeMax.set(14);
//        test
//                .believe("--(x && y)")
//                .believe("its(x,a)")
//                .mustBelieve(cycles, "(its(x,a)<->its(--y,a))", 1f, 0.45f)
//                //.mustBelieve(cycles, "its(--y,a)", 1f, 0.81f)
//        ;
//    }
//
//    @Test void testMutexSwapNeg() {
//        test
//                .believe("--(x && y)")
//                .believe("its(--x,a)")
//                .mustBelieve(cycles, "(its(--x,a)<->its(y,a))", 1f, 0.45f)
//                //.mustBelieve(cycles, "its(y,a)", 1f, 0.81f)
//        ;
//    }
}

