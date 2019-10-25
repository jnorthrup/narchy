package nars.concept.dynamic;

import nars.*;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthTable;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static nars.$.*;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicSectTest {

    private static NAR dummy() {
        NAR n = NARS.shell();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:(--,y)", 0f, 0.9f);
        n.believe("a:z", 0f, 0.9f);
        n.believe("a:(--,z)", 1f, 0.9f);
        n.believe("x:b", 1f, 0.9f);
        n.believe("y:b", 1f, 0.9f);
        n.believe("z:b", 0f, 0.9f);
        n.run(2);
        return n;
    }

    @Test
    void testDynamicIntersection1() throws Narsese.NarseseException {


        NAR n = dummy();


        {
            Task k = n.answer(INSTANCE.$("((x|y)-->a)"), BELIEF, 0);
            assertEquals(1f, k.truth().freq());
            assertEquals("(--,(((--,x)&&(--,y))-->a))", k.term().toString());
//            assertEquals(0f, k.truth().freq());
//            assertEquals("(((--,x)&&(--,y))-->a)", k.term().toString());

        }

        {
            Task k = n.answer(INSTANCE.$("((x~y)-->a)"), BELIEF, 0);
            assertEquals("(--,(((--,x)&&y)-->a))", k.term().toString());
            assertEquals(1f, k.truth().freq());
        }

        {
            Task k = n.answer(INSTANCE.$("((x-y)-->a)"), BELIEF, 0);
            assertEquals("(((--,y)&&x)-->a)", k.term().toString());
            assertEquals(0f, k.truth().freq());
        }
    }

    @Test
    void testDynamicIntersectionAtZero() throws Narsese.NarseseException {
        testDynamicIntersectionAt(0);
    }
    @Test
    void testDynamicIntersectionAtEternal() throws Narsese.NarseseException {
        testDynamicIntersectionAt(ETERNAL);
    }


    final NAR n = dummy();


    void testDynamicIntersectionAt(long now) throws Narsese.NarseseException {


        assertTrue(((BeliefTables)n.conceptualize(INSTANCE.$("((x|y)-->a)")).beliefs())
                .tableFirst(DynamicTruthTable.class)!=null);

        assertTruth("((x&y)-->a)", now, 1, 0.85f);
        assertTruth("((x|y)-->a)", now, 1, 0.85f);
        assertTruth("((x&z)-->a)", now, 0, 0.85f);
        assertTruth("((x|z)-->a)", now, 1, 0.85f);

        assertTruth("(b --> (x|y))", now, 1, 0.81f);
        assertTruth("(b --> (x|z))", now, 1, 0.81f);
        assertTruth("(b --> (x&z))", now, 0, 0.81f);

    }

    private void assertTruth(String tt, long now, float f, float c) throws Narsese.NarseseException {
        Truth t = n.beliefTruth(tt, now);
        PreciseTruth e = $.INSTANCE.t(f, c);
        assertTrue(e.equalTruth(t, 0.1f), new Supplier<String>() {
            @Override
            public String get() {
                return tt + " @ " + now + " => " + t + " , expected=" + e;
            }
        });
    }
}
