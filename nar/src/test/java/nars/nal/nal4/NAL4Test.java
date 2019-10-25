package nars.nal.nal4;

import nars.NAR;
import nars.NARS;
import nars.nal.nal7.NAL7Test;
import nars.term.util.Image;
import nars.test.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.LongPredicate;

import static nars.$.*;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NAL4Test extends NALTest {


    private static final int cycles = 250;

    @BeforeEach
    void setTolerance() {
        test.confTolerance(NAL7Test.CONF_TOLERANCE_FOR_PROJECTIONS); //for NAL4 Identity / StructuralReduction option
    }

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(4);
        n.termVolMax.set(8);
        return n;
    }


    @Test
    void structural_transformation_dont() {

        test
                .mustNotOutput(cycles, "(reaction --> (acid,base))", BELIEF, ETERNAL)
                .believe( "(acid --> (reaction,/,base))")
        ;
    }

    @Test
    void structural_transformationExt_forward_repeats2() {
        test
                .mustBelieve(cycles, "(b --> (bitmap,a,/,a))", 1.0f, 0.9f)
                .mustBelieve(cycles, "(a --> (bitmap,/,b,/))", 1.0f, 0.9f)
                .believe("((a,b,a) --> bitmap)", 1.0f, 0.9f);

    }

    @Test
    void structural_transformationExt_only_first_layer() {

        test
                .mustNotOutputAnything()
                .believe("acid(reaction,/,base)", 1.0f, 0.9f)
//                .mustBelieve(CYCLES, "((reaction,\\,/,base) --> acid)", 1.0f, 0.9f)
//                .mustBelieve(CYCLES, "((reaction,acid,\\,/) --> base)", 1.0f, 0.9f)
//                .mustNotOutput(CYCLES, "((acid,/,base) --> reaction)", BELIEF, ETERNAL)
        ;
    }

    @Test
    void structural_transformationExt_forward_repeats2numeric() {
        test
                .mustBelieve(cycles, "(1 --> (bitmap,0,/,0))", 1.0f, 0.9f)
                .mustBelieve(cycles, "(0 --> (bitmap,/,1,/))", 1.0f, 0.9f)
                .mustNotOutput(cycles, "(bitmap --> (0,1,0))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
                .believe("((0,1,0) --> bitmap)", 1.0f, 0.9f)
        ;
    }
    @Test
    void structural_transformationExt_forward_repeats2numeric_temporal() {
        test
                .mustBelieve(cycles, "(1 --> (bitmap,0,/,0))", 1.0f, 0.9f, 0)
                .mustBelieve(cycles, "(0 --> (bitmap,/,1,/))", 1.0f, 0.9f, 0)
                .mustNotOutput(cycles, "(bitmap --> (0,1,0))", BELIEF, 1f, 1f, 0.9f, 0.9f, new LongPredicate() {
                    @Override
                    public boolean test(long t) {
                        return true;
                    }
                })
                .input("((0,1,0) --> bitmap). |")
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
                .mustBelieve(cycles, "(acid --> (reaction,/,base))", 1.0f, 0.9f)
                .mustBelieve(cycles, "(base --> (reaction,acid,/))", 1.0f, 0.9f)
                .mustNotOutput(cycles, "(reaction --> (acid,base))", BELIEF, ETERNAL)
                .believe("((acid,base) --> reaction)", 1.0f, 0.9f)
        ;
    }

    @Test
    void structural_transformationInt_0() {
        test
                .mustBelieve(cycles, "((reaction,\\,base) --> acid)", 1.0f, 0.9f)
                .mustBelieve(cycles, "((reaction,acid,\\) --> base)", 1.0f, 0.9f)
                .mustNotOutput(cycles, "((acid,base) --> reaction)", BELIEF, ETERNAL)
                .believe("(reaction --> (acid,base))", 1.0f, 0.9f)
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
        test
            .mustBelieve(cycles, "(base --> (reaction,#1,/))", 1.0f, 0.9f)
            .mustBelieve(cycles, "(#1 --> (reaction,/,base))", 1.0f, 0.9f)
            .believe("reaction(#1,base)", 1.0f, 0.9f);
    }

    @Test
    void structural_transformation_DepVar2() {
        test.mustBelieve(cycles, "(acid --> (reaction,/,#1))", 1.0f, 0.9f).
             mustBelieve(cycles, "(#1 --> (reaction,acid,/))", 1.0f, 0.9f).
             believe("reaction(acid,#1)", 1.0f, 0.9f);
    }

    @Test
    void structural_transformation_one_arg() {
        test.mustBelieve(cycles, "(acid --> (reaction,/))", 1.0f, 0.9f);
        //test.mustNotOutput(cycles, "(acid --> (reaction,/))", BELIEF, 0, 1, 0, 1, t->true);
        test.believe("reaction(acid)", 1.0f, 0.9f);
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
    void structural_transformation6_temporal() {
        test
                .mustNotOutput(cycles, "((acid,base) --> neutralization)", BELIEF, 1f, 1.0f, 0.9f, 0.9f, new LongPredicate() {
                    @Override
                    public boolean test(long t) {
                        return true;
                    }
                })
                .mustBelieve(cycles, "(neutralization --> (acid,base))", 1.0f, 0.9f, 0) //en("Something that can be neutralized by an acid is a base.");
                .input("((neutralization,acid,\\) --> base). |") //en("Something that can neutralize a base is an acid.");
        ;
    }

    @Test
    void structural_transformationInt() {
        test
                .mustBelieve(cycles, "((neutralization,\\,base) --> acid)", 1.0f, 0.9f)
                .mustBelieve(cycles, "((neutralization,acid,\\) --> base)", 1.0f, 0.9f)
                .believe("(neutralization --> (acid,base))")
        ;
    }

    @Test
    void structural_transformationInt_neg() {
        test
                .termVolMax(9)
                .confMin(0.89f)
                .mustBelieve(cycles, "((--x,\\,base) --> acid)", 1.0f, 0.9f)
                .mustBelieve(cycles, "((--x,acid,\\) --> base)", 1.0f, 0.9f)
                .believe("(--x --> (acid,base))")
        ;
    }

    @Test
    void structural_transformationInt_neg_focus() {
        test
                .mustBelieve(cycles, "((nothing,\\,--base) --> acid)", 0.0f, 0.9f)
                .mustBelieve(cycles, "((nothing,--acid,\\) --> base)", 0.0f, 0.9f)
                .believe("(nothing --> (--acid,--base))")
        ;
    }

    @Test
    void concludeImageIntInheritImageExt() {
        test
                .termVolMax(12)
                .confMin(0.75f)
                .mustBelieve(cycles, "((neutralization,\\,base) --> (reaction,/,base))", 1.0f, 0.81f)
                .mustBelieve(cycles, "((neutralization,acid,\\) --> (reaction,acid,/))", 1.0f, 0.81f)
                .believe("(neutralization --> (acid,base))")
                .believe("((acid,base) --> reaction)")
        ;
    }

    @Disabled
    @Test
    void testCompositionFromProductInh() {
        test
                .mustBelieve(cycles, "((drink,soda) --> (drink,acid))", 1.0f, 0.81f)
                .believe("(soda --> acid)", 1.0f, 0.9f)
                .ask("((drink,soda) --> ?death)");
    }

    @Disabled
    @Test
    void testCompositionFromProductSim() {

        test
                .mustBelieve(cycles, "((soda,food) <-> (deadly,food))", 1.0f, 0.81f)
                .believe("(soda <-> deadly)", 1.0f, 0.9f)
                .ask("((soda,food) <-> #x)")
                ;
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
                .termVolMax(10)
                .mustNotOutput(cycles, "((o-(i-happy))-->happy)", BELIEF, ETERNAL)
                .believe("happy(L)", 1f, 0.9f)
                .believe("((L)-->(o-(i-happy)))", 1f, 0.9f)
                ;
    }


    @Disabled
    @ValueSource(bytes = {QUESTION, QUEST})
    @ParameterizedTest
    void testTransformQuestionSubj(byte punc) {
        test
                .termVolMax(6)
                .input("((a,b)-->?4)" + (char) punc)
                .mustOutput(cycles, "(b-->(?1,a,/))", punc)
                .mustOutput(cycles, "(a-->(?1,/,b))", punc)
        ;
    }

    @Disabled
    @ValueSource(bytes = {QUESTION, QUEST})
    @ParameterizedTest
    void testTransformQuestionPred(byte punc) {
        test
                .termVolMax(6)
                .input("(x --> (a,b))" + (char) punc)
                .mustOutput(cycles, "b(x,a,\\)", punc)
                .mustOutput(cycles, "a(x,\\,b)", punc)
        ;
    }

    @Test
    void testNormalize0() {
        test
                .mustBelieve(cycles, "(cat-->(likes,/,[blue]))", 1f, 0.9f)
                .mustNotOutput(cycles, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
                .believe("likes(cat,[blue])")
        ;
    }

    @Test
    void testNormalize1() {
        String input = "((likes,cat,\\)-->[blue])";
        assertEquals("(likes-->(cat,[blue]))", Image.imageNormalize(INSTANCE.$$(input)).toString());

        test
                .mustBelieve(cycles, "(likes-->(cat,[blue]))", 1f, 0.9f)
                .mustNotOutput(cycles, "((cat,[blue])-->likes)", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
                .believe(input)
        ;
    }
    @Test
    void testNormalize1a() {

        test
                //.mustBelieve(CYCLES, "((cat,[blue])-->likes)", 1f, 0.9f)
                .mustNotOutput(cycles, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
                .believe("([blue] --> (likes,cat,/))")
        ;
    }

    @Disabled @Test
    void testNormalize1aQ() {

        test
                .mustQuestion(cycles, "((cat,[blue])-->likes)")
                .mustNotOutput(cycles, "(likes-->(cat,[blue]))", QUESTION, 1f, 1f, 0.9f, 0.9f, ETERNAL)
                .ask("([blue] --> (likes,cat,/))")
        ;
    }

    @Disabled @Test
    void testNormalize1b() {
        test
                .mustNotOutputAnything()
                .believe("((likes,cat,/)-->[blue])")

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
                .mustOutput(cycles, "((0,1)-->(1,1))", QUESTION)
                .input("((0,1)-->?1)?")
                .input("((1,1)-->x).");
    }

    @ValueSource(bytes = {QUESTION, QUEST})
    @ParameterizedTest
    @Disabled
    void testTransformRawQuestionSubj(byte punc) {
        test
                .mustOutput(cycles, "(b-->(?1,a,/))", punc)
                .mustOutput(cycles, "(a-->(?1,/,b))", punc)
                .input("(a,b)" + (char) punc)
        ;
    }

    @ParameterizedTest @ValueSource(strings={"-->","<->"})
    void composition_on_both_sides_of_a_statement_2(String op) {
        test
            .termVolMax(9)
            .mustBelieve(cycles, "((bird,plant)"+op+"(animal,plant))", 1.0f, 0.81f) //en(" The relation between bird and plant is a type of relation between animal and plant.")
            .believe("(bird"+op+"animal)",1.0f,0.9f) //en("Bird is a type of animal.");
            .ask("((bird,plant)"+op+"(animal,plant))")
        ;
    }
    @Test
    void composition_on_both_sides_of_a_statement_2_neg() {
        test
               .termVolMax(12)
                .believe("((x|y)-->animal)",1.0f,0.9f)
                .ask("(((x|y),plant) --> (animal,plant))")
                .mustBelieve(cycles, "(((x|y),plant) --> (animal,plant))", 1.0f, 0.81f)
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






































































































































































































































































































