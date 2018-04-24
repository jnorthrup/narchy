package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.TestNAR;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;

public class NAL1Test extends NALTest {

    final int cycles = 100;

    static {
        //
        //Param.TRACE = true;
    }

    @Override protected NAR nar() {
        return NARS.tmp(1);
    }

    @Test
    public void deduction()  {

        test
                .believe("<bird --> animal>")
                /*.en("bird is a type of animal.")
                .es("bird es un tipo de animal.")
                .de("bird ist eine art des animal.");*/
                .believe("<robin --> bird>")
                        //.en("robin is a type of bird.");
                .mustBelieve(cycles, "<robin --> animal>", 0.81f);
    }



    @Test
    public void revision()  {

        String belief = "<bird --> swimmer>";

        test
                .mustBelieve(4, belief, 0.87f, 0.91f)
                .believe(belief)                 //.en("bird is a type of swimmer.");
                .believe(belief, 0.10f, 0.60f)                 //.en("bird is probably not a type of swimmer."); //.en("bird is very likely to be a type of swimmer.");*/
                ;
    }


    @Test
    public void abduction()  {


        //                .believe("<sport --> competition>")
//                .believe("<chess --> competition>", 0.90f, Global.DEFAULT_JUDGMENT_CONFIDENCE)
//                 mustBelieve(time, "<sport --> chess>", 1.0f, 0.42f)
//                .mustBelieve(time, "<chess --> sport>", 0.90f, 0.45f)
//        //.en("chess is a type of competition.");

        test
                .believe("<sport --> competition>", 1f, 0.9f)
                .believe("<chess --> competition>", 0.90f, 0.9f)
                .mustBelieve(cycles, "<chess --> sport>", 0.9f, 0.45f)
                .mustBelieve(cycles, "<sport --> chess>", 1f, 0.42f);

                //.en("I guess chess is a type of sport");
    }

    @Test
    public void abduction2()  {
        
        /*
        <swan --> swimmer>. %0.9;0.9%
        <swan --> bird>.
         */
        //(A --> B), (A --> C), neq(B,C) |- (C --> B), (Belief:Abduction, Desire:Weak, Derive:AllowBackward)

        test
            .believe("<swan --> swimmer>", 0.90f, 0.9f) //.en("Swan is a type of swimmer.");
            .believe("<swan --> bird>") //.en("Swan is a type of bird.");
            .mustBelieve(cycles, "<bird --> swimmer>", 1f, 0.42f) //.en("I guess bird is a type of swimmer.");
            //.mustNotOutput(CYCLES, "<bird --> swimmer>", BELIEF, 1f, 1f, 0.41f, 0.43f, ETERNAL) //test for correct ordering of the premise wrt truth value function
            .mustBelieve(cycles, "<swimmer --> bird>", 0.9f, 0.45f)
            //.mustNotOutput(CYCLES, "<swimmer --> bird>", BELIEF, 0.9f, 0.9f, 0.44f, 0.46f, ETERNAL) //test for correct ordering of the premise wrt truth value function
            ;
    }



    @Test public void induction() {
        //(A --> C), (B --> C), neq(A,B) |- (B --> A), (Belief:Induction, Desire:Weak, Derive:AllowBackward)

//        test.nar.onCycle(()->{
//            nar.exe.print(System.out);
//        });


        test
                .believe("<parakeet --> bird>", 0.90f, 0.9f) //.en("Swan is a type of swimmer.");
                .believe("<pteradactyl --> bird>") //.en("Swan is a type of bird.");
                .mustBelieve(cycles, "<pteradactyl --> parakeet>", 1, 0.42f)
                .mustBelieve(cycles, "<parakeet --> pteradactyl>", 0.9f, 0.45f)
        ;
    }


    @Test
    public void exemplification()  {

        test

            .believe("<robin --> bird>")
            .believe("<bird --> animal>")
            .mustOutput(cycles, "<animal --> robin>. %1.00;0.4475%");
    }

    @Test
    public void conversion() throws nars.Narsese.NarseseException {
        test.believe("<bird --> swimmer>")
            .ask("<swimmer --> bird>") //.en("Is swimmer a type of bird?");
            .mustOutput(cycles, "<swimmer --> bird>. %1.00;0.47%");
    }

//    @Test
//    public void conversionNeg() throws nars.Narsese.NarseseException {
//        test.believe("--plant:animal") //animal isnt a type of plant
//            .ask("animal:plant") //.en("Is plant a type of animal?");
//            .mustOutput(CYCLES, "animal:plant. %0.00;0.47%"); //plant probably is not a type of animal
//    }



    @Test
    public void backwardInference() throws nars.Narsese.NarseseException {

        test
                .believe("<bird --> swimmer>", 1.0f, 0.8f) //Bird is a type of swimmer
                .ask(    "<?1 --> swimmer>") //What is a type of swimmer?
                .mustOutput(cycles, "<?1 --> bird>?") //.en("What is a type of bird?");
                .mustOutput(cycles, "<bird --> ?1>?") //.en("What is the type of bird?");
        ;
    }
    @Test
    public void backwardInference2() throws nars.Narsese.NarseseException {

        /*
        $.14 (1-->(?1,0,/))? {866: 1©}
        $.27 (1-->(x,/,/)). %1.0;.90% {868: 2©}
        */

        test
                .believe("(x --> y)")
                .ask(    "(x --> z)")
                .mustOutput(cycles, "<y --> z>?")
                .mustOutput(cycles, "<z --> y>?")
        ;
    }



    @Test
    public void structureTransformation_InhQuestion_SimBelief() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<bright <-> smart>", 0.9f, 0.9f);
        tester.ask("<bright --> smart>");
        tester.mustBelieve(cycles, "<bright --> smart>", 0.9f, 0.9f);

    }
    @Test
    public void revisionSim() {

        TestNAR tester = test;
        tester.mustBelieve(cycles, "<robin <-> swan>", 0.87f, 0.91f);//;//Robin is probably similar to swan.");
        tester.believe("<robin <-> swan>");//;//Robin is similar to swan.");
        tester.believe("<robin <-> swan>", 0.1f, 0.6f);
    }

    @Test
    public void comparison() {

        TestNAR tester = test;
        tester.believe("<swan --> swimmer>", 0.9f, 0.9f);//Swan is a type of swimmer.");
        tester.believe("<swan --> bird>");//Swan is a type of bird.");
        tester.mustBelieve(cycles, "<bird <-> swimmer>", 0.9f, 0.45f);//I guess that bird is similar to swimmer.");

    }

    @Test
    public void comparison2() {

        TestNAR tester = test;
        tester.believe("<sport --> competition>"); //Sport is a type of competition.");
        tester.believe("<chess --> competition>", 0.9f, 0.9f);//Chess is a type of competition.");
        tester.mustBelieve(cycles, "<chess <-> sport>", 0.9f, 0.45f);//I guess chess is similar to sport.");

    }

//    @Test public void inductionNegation() {
//        //(A --> C), (B --> C), neq(A,B) |- (B --> A), (Belief:Induction, Desire:Weak, Derive:AllowBackward)
//        test().log()
//                .believe("<worm --> bird>", 0.1f, 0.9f)
//                .believe("<tweety --> bird>", 0.9f, 0.9f)
//                .mustBelieve(cycles, "<worm --> tweety>", 0.10f, 0.42f)
//                .mustBelieve(cycles, "<tweety --> worm>", 0.90f, 0.07f)
//                .mustBelieve(cycles, "<tweety <-> worm>", 0.10f, 0.42f)
//        ;
//    }

    @Test
    public void analogy() {

        TestNAR tester = test;
        tester.believe("<swan --> swimmer>");//Swan is a type of swimmer.");
        tester.believe("<gull <-> swan>");//Gull is similar to swan.");
        tester.mustBelieve(cycles, "<gull --> swimmer>", 1.0f, 0.81f);//I think gull is a type of swimmer.");

    }

    @Test
    public void analogy2() {

        TestNAR tester = test;
        tester.believe("<gull --> swimmer>");//Gull is a type of swimmer.");
        tester.believe("<gull <-> swan>");//Gull is similar to swan.");
        tester.mustBelieve(cycles, "<swan --> swimmer>", 1.0f, 0.81f);//I believe a swan is a type of swimmer.");
    }



    @Test
    public void resemblance() {

        TestNAR tester = test;
        tester.believe("<robin <-> swan>");//Robin is similar to swan.");
        tester.believe("<gull <-> swan>");//Gull is similar to swan.");
        tester.mustBelieve(cycles, "<gull <-> robin>", 1.0f, 0.81f);//Gull is similar to robin.");

    }

    @Test
    public void inheritanceToSimilarity() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>");//Swan is a type of bird. ");
        tester.believe("<bird --> swan>", 0.1f, 0.9f);//Bird is not a type of swan.");
        tester.mustBelieve(cycles, "<bird <-> swan>", 0.1f, 0.81f);//Bird is different from swan.");

    }

    @Test
    public void inheritanceToSimilarity2() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>");//Swan is a type of bird.");
        tester.believe("<bird <-> swan>", 0.1f, 0.9f);//Bird is different from swan.");
        tester.mustBelieve(cycles * 4, "<bird --> swan>",
                0.1f, 0.73f);//Bird is probably not a type of swan.");
    }

    @Test
    public void inheritanceToSimilarity3() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<swan --> bird>", 0.9f, 0.9f);//Swan is a type of bird.");
        tester.ask("<bird <-> swan>");//Is bird similar to swan?");
        tester.mustBelieve(cycles, "<bird <-> swan>", 0.9f, 0.45f);//I guess that bird is similar to swan.");

    }

    @Test
    public void inheritanceToSimilarity4() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<bird <-> swan>", 0.9f, 0.9f);//a bird is similar to a swan.");
        tester.ask("<swan --> bird>");//Is swan a type of bird?");
        tester.mustBelieve(cycles, "<swan --> bird>", 0.9f, 0.73f);//A swan is a type of bird.");

    }

}
