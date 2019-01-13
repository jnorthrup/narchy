package nars.nal.nal3;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class NAL3GuessTest extends NALTest {

    static final int cycles = 100;

    @Override protected NAR nar() {
        NAR n= NARS.tmp(3);
        n.termVolumeMax.set(7);
        return n;
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
