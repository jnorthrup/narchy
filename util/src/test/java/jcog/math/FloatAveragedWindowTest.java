package jcog.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloatAveragedWindowTest {

    @Test
    void testSynchronous() {
        final int[] calculations = {0};
        FloatAveragedWindow f = new FloatAveragedWindow(4, 0.5f) {
            @Override
            protected double calculate() {
                calculations[0]++;
                return super.calculate();
            }
        };

        assertTrue(Float.isNaN(f.asFloat()));
        assertEquals(1, calculations[0]);

        assertEquals(1, f.valueOf(1));
        assertEquals(2, calculations[0]);

        assertEquals(0.75f, f.valueOf(0.5f));
        assertEquals(3, calculations[0]);

        f.setAndCommit(0f);
        f.setAndCommit(0f);
        f.setAndCommit(0f);

        assertEquals(3, calculations[0]);

        assertEquals(0.0625f, f.asFloat());
        assertEquals(4, calculations[0]);


    }

    @Test void testDirectionality() {
        for (float alpha : new float[] { 0, 0.1f, 0.25f, 0.5f, 0.75f, 1f }) {
            float F, G;
            {
                FloatAveragedWindow f = new FloatAveragedWindow(4, alpha);
                f.setAndCommit(1);
                f.setAndCommit(2);
                f.setAndCommit(3);
                f.setAndCommit(4);
                F = f.asFloat();
            }
            {
                FloatAveragedWindow g = new FloatAveragedWindow(4, alpha);
                g.setAndCommit(4);
                g.setAndCommit(3);
                g.setAndCommit(2);
                g.setAndCommit(1);
                G = g.asFloat();
            }
            assertTrue(alpha >= 0.5f ? F > G : F < G);
//            assertEquals(1.25, F - G);
        }
    }
    @Test
    void testCumulative() {
        FloatAveragedWindow g = new FloatAveragedWindow(4, 0.5f);
        assertEquals(0, g.window.target());
        assertTrue(g.asFloat()!=g.asFloat());
        g.add(1);

        assertEquals(1, g.asFloat());
        //TODO decide when this should invalidate
        g.add(2);
        g.commit(0);
        assertEquals(1, g.window.target());

        assertEquals(1.5, g.asFloat());

        g.add(100);
        g.commit(0);
        assertEquals(2, g.window.target());

        assertEquals(50.75, g.asFloat());


        g.commit(0);
        assertEquals(3, g.window.target());
        g.commit(0);
        assertEquals(0, g.window.target());

        assertEquals(12.5, g.asFloat());
    }
}