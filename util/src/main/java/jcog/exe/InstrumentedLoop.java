package jcog.exe;

import jcog.math.FloatAveragedWindow;

import java.util.SortedMap;

/** loop which collects timing measurements */
abstract public class InstrumentedLoop extends Loop {



    protected final int windowLength = 8;

    /**
     * in seconds
     */
    public final FloatAveragedWindow dutyTime = new FloatAveragedWindow(windowLength, 0.5f);
    public final FloatAveragedWindow cycleTime =
            new FloatAveragedWindow(windowLength, 1f/windowLength /* == non-exponential mean? */);

    /** the current cycle time delta (nanoseconds) */
    public long cycleTimeNS = 0;

    /** the current cycle time delta (seconds) */
    public double cycleTimeS = 0;

    protected volatile long last;
    protected long beforeIteration;

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
        this.cycleTime.next((float) cycleTimeS);

        long dutyTimeNS = afterIteration - beforeIteration;
        double dutyTimeS = (dutyTimeNS) / 1.0E9;
        this.dutyTime.next((float) dutyTimeS);
    }


    public void stats(String prefix, SortedMap<String, Object> x) {
        x.put(prefix + " cycle time mean", cycleTime.asFloat());
        //x.put(prefix + " cycle time vary", cycleTime.getVariance());
        x.put(prefix + " duty time mean", dutyTime.asFloat());
        //x.put(prefix + " duty time vary", dutyTime.getVariance());
    }

    @Override
    protected void starting() {
        last = System.nanoTime();
        super.starting();
    }

    @Override
    protected void stopping() {
//        cycleTime.clear();
//        dutyTime.clear();
    }
}
