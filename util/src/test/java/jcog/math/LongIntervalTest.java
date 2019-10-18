package jcog.math;

import org.junit.jupiter.api.Test;

import static jcog.math.LongInterval.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LongIntervalTest {


    @Test
    void testTaskNearestTimePoint_point() {
        assertNearTests(15, 15);
    }
    @Test
    void testTaskNearestTimePoint_range() {
        Longerval t = assertNearTests(10, 20);
        assertEquals(18, t.nearestPointExternal(18,18)); 
        assertEquals(12, t.nearestPointExternal(12,12)); 
    }
    @Test
    void testTaskNearestTimePoint_eternal() {
        Longerval t = assertNearTests(ETERNAL, ETERNAL);
    }

    private static Longerval assertNearTests(long sta, long end) {

        Longerval t = new Longerval(sta, end);
        long mid = t.mid();

        assertEquals(sta, t.start());
        assertEquals(end, t.end());

        assertEquals(end, t.nearestPointExternal(end, end));

        if (end!=ETERNAL) {
            assertEquals(end, t.nearestPointExternal(end, end + 5));
            assertEquals(end + 5, t.nearestPointExternal(end + 5, end + 5));
        }
        assertEquals(mid, t.nearestPointExternal(mid,mid)); 
        assertEquals(0, t.nearestPointExternal(0,0));
        assertEquals(ETERNAL, t.nearestPointExternal(ETERNAL,ETERNAL));
        assertEquals(t.mid(), t.nearestPointInternal(ETERNAL, ETERNAL));

        assertEquals(t.start(), t.nearestPointInternal(-1, -1)); 
        assertEquals(t.end(), t.nearestPointInternal(100, 100)); 

        if (sta!=ETERNAL) {
            assertEquals(mid, t.nearestPointInternal(mid - 1, mid + 1)); 
            assertEquals(mid, t.nearestPointInternal(mid, mid));
            assertEquals(sta, t.nearestPointInternal(0, 1));
            assertEquals(sta, t.nearestPointInternal(0, mid));
            assertEquals(end, t.nearestPointInternal(30, 40));
            assertEquals((9+21)/2, t.nearestPointInternal(9, 21)); 
        } else {
            assertEquals(ETERNAL, t.nearestPointInternal(9, 21)); 
        }


        return t;
    }

    @Test
    void testTaskTimeSanity1() {
        Longerval x10 = new Longerval(0, 9);
        assertEquals(100, x10.nearestPointExternal(100, 200));
    }

    @Test
    void testTaskTimeSanity2() {
        Longerval t = new Longerval(1530, 1545);
        assertEquals(2966, t.nearestPointExternal(2966, 2980));
        assertEquals(0, t.nearestPointExternal(0, 0));
        assertEquals(1537, t.nearestPointExternal(1530, 1545)); 
        assertEquals(1545, t.nearestPointExternal(1545, 1545));
        assertEquals(1545, t.nearestPointExternal(1545, 1546));
        assertEquals(1546, t.nearestPointExternal(1546, 1546));
    }

    @Test void testMinTimeTo() {
        Longerval t = new Longerval(1530, 1545);
        assertEquals(0, t.minTimeTo(1530)); //left edge
        assertEquals(0, t.minTimeTo(1537)); //internal
        assertEquals(0, t.minTimeTo(1545)); //right edge

        assertEquals(10, t.minTimeTo(1520)); //before
        assertEquals(10, t.minTimeTo(1555)); //after

        assertEquals(0, t.minTimeTo(ETERNAL)); //eternal
    }

    @Test void testMinTimeToRange() {
        Longerval t = new Longerval(1530, 1545);
        assertEquals(0, t.minTimeTo(1530,1532)); //internal
        assertEquals(0, t.minTimeTo(1520,1531)); //intersect
        assertEquals(5, t.minTimeTo(1520,1525)); //disjoint before
    }

    @Test void testMeanTimeTo() {
        Longerval t = new Longerval(1530, 1545);
        assertEquals(0, t.meanTimeTo(1530)); //left edge
        assertEquals(0, t.meanTimeTo(1537)); //internal
        assertEquals(0, t.meanTimeTo(1545)); //right edge

        assertEquals(17, t.meanTimeTo(1520)); //before
        assertEquals(13, t.meanTimeTo(1551)); //after

        assertEquals(0, t.meanTimeTo(ETERNAL)); //eternal
    }

}