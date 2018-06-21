/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog;

import jcog.meter.FunctionMeter;
import jcog.meter.TemporalMetrics;
import jcog.meter.func.BasicStatistics;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author me
 */
class MetricsTest {

    private static final FunctionMeter<Integer> timeDoubler = new FunctionMeter<Integer>("x") {

        @Override
        public Integer getValue(Object when, int index) {
            assertEquals(0, index);
            assertTrue(when instanceof Double);
            return ((Double)when).intValue() * 2;
        }            
    };
        
    @Test
    void testTemporalMetrics() {

        
        TemporalMetrics tm = new TemporalMetrics(3);
        tm.add(timeDoubler);
        
        assertEquals(0, tm.numRows());
        assertEquals(2, tm.getSignals().size(),"signal columns: time and 'x'");
        
        tm.update(1.0);
        
        assertEquals(1, tm.numRows());
        assertEquals(2, tm.getData(1)[0]);
        
        tm.update(1.5);
        tm.update(2.0);
        
        assertEquals(3, tm.numRows());
        
        tm.update(2.5);
        
        assertEquals(3, tm.numRows());
    }


























    
    @Disabled
    @Test
    void testSummaryStatistics() {

        TemporalMetrics tm = new TemporalMetrics(10);
        tm.add(new BasicStatistics(tm, tm.getSignalIDs()[0]));
        
        for (int i = 0; i < 10; i++) {
            tm.update(0.1 * i);
        }


        
        PrintStream sb = new PrintStream(System.out) {
        
            int line;
            
            @Override
            public void println(String x) {
                String eq = null;
                switch (line++) {
                    case 0: eq = "\"key\",\"key.mean\",\"key.stdev\""; break;
                    case 1: eq = "0,0,0"; break;
                    case 3: eq = "0.2,0.1,0.1"; break;
                }
                if (eq!=null) {
                    assertEquals(eq, x);
                }
            }          
        };
        tm.printCSV(sb);
        
    }
}
