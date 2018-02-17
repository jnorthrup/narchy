package nars;

import jcog.Util;
import nars.test.DeductiveChainTest;
import org.junit.jupiter.api.Test;

import static nars.$.$safe;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubNARTest {

    @Test
    public void testSuperAndSubIsolation() {
        NAR a = NARS.tmp();

        SubNAR ab = new SubNAR(a, (superNAR)-> {

            NAR n = NARS.realtime(1).deriverAdd(1,1).get();

            new DeductiveChainTest(n, 2, 25, DeductiveChainTest.inh);

            n.onTask((t)->{
                if (t.term().toString().equals("(a-->c)")) {
                    superNAR.input(t);
                }
            });

            //n.log();

            n.start();

            return n;
        });
        ab.onDur((aa, bb) -> {
            bb.run();
        });

        a.log();
        a.run(25, ()->Util.sleep(50));

        a.tasks().forEach(System.out::println);
        assertTrue(a.beliefTruth($safe("(a-->c)"), ETERNAL).expectation() > 0.6f);

    }

    @Test public void testCamera() {

    }
}