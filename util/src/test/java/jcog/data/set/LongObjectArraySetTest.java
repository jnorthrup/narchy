package jcog.data.set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongObjectArraySetTest {

    @Test
    void testSort() {
        LongObjectArraySet l = new LongObjectArraySet();
        l.add(2, "x");
        l.add(0, "y");
        l.sortThis();
        assertEquals(0, l.when(0));
        assertEquals(2, l.when(1));
        assertEquals("y", l.get(0));
        assertEquals("x", l.get(1));

        l.add(-1, "z");
        l.sortThis();
        assertEquals(-1, l.when(0));
        assertEquals("z", l.get(0));

        l.add(4, "w");
        assertEquals(4, l.when(3));

        assertEquals("-1:z,0:y,2:x,4:w", l.toString());

        l.add(3, "a");
        l.sortThis();
        assertEquals("-1:z,0:y,2:x,3:a,4:w", l.toString());

    }
}