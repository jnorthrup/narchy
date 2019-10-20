package jcog.signal.meter.resource;

import jcog.Texts;
import jcog.signal.meter.event.DoubleMeter;

/**
 * Relatively slow, use a setResolutionDivisor to sample every Nth cycle
 * @author me
 * Uses Runtime methods to calculate changes in memory use, measured in KiloBytes (1024 bytes)
 * TODO also use https:
 */
public class MemoryUsageMeter extends DoubleMeter {

    

    public MemoryUsageMeter(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return Texts.n2((double) getMemoryUsed() /1024.0) + "kb use";
    }

    public static long getMemoryUsed() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }

    @Override
    public Double[] sample(Object key) {
        return new Double[] { (double)getMemoryUsed() };
    }
    
}
