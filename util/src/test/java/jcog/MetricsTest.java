/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog;

import jcog.signal.meter.FunctionMeter;
import jcog.signal.meter.TemporalMetrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author me
 */
class MetricsTest {

    private static final FunctionMeter<Integer> timeDoubler = new FunctionMeter<Integer>("x") {

        @Override
        public Integer getValue(Object when, int index) {
            assertEquals(0, index);
            assertTrue(when instanceof Double);
            return ((Double) when).intValue() * 2;
        }
    };

    @Test
    void testTemporalMetrics() {


        TemporalMetrics tm = new TemporalMetrics(3);
        tm.add(timeDoubler);

        assertEquals(0, tm.numRows());
        assertEquals(2, tm.getSignals().size(), "signal columns: time and 'x'");

        tm.update(1.0);

        assertEquals(1, tm.numRows());
        assertEquals(2, tm.getData(1)[0]);

        tm.update(1.5);
        tm.update(2.0);

        assertEquals(3, tm.numRows());

        tm.update(2.5);

        assertEquals(3, tm.numRows());
    }


}
