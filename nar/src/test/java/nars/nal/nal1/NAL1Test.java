package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import org.junit.jupiter.api.Test;

public class NAL1Test extends NALTest {

    protected int cycles = 250;

    @Override
    protected NAR nar() {

        NAR n = NARS.tmp(1);
        n.termVolMax.set(7);
        return n;
    }

    @Test
    void deduction() {

        test
                .believe("(bird --> animal)")
                /*.en("bird is a type of animal.")
                .es("bird es un tipo de animal.")
                .de("bird ist eine art des animal.");*/
                .believe("(robin --> bird)")

                .mustBelieve(cycles, "(robin --> animal)", 0.81f);
    }

    @Test
    void deductionReverseAndAbduction() {
        test
                .input("(b-->c).")
                .input("(a-->b).")
                .mustBelieve(cycles, "(a-->c)", 1f, 0.81f)
                .mustBelieve(cycles, "(c-->a)", 1f, 0.45f)
        ;
    }


    @Test
    void revision() {

        String belief = "<bird --> swimmer>";

        test
                .mustBelieve(cycles, belief, 0.87f, 0.91f)
                .believe(belief)
                .believe(belief, 0.10f, 0.60f)
        ;
    }


    @Test
    void abduction() {
        test
            .termVolMax(3)
                .believe("(sport --> competition)", 1f, 0.9f)
              //.en("sport is a type of competition.");
                .believe("(chess --> competition)", 0.90f, 0.9f)
              //.en("chess is a type of competition.");
                .mustBelieve(cycles, "(chess --> sport)", 0.9f, 0.45f)
              //.en("I guess chess is a type of sport");
                .mustBelieve(cycles, "(sport --> chess)", 1f, 0.42f)
            /*  .en("I guess sport is a type of chess.")
	            .en("sport is possibly a type of chess.")
	            .es("es posible que sport es un tipo de chess.");*/
        ;
    }


    @Test
    void exemplification() {
        test
                .believe("<robin --> bird>")
                .believe("<bird --> animal>")
                .mustOutput(cycles, "<animal --> robin>. %1.00;0.4475%");
    }


    @Test
    void backwardInference() {

        test
                .believe("<bird --> swimmer>", 1.0f, 0.8f)
                .ask("<?1 --> swimmer>")
                .mustOutput(cycles, "<?1 --> bird>?")
                .mustOutput(cycles, "<bird --> ?1>?")
        ;
    }

    @Test
    void backwardInference2() {

        test
                .confMin(0.9f)
                .termVolMax(3)
                .believe("(x --> y)")
                .ask("(x --> z)")
                //.mustOutput(cycles, "<y --> z>?")
                .mustOutput(cycles, "<z --> y>?")
        ;
    }


    @Test
    void structureTransformation_InhQuestion_SimBelief() {

        test.believe("<bright <-> smart>", 0.9f, 0.9f);
        test.ask("<bright --> smart>");
        test.mustBelieve(cycles, "<bright --> smart>", 0.9f, 0.81f);

    }

    @Test
    void revisionSim() {

        test.mustBelieve(cycles, "<robin <-> swan>", 0.87f, 0.91f);
        test.believe("<robin <-> swan>");
        test.believe("<robin <-> swan>", 0.1f, 0.6f);
    }

    @Test
    void comparison() {
        test
            .believe("<swan --> swimmer>", 0.9f, 0.9f)
            .believe("<swan --> bird>")
            .mustBelieve(cycles, "<bird <-> swimmer>", 0.9f, 0.45f);

    }

    @Test
    void comparison2() {

        test.believe("<sport --> competition>");
        test.believe("<chess --> competition>", 0.9f, 0.9f);
        test.mustBelieve(cycles, "<chess <-> sport>", 0.9f, 0.45f);

    }

    @Test
    void comparisonPosNeg() {
        test
            .believe("(swan --> swimmer)", 0.9f, 0.9f)
            .believe("--(swan --> dinosaur)")
            .mustBelieve(cycles, "<dinosaur <-> swimmer>", 0f, 0.42f);
    }

    @Test
    void analogy() {
        test
                .believe("<swan --> swimmer>")
                .believe("<gull <-> swan>")
                .mustBelieve(cycles, "<gull --> swimmer>", 1.0f, 0.45f);
    }

    @Test
    void analogy2() {

        test.believe("<gull --> swimmer>");
        test.believe("<gull <-> swan>");
        test.mustBelieve(cycles, "<swan --> swimmer>", 1.0f, 0.45f);
    }
    @Test
    void comparisonStructuralPos() {
        test
            .believe("((a-->b) <-> (a-->c))")
            .mustBelieve(cycles, "(b<->c)", 1.0f, 0.45f);
    }
    @Test
    void comparisonStructuralNeg() {
        test
            .believe("--((a-->b) <-> (a-->c))")
            .mustBelieve(cycles, "(b<->c)", 0.0f, 0.45f);
    }

    @Test
    void resemblance() {

        test.believe("<robin <-> swan>");
        test.believe("<gull <-> swan>");
        test.mustBelieve(cycles, "<gull <-> robin>", 1.0f, 0.81f);

    }

    @Test
    void similarityBelief() {
        test
            .input("(a-->c).")
            .input("(c-->a).")
            .mustBelieve(cycles, "(a<->c)", 1f, 0.81f)
        ;
    }


    @Test
    void inheritanceToSimilarity() {


        test
            .believe("<swan --> bird>")
            .believe("<bird --> swan>", 0.1f, 0.9f)
            .mustBelieve(cycles, "<bird <-> swan>", 0.1f, 0.81f);

    }


    /**
     * ReduceConjunction
     */
    @Test
    void inheritanceToSimilarity2() {

        test.termVolMax(3)
            .believe("<swan --> bird>")
            .believe("<bird <-> swan>", 0.1f, 0.9f)
            .mustBelieve(cycles, "<bird --> swan>", 0.1f, 0.65f /*0.81f*/);
    }



    @Test
    void similarityToInheritance4() {

        test.termVolMax(3);
        test.confMin(0.7f);
        test.believe("<bird <-> swan>", 0.9f, 0.9f);
        test.ask("<swan --> bird>");
        test.mustBelieve(cycles, "<swan --> bird>", 0.9f, 0.81f);

    }

    @Test
    void variable_elimination_sim_subj() {

        test.believe("(($x --> bird) <-> ($x --> swimmer))");
        test.believe("(swan --> bird)", 0.90f, 0.9f);
        test.mustBelieve(cycles, "(swan --> swimmer)", 0.90f,
                0.39f);

    }
    @Test
    void variable_elimination_analogy_substIfUnify() {

        test.believe("((bird --> $x) <-> (swimmer --> $x))");
        test.believe("(bird --> swan)", 0.80f, 0.9f);
        test.mustBelieve(cycles, "(swimmer --> swan)", 0.80f,
                0.49f);

    }

    @Test
    void variable_elimination_analogy_substIfUnifyOther() {
        //same as variable_elimination_analogy_substIfUnify but with sanity test for commutive equivalence
        test.believe("((bird --> $x) <-> (swimmer --> $x))");
        test.believe("(swimmer --> swan)", 0.80f, 0.9f);
        test.mustBelieve(cycles, "(bird --> swan)", 0.80f,
                0.49f);

    }

    @Test
    void analogyPos() {

        test
            .believe("<p1 --> p2>")
            .believe("<p2 <-> p3>")
            .mustBelieve(cycles, "<p1 --> p3>", 1.0f, 0.45f)
        ;

    }

    @Test
    void analogyNeg() {

        test
                .believe("--(p1 --> p2)")
                .believe("(p2 <-> p3)")
                .mustBelieve(cycles, "(p1 --> p3)",
                        0f, 0.45f);

    }
}
