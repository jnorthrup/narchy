package nars.term.util.map;

import nars.$;
import nars.term.anon.Anom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TermHashMapTest {
    @Test
    void test1() {
        TermHashMap m = new TermHashMap();

        m.put($.the("x"), "a");
        assertNotNull(m.other);
        assertEquals(1, m.size());
        assertFalse(m.isEmpty());
        m.put($.varDep(1), "v");
        assertNotNull(m.id);
        assertEquals(2, m.size());
        m.put($.varDep(1), "v");
        assertEquals(2, m.size()); 

        assertEquals("{#1=v, x=a}", m.toString());

        assertEquals("v", m.remove($.varDep(1)));
        assertEquals(1, m.size());
        assertNull(m.remove($.varDep(1)));

        assertEquals("a", m.remove($.the("x")));
        assertEquals(0, m.size());

        
        m.put($.the("x"), "a");

        m.clear();

        assertEquals(0, m.size());
        assertTrue(m.isEmpty());



    }

    @Test public void testNegAnonKeys() {

        TermHashMap m = new TermHashMap();
        m.put(Anom.the(1), "p");
        m.put(Anom.the(1).neg(), "n");
        assertEquals(2, m.size());
        assertEquals("p", m.get(Anom.the(1)));
        assertEquals("n", m.get(Anom.the(1).neg()));
        assertTrue(m.other.isEmpty()); //no need to instantiate other for neg
    }

}