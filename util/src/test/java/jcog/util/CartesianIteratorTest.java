package jcog.util;

import com.google.common.base.Joiner;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CartesianIteratorTest {












    @Test
    void test_0() throws Exception {
        CartesianIterator<String> it = new CartesianIterator(String[]::new,
                List.of(new String[0]));

        assertFalse(it.hasNext());
        try {
            assertEquals("", Joiner.on("").join(it.next()));
            fail("Should throw NoSuchElementException");
        } catch (Exception e) { /* Ignore exception */ }
    }

    @Test
    void test_1() throws Exception {
        CartesianIterator<String> it = new CartesianIterator(String[]::new,
                List.of(new String[] {"a"}));

        assertTrue(it.hasNext());
        assertEquals("a", Joiner.on("").join(it.next()));
        assertFalse(it.hasNext());
    }

    @Test
    void test_2() throws Exception {
        CartesianIterator it = new CartesianIterator(String[]::new,
                Arrays.asList(new String[] {"a", "b"}));

        assertTrue(it.hasNext());
        assertEquals("a", Joiner.on("").join(it.next()));
        assertTrue(it.hasNext());
        assertEquals("b", Joiner.on("").join(it.next()));
        assertFalse(it.hasNext());
    }

    @Test
    void test_2_0() throws Exception {
        CartesianIterator it = new CartesianIterator(String[]::new,
                Arrays.asList(new String[] {"a", "b"}),
                Arrays.asList(new String[] {}));

        assertFalse(it.hasNext());
        try {
            assertEquals("", Joiner.on("").join(it.next()));
            fail("Should throw NoSuchElementException");
        } catch (Exception e) { /* Ignore exception */ }
    }

    @Test
    void test_2_2() throws Exception {
        CartesianIterator it = new CartesianIterator(String[]::new,
                Arrays.asList(new String[] {"a", "b"}),
                Arrays.asList(new String[] {"c", "d"}));

        assertTrue(it.hasNext());
        assertEquals("ac", Joiner.on("").join(it.next()));
        assertTrue(it.hasNext());
        assertEquals("ad", Joiner.on("").join(it.next()));
        assertTrue(it.hasNext());
        assertEquals("bc", Joiner.on("").join(it.next()));
        assertTrue(it.hasNext());
        assertEquals("bd", Joiner.on("").join(it.next()));
        assertFalse(it.hasNext());
    }

    @Test
    void test_2_3_4() throws Exception {
        CartesianIterator it = new CartesianIterator(String[]::new,
                Arrays.asList(new String[] {"a", "b"}),
                Arrays.asList(new String[] {"c", "d", "e"}),
                Arrays.asList(new String[] {"f", "g", "h", "i"}));

        assertTrue(it.hasNext());
        assertEquals("acf", Joiner.on("").join(it.next()));
        assertTrue(it.hasNext());
        assertEquals("acg", Joiner.on("").join(it.next()));
        assertEquals("ach", Joiner.on("").join(it.next()));
        assertEquals("aci", Joiner.on("").join(it.next()));
        assertEquals("adf", Joiner.on("").join(it.next()));
        assertEquals("adg", Joiner.on("").join(it.next()));
        assertEquals("adh", Joiner.on("").join(it.next()));
        assertEquals("adi", Joiner.on("").join(it.next()));
        assertEquals("aef", Joiner.on("").join(it.next()));
        assertEquals("aeg", Joiner.on("").join(it.next()));
        assertEquals("aeh", Joiner.on("").join(it.next()));
        assertEquals("aei", Joiner.on("").join(it.next()));
        assertEquals("bcf", Joiner.on("").join(it.next()));
        assertEquals("bcg", Joiner.on("").join(it.next()));
        assertEquals("bch", Joiner.on("").join(it.next()));
        assertEquals("bci", Joiner.on("").join(it.next()));
        assertEquals("bdf", Joiner.on("").join(it.next()));
        assertEquals("bdg", Joiner.on("").join(it.next()));
        assertEquals("bdh", Joiner.on("").join(it.next()));
        assertEquals("bdi", Joiner.on("").join(it.next()));
        assertEquals("bef", Joiner.on("").join(it.next()));
        assertEquals("beg", Joiner.on("").join(it.next()));
        assertEquals("beh", Joiner.on("").join(it.next()));
        assertTrue(it.hasNext());
        assertEquals("bei", Joiner.on("").join(it.next()));
        assertFalse(it.hasNext());
    }

}