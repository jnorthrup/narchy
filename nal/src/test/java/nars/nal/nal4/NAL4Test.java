package nars.nal.nal4;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
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


    private static final int CYCLES = 50;

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(4);
        n.termVolumeMax.set(9);
        return n;
    }


    @Test
    void structural_transformation_dont() {

        test
                .believe( "(acid --> (reaction,/,base))")
                .mustNotOutput(CYCLES, "(reaction --> (acid,base))", BELIEF, ETERNAL)
        ;
    }

    @Test
    void structural_transformationExt_forward_repeats2() {
        test
                .believe("((a,b,a) --> bitmap)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "(b --> (bitmap,a,/,a))", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "(a --> (bitmap,/,b,/))", 1.0f, 0.9f);
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
                .mustBelieve(CYCLES, "(1 --> (bitmap,0,/,0))", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "(0 --> (bitmap,/,1,/))", 1.0f, 0.9f)
                .mustNotOutput(CYCLES, "(bitmap --> (0,1,0))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }

    @Test
    void structural_transformationExt_forward_repeats3() {
        test
                .believe("((0,1,0,1) --> bitmap)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "(1 --> (bitmap, 0,/,0,/))", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "(0 --> (bitmap, /,1,/,1))", 1.0f, 0.9f);
    }

    @Test
    void structural_transformationExt_reverse() {
        test
                .believe("(acid --> (reaction,/,base))", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "((acid,base) --> reaction)", 1.0f, 0.9f);
    }
    @Test
    void structural_transformationExt() {

        test
                .believe("((acid,base) --> reaction)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "(acid --> (reaction,/,base))", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "(base --> (reaction,acid,/))", 1.0f, 0.9f)
                .mustNotOutput(CYCLES, "(reaction --> (acid,base))", BELIEF, ETERNAL)
        ;
    }

    @Test
    void structural_transformationInt_0() {
        test
                .believe("(reaction --> (acid,base))", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "((reaction,\\,base) --> acid)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "((reaction,acid,\\) --> base)", 1.0f, 0.9f)
                .mustNotOutput(CYCLES, "((acid,base) --> reaction)", BELIEF, ETERNAL)
        ;
    }



    @Test
    void structural_transformationInt_reverse() {


        test
                .believe("((neutralization,\\,base) --> acid)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "(neutralization --> (acid,base))", 1.0f, 0.9f)
                .mustNotOutput(CYCLES, "(neutralization --> (acid,/,\\,base))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }

    @Test
    void structural_transformation_DepVar1() {
        test.believe("reaction(#1,base)", 1.0f, 0.9f);
        test.mustBelieve(CYCLES, "(base --> (reaction,#1,/))", 1.0f, 0.9f);
        test.mustBelieve(CYCLES, "(#1 --> (reaction,/,base))", 1.0f, 0.9f);
    }

    @Test
    void structural_transformation_DepVar2() {
        test.believe("reaction(acid,#1)", 1.0f, 0.9f);
        test.mustBelieve(CYCLES, "(acid --> (reaction,/,#1))", 1.0f, 0.9f);
        test.mustBelieve(CYCLES, "(#1 --> (reaction,acid,/))", 1.0f, 0.9f);
    }

    @Test
    void structural_transformation_one_arg() {
        test.believe("reaction(acid)", 1.0f, 0.9f);
        //test.mustBelieve(CYCLES, "(acid --> (reaction,/))", 1.0f, 0.9f);
        test.mustNotOutput(CYCLES, "(acid --> (reaction,/))", BELIEF, 0, 1, 0, 1, t->true);
    }

    @Test
    public void structural_transformation6() {
        test.believe("((neutralization,acid,\\) --> base)", 1.0f, 0.9f) //en("Something that can neutralize a base is an acid.");
                .mustBelieve(CYCLES, "(neutralization --> (acid,base))", 1.0f, 0.9f) //en("Something that can be neutralized by an acid is a base.");
                .mustNotOutput(CYCLES, "((acid,base) --> neutralization)", BELIEF, 1f, 1.0f, 0.9f, 0.9f, ETERNAL)
        ;
    }

    @Test
    void structural_transformationInt() {
        test
                .believe("(neutralization --> (acid,base))")
                .mustBelieve(CYCLES, "((neutralization,\\,base) --> acid)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "((neutralization,acid,\\) --> base)", 1.0f, 0.9f)
        ;
    }
    @Test
    void concludeImageIntInheritImageExt() {
        test
                .termVolMax(9)
                .believe("(neutralization --> (acid,base))")
                .believe("((acid,base) --> reaction)")
                .mustBelieve(CYCLES*12, "((neutralization,\\,base) --> (reaction,/,base))", 1.0f, 0.81f)
                .mustBelieve(CYCLES*12, "((neutralization,acid,\\) --> (reaction,acid,/))", 1.0f, 0.81f)
        ;
    }

    @Disabled
    @Test
    void testCompositionFromProductInh() throws nars.Narsese.NarseseException {


        test
                .believe("(soda --> acid)", 1.0f, 0.9f)
                .ask("((drink,soda) --> ?death)")
                .mustBelieve(CYCLES, "((drink,soda) --> (drink,acid))", 1.0f, 0.81f);
    }

    @Disabled
    @Test
    void testCompositionFromProductSim() throws nars.Narsese.NarseseException {

        test
                .believe("(soda <-> deadly)", 1.0f, 0.9f)
                .ask("((soda,food) <-> #x)")
                .mustBelieve(CYCLES, "((soda,food) <-> (deadly,food))", 1.0f, 0.81f);
    }

    @Test
    void testIntersectionOfProductSubterms1() {
        test
                .believe("f(x)", 1.0f, 0.9f)
                .believe("f(y)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "f:((x)&(y))", 1.0f, 0.81f)
                .mustBelieve(CYCLES, "f:((x)|(y))", 1.0f, 0.81f)
        ;
    }

    @Test
    void testIntersectionOfProductSubterms2() {

        test
                .believe("f(x,z)", 1.0f, 0.9f)
                .believe("f(y,z)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "f((x|y),z)", 1.0f, 0.81f)
                .mustBelieve(CYCLES, "f((x&y),z)", 1.0f, 0.81f)
                .mustBelieve(CYCLES, "(((x,z)|(y,z))-->f)", 1.0f, 0.81f)
                .mustBelieve(CYCLES, "(((x,z)&(y,z))-->f)", 1.0f, 0.81f)

//                .mustBelieve(CYCLES*5 , "f((x,z)&(y,z))", 1.0f, 0.81f)
//                .mustBelieve(CYCLES*5 , "f((x,z)|(y,z))", 1.0f, 0.81f)
        ;

    }

    @Test
    void testIntersectionOfProductSubterms2ReverseIntensional() {

        test
                .believe("f((x|y),z)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "((x|y)-->(f,/,z))", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "(x-->(f,/,z))", 1.0f, 0.81f)
                .mustBelieve(CYCLES, "(y-->(f,/,z))", 1.0f, 0.81f)
                .mustBelieve(CYCLES , "f(x,z)", 1.0f, 0.81f)
                .mustBelieve(CYCLES , "f(y,z)", 1.0f, 0.81f)
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
                .believe("happy(L)", 1f, 0.9f)
                .believe("((L)-->(o-(i-happy)))", 1f, 0.9f)
                .mustNotOutput(CYCLES * 2, "((o-(i-happy))-->happy)", BELIEF, ETERNAL);
    }


    @ValueSource(bytes = {QUESTION, QUEST})
    @ParameterizedTest
    void testTransformQuestionSubj(byte punc) {
        test
                .input("((a,b)-->?4)" + Character.valueOf((char) punc))
                .mustOutput(CYCLES, "(b-->(?1,a,/))", punc)
                .mustOutput(CYCLES, "(a-->(?1,/,b))", punc)
        ;
    }

    @ValueSource(bytes = {QUESTION, QUEST})
    @ParameterizedTest
    void testTransformQuestionPred(byte punc) {
        test
                .input("(x --> (a,b))" + Character.valueOf((char) punc))
                .mustOutput(CYCLES, "b(x,a,\\)", punc)
                .mustOutput(CYCLES, "a(x,\\,b)", punc)
        ;
    }

    @Test
    public void testNormalize0() {
        test.believe("likes(cat,[blue])")
                .mustBelieve(CYCLES, "(cat-->(likes,/,[blue]))", 1f, 0.9f)
                .mustNotOutput(CYCLES, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }

    @Test
    public void testNormalize1() {
        String input = "((likes,cat,\\)-->[blue])";
        assertEquals("(likes-->(cat,[blue]))", Image.imageNormalize($$(input)).toString());

        test.believe(input)
                .mustBelieve(CYCLES, "(likes-->(cat,[blue]))", 1f, 0.9f)
                .mustNotOutput(CYCLES, "((cat,[blue])-->likes)", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }
    @Test
    public void testNormalize1a() {

        test
                .believe("([blue] --> (likes,cat,/))")
                //.mustBelieve(CYCLES, "((cat,[blue])-->likes)", 1f, 0.9f)
                .mustNotOutput(CYCLES, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }
    @Test
    public void testNormalize1aQ() throws Narsese.NarseseException {

        test
                .ask("([blue] --> (likes,cat,/))")
                .mustQuestion(CYCLES, "((cat,[blue])-->likes)")
                .mustNotOutput(CYCLES, "(likes-->(cat,[blue]))", QUESTION, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }

    @Test
    public void testNormalize1b() {
        test.believe("((likes,cat,/)-->[blue])")
                .mustNotOutputAnything()
        ;
    }

    @Test
    public void testNormalize2() {
        test.believe("([blue] --> (likes,cat,/))")
                .mustBelieve(CYCLES, "likes(cat,[blue])", 0.9f)
                .mustNotOutput(CYCLES, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
        ;
    }

    @Disabled
    @Test
    void testQuestionAnswering() {
        test
                .input("((0,1)-->?1)?")
                .input("((1,1)-->x).")
                .mustOutput(CYCLES, "((0,1)-->(1,1))", QUESTION);
    }

    @ValueSource(bytes = {QUESTION, QUEST})
    @ParameterizedTest
    @Disabled
    void testTransformRawQuestionSubj(byte punc) {
        test
                .input("(a,b)" + Character.valueOf((char) punc))
                .mustOutput(CYCLES, "(b-->(?1,a,/))", punc)
                .mustOutput(CYCLES, "(a-->(?1,/,b))", punc)
        ;
    }

    @Test
    public void composition_on_both_sides_of_a_statement_2() throws Narsese.NarseseException {

        test.nar.termVolumeMax.set(9);
        test.believe("(bird-->animal)",1.0f,0.9f) //en("Bird is a type of animal.");
        .ask("((bird,plant) --> (animal,plant))")
        .mustBelieve(CYCLES, "((bird,plant) --> (animal,plant))", 1.0f, 0.81f) //en("The relation between bird and plant is a type of relation between animal and plant.");
        ;
    }


}






































































































































































































































































































