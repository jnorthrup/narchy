package nars.nal.nal4;

import nars.NAR;
import nars.NARS;
import nars.term.util.Image;
import nars.test.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.$.$$;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NAL4Test extends NALTest {


    private static final int cycles = 450;

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(4);
        n.termVolumeMax.set(11);
        return n;
    }


    @Test
    void structural_transformation_dont() {

        test
                .believe( "(acid --> (reaction,/,base))")
                .mustNotOutput(cycles, "(reaction --> (acid,base))", BELIEF, ETERNAL)
        ;
    }

    @Test
    void structural_transformationExt_forward_repeats2() {
        test
                .believe("((a,b,a) --> bitmap)", 1.0f, 0.9f)
                .mustBelieve(cycles, "(b --> (bitmap,a,/,a))", 1.0f, 0.9f)
                .mustBelieve(cycles, "(a --> (bitmap,/,b,/))", 1.0f, 0.9f);
    }

    @Test
    void structural_transformationExt_only_first_layer() {

        test
                .believe("acid(reaction,/,base)", 1.0f, 0.9f)
                .mustNotOutputAnything()
//                .mustBelieve(CYCLES, "((reaction,\\,/,base) --> acid)", 1.0f, 0.9f)
//                .mustBelieve(CYCLES, "((reaction,acid,\\,/) --> base)", 1.0f, 0.9f)
//                .mustNotOutput(CYCLES, "((acid,/,base) --> reaction)", BELIEF, ETERNAL)
        ;
    }

    @Test
    void structural_transformationExt_forward_repeats2numeric() {
        test
                .believe("((0,1,0) --> bitmap)", 1.0f, 0.9f)
                .mustBelieve(cycles, "(1 --> (bitmap,0,/,0))", 1.0f, 0.9f)
                .mustBelieve(cycles, "(0 --> (bitmap,/,1,/))", 1.0f, 0.9f)
                .mustNotOutput(cycles, "(bitmap --> (0,1,0))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }

    @Test
    void structural_transformationExt_forward_repeats3() {
        test
                .believe("((0,1,0,1) --> bitmap)", 1.0f, 0.9f)
                .mustBelieve(cycles, "(1 --> (bitmap, 0,/,0,/))", 1.0f, 0.9f)
                .mustBelieve(cycles, "(0 --> (bitmap, /,1,/,1))", 1.0f, 0.9f);
    }

    @Test
    void structural_transformationExt_reverse() {
        test
                .mustBelieve(cycles, "reaction(acid,base)", 1.0f, 0.9f)
                .believe("(acid --> (reaction,/,base))", 1.0f, 0.9f);
    }
    @Test
    void structural_transformationExt() {

        test
                .believe("((acid,base) --> reaction)", 1.0f, 0.9f)
                .mustBelieve(cycles, "(acid --> (reaction,/,base))", 1.0f, 0.9f)
                .mustBelieve(cycles, "(base --> (reaction,acid,/))", 1.0f, 0.9f)
                .mustNotOutput(cycles, "(reaction --> (acid,base))", BELIEF, ETERNAL)
        ;
    }

    @Test
    void structural_transformationInt_0() {
        test
                .believe("(reaction --> (acid,base))", 1.0f, 0.9f)
                .mustBelieve(cycles, "((reaction,\\,base) --> acid)", 1.0f, 0.9f)
                .mustBelieve(cycles, "((reaction,acid,\\) --> base)", 1.0f, 0.9f)
                .mustNotOutput(cycles, "((acid,base) --> reaction)", BELIEF, ETERNAL)
        ;
    }



    @Test
    void structural_transformationInt_reverse() {
        test
                .mustBelieve(cycles, "(neutralization --> (acid,base))", 1.0f, 0.9f)
                .mustNotOutput(cycles, "(neutralization --> (acid,/,\\,base))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
                .believe("((neutralization,\\,base) --> acid)", 1.0f, 0.9f)
        ;
    }

    @Test
    void structural_transformation_DepVar1() {
        test.believe("reaction(#1,base)", 1.0f, 0.9f);
        test.mustBelieve(cycles, "(base --> (reaction,#1,/))", 1.0f, 0.9f);
        test.mustBelieve(cycles, "(#1 --> (reaction,/,base))", 1.0f, 0.9f);
    }

    @Test
    void structural_transformation_DepVar2() {
        test.believe("reaction(acid,#1)", 1.0f, 0.9f);
        test.mustBelieve(cycles, "(acid --> (reaction,/,#1))", 1.0f, 0.9f);
        test.mustBelieve(cycles, "(#1 --> (reaction,acid,/))", 1.0f, 0.9f);
    }

    @Test
    void structural_transformation_one_arg() {
        test.believe("reaction(acid)", 1.0f, 0.9f);
        //test.mustBelieve(CYCLES, "(acid --> (reaction,/))", 1.0f, 0.9f);
        test.mustNotOutput(cycles, "(acid --> (reaction,/))", BELIEF, 0, 1, 0, 1, t->true);
    }

    @Test
    void structural_transformation6() {
        test

                .mustBelieve(cycles, "(neutralization --> (acid,base))", 1.0f, 0.9f) //en("Something that can be neutralized by an acid is a base.");
                .mustNotOutput(cycles, "((acid,base) --> neutralization)", BELIEF, 1f, 1.0f, 0.9f, 0.9f, ETERNAL)
                .believe("((neutralization,acid,\\) --> base)", 1.0f, 0.9f) //en("Something that can neutralize a base is an acid.");
        ;
    }

    @Test
    void structural_transformationInt() {
        test
                .believe("(neutralization --> (acid,base))")
                .mustBelieve(cycles, "((neutralization,\\,base) --> acid)", 1.0f, 0.9f)
                .mustBelieve(cycles, "((neutralization,acid,\\) --> base)", 1.0f, 0.9f)
        ;
    }
    @Test
    void structural_transformationInt_neg() {
        test
                .believe("(--(x && y) --> (acid,base))")
                .mustBelieve(cycles, "((--(x && y),\\,base) --> acid)", 1.0f, 0.9f)
                .mustBelieve(cycles, "((--(x && y),acid,\\) --> base)", 1.0f, 0.9f)
        ;
    }
    @Test
    void structural_transformationInt_neg_focus() {
        test
                .believe("(nothing --> (--acid,--base))")
                .mustBelieve(cycles, "((nothing,\\,--base) --> acid)", 0.0f, 0.9f)
                .mustBelieve(cycles, "((nothing,--acid,\\) --> base)", 0.0f, 0.9f)
        ;
    }

    @Test
    void concludeImageIntInheritImageExt() {
        test
                .termVolMax(12)
//                .confMin(0.6f)
                .believe("(neutralization --> (acid,base))")
                .believe("((acid,base) --> reaction)")
                .mustBelieve(cycles, "((neutralization,\\,base) --> (reaction,/,base))", 1.0f, 0.81f)
                .mustBelieve(cycles, "((neutralization,acid,\\) --> (reaction,acid,/))", 1.0f, 0.81f)
        ;
    }

    @Disabled
    @Test
    void testCompositionFromProductInh() {
        test
                .believe("(soda --> acid)", 1.0f, 0.9f)
                .ask("((drink,soda) --> ?death)")
                .mustBelieve(cycles, "((drink,soda) --> (drink,acid))", 1.0f, 0.81f);
    }

    @Disabled
    @Test
    void testCompositionFromProductSim() {

        test
                .believe("(soda <-> deadly)", 1.0f, 0.9f)
                .ask("((soda,food) <-> #x)")
                .mustBelieve(cycles, "((soda,food) <-> (deadly,food))", 1.0f, 0.81f);
    }


    @Test
    void testNeqComRecursiveConstraint() {

        /*
        SHOULD NOT HAPPEN:
        $.02;.09$ ((o-(i-happy))-->happy). 497⋈527 %.55;.18% {497⋈527: æ0IáËÑþKn;æ0IáËÑþKM;æ0IáËÑþKÄ;æ0IáËÑþKÉ;æ0IáËÑþKÌ} (((%1-->%2),(%1-->%3),neqCom(%2,%3)),((%3-->%2),((Abduction-->Belief),(Weak-->Goal),(Backward-->Permute))))
            $.04;.75$ happy(L). 497⋈512 %.55;.75% {497⋈512: æ0IáËÑþKÄ}
            $.05;.53$ ((L)-->(o-(i-happy))). 527 %.54;.53% {527: æ0IáËÑþKn;æ0IáËÑþKM;æ0IáËÑþKÉ;æ0IáËÑþKÌ} Dynamic
        */

        test
                .believe("happy(L)", 1f, 0.9f)
                .believe("((L)-->(o-(i-happy)))", 1f, 0.9f)
                .mustNotOutput(cycles * 2, "((o-(i-happy))-->happy)", BELIEF, ETERNAL);
    }


    @ValueSource(bytes = {QUESTION, QUEST})
    @ParameterizedTest
    void testTransformQuestionSubj(byte punc) {
        test
                .input("((a,b)-->?4)" + Character.valueOf((char) punc))
                .mustOutput(cycles, "(b-->(?1,a,/))", punc)
                .mustOutput(cycles, "(a-->(?1,/,b))", punc)
        ;
    }

    @ValueSource(bytes = {QUESTION, QUEST})
    @ParameterizedTest
    void testTransformQuestionPred(byte punc) {
        test
                .termVolMax(8)
                .input("(x --> (a,b))" + Character.valueOf((char) punc))
                .mustOutput(cycles, "b(x,a,\\)", punc)
                .mustOutput(cycles, "a(x,\\,b)", punc)
        ;
    }

    @Test
    void testNormalize0() {
        test.believe("likes(cat,[blue])")
                .mustBelieve(cycles, "(cat-->(likes,/,[blue]))", 1f, 0.9f)
                .mustNotOutput(cycles, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }

    @Test
    void testNormalize1() {
        String input = "((likes,cat,\\)-->[blue])";
        assertEquals("(likes-->(cat,[blue]))", Image.imageNormalize($$(input)).toString());

        test
                .mustBelieve(cycles, "(likes-->(cat,[blue]))", 1f, 0.9f)
                .mustNotOutput(cycles, "((cat,[blue])-->likes)", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
                .believe(input)
        ;
    }
    @Test
    void testNormalize1a() {

        test
                .believe("([blue] --> (likes,cat,/))")
                //.mustBelieve(CYCLES, "((cat,[blue])-->likes)", 1f, 0.9f)
                .mustNotOutput(cycles, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }

    @Disabled @Test
    void testNormalize1aQ() {

        test
                .ask("([blue] --> (likes,cat,/))")
                .mustQuestion(cycles, "((cat,[blue])-->likes)")
                .mustNotOutput(cycles, "(likes-->(cat,[blue]))", QUESTION, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }

    @Test
    void testNormalize1b() {
        test.believe("((likes,cat,/)-->[blue])")
                .mustNotOutputAnything()
        ;
    }

    @Test
    void testNormalize2() {
        test
                .mustBelieve(cycles, "likes(cat,[blue])", 0.9f)
                .mustNotOutput(cycles, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
                .believe("([blue] --> (likes,cat,/))")
        ;
    }

    @Disabled
    @Test
    void testQuestionAnswering() {
        test
                .input("((0,1)-->?1)?")
                .input("((1,1)-->x).")
                .mustOutput(cycles, "((0,1)-->(1,1))", QUESTION);
    }

    @ValueSource(bytes = {QUESTION, QUEST})
    @ParameterizedTest
    @Disabled
    void testTransformRawQuestionSubj(byte punc) {
        test
                .input("(a,b)" + Character.valueOf((char) punc))
                .mustOutput(cycles, "(b-->(?1,a,/))", punc)
                .mustOutput(cycles, "(a-->(?1,/,b))", punc)
        ;
    }

    @ParameterizedTest @ValueSource(strings={"-->","<->"})
    void composition_on_both_sides_of_a_statement_2(String op) {
        test
            .termVolMax(9)
            .believe("(bird"+op+"animal)",1.0f,0.9f) //en("Bird is a type of animal.");
            .ask("((bird,plant)"+op+"(animal,plant))")
            .mustBelieve(cycles, "((bird,plant)"+op+"(animal,plant))", 1.0f, 0.81f) //en(" The relation between bird and plant is a type of relation between animal and plant.");
        ;
    }
    @Test
    void composition_on_both_sides_of_a_statement_2_neg() {
        test
                .termVolMax(13)
                .believe("((x||y)-->animal)",1.0f,0.9f) //en("Bird is a type of animal.");
                .ask("(((x||y),plant) --> (animal,plant))")
                .mustBelieve(cycles, "(((x||y),plant) --> (animal,plant))", 1.0f, 0.81f) //en("The relation between bird and plant is a type of relation between animal and plant.");
        ;
    }

    @Test
    void one_element_unwrap() {
        test
                .termVolMax(5)
                .believe("((x)-->(y))",1.0f,0.9f)
                .ask("(x --> y)")
                .mustBelieve(cycles, "(x --> y)", 1.0f, 0.81f)
        ;
    }
    @Test
    void one_element_wrap() {
        test
                .termVolMax(5)
                .believe("(x-->y)",1.0f,0.9f)
                .ask("((x) --> (y))")
                .mustBelieve(cycles, "((x) --> (y))", 1.0f, 0.81f)
        ;
    }

    @Test
    void composition_on_both_sides_of_a_statement_2_alternating_position() {

        test
                .termVolMax(9)
                .believe("(bird-->animal)",1.0f,0.9f) //en("Bird is a type of animal.");
                .ask("((bird,plant) --> (plant,animal))")
                .mustBelieve(cycles, "((bird,plant) --> (plant,animal))", 1.0f, 0.81f) //en("The relation between bird and plant is a type of relation between animal and plant.");
                ////                .mustNotOutput(cycles, "((bird,plant) --> (plant,animal))", BELIEF)
        ;
    }
    @Test
    void composition_on_both_sides_of_a_statement_3() {

        test
                .termVolMax(9)
                .believe("(bird-->animal)",1.0f,0.9f) //en("Bird is a type of animal.");
                .ask("((wtf,bird,plant) --> (wtf,animal,plant))")
                .mustBelieve(cycles, "((wtf,bird,plant) --> (wtf,animal,plant))", 1.0f, 0.81f) //en("The relation between bird and plant is a type of relation between animal and plant.");
        ;
    }

}






































































































































































































































































































