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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TruthletTaskTest {

    @Test
    public void testImpulseTruthlet() {
        NAR n = NARS.tmp();
        Term x = $.the("x");
        float conf = 0.9f;
        n.input(new TruthletTask(x, BELIEF,
                Truthlet.impulse(1, 2, 1f, 0f, c2w(conf)), n)
        );

        BeliefTable xb = n.truths(x, BELIEF);
        for (int i = -1; i < 5; i++) {
            Truth ti = xb.truth(i, n);
            assertEquals((i <= 0 || i >= 3) ? 0 : 1, ti.freq());
            assertEquals(conf, ti.conf());
            System.out.println(i + ": " + ti);
        }

    }

    @Test
    public void testLinearTruthlet() {
        NAR n = NARS.tmp();
        Term x = $.the("x");
        float conf = 0.9f;
        n.input(new TruthletTask(x, BELIEF,
                Truthlet.slope(0, 1f, 3, 0f, c2w(conf)), n)
        );

        BeliefTable xb = n.truths(x, BELIEF);
        for (int i = -1; i < 5; i++) {
            Truth ti = xb.truth(i, n);
            if (i < 0 || i > 3) {
                assertNull(ti);
            } else {
                assertEquals(0.9f, ti.conf());
                float f = ti.freq();
                switch (i) {
                    case 0: assertEquals(1f, f, 0.01f); break;
                    case 1: assertEquals(0.666f, f, 0.01f); break;
                    case 2: assertEquals(0.333f, f, 0.01f); break;
                    case 3: assertEquals(0f, f, 0.01f); break;
                }
            }


            System.out.println(i + ": " + ti);
        }

    }
}