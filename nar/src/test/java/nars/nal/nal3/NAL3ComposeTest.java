package nars.nal.nal3;

import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

public class NAL3ComposeTest extends NAL3Test {
    @Test
    void compound_composition_two_premises() {

        TestNAR tester = test;
        tester.believe("(swan --> swimmer)", 0.9f, 0.9f);
        tester.believe("(swan --> bird)", 0.8f, 0.9f);
        //tester.mustBelieve(cycles, "(swan --> (bird | swimmer))", 0.98f, 0.81f);
        tester.mustBelieve(cycles, "(swan --> (bird & swimmer))", 0.72f, 0.81f);

    }

    @Test
    void compound_composition_two_premises2() {

        TestNAR tester = test;

        tester.termVolMax(8);
        tester.believe("(sport --> competition)", 0.9f, 0.9f);
        tester.believe("(chess --> competition)", 0.8f, 0.9f);
        tester.mustBelieve(cycles, "((chess & sport) --> competition)", 0.72f, 0.81f);
        //tester.mustBelieve(cycles, "((chess | sport) --> competition)", 0.98f, 0.81f);

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
