package jcog.sort;

import jcog.data.list.FasterList;
import org.junit.jupiter.api.Test;

import static java.lang.Float.NEGATIVE_INFINITY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** tests for Top, TopN, and CachedTopN */
class TopTest {

    @Test
    void testTop() {
        Top<String> c = new Top<String>(String::length);
        assertAdd(c, "x", "x");
        assertAdd(c, "xx", "xx");
        assertAdd(c, "y", "xx"); //unchanged, less
        assertAdd(c, "yy", "xx"); //unchanged, equal
        assertAdd(c, "yyy", "yyy");

    }

    @Test
    void testTopN() {
        TopN<String> c = new TopN<String>(new String[3], String::length);
        assertEquals(NEGATIVE_INFINITY, c.minValueIfFull());
        assertAdd(c, "a", "[a]");
        assertAdd(c, "a", "[a, a]"); //duplicate kept
        assertEquals(1, c.minValue());
        assertEquals(NEGATIVE_INFINITY, c.minValueIfFull());
        c.clear();
        assertAdd(c, "a", "[a]");
        assertAdd(c, "bbb", "[bbb, a]");
        assertAdd(c, "cc", "[bbb, cc, a]");
        assertEquals(1, c.minValueIfFull());
        assertAdd(c, "dd", "[bbb, cc, dd]");
        assertEquals(2, c.minValueIfFull());
        assertAdd(c, "eee", "[bbb, eee, cc]");
        assertAdd(c, "ff", "[bbb, eee, cc]");  //disallow replacement of equal to weakest
        assertAdd(c, "BBB", "[bbb, eee, BBB]");
        assertAdd(c, "xxxx", "[xxxx, bbb, eee]");
        assertAdd(c, "yyyyy", "[yyyyy, xxxx, bbb]");
        c.clear();
        assertEquals(NEGATIVE_INFINITY, c.minValueIfFull());
    }

//    @Test
//    void testCachedTopN() {
//        CachedTopN<String> c = new CachedTopN<String>(3, String::length);
//        assertAdd(c, "a", "[1 a]");
//        assertAdd(c, "a", "[1 a]"); //duplicate absorbed
//        assertAdd(c, "bbb", "[3 bbb, 1 a]");
//        assertAdd(c, "cc", "[3 bbb, 2 cc, 1 a]");
//        assertAdd(c, "dd", "[3 bbb, 2 cc, 2 dd]");
//        assertAdd(c, "eee", "[3 bbb, 3 eee, 2 cc]");
//        assertAdd(c, "ff", "[3 bbb, 3 eee, 2 cc]");  //disallow replacement of equal to weakest
//    }

    private static void assertAdd(Top<String> c, String x, String expect) {
        c.accept(x); assertEquals(expect, c.the.toString());
    }
    private static void assertAdd(TopN<String> c, String x, String expect) {
        c.accept(x); assertEquals(expect, new FasterList(c).toString());
    }
//    private static void assertAdd(CachedTopN<String> c, String x, String expect) {
//        c.accept(x); assertEquals(expect, new FasterList(c.list).toString());
//    }
}