package jcog.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloatAveragedWindowTest {

    @Test
    void tetsOrdering() {
        FloatAveragedWindow f = new FloatAveragedWindow(4, 0.5f);
        assertEquals("NaN,NaN,NaN,NaN", f.toString());
        assertTrue(!Float.isFinite( f.window.iterator(Float::new).next()) );
        assertEquals("NaN,NaN,NaN,NaN", f.window.buffer.toString());
        f.next(1);
        assertEquals("1.0,NaN,NaN,NaN", f.window.buffer.toString());
        assertEquals("NaN,NaN,NaN,1.0", f.toString());
        assertEquals(1, f.window.iteratorReverse(Float::new).next());
        f.next(2);
//        assertEquals("1.0,2.0,NaN,NaN", f.window.buffer.toString());
        assertEquals("NaN,NaN,1.0,2.0", f.toString());
        f.next(3);
//        assertEquals("1.0,2.0,3.0,NaN", f.window.buffer.toString());
        assertEquals("NaN,1.0,2.0,3.0", f.toString());
        f.next(4);
//        assertEquals("1.0,2.0,3.0,4.0", f.window.buffer.toString());
        assertEquals("1.0,2.0,3.0,4.0", f.toString());
        f.next(5);
        assertEquals("2.0,3.0,4.0,5.0", f.toString());
        f.next(6);
        assertEquals("3.0,4.0,5.0,6.0", f.toString());
        assertEquals(3, f.window.getAt(0));
        assertEquals(6, f.window.getAt(3));
    }

    @Test
    void testSynchronous() {
        int[] calculations = {0};
        FloatAveragedWindow f = new FloatAveragedWindow(4, 0.5f) {
            @Override
            protected double calculate() {
                calculations[0]++;
                return super.calculate();
            }
        };
        assertEquals(4, f.window.volume());
        assertEquals(0, f.window.target());
        assertEquals("NaN,NaN,NaN,NaN", f.toString());
        assertTrue(Float.isNaN(f.asFloat()));
        assertEquals(1, calculations[0]);

        assertEquals(1, f.valueOf(1));
        assertEquals("NaN,NaN,NaN,1.0", f.toString());

        assertEquals(2, calculations[0]);

        float next = f.valueOf(0.5f);
        assertEquals("NaN,NaN,1.0,0.5", f.toString());
        assertEquals(0.75f, next );

        assertEquals(3, calculations[0]);

        f.next(0f);

        assertEquals(0.375f, f.asFloat());

        f.next(0f);
        assertEquals("1.0,0.5,0.0,0.0", f.toString());
//        assertEquals(0.25f, f.asFloat());

        f.next(0f);

        assertEquals(4, calculations[0]);

        assertEquals(0.0625, f.asFloat());
        assertEquals(5, calculations[0]);


    }

    @Test void testDirectionality() {
        for (float alpha : new float[] {  0.1f, 0.25f, 0.5f, 0.75f, 0.9f }) {
            float inc, dec;
            {
                FloatAveragedWindow f = new FloatAveragedWindow(4, alpha);
                f.next(1);
                f.next(2);
                f.next(3);
                f.next(4);
                inc = f.asFloat();
            }
            {
                FloatAveragedWindow g = new FloatAveragedWindow(4, alpha);
                g.next(4);
                g.next(3);
                g.next(2);
                g.next(1);
                dec = g.asFloat();
            }
            System.out.println(alpha + " increasing=" + inc + " decreasing=" + dec);
            //assertTrue(alpha >= 0.5f ? F > G : F < G);
//            assertEquals(1.25, F - G);
        }
    }

    @Test
    void testCumulative() {
        FloatAveragedWindow g = new FloatAveragedWindow(4, 0.5f).clear(0);

        assertEquals(0, g.window.target());
        g.add(1);
        assertEquals(0.125, g.asFloat());
        g.add(0);
        assertEquals(0.125, g.asFloat()); //unchanged

        //TODO decide when this should invalidate
        g.add(2);
        g.resetNext(0);
        assertEquals(1, g.window.target());

        assertEquals(1.5, g.asFloat());

        g.add(100);
        g.resetNext(0);
        assertEquals(2, g.window.target());

        assertEquals(50.75, g.asFloat());


        g.resetNext(0);
        assertEquals(3, g.window.target());
        g.resetNext(0);
        assertEquals(0, g.window.target());

        assertEquals(12.5, g.asFloat());
    }
}