package nars.util;

import nars.$;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TermHashMapTest {
    @Test
    public void test1() {
        TermHashMap m = new TermHashMap();

        m.put($.the("x"), "a");
        assertNotNull(m.other);
        assertEquals(1, m.size());
        assertFalse(m.isEmpty());
        m.put($.varDep(1), "v");
        assertNotNull(m.id);
        assertEquals(2, m.size());
        m.put($.varDep(1), "v");
        assertEquals(2, m.size()); //no change

        assertEquals("{#1=v, x=a}", m.toString());

        assertEquals("v", m.remove($.varDep(1)));
        assertEquals(1, m.size());
        assertNull(m.remove($.varDep(1)));

        assertEquals("a", m.remove($.the("x")));
        assertEquals(0, m.size());

        //reinsert something to test clear:
        m.put($.the("x"), "a");

        m.clear();

        assertEquals(0, m.size());
        assertTrue(m.isEmpty());

        m.delete();
        assertNull(m.id);
        assertNull(m.other);

    }
}