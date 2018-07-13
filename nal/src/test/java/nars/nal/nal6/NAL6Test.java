package nars.nal.nal6;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.QUESTION;
import static nars.time.Tense.ETERNAL;

public class NAL6Test extends NALTest {

    private static final int cycles = 300;

    @BeforeEach
    void setup() {
        test.confTolerance(0.2f);
    }

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(6);
        n.termVolumeMax.set(20);
        n.confMin.set(0.05f);
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

                .believe("<<$x --> bird> ==> <$x --> animal>>")
                .believe("<<$y --> robin> ==> <$y --> bird>>")
                .mustBelieve(cycles, "<<$1 --> robin> ==> <$1 --> animal>>", 1.00f, 0.81f)
                .mustBelieve(cycles, "<<$1 --> animal> ==> <$1 --> robin>>", 1.00f, 0.45f);

    }


    @Test
    void variable_unification3() {

        TestNAR tester = test;


        tester.believe("<<$x --> swan> ==> <$x --> bird>>", 1.00f, 0.80f);
        tester.believe("<<$y --> swan> ==> <$y --> swimmer>>", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "<<$1 --> swan> ==> (&&,<$1 --> bird>,<$1 --> swimmer>)>", 0.80f, 0.72f);
        tester.mustBelieve(cycles, "<<$1 --> swimmer> ==> <$1 --> bird>>", 1f, 0.37f);
        tester.mustBelieve(cycles, "<<$1 --> bird> ==> <$1 --> swimmer>>", 0.80f, 0.42f);


    }


    @Test
    void variable_unification4() {

        TestNAR tester = test;
        tester.believe("<<bird --> $x> ==> <robin --> $x>>");
        tester.believe("<<swimmer --> $y> ==> <robin --> $y>>", 0.70f, 0.90f);
        tester.mustBelieve(cycles, "<(&&,<bird --> $1>,<swimmer --> $1>) ==> <robin --> $1>>", 0.7f /*1f? */, 0.81f);

        tester.mustBelieve(cycles, "<<bird --> $1> ==> <swimmer --> $1>>", 1f, 0.36F);
        tester.mustBelieve(cycles, "<<swimmer --> $1> ==> <bird --> $1>>", 0.7f, 0.45f);


    }


    @Test
    void variable_unification5() {

        TestNAR tester = test;

        tester.believe("<(&&,<$x --> flyer>,<$x --> [chirping]>) ==> <$x --> bird>>");
        tester.believe("<<$y --> [withWings]> ==> <$y --> flyer>>");
        tester.mustBelieve(cycles, "<(&&,<$1 --> [chirping]>,($1 --> [withWings])) ==> <$1 --> bird>>", 1.00f, 0.81f);

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
        tester.believe("<(&&,<$x --> flyer>,<($x,worms) --> food>) ==> <$x --> bird>>");
        tester.believe("<<$y --> flyer> ==> <$y --> [withWings]>>");
        tester.mustBelieve(cycles, "<(&&,($1 --> [withWings]),<($1,worms) --> food>) ==> <$1 --> bird>>", 1.00f, 0.45f);

    }


    @Test
    void variable_elimination_impl_fwd_pos_pos() {

        test
                .believe("<<$x --> bird> ==> <$x --> animal>>")
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

        tester.believe("<<$x --> bird> ==> <$x --> animal>>");
        tester.believe("<tiger --> animal>");
        tester.mustBelieve(cycles * 2, "<tiger --> bird>", 1.00f, 0.45f);

    }


    @Test
    void variable_elimination_conj() {

        TestNAR tester = test;

        tester.believe("(&&,<#x --> bird>,<#x --> swimmer>)");
        tester.believe("<swan --> bird>", 0.90f, 0.9f);
        tester.mustBelieve(cycles, "<swan --> swimmer>", 0.90f,

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
        tester.believe("(bird --> swan)", 0.90f, 0.9f);
        tester.mustBelieve(cycles, "(swimmer --> swan)", 0.90f,
                0.81f);

    }

    @Test
    void variable_elimination_analogy_substIfUnifyOther() {
        //same as variable_elimination_analogy_substIfUnify but with sanity test for commutive equivalence
        TestNAR tester = test;
        tester.believe("((bird --> $x) <-> (swimmer --> $x))");
        tester.believe("(swimmer --> swan)", 0.90f, 0.9f);
        tester.mustBelieve(cycles, "(bird --> swan)", 0.90f,
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

        TestNAR tester = test;
        tester.believe("<{Tweety} --> [withWings]>");
        tester.believe("<(&&,<$x --> [chirping]>,<$x --> [withWings]>) ==> <$x --> bird>>");
        tester.mustBelieve(cycles*2, "<<{Tweety} --> [chirping]> ==> <{Tweety} --> bird>>", 1.00f, 0.73f);

    }


    @Test
    void variable_elimination6_easier() {


        TestNAR tester = test;

        tester.believe("((&&, flyer:$x, chirping:$x, eatsWorms:$x) ==> bird:$x)");
        tester.believe("flyer:Tweety");
        tester.mustBelieve(cycles * 3,
                "((chirping:Tweety && eatsWorms:Tweety) ==> bird:Tweety)",
                1.0f, 0.73f);

    }

    @Test
    void variable_elimination6simpler() {


        test
                .believe("((&&, flyer:$x, chirping:$x, food:worms) ==> bird:$x)")
                .believe("flyer:Tweety")
                .mustBelieve(cycles, "((chirping:Tweety && food:worms) ==> bird:Tweety)",
                        1.0f,
                        0.73f);


    }

    @Test
    void variable_elimination6simplerReverse() {


        test
                .believe("(bird:$x ==> (&&, flyer:$x, chirping:$x, food:worms))")
                .believe("flyer:Tweety")
                .mustBelieve(cycles, "(bird:Tweety ==> (chirping:Tweety && food:worms))",
                        1.0f,
                        0.42f);


    }
    @Test
    void variable_elimination6() {


        test
                .believe("((&&, flyer:$x, ($x-->[chirping]), food($x, worms)) ==> bird:$x)")
                .believe("flyer:Tweety")
                .mustBelieve(cycles * 2, "(((Tweety-->[chirping]) && food(Tweety,worms)) ==> bird:Tweety)",
                        1.0f,
                        0.73f);


    }


    @Test
    void multiple_variable_elimination() {

        TestNAR tester = test;
        tester.believe("((($x --> key) && ($y --> lock)) ==> open($x, $y))");
        tester.believe("({lock1} --> lock)");
        tester.mustBelieve(cycles * 2, "(($1 --> key) ==> open($1, {lock1}))", 1.00f, 0.73f);

    }


    @Test
    void multiple_variable_elimination2() {

        TestNAR tester = test;
        tester.believe("<<$x --> lock> ==> (<#y --> key> && open(#y,$x))>");
        tester.believe("<{lock1} --> lock>");
        tester.mustBelieve(cycles, "(<#1 --> key> && open(#1,{lock1}))", 1.00f, 0.81f);

    }


    @Test
    void multiple_variable_elimination3() {

        TestNAR tester = test;
        tester.believe("(&&,<#x --> lock>,(<$y --> key> ==> open($y,#x)))");
        tester.believe("<{lock1} --> lock>");
        tester.mustBelieve(cycles * 2, "<<$1 --> key> ==> open($1,{lock1})>", 1.00f,

                0.43f);

    }


    @Test
    void multiple_variable_elimination4() {

        TestNAR tester = test;
        tester.believe("(&&,open(#y,#x),<#x --> lock>,<#y --> key>)");
        tester.believe("({lock1} --> lock)");
        tester.mustBelieve(cycles, "(<#1 --> key> && open(#1,{lock1}))",
                1.00f,


                0.43f
        );
    }


    @Test
    void variable_introduction() {

        TestNAR tester = test;

        tester.believe("<swan --> bird>");
        tester.believe("<swan --> swimmer>", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "<<$1 --> swimmer> ==> <$1 --> bird>>", 1.00f, 0.39f);
        tester.mustBelieve(cycles, "<<$1 --> bird> ==> <$1 --> swimmer>>", 0.80f, 0.45f);

        tester.mustBelieve(cycles, "(&&, <#1 --> swimmer>, <#1 --> bird>)", 0.80f, 0.81f);

    }

    @Test
    void variable_introduction_with_existing_vars() {


        TestNAR tester = test;
        tester.believe("<swan --> <#1 --> birdlike>>");
        tester.believe("<swan --> swimmer>", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "<<$1 --> <#2 --> birdlike>> ==> <$1 --> swimmer>>", 0.80f, 0.45f);
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
        tester.believe("<gull --> swimmer>");
        tester.believe("<swan --> swimmer>", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "<<gull --> $1> ==> <swan --> $1>>", 0.80f, 0.45f);
        tester.mustBelieve(cycles, "<<swan --> $1> ==> <gull --> $1>>", 1.00f, 0.39f);

        tester.mustBelieve(cycles, "(&&,<gull --> #1>,<swan --> #1>)", 0.80f, 0.81f);
    }

    @Test
    @Disabled
    void variable_introduction3() {

        TestNAR tester = test;
        tester.believe("<gull --> swimmer>", 1f, 0.9f);
        tester.believe("<swan --> swimmer>", 0f, 0.9f);
        tester.mustBelieve(cycles, "(&&,<gull --> #1>,<swan --> #1>)", 0.0f, 0.81f);
        tester.mustBelieve(cycles, "(&&,<gull --> #1>,(--,<swan --> #1>))", 1.0f, 0.81f);


    }

    @Test
    void variable_introduction_with_existing_vars2() {


        TestNAR tester = test;

        tester.believe("<#1 --> swimmer>");
        tester.believe("<swan --> swimmer>", 0.80f, 0.9f);

        tester.mustBelieve(cycles, "<<#1 --> $2> ==> <swan --> $2>>", 0.80f, 0.45f);
        tester.mustBelieve(cycles, "<<swan --> $1> ==> <#2 --> $1>>", 1.00f, 0.39f);

        tester.mustBelieve(cycles, "(&&,<#1 --> #2>,<swan --> #2>)", 0.80f, 0.81f);
    }

    @Test
    void variables_introduction() {

        test
                .believe("open({key1},{lock1})")
                .believe("key:{key1}")
                .mustBelieve(cycles, "(key:$1 ==> open($1,{lock1}))", 1.00f, 0.45f)
                .mustBelieve(cycles, "(&&,open(#1,{lock1}),key:#1)", 1.00f, 0.81f);

    }

    @Test
    void testConjunctionContradictionInduction() {

        test
                .believe("((x && y) ==> z)")
                .believe("((x && --y) ==> z)")
                .mustBelieve(cycles, "(x ==> z)", 1.00f, 0.45f);

    }

    @Test
    void multiple_variables_introduction() {

        TestNAR tester = test;

        tester.believe("(key:$x ==> open($x,lock1))");
        tester.believe("lock:lock1");

        tester.mustBelieve(cycles*2, "((key:$1 && lock:$2) ==> open($1,$2))",
                1.00f, 0.81f);


    }


    @Test
    void multiple_variables_introduction2() {

        TestNAR tester = test;
        tester.believe("(key:#x && open(#x,{lock1}))");
        tester.believe("lock:{lock1}");

        tester.mustBelieve(cycles, "(&&, key:#1, lock:#2, open(#1,#2))", 1.00f, 0.81f);


        tester.mustBelieve(cycles, "(lock:$1 ==> (key:#2 && open(#2,$1)))", 1.00f, 0.45f);

    }


    @Test
    void second_level_variable_unificationNoImgAndAsPrecondition() {

        TestNAR tester = test;
        tester.believe("((<#1 --> lock>&&<$2 --> key>) ==> open(#1,$2))", 1.00f, 0.90f);


        tester.believe("<{key1} --> key>", 1.00f, 0.90f);
        tester.mustBelieve(cycles , "((#1-->lock)==>open(#1,{key1}))", 1.00f,
                0.73f
                /*0.81f*/);
    }

    @Test
    void second_level_variable_unification() {

        TestNAR tester = test;
        tester.believe("(((#1 --> lock) && ($2 --> key)) ==> open($2, #1))", 1.00f, 0.90f);
        tester.believe("({key1} --> key)", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "((#1 --> lock) && open({key1}, #1))", 1.00f, 0.81f);
    }

    @Test
    void second_level_variable_unification_neg() {

        TestNAR tester = test;
        tester.believe("(((#1 --> lock) && --($2 --> key)) ==> open($2, #1))");
        tester.believe("--({key1} --> key)");
        tester.mustBelieve(cycles, "((#1 --> lock) && open({key1}, #1))", 1.00f, 0.81f);
    }


    @Test
    void second_level_variable_unification2() {

        TestNAR tester = test;
        tester.believe("<<$1 --> lock> ==> (&&,<#2 --> key>,open(#2,$1))>", 1.00f, 0.90f);
        tester.believe("<{key1} --> key>", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "<<$1 --> lock> ==> open({key1},$1)>", 1.00f,
                //0.73f
                0.43f);

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

        tester.believe("(open($1,lock1) ==> key:$1)");
        tester.believe("open(lock,lock1)");
        tester.mustBelieve(cycles*2,
                "((open(lock,#1) && open($2,#1)) ==> key:$2)",
                1.00f, 0.81f);

    }


    @Test
    void variable_elimination_deduction() {

        test
                .believe("((&&,(#1 --> lock),open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .believe("(lock1 --> lock)", 1.00f, 0.90f)
                .mustBelieve(cycles, "(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.73f);
    }

    @Test
    void variable_elimination_deduction_neg() {

        test
                .believe("((&&, --(#1 --> lock), open($2,#1)) ==> ($2 --> key))")
                .believe("--(lock1 --> lock)")
                .mustBelieve(cycles, "(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.73f);
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

        TestNAR tester = test;
        tester.believe("( --sentence($a,is,$b) ==> ($a --> $b) )", 1.00f, 0.90f);
        tester.believe("sentence(bmw,is,car)", 0.00f, 0.90f);
        tester.mustBelieve(cycles, "car:bmw", 1.00f, 0.81f);

    }

    @Test
    void strong_elimination() {

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
    @Test
    void testDecomposeImplSubjDisjBelief() {
        test
                .believe("( (||, y, z) ==> x )")
                .mustBelieve(cycles, "( y ==> x )", 1f, 0.81f)
                .mustBelieve(cycles, "( z ==> x )", 1f, 0.81f)
        ;
    }

    @Test
    void testDecomposeImplSubjConjQuestion() throws Narsese.NarseseException {
        test
                .ask("( (&&, y, z) ==> x )")
                .mustOutput(cycles, "( y ==>+- x )", QUESTION)
                .mustOutput(cycles, "( z ==>+- x )", QUESTION)
        ;
    }

    @Test
    void testDecomposeImplSubjDisjQuestion() throws Narsese.NarseseException {
        test
                .ask("( (||, y, z) ==> x )")
                .mustOutput(cycles, "( y ==>+- x )", QUESTION)
                .mustOutput(cycles, "( z ==>+- x )", QUESTION)
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

    @Test
    void testDecomposeImplPredDisjBelief() {
        test
                .believe("( x ==> (||, y, z))")
                .mustBelieve(cycles, "( x ==> y )", 1f, 0.81f)
                .mustBelieve(cycles, "( x ==> z )", 1f, 0.81f)
        ;
    }

    @Test
    void testDecomposeImplPredConjQuestion() throws Narsese.NarseseException {
        test
                .ask("( x ==> (&&, y, z) )")
                .mustOutput(cycles, "( x ==>+- y )", QUESTION)
                .mustOutput(cycles, "( x ==>+- z )", QUESTION)
        ;
    }

    @Test
    void testDecomposeImplPredDisjQuestion() throws Narsese.NarseseException {
        test
                .ask("( x ==> (||, y, z) )")
                .mustOutput(cycles, "( x ==>+- y )", QUESTION)
                .mustOutput(cycles, "( x ==>+- z )", QUESTION)
        ;
    }

    @Test
    void testDecomposeImplSubj1b() {
        test.confTolerance(0.03f)
                .believe("( (&&, y, z, w) ==> x )")
                .mustBelieve(cycles, "( y ==> x )", 1f, 0.73f)
                .mustBelieve(cycles, "( z ==> x )", 1f, 0.73f)
                .mustBelieve(cycles, "( w ==> x )", 1f, 0.73f)
        ;
    }

    @Test
    void testDecomposeImplSubj1bNeg() {
        test.confTolerance(0.03f)
                .believe("( (&&, --y, --z, --w) ==> x )")
                .mustBelieve(cycles, "( --y ==> x )", 1f, 0.73f)
                .mustBelieve(cycles, "( --z ==> x )", 1f, 0.73f)
                .mustBelieve(cycles, "( --w ==> x )", 1f, 0.73f)
        ;
    }

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
    void recursionSmall() throws nars.Narsese.NarseseException {


        test
                .believe("num:x", 1.0f, 0.9f)
                .believe("( num:$1 ==> num($1) )", 1.0f, 0.9f)
                //.ask("num(((x)))")
                .mustBelieve(cycles, "num(x)", 1.0f, 1.0f, 0.81f, 1.0f)
                .mustBelieve(cycles*2, "num((x))", 0.99f, 1.0f, 0.50f, 1.0f)
                .mustBelieve(cycles*3, "num(((x)))", 0.99f, 1.0f, 0.25f, 1.0f)


        ;
    }

    @Test
    void recursionSmall1() throws nars.Narsese.NarseseException {


        test.nar.freqResolution.set(0.1f);
        test
                .believe("num(x)", 1.0f, 0.9f)
                .believe("( num($1) ==> num(($1)) )", 1.0f, 0.9f)
                .ask("num(((x)))")
                .mustBelieve(cycles * 2, "num((x))", 1.0f, 1.0f, 0.8f, 1.0f)
                .mustBelieve(cycles * 2, "num(((x)))", 1.0f, 1.0f, 0.1f /*0.66f*/, 1.0f);


    }


    @Test
    void testRawProductDifference() {
        test
                .believe("(x,0)", 1f, 0.9f)
                .believe("(x,1)", 0.5f, 0.9f)
                .mustBelieve(cycles, "((x,1)~(x,0))", 0.0f, 0.81f, ETERNAL)
                .mustBelieve(cycles, "((x,0)~(x,1))", 0.5f, 0.81f, ETERNAL);
    }

}
