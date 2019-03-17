package jcog.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloatAveragedWindowTest {

    @Test
    void test1() {
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

        f.put(0f);
        f.put(0f);
        f.put(0f);

        assertEquals(3, calculations[0]);

        assertEquals(0.0625f, f.asFloat());
        assertEquals(4, calculations[0]);


    }
    @Test void testDirectionality() {
        for (float alpha : new float[] { 0, 0.1f, 0.25f, 0.5f, 0.75f, 1f }) {
            float F, G;
            {
                FloatAveragedWindow f = new FloatAveragedWindow(4, alpha);
                f.put(1);
                f.put(2);
                f.put(3);
                f.put(4);
                F = f.asFloat();
            }
            {
                FloatAveragedWindow g = new FloatAveragedWindow(4, alpha);
                g.put(4);
                g.put(3);
                g.put(2);
                g.put(1);
                G = g.asFloat();
            }
            assertTrue(alpha >= 0.5f ? F > G : F < G);
//            assertEquals(1.25, F - G);
        }
    }

}