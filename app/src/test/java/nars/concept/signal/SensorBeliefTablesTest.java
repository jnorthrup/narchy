package nars.concept.signal;

import jcog.data.atomic.AtomicFloat;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.agent.NAgent;
import nars.concept.sensor.Signal;
import nars.table.dynamic.SensorBeliefTables;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorBeliefTablesTest {

    @Test
    void test1() {
        NAR n = NARS.shell();

        NAgent a = new NAgent("a", n);

        AtomicFloat xx = new AtomicFloat(0);

        Signal x = new Signal($.the("x"), xx, n);
        a.addSensor(x);

        SensorBeliefTables xb = (SensorBeliefTables) x.beliefs();

        step(n, xb);
        assertEquals(1, xb.series.size());
        assertEquals(1, xb.size());

        xx.set(0.5f);
        step(n, xb);
        step(n, xb);

        assertEquals(2, xb.series.size());
        assertEquals(2, xb.size());

        {
            List<Task> tt = xb.streamTasks().collect(toList());
            assertEquals(2, tt.size());
            assertEquals(2, tt.get(0).range());
            assertEquals(2, tt.get(1).range());

            assertEquals(1, tt.get(0).stamp()[0]);
            assertEquals(2, tt.get(1).stamp()[0]);
            //assertTrue(!Arrays.equals(tt.get(0).stamp(), tt.get(1).stamp()));
        }

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