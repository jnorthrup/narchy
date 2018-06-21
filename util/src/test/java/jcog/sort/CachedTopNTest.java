package jcog.sort;

import jcog.list.FasterList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CachedTopNTest {

    @Test
    void testTopN() {
        TopN<String> c = new TopN<String>(new String[3], s -> s.length());
        assertAdd(c, "a", "[a]");
        assertAdd(c, "a", "[a, a]"); //duplicate kept
        c.clear();
        assertAdd(c, "a", "[a]");
        assertAdd(c, "bbb", "[bbb, a]");
        assertAdd(c, "cc", "[bbb, cc, a]");
        assertAdd(c, "dd", "[bbb, cc, dd]");
        assertAdd(c, "eee", "[bbb, eee, dd]");
        assertAdd(c, "ff", "[bbb, eee, dd]");  //disallow replacement of equal to weakest
    }

    @Test
    void testCachedTopN() {
        CachedTopN<String> c = new CachedTopN<String>(3, s -> s.length());
        assertAdd(c, "a", "[1.0000 a]");
        assertAdd(c, "a", "[1.0000 a]"); //duplicate absorbed
        assertAdd(c, "bbb", "[3.0000 bbb, 1.0000 a]");
        assertAdd(c, "cc", "[3.0000 bbb, 2.0000 cc, 1.0000 a]");
        assertAdd(c, "dd", "[3.0000 bbb, 2.0000 cc, 2.0000 dd]");
        assertAdd(c, "eee", "[3.0000 bbb, 3.0000 eee, 2.0000 dd]");
        assertAdd(c, "ff", "[3.0000 bbb, 3.0000 eee, 2.0000 dd]");  //disallow replacement of equal to weakest
    }

    private static void assertAdd(TopN<String> c, String x, String expect) {
        c.add(x); assertEquals(expect, new FasterList(c).toString());
    }
    private static void assertAdd(CachedTopN<String> c, String x, String expect) {
        c.accept(x); assertEquals(expect, new FasterList(c.list).toString());
    }
}