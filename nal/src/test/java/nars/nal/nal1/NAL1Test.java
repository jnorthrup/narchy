package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class NAL1Test extends NALTest {

    private final int cycles = 50;

    @Override protected NAR nar() {

        return NARS.tmp(1);
    }

    @Test
    void deduction()  {

        test
                .believe("<bird --> animal>")
                /*.en("bird is a type of animal.")
                .es("bird es un tipo de animal.")
                .de("bird ist eine art des animal.");*/
                .believe("<robin --> bird>")
                        
                .mustBelieve(cycles, "<robin --> animal>", 0.81f);
    }



    @Test
    void revision()  {

        String belief = "<bird --> swimmer>";

        test
                .mustBelieve(4, belief, 0.87f, 0.91f)
                .believe(belief)                 
                .believe(belief, 0.10f, 0.60f)                 
                ;
    }


    @Test
    void abduction()  {
        test
                .believe("<sport --> competition>", 1f, 0.9f)
                .believe("<chess --> competition>", 0.90f, 0.9f)
                .mustBelieve(cycles, "<chess --> sport>", 0.9f, 0.45f)
                .mustBelieve(cycles, "<sport --> chess>", 1f, 0.42f);
    }

    @Test
    void abduction2()  {
        
        /*
        <swan --> swimmer>. %0.9;0.9%
        <swan --> bird>.
         */
        

        test
            .believe("<swan --> swimmer>", 0.90f, 0.9f) 
            .believe("<swan --> bird>") 
            .mustBelieve(cycles, "<bird --> swimmer>", 1f, 0.42f) 
            
            .mustBelieve(cycles, "<swimmer --> bird>", 0.9f, 0.45f)
            
            ;
    }



    @Test
    void induction() {
        






        test
                .believe("<parakeet --> bird>", 0.90f, 0.9f) 
                .believe("<pteradactyl --> bird>") 
                .mustBelieve(cycles, "<pteradactyl --> parakeet>", 1, 0.42f)
                .mustBelieve(cycles, "<parakeet --> pteradactyl>", 0.9f, 0.45f)
        ;
    }


    @Test
    void exemplification()  {

        test

            .believe("<robin --> bird>")
            .believe("<bird --> animal>")
            .mustOutput(cycles, "<animal --> robin>. %1.00;0.4475%");
    }









    @Test
    void backwardInference() throws nars.Narsese.NarseseException {

        test
                .believe("<bird --> swimmer>", 1.0f, 0.8f) 
                .ask(    "<?1 --> swimmer>") 
                .mustOutput(cycles, "<?1 --> bird>?") 
                .mustOutput(cycles, "<bird --> ?1>?") 
        ;
    }
    @Test
    void backwardInference2() throws nars.Narsese.NarseseException {

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



    @Disabled
    @Test
    void structureTransformation_InhQuestion_SimBelief() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<bright <-> smart>", 0.9f, 0.9f);
        tester.ask("<bright --> smart>");
        tester.mustBelieve(cycles, "<bright --> smart>", 0.9f, 0.9f);

    }
    @Test
    void revisionSim() {

        TestNAR tester = test;
        tester.mustBelieve(cycles, "<robin <-> swan>", 0.87f, 0.91f);
        tester.believe("<robin <-> swan>");
        tester.believe("<robin <-> swan>", 0.1f, 0.6f);
    }

    @Test
    void comparison() {

        TestNAR tester = test;
        tester.believe("<swan --> swimmer>", 0.9f, 0.9f);
        tester.believe("<swan --> bird>");
        tester.mustBelieve(cycles, "<bird <-> swimmer>", 0.9f, 0.45f);

    }

    @Test
    void comparison2() {

        TestNAR tester = test;
        tester.believe("<sport --> competition>"); 
        tester.believe("<chess --> competition>", 0.9f, 0.9f);
        tester.mustBelieve(cycles, "<chess <-> sport>", 0.9f, 0.45f);

    }












    @Test
    void analogy() {
        test
            .believe("<swan --> swimmer>")
            .believe("<gull <-> swan>")
            .mustBelieve(cycles, "<gull --> swimmer>", 1.0f, 0.81f);
    }

    @Test
    void analogy2() {

        TestNAR tester = test;
        tester.believe("<gull --> swimmer>");
        tester.believe("<gull <-> swan>");
        tester.mustBelieve(cycles, "<swan --> swimmer>", 1.0f, 0.81f);
    }



    @Test
    void resemblance() {

        TestNAR tester = test;
        tester.believe("<robin <-> swan>");
        tester.believe("<gull <-> swan>");
        tester.mustBelieve(cycles, "<gull <-> robin>", 1.0f, 0.81f);

    }

    @Test
    void inheritanceToSimilarity() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>");
        tester.believe("<bird --> swan>", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<bird <-> swan>", 0.1f, 0.81f);

    }

    /** ReduceConjunction */
    @Test void inheritanceToSimilarity2() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>");
        tester.believe("<bird <-> swan>", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<bird --> swan>",
                0.1f, 0.73f);
    }


    @Test
    void similarityToInheritance4() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<bird <-> swan>", 0.9f, 0.9f);
        tester.ask("<swan --> bird>");
        tester.mustBelieve(cycles, "<swan --> bird>", 0.9f, 0.73f /*0.9f*/);

    }

}
