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
public class HitMeter extends FunctionMeter<Long> {
    
    private boolean autoReset;
    public final MutableLong hits = new MutableLong();
    
    public HitMeter(String id, boolean autoReset) {
        super(id);
        this.autoReset = autoReset;
    }

    @Override
    public String toString() {
        return signalID(0) + '=' + hits.get();
    }

    public HitMeter(String id) {
        this(id, true);
    }    
    
    public HitMeter reset() {
        hits.set(0L);
        return this;
    }

    public long hit() {
        hits.add(1L);
        return hits.get();
    }

    public long hit(int n) {
        hits.add((long) n); return hits.get();
    }
    
    public long count() {
        return hits.get();
    }
    
    @Override
    public Long getValue(Object key, int index) {
        long c = count();
        if (autoReset) {
            reset();
        }
        return c;        
    }

    /** whether to reset the hit count after the count is stored in the Metrics */
    public void setAutoReset(boolean autoReset) {
        this.autoReset = autoReset;
    }
    
    public boolean getAutoReset() { return autoReset; }
    
    
    
}
