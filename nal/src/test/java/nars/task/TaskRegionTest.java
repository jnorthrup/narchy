package nars.task;

import jcog.math.LongInterval;
import jcog.tree.rtree.HyperRegion;
import nars.$;
import nars.DummyNAL;
import nars.NAL;
import nars.Task;
import nars.task.util.TaskRegion;
import nars.task.util.TasksRegion;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.*;

class TaskRegionTest {

    static final Term x = Atomic.atom("x");
    private static final NAL n = new DummyNAL();

    @Test void testTasksRegionContainsTask() {
        //@1..9[0.6..1;0.9..0.9%]
        //$.50 x. 1 %.60;.90%

        TasksRegion c = new TasksRegion(1, 9, 0.6f, 1f, 0.9f, 0.9f);
        Task x = _task(0.6f, 0.9f, 1, 1);
        assertTrue(c.contains((LongInterval)x));
        assertTrue(c.intersects((LongInterval)x));
        assertTrue(c.contains((HyperRegion)x));
        assertTrue(c.intersects((HyperRegion)x));
        assertEquals(c, c.mbr(x));
        assertEquals(c, x.mbr(c));
    }

    @Test
    void testMBR() {

        Task a = _task(0.5f, 0.5f, 0, 1);
        assertEquals(0, a.start());
        assertEquals(1, a.end());
        assertEquals(2, a.range());
        assertSame(a, a.mbr(a));

        //stretch only f, c
        Task b = _task(0.3f, 0.7f, 0, 1);
        validateStretch("@0..1[0.3..0.5;0.5..0.7%]", a, b);

        //stretch only time
        Task c = _task(0.5f, 0.5f, -1, 3);
        validateStretch("@-1..3[0.5..0.5;0.5..0.5%]", a, c);

        //stretch both time and f,c:
        Task d = _task(0.3f, 0.7f, -1, 3);
        validateStretch("@-1..3[0.3..0.5;0.5..0.7%]", a, d);

        assertEquals(a.mbr(d), a.mbr(b).mbr(a.mbr(c)));
    }

    static private void validateStretch(String expect, Task a, Task b) {
        TaskRegion ba = b.mbr(a);
        TaskRegion ab = a.mbr(b);
        assertEquals(ba, ab);

        assertFalse(ba.equals(a));
        assertFalse(ab.equals(a));
        assertFalse(ba.equals(b));
        assertFalse(ab.equals(b));

        assertEquals(expect, ab.toString());
        assertEquals(expect, ba.toString());

        assertTrue(ba.contains((LongInterval)a));
        assertTrue(ab.contains((LongInterval)a));
        assertTrue(ba.contains((LongInterval)b));
        assertTrue(ab.contains((LongInterval)b));
        assertTrue(ba.intersects((LongInterval)a));
        assertTrue(ab.intersects((LongInterval)a));
        assertTrue(ba.intersects((LongInterval)b));
        assertTrue(ab.intersects((LongInterval)b));

        assertSame(ab, ab.mbr(ba));
        assertSame(ab, ab.mbr(ab));
        assertSame(ba, ba.mbr(ba));
        assertSame(ba, ba.mbr(ab));
    }

    private static Task _task(float f, float c, long s, long e) {
        return new TaskBuilder(x, BELIEF, $.t(f, c)).time(s, e).apply(n);
    }
}
