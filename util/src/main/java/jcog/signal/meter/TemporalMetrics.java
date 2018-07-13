/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.signal.meter;

import jcog.signal.meter.event.DoubleMeter;

import java.util.function.DoubleSupplier;

/**
 *
 * @author me
 */
public class TemporalMetrics extends Metrics<Double> {

    public TemporalMetrics(int historySize) {
        super(historySize);
    }








    /** allows updating with an integer/long time, because it will be converted
     * to double internally
     */
    public void update(long integerTime) {
        update((double)integerTime);
    }

    public void add(String id, DoubleSupplier x) {
        add(DoubleMeter.get(id, x));
    }

}
