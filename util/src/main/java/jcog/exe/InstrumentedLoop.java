package jcog.exe;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.SortedMap;

/** loop which collects timing measurements */
abstract public class InstrumentedLoop extends Loop {

    private long beforeIteration;


    protected final int windowLength = 4;

    /**
     * in seconds
     */
    public final DescriptiveStatistics dutyTime = new DescriptiveStatistics(windowLength); 
    public final DescriptiveStatistics cycleTime = new DescriptiveStatistics(windowLength); 

    public long cycleTimeNS = 0;
    public double cycleTimeS = 0;

    protected volatile long last = System.nanoTime();

    @Override
    protected void beforeNext() {
        beforeIteration = System.nanoTime();
    }

    @Override
    protected void afterNext() {
        long afterIteration = System.nanoTime();
        long lastIteration = this.last;
        this.last = afterIteration;

        cycleTimeNS = afterIteration - lastIteration;
        cycleTimeS = cycleTimeNS / 1.0E9;
        this.cycleTime.addValue(cycleTimeS);

        long dutyTimeNS = afterIteration - beforeIteration;
        double dutyTimeS = (dutyTimeNS) / 1.0E9;
        this.dutyTime.addValue(dutyTimeS);
    }


    public void stats(String prefix, SortedMap<String, Object> x) {
        x.put(prefix + " cycle time mean", cycleTime.getMean()); 
        x.put(prefix + " cycle time vary", cycleTime.getVariance()); 
        x.put(prefix + " duty time mean", dutyTime.getMean()); 
        x.put(prefix + " duty time vary", dutyTime.getVariance()); 
        
    }
}
