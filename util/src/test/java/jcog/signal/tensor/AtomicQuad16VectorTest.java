package jcog.signal.tensor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** TODO multithread tests */
class AtomicQuad16VectorTest {

    @Test void testFloatShort() {

        assertEquals(0, AtomicQuad16Vector.toShort(0));
        assertEquals(Short.MAX_VALUE*2+1, AtomicQuad16Vector.toShort(1));
        assertEquals(16, Integer.bitCount(AtomicQuad16Vector.toShort(1)), "use all 16 bits");

        assertEquals(0, AtomicQuad16Vector.toFloat(0), Float.MIN_NORMAL);

    }

    @Test
    void test1() {


        AtomicQuad16Vector x = new AtomicQuad16Vector();
        assertEquals("0,0,0,0", x.toString());
        assertEquals(0, x.getAt(0));

        x.setAt(0, 1f);
        assertEquals(1f, x.getAt(0), 0.001f);
        assertEquals("1,0,0,0", x.toString());
        
        x.setAt(1, 0.5f);assertEquals("1,0.5,0,0", x.toString());
        x.setAt(2, 0.25f); assertEquals("1,0.5,0.25,0", x.toString());
        x.setAt(3, 0.125f); assertEquals("1,0.5,0.25,0.125", x.toString());

        //sum
        assertEquals(1+0.5+0.25+.125, x.sumValues(), 0.001f);

        //multiply arg 3 by 0.5
        x.mergeAndGet(3, 0.5f,  (v, a) -> v * a); assertEquals("1,0.5,0.25,0.0625", x.toString());
        //multiply arg 2 by 0.5
        x.mergeAndGet(2, 0.5f,  (v, a) -> v * a); assertEquals("1,0.5,0.125,0.0625", x.toString());
        //multiply arg 1 by 0.5
        x.mergeAndGet(1, 0.5f,  (v, a) -> v * a); assertEquals("1,0.25,0.125,0.0625", x.toString());
        //multiply arg 0 by 0.5
        x.mergeAndGet(0, 0.5f,  (v, a) -> v * a); assertEquals("0.5,0.25,0.125,0.0625", x.toString());

        //no change TODO test write elide
        x.mergeAndGet(0, 1, (v, a) -> v * a);

        x.fill(0.33f); assertEquals("0.33,0.33,0.33,0.33", x.toString());

    }

}