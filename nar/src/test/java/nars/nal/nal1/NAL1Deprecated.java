package nars.nal.nal1;

import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class NAL1Deprecated extends NALTest {

    static final int cycles = 20;

    @Test
    @Disabled void inheritanceToSimilarity3() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>", 0.9f, 0.9f);
        tester.ask("<bird <-> swan>");
        tester.mustBelieve(cycles, "<bird <-> swan>", 0.9f, 0.45f);

    }

    @Test
    @Disabled void conversion() {
        test.believe("<bird --> swimmer>")
                .ask("<swimmer --> bird>")
                .mustOutput(cycles, "<swimmer --> bird>. %1.00;0.47%");
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
    void variable_elimination_sim_subj() {

        test.believe("(($x --> bird) <-> ($x --> swimmer))");
        test.believe("(swan --> bird)", 0.90f, 0.9f);
        test.mustBelieve(cycles, "(swan --> swimmer)", 0.90f,
            0.39f);

    }
}
