/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.signal.meter.event;

import jcog.signal.meter.FunctionMeter;
import jcog.util.MutableLong;

/**
 *
 * @author me
 */
public class HitMissMeter extends FunctionMeter<Double> {

    private boolean autoReset;
    public final MutableLong hit = new MutableLong();
    public final MutableLong miss = new MutableLong();

    public HitMissMeter(String id, boolean autoReset) {
        super(id);
        this.autoReset = autoReset;
    }

    @Override
    public String toString() {
        return signalID(0) + '=' + hits() + '/' + (hits() + miss.longValue()); }

    public long hits() {
        return hit.longValue();
    }
    public long misses() {
        return miss.longValue();
    }
    public float ratio() {
        long h = hits();
        long sum = h + misses();
        if (sum == 0) return Float.NaN;
        return h/((float)sum);
    }



    public HitMissMeter(String id) {
        this(id, true);
    }    
    
    public HitMissMeter reset() {
        hit.set(0);
        miss.set(0);
        return this;
    }

    public void hit() {
        hit.add(1);
    }
    public void miss() {
        miss.add(1);
    }


    public long count() {
        return hits()+misses();
    }
    
    @Override
    public Double getValue(Object key, int index) {
        float r = ratio();
        if (autoReset) {
            reset();
        }
        return (double) r;
    }

    /** whether to reset the hit count after the count is stored in the Metrics */
    public void setAutoReset(boolean autoReset) {
        this.autoReset = autoReset;
    }
    
    public boolean getAutoReset() { return autoReset; }
    
    
    
}
