package nars.concept.signal;

import jcog.data.atomic.AtomicFloat;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.sensor.Signal;
import nars.control.DurService;
import nars.table.dynamic.SensorBeliefTables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensorBeliefTablesTest {

    @Test
    void test1() {
        NAR n = NARS.shell();

        AtomicFloat xx = new AtomicFloat(0);
        Signal x = new Signal($.the("x"), xx, n);

        DurService xAuto = x.auto(n, 1);
        n.run(1);

        SensorBeliefTables xb = (SensorBeliefTables) x.beliefs();
        n.run(1);
        assertEquals(1, xb.series.size());
        assertEquals(1, xb.size());

        xx.set(0.5f);
        n.run(1);

        assertEquals(2, xb.series.size());
        assertEquals(2, xb.size());

    }

}