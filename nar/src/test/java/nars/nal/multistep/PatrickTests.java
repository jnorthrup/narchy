package nars.nal.multistep;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.Op.GOAL;

/**
 * see Natural_Language_Processing2.md
 */

public class PatrickTests extends NALTest {


    @Test
    void testExample1() {
        /*
        
        

        <(&&,<$1 --> (/,REPRESENT,_,$3)>,<$2 --> (/,REPRESENT,_,$4)>) ==> <(*,(*,$1,$2),(*,$3,$4)) --> REPRESENT>>.
        
        <cat --> (/,REPRESENT,_,ANIMAL)>.
        
        <eats --> (/,REPRESENT,_,EATING)>.

        
        <(*,(*,cat,eats),?what) --> REPRESENT>?
        
         */



        TestNAR tt = test;
        tt.nar.freqResolution.set(0.1f);
        tt.nar.termVolMax.set(22);
        tt
                .believe("(( ($1-->(REPRESENT,/,$3)) && ($2-->(REPRESENT,/,$4))) ==> REPRESENT({$1,$2},{$3,$4}))")
                .believe("(cat-->(REPRESENT,/,ANIMAL))")
                .believe("(eats-->(REPRESENT,/,EATING))")


                .ask( "REPRESENT({cat,eats},?1)")

                //.mustBelieve(2000, "REPRESENT((eats,cat),(EATING,ANIMAL))", 0.9f, 1f, 0.15f, 0.99f);
                .mustBelieve(1600, "REPRESENT({cat,eats},{ANIMAL,EATING})", 0.9f, 1f, 0.15f, 0.99f);

    }

    @Test
    void testExample1_NoImages() {

        TestNAR tt = test;
        tt.nar.freqResolution.set(0.1f);
        tt.nar.termVolMax.set(22);
        tt.nar.confMin.set(0.2f);
        tt.confTolerance(0.8f);
        tt
                .believe("(( REPRESENT($1,$3) && REPRESENT($2, $4) ) ==> REPRESENT({$1,$2},{$3,$4}))")
                .believe("REPRESENT(cat,ANIMAL)")
                .believe("REPRESENT(eats,EATING)")
                .ask( "REPRESENT({cat,eats},?1)")
                .mustBelieve(600, "REPRESENT({cat,eats},{ANIMAL,EATING})", 1, 0.4f);
    }

    @Test
    void testToothbrush() {
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


        TestNAR n = test;


        int cycles = 2000;

        n.confTolerance(0.5f);

//        tt.nar.freqResolution.set(0.25f);
//        tt.nar.confResolution.set(0.02f);

//        n.nar.time.dur(100);
        n.nar.termVolMax.set(18);

        n.nar.dtDither.set(5);

        n.input(
                "made_of(toothbrush,plastic).",
                "( ( made_of($1, plastic) && lighter(I, $1) ) ==>+10 <$1 --> [heated]>).",
                "(<$1 --> [heated]> ==>+10 <$1 --> [melted]>).",
                "(<$1 --> [melted]> ==> <$1 --> [pliable]>).",
                "(<$1 --> [pliable]> ==> <$1 --> [melted]>).",
                "(( <$1 --> [pliable]> && reshape(I,$1)) ==>+10 <$1 --> [hardened]>).",
                "(<$1 --> [hardened]> ==> <$1 --> [unscrews]>).",


                "(toothbrush --> [unscrews])! |"

        );

//        test.nar.onTask(t->{
//           System.out.println(t.proof());
//        },GOAL);

        n.mustGoal(cycles, "lighter(I, toothbrush)", 1f,
                0.2f,

                t -> t >= 0
        );


    }

    @Test
    void testToothbrushSimpler() {


        TestNAR tt = test;


        int cycles = 3000;

        tt.confTolerance(0.9f);

        tt.nar.freqResolution.set(0.25f);
//        tt.nar.confResolution.setAt(0.05f);


        tt.nar.time.dur(100);
        tt.nar.termVolMax.set(14);
        tt.nar.dtDither.set(5);

        tt.input(
                "made_of(toothbrush,plastic).",
                "( ( made_of($1, plastic) &| lighter($1) ) ==>+10 hot:$1).",
                "(hot:$1 ==>+10 molten:$1).",
                "(molten:$1 =|> pliable:$1).",
                "(pliable:$1 =|> molten:$1).",
                "( (pliable:$1 &| reshape($1)) ==>+10 hard:$1).",
                "(hard:$1 =|> unscrews:$1).",
                "unscrews:toothbrush! |"
        );

        tt.mustGoal(cycles, "hot:toothbrush", 1f, 0.5f, (t) -> t >= 0);

        tt.mustGoal(cycles, "hard:toothbrush", 1f, 0.5f, (t) -> t >= 0);
        tt.mustGoal(cycles, "pliable:toothbrush", 1f, 0.5f, (t) -> t >= 0);
        tt.mustGoal(cycles, "molten:toothbrush", 1f, 0.5f, (t) -> t >= 0);
        tt.mustGoal(cycles, "lighter(toothbrush)", 1f,
                0.3f,
                t -> t >= 0);

    }

    /**
     * TODO
     */
    @Disabled
    @Test
    void testConditioningWithoutAnticipation() throws Narsese.NarseseException {
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
        <?1 =/> <c --> C>>? 

        Expected result: (also in OpenNARS syntax)
        For appropriate Interval target "time", "time2",
        <(&/,<a --> A>,time) =/> <c --> C>>.
        and
        <(&/,<b --> B>,time) =/> <c --> C>>.
        needs to be reduced in frequency, making
        <(&/,<a --> A>,time,<b --> B>,time2) =/> <c --> C>>.
        the strongest hypothesis based on the last two inputs where neither a nor b "leaded to" c.
         */

        NAR n = NARS.tmp();
        n.beliefPriDefault.pri(0.01f);
        n.termVolMax.set(16);


        n.inputAt(0, "  A:a. :|:    --B:b. :|:    --C:c. :|:");
        n.inputAt(8, "  B:b. :|:    --A:a. :|:    --C:c. :|:");
        n.inputAt(16, "  C:c. :|:    --A:a. :|:    --B:b. :|:");
        n.inputAt(24, "  A:a. :|:    --B:b. :|:    --C:c. :|:");
        n.inputAt(124, "  B:b. :|:    --A:a. :|:    --C:c. :|:");

        n.run(224);
        //n.clear();

        n.input("       $0.9 (?x ==>   C:c)?");


        n.run(2000);

        /*
        Expected result: (also in OpenNARS syntax)
        For appropriate Interval target "time", "time2",
        <(&/,<a --> A>,time) =/> <c --> C>>.
        and
        <(&/,<b --> B>,time) =/> <c --> C>>.
        needs to be reduced in frequency, making
        <(&/,<a --> A>,time,<b --> B>,time2) =/> <c --> C>>.
        the strongest hypothesis based on the last two inputs where neither a nor b "leaded to" c.
         */

    }

    /**
     * TODO
     */
    @Test
    @Disabled
    void testPixelImage() throws Narsese.NarseseException {


        NAR n = NARS.tmp();


        n.termVolMax.set(60);
        n.beliefPriDefault.pri(0.05f);
        n.questionPriDefault.pri(0.9f);

        n.input("<#x --> P>. %0.0;0.25%");


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


        n.question($.parallel($("P:p_2_3"), $("P:p_3_2"), $("P:p_3_4"), $("P:p_4_3"), $("name:example1")));


        n.run(6000);

        n.clear();


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


        n.question($.parallel($("P:p_2_3"), $("P:p_3_2"), $("P:p_3_3"), $("P:p_3_4"), $("P:p_4_3"), $("name:example2")));
        n.run(6000);


    }

    @Test void memorizePreconditionParallel() {
        /*
        <(&/,(&|,<a --> #A>,<a2 --> #A>),+10,(^pick,{SELF},a),+10) =/> <a --> B>>.
        100
        <a --> A>. :|:
        <a2 --> A>. :|:
        10
        <a --> B>!
        ''outputMustContain('(^pick,{SELF},a). :!110: %1.00;0.90%')
         */
        test
            .believe("(((#A:a &| #A:b) &&+1 pick(#A)) ==>+1 c)")
            .input("x:a. |")
            .input("x:b. |")
            .inputAt(3, "c! |")
            .mustOutput(500, "pick(x)", GOAL, 1f, 1f, 0.05f, 0.81f,
                    (t)->t>=1)
            ;
    }
}
