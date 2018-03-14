package nars.nal.nal5;

import nars.NAR;
import nars.NARS;
import nars.test.TestNAR;
import nars.util.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

//@RunWith(Parameterized.class)
public class NAL5Test extends NALTest {

    final int cycles = 3000;

    @Override
    protected NAR nar() {
        return NARS.tmp(6);
    }


    @Test
    public void revision() {

        test
                .mustBelieve(cycles, "<<robin --> [flying]> ==> <robin --> bird>>", 0.86f, 0.91f) //.en("If robin can fly then robin is a type of bird.");
                .believe("<<robin --> [flying]> ==> <robin --> bird>>") //.en("If robin can fly then robin is a type of bird.");
                .believe("<<robin --> [flying]> ==> <robin --> bird>>", 0.00f, 0.60f); //.en("If robin can fly then robin may not a type of bird.");

    }

    @Test
    public void deduction() {

        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); //.en("If robin is a type of bird then robin is a type of animal.");
        tester.believe("<<robin --> [flying]> ==> <robin --> bird>>"); //.en("If robin can fly then robin is a type of bird.");
        tester.mustBelieve(cycles, "<<robin --> [flying]> ==> <robin --> animal>>", 1.00f, 0.81f); //.en("If robin can fly then robin is a type of animal.");

    }

    @Test
    public void exemplification() {

        TestNAR tester = test;
        tester.believe("<<robin --> [flying]> ==> <robin --> bird>>"); //.en("If robin can fly then robin is a type of bird.");
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); //.en("If robin is a type of bird then robin is a type of animal.");
        tester.mustBelieve(cycles, "<<robin --> animal> ==> <robin --> [flying]>>.", 1.00f, 0.45f); //.en("I guess if robin is a type of animal then robin can fly.");

    }


    @Test
    public void induction() {
        /*
         <<robin --> bird> ==> <robin --> animal>>. // If robin is a type of bird then robin is a type of animal.
         <<robin --> [flying]> ==> <robin --> animal>>. %0.8%  // If robin can fly then robin is probably a type of animal.
         OUT: <<robin --> bird> ==> <robin --> [flying]>>. %1.00;0.39% // I guess if robin is a type of bird then robin can fly.
         OUT: <<robin --> [flying]> ==> <robin --> bird>>. %0.80;0.45% // I guess if robin can fly then robin is a type of bird.
         */
        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); //.en("If robin is a type of bird then robin is a type of animal.");
        tester.believe("<<robin --> [flying]> ==> <robin --> animal>>", 0.8f, 0.9f); //.en("If robin can fly then robin is probably a type of animal.");
        tester.mustBelieve(cycles, "<<robin --> bird> ==> <robin --> [flying]>>", 1.00f, 0.39f); //.en("I guess if robin is a type of bird then robin can fly.");
        tester.mustBelieve(cycles, "<<robin --> [flying]> ==> <robin --> bird>>", 0.80f, 0.45f); //.en("I guess if robin can fly then robin is a type of bird.");

    }

//    @Test
//    public void inductionNegNeg() {
//        TestNAR tester = test;
//        tester.believe("<<robin --> bird> ==> --<robin --> cyborg>>"); //.en("If robin is a type of bird then robin is not of type cyborg.");
//        tester.believe("<<robin --> [flying]> ==> --<robin --> cyborg>>", 0.8f, 0.9f); //.en("If robin can fly then robin is probably not of type cyborg.");
//        tester.mustBelieve(cycles, "<<robin --> bird> ==> <robin --> [flying]>>", 1.00f, 0.39f); //.en("I guess if robin is a type of bird then robin can fly.");
//        tester.mustBelieve(cycles, "<<robin --> [flying]> ==> <robin --> bird>>", 0.80f, 0.45f); //.en("I guess if robin can fly then robin is a type of bird.");
//
//    }

    @Test
    public void abduction() {

        /*
        <<robin --> bird> ==> <robin --> animal>>.         // If robin is a type of bird then robin is a type of animal.
        <<robin --> bird> ==> <robin --> [flying]>>. %0.80%  // If robin is a type of bird then robin can fly.
        14
         OUT: <<robin --> [flying]> ==> <robin --> animal>>. %1.00;0.39% // I guess if robin can fly then robin is a type of animal.
         OUT: <<robin --> animal> ==> <robin --> [flying]>>. %0.80;0.45% // I guess if robin is a type of animal then robin can fly.
         */
        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); //.en("If robin is a type of bird then robin is a type of animal.");
        tester.believe("<<robin --> bird> ==> <robin --> [flying]>>", 0.8f, 0.9f); //.en("If robin is a type of bird then robin can fly.");
        tester.mustBelieve(cycles, "<<robin --> [flying]> ==> <robin --> animal>>", 1.00f, 0.39f); //.en("I guess if robin can fly then robin is a type of animal.");
        tester.mustBelieve(cycles, "<<robin --> animal> ==> <robin --> [flying]>>", 0.80f, 0.45f); //.en("I guess if robin is a type of animal then robin can fly.");


    }


    @Test
    public void testImplBeliefPosPos() {
        //B, (A ==> C), belief(positive) |- subIfUnifiesAny(C,A,B), (Belief:Deduction, Goal:Induction)

        test
                //.log()
                .believe("(b)")
                .believe("((b)==>(c))", 1, 0.9f)
                .mustBelieve(cycles, "(c)", 1.00f, 0.81f);
    }
//    @Test public void testImplBeliefNegPos() {
//        //B, (A ==> C), belief(positive) |- subIfUnifiesAny(C,A,B), (Belief:Deduction, Goal:Induction)
//        test()
//                .log()
//                .believe("(--,(b))")
//                .believe("((b)==>(c))",1,0.9f)
//                .mustNotOutput(cycles,"(c)",BELIEF);
//    }

    @Test
    public void testImplBeliefPosNeg() {
        //B, (A ==> C), belief(negative) |- subIfUnifiesAny(C,A,B), (Belief:Deduction, Goal:Induction)

        test
                .believe("(b)")
                .believe("((b) ==> --(c))", 1, 0.9f)
                .mustBelieve(cycles, "(c)", 0.00f, 0.81f);
    }

    @Test
    public void detachment() {

        test
                .believe("<<robin --> bird> ==> <robin --> animal>>") //.en("If robin is a type of bird then robin can fly.");
                .believe("<robin --> bird>") //.en("Robin is a type of bird.");
                .mustBelieve(cycles, "<robin --> animal>", 1.00f, 0.81f); //.en("Robin is a type of animal.");

    }


    @Test
    public void detachment2() {

        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>", 0.70f, 0.90f); //.en("Usually if robin is a type of bird then robin is a type of animal.");
        tester.believe("<robin --> animal>"); //.en("Robin is a type of animal.");
        tester.mustBelieve(cycles, "<robin --> bird>",
                0.7f, 0.45f); //.en("I guess robin is a type of bird.");
        //0.7f, 0.9f);
        //1.00f, 0.36f);

    }


    //    @Test
//    public void comparisonNegNeg(){
//        TestNAR tester = test();
//        tester.log();
//        tester.believe("<(x) ==> (z)>", 0.0f, 0.9f);
//        tester.believe("<(y) ==> (z)>", 0.0f, 0.9f);
//        tester.mustBelieve(cycles,"<(x) <=> (y)>", ????
//    }

    @Test
    public void anonymous_analogy1_depvar() {
        test
                .believe("(a:#1 && y)")
                .believe("a:x", 0.80f, 0.9f)
                .mustBelieve(cycles * 2, "y", 0.80f, 0.43f);
    }

    @Test
    public void anonymous_analogy1_pos2() {
        test
                .believe("(x && y)")
                .believe("x", 0.80f, 0.9f)
                .mustBelieve(cycles * 2, "y", 0.80f, 0.43f);
    }

    @Test
    public void anonymous_analogy1_pos3() {
        test
                .believe("(&&, x, y, z)")
                .believe("x", 0.80f, 0.9f)
                .mustBelieve(cycles, "(y && z)", 0.80f, 0.43f);
    }

    @Test
    public void anonymous_analogy1_neg2() {
        test
                .believe("(&&, --x, y, z)")
                .believe("x", 0.20f, 0.9f)
                .mustBelieve(cycles, "(&&,y,z)", 0.80f, 0.43f /*0.43f*/);
    }

    @Test
    public void compound_composition_Pred() {

        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); //.en("If robin is a type of bird then robin is a type of animal.");
        tester.believe("<<robin --> bird> ==> <robin --> [flying]>>", 0.9f, 0.9f); //.en("If robin is a type of bird then robin can fly.");
        tester.mustBelieve(cycles, " <<robin --> bird> ==> (&&,<robin --> [flying]>,<robin --> animal>)>", 0.90f, 0.81f); //.en("If robin is a type of bird then usually robin is a type of animal and can fly.");
        //tester.mustBelieve(cycles,
        //"((robin-->bird)==>((--,(robin-->animal))&&(--,(robin-->[flying]))))", 0f, 0.81f);
        //" <<robin --> bird> ==> (||,<robin --> [flying]>,<robin --> animal>)>",1.00f,0.81f); //.en("If robin is a type of bird then robin is a type of animal or can fly.");

    }


    @Test
    public void compound_composition_Subj() {

        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); //.en("If robin is a type of bird then robin is a type of animal.");
        tester.believe("<<robin --> [flying]> ==> <robin --> animal>>", 0.9f, 0.81f); //.en("If robin can fly then robin is a type of animal.");
        tester.mustBelieve(cycles, " <(&&,<robin --> bird>, <robin --> [flying]>) ==> <robin --> animal>>", 0.9f, 0.73f); //.en("If robin can fly and is a type of bird then robin is a type of animal.");
        //tester.mustBelieve(cycles," <(||,<robin --> bird>, <robin --> [flying]>) ==> <robin --> animal>>",0.9f /*1f ? */,0.73f); //.en("If robin can fly or is a type of bird then robin is a type of animal.");

    }

//    @Test
//    public void compound_composition_SubjPosNeg() {
//        test
//            .believe("<<robin --> bird> ==> <robin --> animal>>") //.en("If robin is a type of bird then robin is a type of animal.")
//            .believe("--<<robin --> [alien]> ==> <robin --> animal>>") //.en("If robin is alien then robin isnt a type of animal.");
//            .mustBelieve(cycles, " <(&&,<robin --> bird>, --<robin --> [alien]>) ==> <robin --> animal>>", 1f, 0.81f); //.en("If robin isnt alien and is a type of bird then robin is a type of animal.");
//    }

    @Test
    public void compound_decomposition_two_premises1() {

        TestNAR tester = test;
        tester.believe("--(bird:robin ==> (animal:robin && [flying]:robin))", 1.0f, 0.9f); //.en("If robin is a type of bird then robin is not a type of flying animal.");
        tester.believe("(bird:robin ==> [flying]:robin)"); //.en("If robin is a type of bird then robin can fly.");
        tester.mustBelieve(cycles * 2, "--(bird:robin ==> animal:robin)", 1.00f, 0.81f); //.en("It is unlikely that if a robin is a type of bird then robin is a type of animal.");

    }

    @Test
    public void compound_decomposition_subj_posneg() {
        test.believe("((b && --c)==>a)", 1.0f, 0.9f)
                .mustBelieve(cycles, "(b==>a)", 1.00f, 0.81f)
                .mustBelieve(cycles, "(--c==>a)", 1.00f, 0.81f);
    }

    @Test
    public void compound_decomposition_pred_posneg() {

        test.believe("(a==>(b && --c))", 1.0f, 0.9f)
                .mustBelieve(cycles, "(a==>b)", 1.00f, 0.81f)
                .mustBelieve(cycles, "(a==>c)", 0.00f, 0.81f);
    }

    @Test
    public void compound_decomposition_one_premise_pos() {

        TestNAR tester = test;

        tester.believe("(<robin --> [flying]> && <robin --> swimmer>)", 1.0f, 0.9f); //.en("Robin cannot be both a flyer and a swimmer.");
        tester.mustBelieve(cycles, "<robin --> swimmer>", 1.00f, 0.81f); //.en("Robin cannot swim.");
    }

    @Test
    public void compound_decomposition_one_premise_neg() {
        //freq 0 conjunction would not be decomposed

        TestNAR tester = test;
        tester.believe("(&&,<robin --> [flying]>,<robin --> swimmer>)", 0.0f, 0.9f); //.en("Robin cannot be both a flyer and a swimmer.");
        tester.mustNotOutput(cycles, "<robin --> swimmer>", BELIEF, ETERNAL); //.en("Robin cannot swim.");
    }


    @Test
    public void compound_decomposition_two_premises3() {

        TestNAR tester = test;
        tester.believe("(||,<robin --> [flying]>,<robin --> swimmer>)"); //.en("Robin can fly or swim.");
        tester.believe("<robin --> swimmer>", 0.0f, 0.9f); //.en("Robin cannot swim.");
        tester.mustBelieve(cycles, "<robin --> [flying]>", 1.00f, 0.81f); //.en("Robin can fly.");

    }


    /**
     * not sure this one makes logical sense
     */
    @Disabled
    @Test
    public void compound_composition_one_premises() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<robin --> [flying]>"); //.en("Robin can fly.");
        tester.ask("(||,<robin --> [flying]>,<robin --> swimmer>)"); //.en("Can robin fly or swim?");
        //tester.mustBelieve(cycles*2," (||,<robin --> swimmer>,<robin --> [flying]>)",1.00f,0.81f); //.en("Robin can fly or swim.");
        tester.mustBelieve(cycles * 2, " (&&,(--,<robin --> swimmer>),(--,<robin --> [flying]>))", 0.00f, 0.81f); //.en("Robin can fly or swim.");
    }


    //    static {
//        Param.TRACE = true;
//    }
    @Test
    public void compound_decomposition_one_premises() {

        test
                //.log()
                .believe("(&&,<robin --> swimmer>,<robin --> [flying]>)", 0.9f, 0.9f) //.en("Robin can fly and swim.");
                .mustBelieve(cycles, "<robin --> swimmer>", 0.9f, 0.73f) //.en("Robin can swim.");
                .mustBelieve(cycles, "<robin --> [flying]>", 0.9f, 0.73f); //.en("Robin can fly.");

    }
//    @Test public void compound_decomposition_one_premises_2(){
//        //TODO, mirroring: compound_decomposition_one_premises
////        TestNAR tester = test();
////        tester.believe("(||,<robin --> swimmer>,<robin --> [flying]>)",0.9f,0.9f); //.en("Robin can fly and swim.");
////        tester.mustBelieve(cycles*4,"<robin --> swimmer>", ..); //.en("Robin can swim.");
////        tester.mustBelieve(cycles*4,"<robin --> [flying]>", ..); //.en("Robin can fly.");
////        tester.run();
//    }


    @Test
    public void negation0() {

        test
                .mustBelieve(cycles, "<robin --> [flying]>", 0.10f, 0.90f) //.en("Robin can fly.");
                .believe("(--,<robin --> [flying]>)", 0.9f, 0.9f); //.en("It is unlikely that robin cannot fly.");


    }

    @Test
    public void negation1() {

        test
                .mustBelieve(cycles, "<robin <-> parakeet>", 0.10f, 0.90f)
                .believe("(--,<robin <-> parakeet>)", 0.9f, 0.9f);


    }

//    @Disabled
//    @Test
//    public void negation2() throws nars.Narsese.NarseseException {
//
//        TestNAR tester = test;
//        tester.believe("<robin --> [flying]>", 0.9f, 0.9f); //.en("Robin can fly.");
//        tester.ask("(--,<robin --> [flying]>)"); //.en("Can robin fly or not?");
//        tester.mustBelieve(cycles, "(--,<robin --> [flying]>)", 0.10f, 0.90f); //.en("It is unlikely that robin cannot fly.");
//
//    }


    @Test
    public void contraposition1() throws nars.Narsese.NarseseException {

        TestNAR tester = test;

        /*
        IN:   <(--,<robin --> bird>) ==> <robin --> [flying]>>. %0.1%
                      // It is unlikely that if robin is not a type of bird then robin can fly.
        IN:   <(--,<robin --> [flying]>) ==> <robin --> bird>>?
                      // If robin cannot fly then is robin a type of bird?

         OUT: <(--,<robin --> [flying]>) ==> <robin --> bird>>. %0.00;0.45%
                      // I guess it is unlikely that if robin cannot fly then robin is a type of bird.
         */
        tester.believe("<(--,<robin --> bird>) ==> <robin --> [flying]>>", 0.1f, 0.9f); //.en("It is unlikely that if robin is not a type of bird then robin can fly.");
        tester.ask("<(--,<robin --> [flying]>) ==> <robin --> bird>>"); //.en("If robin cannot fly then is robin a type of bird ? ");
        tester.mustBelieve(cycles, " <(--,<robin --> [flying]>) ==> <robin --> bird>>", 0f, 0.45f); //.en("I guess it is unlikely that if robin cannot fly then robin is a type of bird.");

    }


    @Test
    public void conditional_deduction() {

        TestNAR tester = test;
        tester.believe("<(&&,<robin --> [flying]>,<robin --> [withWings]>) ==> <robin --> bird>>"); //.en("If robin can fly and has wings then robin is a bird.");
        tester.believe("<robin --> [flying]>"); //.en("robin can fly.");
        tester.mustBelieve(cycles, " <<robin --> [withWings]> ==> <robin --> bird>>", 1.00f, 0.81f); //.en("If robin has wings then robin is a bird");

    }

    @Test
    public void conditional_deduction_neg() {

        TestNAR tester = test;
        tester.believe("<(&&,--<robin --> [swimming]>,<robin --> [withWings]>) ==> <robin --> bird>>"); //.en("If robin can fly and has wings then robin is a bird.");
        tester.believe("--<robin --> [swimming]>"); //.en("robin can fly.");
        tester.mustBelieve(cycles, " <<robin --> [withWings]> ==> <robin --> bird>>", 1.00f, 0.81f); //.en("If robin has wings then robin is a bird");

    }


    @Test
    public void conditional_deduction2() {

        TestNAR tester = test;
        //tester.log();
        tester.believe("<(&&,<robin --> [chirping]>,<robin --> [flying]>,<robin --> [withWings]>) ==> <robin --> bird>>"); //.en("If robin can fly, has wings, and chirps, then robin is a bird");
        tester.believe("<robin --> [flying]>"); //.en("robin can fly.");
        tester.mustBelieve(cycles * 2, " <(&&,<robin --> [chirping]>,<robin --> [withWings]>) ==> <robin --> bird>>", 1.00f, 0.81f); //.en("If robin has wings and chirps then robin is a bird.");

    }


    @Test
    public void conditional_deduction3() {

        TestNAR tester = test;
        tester.believe("<(&&,<robin --> bird>,<robin --> [living]>) ==> <robin --> animal>>"); //.en("If robin is a bird and it's living, then robin is an animal");
        tester.believe("<<robin --> [flying]> ==> <robin --> bird>>"); //.en("If robin can fly, then robin is a bird");
        tester.mustBelieve(cycles * 2, " <(&&,<robin --> [flying]>,<robin --> [living]>) ==> <robin --> animal>>", 1.00f, 0.81f); //.en("If robin is living and it can fly, then robin is an animal.");

    }


    @Test
    public void conditional_abduction_viaMultiConditionalSyllogism() {
        //((&&,M,A_1..n) ==> C), ((&&,A_1..n) ==> C) |- M, (Truth:Abduction, Order:ForAllSame)

        TestNAR tester = test;
        //tester.log();
        tester.believe("([flying]:robin ==> bird:robin)"); //.en("If robin can fly then robin is a bird.");
        tester.believe("((swimmer:robin && [flying]:robin) ==> bird:robin)"); //.en("If robin both swims and flys then robin is a bird.");
        tester.mustBelieve(cycles * 4, "swimmer:robin", 1.00f, 0.45f /*0.4f*/); //.en("I guess robin swims.");

    }

    @Test
    public void conditional_abduction_viaMultiConditionalSyllogismEasier() {
        //((&&,M,A_1..n) ==> C), ((&&,A_1..n) ==> C) |- M, (Truth:Abduction, Order:ForAllSame)

        TestNAR tester = test;
        //tester.log();
        tester.believe("(flyingrobin ==> birdrobin)"); //.en("If robin can fly then robin is a bird.");
        tester.believe("((swimmerrobin && flyingrobin) ==> birdrobin)"); //.en("If robin both swims and flys then robin is a bird.");
        tester.mustBelieve(cycles * 2, "swimmerrobin", 1.00f, 0.45f /*0.4f*/); //.en("I guess robin swims.");

    }

    @Test
    public void conditional_abduction2_viaMultiConditionalSyllogism() {
        //((&&,M,A_1..n) ==> C), ((&&,A_1..n) ==> C) |- M, (Truth:Abduction, Order:ForAllSame)

        test
                //.log()
                .believe("<(&&,<robin --> [withWings]>,<robin --> [chirping]>) ==> <robin --> bird>>") //.en("If robin is has wings and chirps, then robin is a bird")
                .believe("<(&&,<robin --> [flying]>,<robin --> [withWings]>,<robin --> [chirping]>) ==> <robin --> bird>>") //.en("If robin can fly, has wings, and chirps, then robin is a bird");
                .mustBelieve(cycles, "<robin --> [flying]>",
                        1.00f, 0.45f
                ) //.en("I guess that robin can fly.");
                .mustNotOutput(cycles, "<robin --> [flying]>", BELIEF, 0f, 0.5f, 0, 1, ETERNAL);
    }


    //    @Test
//    public void conditional_abduction3_semigeneric(){
//        TestNAR tester = test();
//        tester.believe("<(&&,<robin --> [f]>,<robin --> [w]>) ==> <robin --> [l]>>",0.9f,0.9f);
//        tester.believe("<(&&,<robin --> [f]>,<robin --> b>) ==> <robin --> [l]>>");
//        tester.mustBelieve(cycles*2,"<<robin --> b> ==> <robin --> [w]>>",1.00f,0.42f);
//        tester.mustBelieve(cycles*2,"<<robin --> [w]> ==> <robin --> b>>",0.90f,0.45f);
//    }
    @Test
    public void conditional_abduction3_semigeneric2() {

        TestNAR tester = test;
        tester.believe("<(&&,<ro --> [f]>,<ro --> [w]>) ==> <ro --> [l]>>", 0.9f, 0.9f);
        tester.believe("<(&&,<ro --> [f]>,<ro --> b>) ==> <ro --> [l]>>");
        tester.mustBelieve(cycles * 2, "<<ro --> b> ==> <ro --> [w]>>", 1.00f, 0.42f);
        tester.mustBelieve(cycles * 2, "<<ro --> [w]> ==> <ro --> b>>", 0.90f, 0.45f);
    }


    @Test
    public void conditional_abduction3_semigeneric3() {

        TestNAR tester = test;
        tester.believe("<(&&,<R --> [f]>,<R --> [w]>) ==> <R --> [l]>>", 0.9f, 0.9f);
        tester.believe("<(&&,<R --> [f]>,<R --> b>) ==> <R --> [l]>>");
        tester.mustBelieve(cycles * 4, "<<R --> b> ==> <R --> [w]>>", 1f, 0.42f /*0.36f*/);
        tester.mustBelieve(cycles * 4, "<<R --> [w]> ==> <R --> b>>", 0.90f, 0.45f);
    }

    @Test
    public void conditional_abduction3() {

        TestNAR tester = test;
        tester.believe("<(&&,<robin --> [flying]>,<robin --> [withWings]>) ==> <robin --> [living]>>", 0.9f, 0.9f); //.en("If robin can fly and it has wings, then robin is living.");
        tester.believe("<(&&,<robin --> [flying]>,<robin --> bird>) ==> <robin --> [living]>>"); //.en("If robin can fly and robin is a bird then robin is living.");
        tester.mustBelieve(cycles * 2, "<<robin --> bird> ==> <robin --> [withWings]>>",
                //0.90f,0.45f);
                1.00f, 0.42f); //.en("I guess if robin is a bird, then robin has wings.");
        tester.mustBelieve(cycles * 2, "<<robin --> [withWings]> ==> <robin --> bird>>",
                0.90f, 0.45f); //.en("I guess if robin has wings, then robin is a bird.");

    }


    @Test
    public void conditional_abduction3_generic() {

        TestNAR tester = test;
        tester.believe("<(&&,<r --> [f]>,<r --> [w]>) ==> <r --> [l]>>", 0.9f, 0.9f);
        tester.believe("<(&&,<r --> [f]>,<r --> b>) ==> <r --> [l]>>");
        tester.mustBelieve(cycles * 4, "<<r --> b> ==> <r --> [w]>>", 1f, 0.42f);
        tester.mustBelieve(cycles * 4, "<<r --> [w]> ==> <r --> b>>", 0.90f, 0.45f);
    }

    @Test
    public void conditional_induction() {

        TestNAR tester = test;
        tester.believe("<(&&,<robin --> [chirping]>,<robin --> [flying]>) ==> <robin --> bird>>"); //.en("If robin can fly and robin chirps, then robin is a bird");
        tester.believe("<<robin --> [flying]> ==> <robin --> [withBeak]>>", 0.9f, 0.9f); //.en("If robin can fly then usually robin has a beak.");
        tester.mustBelieve(cycles, "<(&&,<robin --> [chirping]>,<robin --> [withBeak]>) ==> <robin --> bird>>", 1.00f, 0.42f); //.en("I guess that if robin chirps and robin has a beak, then robin is a bird.");

    }

    @Test
    public void conditional_induction0Simple() {
        TestNAR tester = test;
        tester.believe("((&&,x1,a) ==> c)");
        tester.believe("((&&,y1,a) ==> c)");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    public void conditional_induction0SimpleDepVar() {
        TestNAR tester = test;
        tester.believe("((&&,x1,#1) ==> c)");
        tester.believe("((&&,y1,#1) ==> c)");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    public void conditional_induction0SimpleDepVar2() {
        TestNAR tester = test;
        tester.believe("((&&,x1,#1) ==> (a && #1))");
        tester.believe("((&&,y1,#1) ==> (a && #1))");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    public void conditional_induction0SimpleDepVar3() {
        TestNAR tester = test;
        tester.believe("((&&,x1,#1) ==> (a && #1))");
        tester.believe("((&&,#1,#2) ==> (a && #2))");
        tester.mustBelieve(cycles, "(x1 ==> #1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(#1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    public void conditional_induction0SimpleIndepVar() {
        TestNAR tester = test;
        tester.believe("((&&,x1,$1) ==> (a,$1))");
        tester.believe("((&&,y1,$1) ==> (a,$1))");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    public void conditional_induction0() {
        ////((&&,M,A..+) ==> C), ((&&,B,A..+)==>C)   |- (B ==>+- M), (Belief:Induction)

        TestNAR tester = test;
        tester.believe("((&&,x1,x2,a) ==> c)");
        tester.believe("((&&,y1,y2,a) ==> c)");
        tester.mustBelieve(cycles, "((x1&&x2) ==> (y1&&y2))", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "((y1&&y2) ==> (x1&&x2))", 1.00f, 0.45f);
    }

    @Test
    public void conditional_induction0NegBothSimple() {
        TestNAR tester = test;
        tester.believe("--((x&&a) ==> c)");
        tester.believe("--((x&&b) ==> c)");
        tester.mustBelieve(cycles, "(a ==> b)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(b ==> a)", 1.00f, 0.45f);
    }

    @Test
    public void conditional_induction0NegBoth() {
        TestNAR tester = test;
        tester.believe("--((&&,x1,x2,a) ==> c)");
        tester.believe("--((&&,y1,y2,a) ==> c)");
        tester.mustBelieve(cycles, "((x1&&x2) ==> (y1&&y2))", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "((y1&&y2) ==> (x1&&x2))", 1.00f, 0.45f);
    }

    @Test
    public void conditional_induction0NegInner() {
        TestNAR tester = test;
        tester.believe("((x&&a) ==> c)");
        tester.believe("(--(x&&b) ==> c)");
        tester.mustBelieve(cycles, "(a ==> b)", 0.00f, 0.45f);
        tester.mustBelieve(cycles, "(--b ==> a)", 1.00f, 0.45f);
    }

    /* will be moved to NAL multistep test file!!
    //this is a multistep example, I will add a special test file for those with Default configuration
    //it's not the right place here but the example is relevant
    @Test public void deriveFromConjunctionComponents() { //this one will work after truthfunctions which allow evidental base overlap are allowed
        TestNAR tester = test();
        tester.believe("(&&,<a --> b>,<b-->a>)", Eternal, 1.0f, 0.9f);

        //TODO find the actual value for these intermediate steps, i think it is 81p
        tester.mustBelieve(70, "<a --> b>", 1f, 0.81f);
        tester.mustBelieve(70, "<b --> a>", 1f, 0.81f);

        tester.mustBelieve(70, "<a <-> b>", 1.0f, 0.66f);
        tester.run();
    }*/


//    @Test
//    public void missingEdgeCase1() {
//        //((<p1 ==> p2>, <(&&, p1, p3) ==> p2>), (<p3 ==> p2>, (<DecomposeNegativePositivePositive --> Truth>, <ForAllSame --> Order>)))
//        //((<p1 ==> p2>, <(&&, p1, p3) ==> p2>), (<p3 ==> p2>, (<DecomposeNegativePositivePositive --> Truth>, <ForAllSame --> Order>)))
//        new RuleTest(
//                "<p1 ==> p2>. %0.05;0.9%", "<(&&, p1, p3) ==> p2>.",
//                "<p3 ==> p2>.",
//                0.95f, 0.95f, 0.77f, 0.77f)
//                .run();
//    }


    @Test
    public void testPosPosImplicationConc() {

        // ((%1,(%2==>%3),belief(positive),notImpl(%1),time(urgent)),(subIfUnifiesAny(%3,%2,%1),((DeductionRecursive-->Belief),(InductionRecursive-->Goal))))
        test
                //.log()
                .input("(x). %1.0;0.90%")
                .input("((x) ==> (y)).")
                .mustBelieve(cycles, "(y)", 1.0f, 0.81f)
                .mustNotOutput(cycles, "(y)", BELIEF, 0f, 0.5f, 0, 1, ETERNAL);

    }

//    @Test public void testPosNegImplicationConc() {
//        test()
//                .input("(x). %1.0;0.90%")
//                .input("((x) ==> (--,(y))).")
//                .mustBelieve(cycles, "(y)", 0.0f, 0.81f)
//                .mustNotOutput(cycles, "(y)", BELIEF, 0.5f, 1f, 0, 1, ETERNAL);
//    }

    //    static {
//        Param.TRACE = true;
//    }
    @Test
    public void testImplNegPos() {

        test
                .input("--(x).")
                .input("(--(x) ==> (y)).")
                .mustBelieve(cycles, "(y)", 1.0f, 0.81f)
                .mustNotOutput(cycles, "((--,#1)==>(y))", BELIEF, 0f, 0.5f, 0, 1, ETERNAL) //not negative
                .mustNotOutput(cycles, "(y)", BELIEF, 0f, 0.5f, 0, 1, ETERNAL)
        ;
    }


    @Test
    public void testImplNegNeg() {

        test
                .input("--x.")
                .input("(--x ==> --y).")
                .mustBelieve(cycles, "y", 0.0f, 0.81f)
                .mustNotOutput(cycles, "y", BELIEF, 0.5f, 1f, 0.1f, 1, ETERNAL)
        ;
    }

//    @Test
//    public void testDeductionNegPosImplicationPred() {
//
//        //nothing hsould be derived
//        test
//                .input("(y). %1.0;0.90%")
//                .input("((--,(y)) ==> (x)).")
////                .mustBelieve(cycles, "(x)", 0.0f, 0.81f)
//                .mustNotOutput(cycles, "(x)", BELIEF, 0f, 1f, 0, 1, ETERNAL)
//        ;
//    }

    @Test
    public void testAbductionNegPosImplicationPred() {

        test
                //.log()
                .input("y. %1.0;0.90%")
                .input("(--x ==> y).")
                .mustBelieve(cycles, "x", 0.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }


    @Disabled
    @Test //???
    public void testAbductionPosNegImplicationPred() {

        test
                .input("y. %1.0;0.90%")
                .input("--(x ==> y).")
                .mustBelieve(cycles, "x", 0.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }

    @Disabled
    @Test //???
    public void testAbductionNegNegImplicationPred() {

        /*
        via contraposition:
        $.32 x. %1.0;.30% {11: 1;2} ((%1,%2,time(raw),belief(positive),task("."),time(dtEvents),notImpl(%2)),((%2 ==>+- %1),((Induction-->Belief))))
            $.21 ((--,y)==>x). %0.0;.47% {1: 2;;} ((((--,%1)==>%2),%2),(((--,%2) ==>+- %1),((Contraposition-->Belief))))
              $.50 ((--,x)==>y). %0.0;.90% {0: 2}
            $.50 y. %1.0;.90% {0: 1}
         */
        test
                .input("y. %1.0;0.90%")
                .input("--(--x ==> y).")
                .mustBelieve(cycles, "x", 1.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.0f, 0.5f, 0, 1, ETERNAL)
        ;
    }

    @Test
    public void testDeductionPosNegImplicationPred() {

        test
                .believe("(y)")
                .believe("((y) ==> --(x))")
                .mustBelieve(cycles, "(x)", 0.0f, 0.81f)
                .mustNotOutput(cycles, "(x)", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }

//    @Test public void testNegNegImplicationPred() {
//        test()
//                .input("(--,(y)).")
//                .input("((--,(x)) ==> (--,(y))).")
//                .mustBelieve(cycles, "(x)", 0.0f, 0.45f)
//                .mustNotOutput(cycles, "(x)", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
//        ;
//    }

    //    @Test public void testNegNegImplicationConc() {
//        test()
//                .log()
//                .input("(x). %0.0;0.90%")
//                .input("((--,(x)) ==> (--,(y))).")
//                .mustBelieve(cycles, "(y)", 0.0f, 0.81f)
//                .mustNotOutput(cycles, "(y)", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
//        ;
//    }

    @Test
    public void testConversion0() {

        test
                .input("(x==>y)?")
                .input("(y==>x).")
                .mustBelieve(cycles, "(x==>y).", 1.0f, 0.47f)
        ;
    }

    @Test
    public void testConversion() {

        test
                //.log()
                .input("((x)==>(y))?")
                .input("((y)==>(x)).")
                .mustBelieve(cycles, "((x)==>(y)).", 1.0f, 0.47f)
        ;
    }

    @Test
    public void testConversionNeg() {

        test
                .input("((x) ==> (y))?")
                .input("(--(y) ==> (x)).")
                .mustBelieve(cycles, "((x)==>(y)).", 0.0f, 0.47f)
        ;
    }

//    @Test
//    public void testConversionNeg2() {
//        test
//                .input("((x) ==> (y))?")
//                .input("((y) ==> --(x)).")
//                .mustBelieve(cycles, "(--(x)==>(y)).", 1.0f, 0.47f)
//        ;
//    }

    @Test
    public void testConversionNeg3() {
        test
                .input("(--x ==> y)?")
                .input("(y ==> --x).")
                .mustBelieve(cycles, "(--x ==> y).", 0f, 0.47f)
        ;
    }


}

