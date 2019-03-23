package jcog.signal.tensor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtomicQuad16VectorTest {

    @Test
    void test1() {
        AtomicQuad16Vector x = new AtomicQuad16Vector();
        assertEquals("0,0,0,0", x.toString());
        assertEquals(0, x.getAt(0));

        x.setAt(1f, 0);
        assertEquals(1f, x.getAt(0), 0.001f);
        assertEquals("1,0,0,0", x.toString());
        
        x.setAt(0.5f, 1); assertEquals("1,0.5,0,0", x.toString());
        x.setAt(0.25f, 2); assertEquals("1,0.5,.25,0", x.toString());
        x.setAt(0.125f, 3); assertEquals("1,0.5,.25,.13", x.toString());

    }

}