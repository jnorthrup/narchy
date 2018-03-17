package nars;

import jcog.Util;
import nars.test.DeductiveChainTest;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
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
                    try {
                        superNAR.believe("(a-->c)"); //HACK dont use the same task, the causes wont correspond
                    } catch (Narsese.NarseseException e) {
                        e.printStackTrace();
                    }
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
        assertTrue(a.beliefTruth($$("(a-->c)"), ETERNAL).expectation() > 0.6f);

    }

    @Test public void testCamera() {

    }
}