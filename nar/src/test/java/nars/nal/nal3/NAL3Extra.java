package nars.nal.nal3;

import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class NAL3Extra extends NAL3Test {


    @Test
    void unionOfOppositesInt() {
        //Coincidentia oppositorum
        test
                .termVolMax(6)
                .believe("((  x&z)-->a)")
                .believe("((--x&y)-->a)")
                .mustBelieve(cycles, "((y&z)-->a)", 1f, 0.81f)
        ;
    }

    @Test
    void unionOfOppositesExt() {
        //Coincidentia oppositorum

        test
                .termVolMax(6)
                .believe("(a-->(  x|z))")
                .believe("(a-->(--x|y))")
                .mustBelieve(cycles, "(a-->(y|z))", 1f, 0.81f)
        ;
    }

    @Test
    void drilldown1() {

        TestNAR tester = test;
        tester.believe("((x|y)-->z)");
        tester.mustQuestion(cycles, "((|,x,y,?1)-->z)");

    }

    @Test
    void composition_on_both_sides_of_a_statement() {

        TestNAR tester = test;
        tester.believe("<bird --> animal>", 0.9f, 0.9f);
        tester.ask("<(&,bird,swimmer) --> (&,animal,swimmer)>");
        tester.mustBelieve(cycles, "<(&,bird,swimmer) --> (&,animal,swimmer)>", 0.90f, 0.73f);

    }

    @Test
    void composition_on_both_sides_of_a_statement_2() {

        TestNAR tester = test;
        tester.believe("<bird --> animal>", 0.9f, 0.9f);
        tester.ask("<(|,bird,swimmer) --> (|,animal,swimmer)>");
        tester.mustBelieve(cycles, "<(|,bird,swimmer) --> (|,animal,swimmer)>", 0.90f, 0.73f);

    /*<bird --> animal>. %0.9;0.9%
            <(|,bird,swimmer) --> (|,animal,swimmer)>?*/
    }

    @Test
    void composition_on_both_sides_of_a_statement2() {

        TestNAR tester = test;
        tester.believe("<bird --> animal>", 0.9f, 0.9f);
        tester.ask("<(-,swimmer,animal) --> (-,swimmer,bird)>");
        tester.mustBelieve(cycles, "<(-,swimmer,animal) --> (-,swimmer,bird)>", 0.90f, 0.73f);

    }

    @Test
    void composition_on_both_sides_of_a_statement2_2() {

        TestNAR tester = test;
        tester.believe("<bird --> animal>", 0.9f, 0.9f);
        tester.ask("<(~,swimmer,animal) --> (~,swimmer,bird)>");
        tester.mustBelieve(cycles, "<(~,swimmer,animal) --> (~,swimmer,bird)>", 0.90f, 0.73f);

    }
    @Test
    void compound_composition_one_premise() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>", 0.9f, 0.9f);
        tester.ask("<swan --> (|,bird,swimmer)>");
        tester.mustBelieve(cycles, "<swan --> (|,bird,swimmer)>", 0.90f, 0.73f);

    }

    @Test
    void compound_composition_one_premise2() {
        test
                .believe("(swan --> bird)", 0.9f, 0.9f)
                .ask("((swan&swimmer) --> bird)")
                .mustBelieve(cycles, "((swan&swimmer) --> bird)", 0.90f, 0.73f);
    }

    @Test
    void compound_composition_one_premise3() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>", 0.9f, 0.9f);
        tester.askAt(cycles / 2, "<swan --> (swimmer - bird)>");
        tester.mustBelieve(cycles, "<swan --> (swimmer - bird)>", 0.10f, 0.73f);

    }

    @Test
    void compound_composition_one_premise4() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>", 0.9f, 0.9f);
        tester.askAt(cycles / 2, "<(swimmer ~ swan) --> bird>");
        tester.mustBelieve(cycles, "<(swimmer ~ swan) --> bird>", 0.10f, 0.73f);

    }



}
