package nars.concept.signal;

import jcog.data.atomic.AtomicFloat;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.game.Game;
import nars.game.sensor.ScalarSignal;
import nars.game.sensor.Signal;
import nars.table.dynamic.SensorBeliefTables;
import nars.task.util.series.RingBufferTaskSeries;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.*;

class SensorBeliefTablesTest {

    @Test
    void test1() {
        NAR n = NARS.shell();

        Game a = new Game("a");
        n.add(a);

        AtomicFloat xx = new AtomicFloat(0);

        Signal x = new ScalarSignal($.INSTANCE.the("x"), xx, n);
        a.addSensor(x);

        SensorBeliefTables xb = (SensorBeliefTables) x.beliefs();

        step(n, xb);
        assertEquals(1, xb.taskCount());
        step(n, xb);
        assertEquals(1, xb.taskCount()); //same but stretch

        xx.set(0.5f);
        step(n, xb);
        step(n, xb);

        assertEquals(2, xb.taskCount());

        {
            List<Task> tt = xb.taskStream().sorted(Comparator.comparing(Task::start)).collect(toList());
            assertEquals(2, tt.size());
            assertEquals(2, tt.get(0).range());
            assertEquals(2, tt.get(1).range());

            assertArrayEquals(new long[] { 1 }, tt.get(0).stamp());
            assertArrayEquals(new long[] { 2 }, tt.get(1).stamp());
            //assertTrue(!Arrays.equals(tt.get(0).stamp(), tt.get(1).stamp()));
        }

        xx.set(0.75f);
        step(n, xb);
        {
            List<Task> tt = xb.taskStream().collect(toList());
            assertEquals(3, tt.size());
        }

        RingBufferTaskSeries rb = (RingBufferTaskSeries) (xb.series.series);
        int head = rb.q.head();
        assertEquals(0, rb.indexNear(head,0));
        assertEquals(0, rb.indexNear(head,1));
        assertEquals(1, rb.indexNear(head,2));
        assertEquals(2, rb.indexNear(head,4));
        assertEquals(2, rb.indexNear(head,5));
        assertEquals(2, rb.indexNear(head,6));
        assertEquals(2, rb.indexNear(head,1000));
        assertEquals(0, rb.indexNear(head,-5));
        assertTrue(rb.first().start() < rb.last().start());

        //stretch another step
        step(n, xb);


        //test truthpolation of projected future signal, which should decay in confidence
        assertTrue(n.beliefTruth(x, n.time() + 10).conf() < n.confDefault(BELIEF) - 0.1f);

//        for (int i = 0; i < 100; i++) {
//            long then = n.time() + i;
//            System.out.println("@" + then + ": " + n.beliefTruth(x, then));
//        }







    }

    private static void step(NAR n, SensorBeliefTables xb) {
        n.run(1);
        System.out.println("@" + n.time());
        xb.print();
        System.out.println();
    }

}