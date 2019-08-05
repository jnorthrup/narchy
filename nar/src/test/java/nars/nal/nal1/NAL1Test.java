package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

public class NAL1Test extends NALTest {

    protected int cycles = 150;

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
                .mustBelieve(4, belief, 0.87f, 0.91f)
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

        TestNAR tester = test;
        tester.believe("<bright <-> smart>", 0.9f, 0.9f);
        tester.ask("<bright --> smart>");
        tester.mustBelieve(cycles, "<bright --> smart>", 0.9f, 0.81f);

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
        test
            .believe("<swan --> swimmer>", 0.9f, 0.9f)
            .believe("<swan --> bird>")
            .mustBelieve(cycles, "<bird <-> swimmer>", 0.9f, 0.45f);

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
                .mustBelieve(cycles, "<gull --> swimmer>", 1.0f, 0.45f);
    }

    @Test
    void analogy2() {

        TestNAR tester = test;
        tester.believe("<gull --> swimmer>");
        tester.believe("<gull <-> swan>");
        tester.mustBelieve(cycles, "<swan --> swimmer>", 1.0f, 0.45f);
    }


    @Test
    void resemblance() {

        TestNAR tester = test;
        tester.believe("<robin <-> swan>");
        tester.believe("<gull <-> swan>");
        tester.mustBelieve(cycles, "<gull <-> robin>", 1.0f, 0.81f);

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
            .mustBelieve(cycles, "<bird --> swan>", 0.1f, 0.81f);
    }



    @Test
    void similarityToInheritance4() {

        TestNAR tester = test;
        tester.termVolMax(3);
        tester.confMin(0.7f);
        tester.believe("<bird <-> swan>", 0.9f, 0.9f);
        tester.ask("<swan --> bird>");
        tester.mustBelieve(cycles, "<swan --> bird>", 0.9f, 0.81f);

    }

    @Test
    void variable_elimination_sim_subj() {

        TestNAR tester = test;
        tester.believe("(($x --> bird) <-> ($x --> swimmer))");
        tester.believe("(swan --> bird)", 0.90f, 0.9f);
        tester.mustBelieve(cycles, "(swan --> swimmer)", 0.90f,
                0.45f);

    }
    @Test
    void variable_elimination_analogy_substIfUnify() {

        TestNAR tester = test;
        tester.believe("((bird --> $x) <-> (swimmer --> $x))");
        tester.believe("(bird --> swan)", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "(swimmer --> swan)", 0.80f,
                0.45f);

    }

    @Test
    void variable_elimination_analogy_substIfUnifyOther() {
        //same as variable_elimination_analogy_substIfUnify but with sanity test for commutive equivalence
        TestNAR tester = test;
        tester.believe("((bird --> $x) <-> (swimmer --> $x))");
        tester.believe("(swimmer --> swan)", 0.80f, 0.9f);
        tester.mustBelieve(cycles, "(bird --> swan)", 0.80f,
                0.45f);

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
