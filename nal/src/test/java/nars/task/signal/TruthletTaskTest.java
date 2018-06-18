package nars.task.signal;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.truth.TruthFunctions.c2w;
import static org.junit.jupiter.api.Assertions.*;

class TruthletTaskTest {

    @Test
    void testImpulseTruthlet() {
        NAR n = NARS.shell();
        Term x = $.the("x");
        float conf = 0.9f;
        n.input(new TruthletTask(x, BELIEF,
                Truthlet.impulse(1, 2, 1f, 0f, c2w(conf)), n)
        );

        BeliefTable xb = n.truths(x, BELIEF);
        for (int i = -1; i < 5; i++) {
            Truth ti = xb.truth(i, n);
            assertEquals((i <= 0 || i >= 3) ? 0 : 1, ti.freq());
            if (i >=1 && i <= 2)
                assertEquals(conf, ti.conf());
            else
                assertTrue(conf > ti.conf());
            System.out.println(i + ": " + ti);
        }

    }

    @Test
    void test_LinearTruthlet_And_SustainTruthlet() {

        float conf = 0.9f;

        RangeTruthlet s = Truthlet.slope(0, 1f, 3, 0f, c2w(conf));
        for (Truthlet t : new Truthlet[]{s, new SustainTruthlet(s, 1)}) {

            NAR n = NARS.shell();
            Term x = $.the("x");

            System.out.println(t);

            n.input(new TruthletTask(x, BELIEF, t, n));

            BeliefTable xb = n.truths(x, BELIEF);
            for (int i = -3; i < 7; i++) {
                Truth ti = xb.truth(i, n);
                if (i < 0 || i > 3) {
                    if (t instanceof SustainTruthlet) {
                        assertTrue(ti.conf() < 0.9f); 
                    } else {
                        assertNull(ti);
                    }
                } else {
                    assertEquals(0.9f, ti.conf());
                    float f = ti.freq();
                    switch (i) {
                        case 0:
                            assertEquals(1f, f, 0.01f);
                            break;
                        case 1:
                            assertEquals(0.666f, f, 0.01f);
                            break;
                        case 2:
                            assertEquals(0.333f, f, 0.01f);
                            break;
                        case 3:
                            assertEquals(0f, f, 0.01f);
                            break;
                    }
                }


                System.out.println(i + ": " + ti);
            }
        }

    }
}