package nars.nal.multistep;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.TestNAR;
import nars.util.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.Op.GOAL;

/**
 * see Natural_Language_Processing2.md
 */
//@RunWith(Parameterized.class)
public class PatrickTests extends NALTest {

//    public PatrickTests(Supplier<NAR> b) {
//        super(b);
//    }
//
//    @Parameterized.Parameters(name = "{0}")
//    public static Iterable<Supplier<NAR>> configurations() {
//        return AbstractNALTest.nars(8);
//    }
//

    @Test
    public void testExample1() {
        /*
        ////Example 1, REPRESENT relation with lifting
        //the whole can sometimes be understood by understanding what the parts represent (lifting)

        <(&&,<$1 --> (/,REPRESENT,_,$3)>,<$2 --> (/,REPRESENT,_,$4)>) ==> <(*,(*,$1,$2),(*,$3,$4)) --> REPRESENT>>.
        //the word fish represents the concept FOOD
        <cat --> (/,REPRESENT,_,ANIMAL)>.
        //the word eats represents the concept EATING
        <eats --> (/,REPRESENT,_,EATING)>.

        //what does cat eats represent?
        <(*,(*,cat,eats),?what) --> REPRESENT>?
        //RESULT: <(*,(*,cat,eats),(*,ANIMAL,EATING)) --> REPRESENT>. %1.00;0.73%
         */

        TestNAR tt = test;
        tt.nar.freqResolution.set(0.05f);
        tt.confTolerance(0.2f);

        tt
//.log()
                .believe("(((REPRESENT,_,$3):$1 && (REPRESENT,_,$4):$2) ==> REPRESENT(($1,$2),($3,$4)))")
                .believe("(REPRESENT,_,ANIMAL):cat")
                .believe("(REPRESENT,_,EATING):eats")

//should WORK with either of these two questions:
//.askAt(1250,"REPRESENT:((eats,cat),?what)")
                .askAt(500, "REPRESENT:((cat,eats),(?x, ?y))")

                .mustBelieve(2000, "REPRESENT((eats,cat),(EATING,ANIMAL))", 0.9f, 1f, 0.15f, 0.99f);
        //.mustBelieve(2500, "REPRESENT:((eats, cat),(EATING,ANIMAL))", 1f, 0.73f);

    }

//    @Test public void testExample1a() {
//        /*
//        ////Example 1, REPRESENT relation with lifting
//        //the whole can sometimes be understood by understanding what the parts represent (lifting)
//
//        <(&&,<$1 --> (/,REPRESENT,_,$3)>,<$2 --> (/,REPRESENT,_,$4)>) ==> <(*,(*,$1,$2),(*,$3,$4)) --> REPRESENT>>.
//        //the word fish represents the concept FOOD
//        <cat --> (/,REPRESENT,_,ANIMAL)>.
//        //the word eats represents the concept EATING
//        <eats --> (/,REPRESENT,_,EATING)>.
//
//        //what does cat eats represent?
//        <(*,(*,cat,eats),?what) --> REPRESENT>?
//        //RESULT: <(*,(*,cat,eats),(*,ANIMAL,EATING)) --> REPRESENT>. %1.00;0.73%
//         */
//        TestNAR t = test();
//        //t.nar.DEFAULT_JUDGMENT_PRIORITY = 0.1f;
//        //t.nar.DEFAULT_QUESTION_PRIORITY = 0.1f;
//        t
//.log()
//.believe("(($1:(/,REPR,_,$3) && $2:(/,REPR,_,$4)) ==> REPR:(($1,$3)<->($2,$4)))")
//.believe("cat:(/,REPR,_,ANIMATING)")
//.believe("eats:(/,REPR,_,EATING)")
//
////should WORK with either of these two questions:
////.askAt(100,"REPR:((cat,ANIMATING)<->?what)")
//.askAt(10000,"REPR:((cat,ANIMATING)<->(?x, ?y))")
//
//.mustBelieve(11250, "REPR:((eats,EATING)<->(cat,ANIMATING))", 1f, 0.73f);
//
//
//    }


    @Test
    public void testToothbrush() {
        /*
        <(*,toothbrush,plastic) --> made_of>.
        <(&/,<(*,$1,plastic) --> made_of>,<({SELF},$1) --> op_lighter>) =/> <$1 --> [heated]>>.
        <<$1 --> [heated]> =/> <$1 --> [melted]>>.
        <<$1 --> [melted]> <|> <$1 --> [pliable]>>.
        <(&/,<$1 --> [pliable]>,<({SELF},$1) --> op_reshape>) =/> <$1 --> [hardened]>>.
        <<$1 --> [hardened]> =|> <$1 --> [unscrewing]>>.
        <toothbrush --> object>.
        (&&,<#1 --> object>,<#1 --> [unscrewing]>)!

            >> lighter({SELF},$1) instead of <({SELF},$1) --> op_lighter>

        */


        TestNAR tt = test;
        //Param.TRACE = true;

        int cycles = 2000;

        tt.confTolerance(0.5f);
//        MetaGoal.Desire.want(nar.want, 0.5f);
        tt.nar.freqResolution.set(0.05f);
        tt.nar.confResolution.set(0.02f);
        //tt.nar.DEFAULT_BELIEF_PRIORITY = 0.1f;
        tt.nar.time.dur(10);
        tt.nar.termVolumeMax.set(38);



//        tt.nar.onCycle(()->{
//            System.err.println(tt.nar.time());
//        });
        //tt.log();
        tt.input(
                "made_of(toothbrush,plastic).",
                "( ( made_of($1, plastic) &| lighter(I, $1) ) ==>+10 <$1 --> [heated]>).",
                "(<$1 --> [heated]> ==>+10 <$1 --> [melted]>).",
                "(<$1 --> [melted]> =|> <$1 --> [pliable]>).",
                "(<$1 --> [pliable]> =|> <$1 --> [melted]>).",
                "(( <$1 --> [pliable]> &| reshape(I,$1)) ==>+10 <$1 --> [hardened]>).",
                "(<$1 --> [hardened]> =|> <$1 --> [unscrews]>).",

//                "<toothbrush --> [unscrews]>! :|:", //make something that is here a screwdriver
//                "<toothbrush --> [unscrews]>! :|:", //make something that is here a screwdriver
//                "<toothbrush --> [unscrews]>! :|:", //make something that is here a screwdriver
//                "<toothbrush --> [unscrews]>! :|:", //make something that is here a screwdriver
                "$1.0 (toothbrush --> [unscrews])! :|:" //make something that is here a screwdriver
                //"<toothbrush --> here>. :|:" //there is a toothbrush here NOW
        );

        tt.mustGoal(cycles, "lighter(I, toothbrush)", 1f,
                0.3f,
                (t) -> t == 0);
        tt.mustNotOutput(cycles,  "lighter(I, toothbrush)", GOAL, (t)->t!=0);



    }

    /** TODO */
    @Disabled @Test
    public void testConditioningWithoutAnticipation() throws Narsese.NarseseException {
        /*
        <a --> A>. :|: <b --> B>. :|: %0% <c --> C>. %0%
        8
        <b --> B>. :|: <a --> A>. :|: %0% <c --> C>. %0%
        8
        <c --> C>. :|: <a --> a>. :|: %0% <b --> B>. %0%
        8
        <a --> A>. :|: <b --> B>. :|: %0% <c --> C>. %0%
        100
        <b --> B>. :|: <a --> A>. :|: %0% <c --> C>. %0%
        100
        <?1 =/> <c --> C>>? //this line needs to be translated to NARchy syntax

        Expected result: (also in OpenNARS syntax)
        For appropriate Interval term "time", "time2",
        <(&/,<a --> A>,time) =/> <c --> C>>.
        and
        <(&/,<b --> B>,time) =/> <c --> C>>.
        needs to be reduced in frequency, making
        <(&/,<a --> A>,time,<b --> B>,time2) =/> <c --> C>>.
        the strongest hypothesis based on the last two inputs where neither a nor b "leaded to" c.
         */

        NAR n = NARS.tmp();
        n.DEFAULT_BELIEF_PRIORITY = 0.01f;
        n.termVolumeMax.set(16);


        //n.log();
        n.inputAt(0, "  A:a. :|:    --B:b. :|:    --C:c. :|:");
        n.inputAt(8, "  B:b. :|:    --A:a. :|:    --C:c. :|:");
        n.inputAt(16, "  C:c. :|:    --A:a. :|:    --B:b. :|:");
        n.inputAt(24, "  A:a. :|:    --B:b. :|:    --C:c. :|:");
        n.inputAt(124, "  B:b. :|:    --A:a. :|:    --C:c. :|:");

        n.run(224);
        n.clear();

        n.input("       $0.9 (?x ==>   C:c)?");
        //n.input("       $0.9;0.9$ (?x ==>+8 C:c)?");
        //n.input("       $0.9;0.9$ ((A:a && B:b) ==> C:c)?");
        //n.input("       $0.9;0.9$ ((A:a && B:b) ==> C:c)? :|:");
        n.run(2000);

        /*
        Expected result: (also in OpenNARS syntax)
        For appropriate Interval term "time", "time2",
        <(&/,<a --> A>,time) =/> <c --> C>>.
        and
        <(&/,<b --> B>,time) =/> <c --> C>>.
        needs to be reduced in frequency, making
        <(&/,<a --> A>,time,<b --> B>,time2) =/> <c --> C>>.
        the strongest hypothesis based on the last two inputs where neither a nor b "leaded to" c.
         */

    }

    /** TODO */
    @Test @Disabled
    public void testPixelImage() throws Narsese.NarseseException {

        //this.activeTasks = activeTasks;
        NAR n = NARS.tmp();
        //n.log();
        //n.truthResolution.setValue(0.05f);
        n.termVolumeMax.set(60);
        n.DEFAULT_BELIEF_PRIORITY = 0.05f;
        n.DEFAULT_QUESTION_PRIORITY = 0.9f;

        n.input("<#x --> P>. %0.0;0.25%"); //assume that unless pixel isnt specified then it's black

// to what extent was
//    |          |
//    |    ██    |
//    |  ██████  |
//    |    ██    |
//    |          |
//observed in experience?

//imperfectly observed pattern
//    |      ░░  |
//    |    ▓▓    |
//    |░░▓▓██    |
//    |    ▒▒  ░░|
//    |      ░░  |
        String image1 =
                "<p_1_1 --> P>. :|: %0.5;0.9%\n" +
                        "<p_1_2 --> P>. :|: %0.5;0.9%\n" +
                        "<p_1_3 --> P>. :|: %0.6;0.9%\n" +
                        "<p_1_4 --> P>. :|: %0.6;0.9%\n" +
                        "<p_1_5 --> P>. :|: %0.5;0.9%\n" +
                        "<p_2_1 --> P>. :|: %0.5;0.9%\n" +
                        "<p_2_2 --> P>. :|: %0.5;0.9%\n" +
                        "<p_2_3 --> P>. :|: %0.8;0.9%\n" +
                        "<p_2_4 --> P>. :|: %0.5;0.9%\n" +
                        "<p_2_5 --> P>. :|: %0.5;0.9%\n" +
                        "<p_3_1 --> P>. :|: %0.6;0.9%\n" +
                        "<p_3_2 --> P>. :|: %0.8;0.9%\n" +
                        "<p_3_3 --> P>. :|: %0.9;0.9%\n" +
                        "<p_3_4 --> P>. :|: %0.5;0.9%\n" +
                        "<p_3_5 --> P>. :|: %0.5;0.9%\n" +
                        "<p_4_1 --> P>. :|: %0.5;0.9%\n" +
                        "<p_4_2 --> P>. :|: %0.5;0.9%\n" +
                        "<p_4_3 --> P>. :|: %0.7;0.9%\n" +
                        "<p_5_4 --> P>. :|: %0.6;0.9%\n" +
                        "<p_4_4 --> P>. :|: %0.5;0.9%\n" +
                        "<p_4_5 --> P>. :|: %0.6;0.9%\n" +
                        "<p_5_1 --> P>. :|: %0.5;0.9%\n" +
                        "<p_5_2 --> P>. :|: %0.5;0.9%\n" +
                        "<p_5_3 --> P>. :|: %0.5;0.9%\n" +
                        "<p_5_5 --> P>. :|: %0.5;0.9%\n" +
                        "<example1 --> name>. :|:";


        n.input(image1.split("\n"));


        //(&|,<p_2_3 --> pixel>,<p_3_2 --> pixel>,<p_3_3 --> pixel>,<p_3_4 --> pixel>,<p_4_3 --> pixel>,<example1 --> name>)?\n" +

        //for (int i = 0; i < 2; i++) {
        n.question($.parallel($("P:p_2_3"), $("P:p_3_2"), $("P:p_3_4"), $("P:p_4_3"), $("name:example1")));
        //}

        //Answer (&|,<example1 --> name>,<p_2_3 --> pixel>,<p_3_2 --> pixel>,<p_3_3 --> pixel>,<p_3_4 --> pixel>,<p_4_3 --> pixel>). :-1: %0.80;0.16%
        //ex: (&&,(example1-->name),(p_2_3-->pixel),(p_3_2-->pixel),(p_3_4-->pixel),(p_4_3-->pixel)). %.61;.06%"".


        n.run(6000);

        n.clear();

//imperfectly observed pattern
//    |      ░░  |
//    |    ▓▓    |
//    |░░    ▓▓  |
//    |    ▒▒  ░░|
//    |      ░░  |
        String image2 =
                "<p_1_1 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_1_2 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_1_3 --> pixel>. :|: %0.6;0.9%\n" +
                        "<p_1_4 --> pixel>. :|: %0.6;0.9%\n" +
                        "<p_1_5 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_2_1 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_2_2 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_2_3 --> pixel>. :|: %0.8;0.9%\n" +
                        "<p_2_4 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_2_5 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_3_1 --> pixel>. :|: %0.6;0.9%\n" +
                        "<p_3_2 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_3_3 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_3_4 --> pixel>. :|: %0.8;0.9%\n" +
                        "<p_3_5 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_4_1 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_4_2 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_4_3 --> pixel>. :|: %0.7;0.9%\n" +
                        "<p_5_4 --> pixel>. :|: %0.6;0.9%\n" +
                        "<p_4_4 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_4_5 --> pixel>. :|: %0.6;0.9%\n" +
                        "<p_5_1 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_5_2 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_5_3 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_5_5 --> pixel>. :|: %0.5;0.9%\n" +
                        "<example2 --> name>. :|:";

        n.input(image2.split("\n"));


        //(&|,<p_2_3 --> pixel>,<p_3_2 --> pixel>,<p_3_3 --> pixel>,<p_3_4 --> pixel>,<p_4_3 --> pixel>,<example2 --> name>)?

        //for (int i = 0; i < 8; i++) {
        n.question($.parallel($("P:p_2_3"), $("P:p_3_2"), $("P:p_3_3"), $("P:p_3_4"), $("P:p_4_3"), $("name:example2")));
        n.run(6000);
        //}

        //Answer (&|,<example2 --> name>,<p_2_3 --> pixel>,<p_3_2 --> pixel>,<p_3_3 --> pixel>,<p_3_4 --> pixel>,<p_4_3 --> pixel>). %0.50;0.40%


    }
}
