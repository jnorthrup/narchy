/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.signal.meter.event;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Stores the latest provided value for retrieval by a Metrics 
 */
public class DoubleMeter extends SourceFunctionMeter<Double> implements DoubleConsumer {
    
    boolean autoReset;
    
    double val;
    private final String name;
    
    
    
    public DoubleMeter(String id, boolean autoReset) {
        super(id);
        name = id;
        this.autoReset = autoReset;
    }
    
    
    public DoubleMeter(String id) {
        this(id, false);
    }

    public static DoubleMeter get(String id, DoubleSupplier x) {
        return new DoubleMeter(id, true) {
            @Override
            public DoubleMeter reset() {
                set(x.getAsDouble());
                return this;
            }
        };
    }


    public DoubleMeter reset() {
        set(Double.NaN);
        return this;
    }
    
    /** returns the previous value, or NaN if none were set  */
    public double set(double newValue) {
        double oldValue = val;
        val = (newValue);
        return oldValue;
    }

    /** current stored value */
    public double get() { return val; }

    
    @Override
    public Double getValue(Object key, int index) {
        double c = val;
        if (autoReset) {
            reset();
        }
        return c;        
    }

    /** whether to reset to NaN after the count is next stored in the Metrics */
    public void setAutoReset(boolean autoReset) {
        this.autoReset = autoReset;
    }
    
    public boolean getAutoReset() { return autoReset; }

    @Override
    public String toString() {
        return name + super.toString();
    }

    @Override
    public void accept(double value) {
        set(value);
    }
}
