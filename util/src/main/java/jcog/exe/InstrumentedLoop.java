package jcog.exe;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.SortedMap;

/** loop which collects timing measurements */
abstract public class InstrumentedLoop extends Loop {

    private long beforeIteration;
//    private float lag, lagSum;

    protected final int windowLength = 4;


    //    /**
//     * in seconds
//     */
    public final DescriptiveStatistics dutyTime = new DescriptiveStatistics(windowLength); //in millisecond
    public final DescriptiveStatistics cycleTime = new DescriptiveStatistics(windowLength); //in millisecond


    @Override
    protected void beforeNext() {
        beforeIteration = System.nanoTime();
    }

    @Override
    protected void afterNext() {
        long lastIteration = this.last;
        long afterIteration = System.nanoTime();
        this.last = afterIteration;

        long dutyTimeNS = afterIteration - beforeIteration;
        double dutyTimeS = (dutyTimeNS) / 1.0E9;

        double cycleTimeS = (afterIteration - lastIteration) / 1.0E9;

        this.dutyTime.addValue(dutyTimeS);
        this.cycleTime.addValue(cycleTimeS);

    }


    public void stats(String prefix, SortedMap<String, Object> x) {
        x.put(prefix + " cycle time mean", cycleTime.getMean()); //in seconds
        x.put(prefix + " cycle time vary", cycleTime.getVariance()); //in seconds
        x.put(prefix + " duty time mean", dutyTime.getMean()); //in seconds
        x.put(prefix + " duty time vary", dutyTime.getVariance()); //in seconds
        //x.put(prefix + " lag", lag);
    }
}
