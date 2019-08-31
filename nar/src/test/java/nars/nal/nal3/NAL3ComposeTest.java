package nars.nal.nal3;

import org.junit.jupiter.api.Test;

public class NAL3ComposeTest extends NAL3Test {
    @Test
    void compound_composition_two_premises() {

        test.believe("(swan --> swimmer)", 0.9f, 0.9f);
        test.believe("(swan --> bird)", 0.8f, 0.9f);
        //tester.mustBelieve(cycles, "(swan --> (bird | swimmer))", 0.98f, 0.81f);
        test.mustBelieve(cycles, "(swan --> (bird & swimmer))", 0.72f, 0.81f);

    }

    @Test
    void compound_composition_two_premises2() {

        test.termVolMax(8);
        test.believe("(sport --> competition)", 0.9f, 0.9f);
        test.believe("(chess --> competition)", 0.8f, 0.9f);
        test.mustBelieve(cycles, "((chess & sport) --> competition)", 0.72f, 0.81f);
        //tester.mustBelieve(cycles, "((chess | sport) --> competition)", 0.98f, 0.81f);

    }
    @Test
    void compound_composition_two_premises_subj_intersection_and_union() {

        test.termVolMax(8);
        test.believe("(sport --> competition)", 0.9f, 0.9f);
        test.believe("(chess --> competition)", 0.8f, 0.9f);
        test.ask("((chess|sport) --> competition)");
        test.mustBelieve(cycles, "((chess | sport) --> competition)", 0.98f, 0.81f);
        test.mustBelieve(cycles, "((chess & sport) --> competition)", 0.72f, 0.81f);
    }

    @Test
    void compound_composition_two_premises_pred_intersection_and_union() {
        test.termVolMax(8);
        test.believe("(competition --> sport)", 0.9f, 0.9f);
        test.believe("(competition --> chess)", 0.8f, 0.9f);
        test.ask("(competition --> (chess|sport))");
        test.mustBelieve(cycles, "(competition --> (chess | sport))", 0.98f, 0.81f);
        test.mustBelieve(cycles, "(competition --> (chess & sport))", 0.72f, 0.81f);
    }

    @Test
    void intersectionComposition() {
        test
                .believe("(swan --> bird)")
                .believe("(swimmer--> bird)")
                .mustBelieve(cycles, "((swan&swimmer) --> bird)", 1f, 0.81f);
    }

    @Test
    void intersectionCompositionWrappedInProd() {
        test
                .termVolMax(7)
                .believe("((swan) --> bird)")
                .believe("((swimmer)--> bird)")
                .mustBelieve(cycles, "(((swan)&(swimmer)) --> bird)", 1f, 0.81f);
    }


}
