package jcog.exe;

import jcog.math.FloatAveragedWindow;

import java.util.SortedMap;

/** loop which collects timing measurements */
public abstract class InstrumentedLoop extends Loop {



    protected static final int windowLength = 4;

    /**
     * in seconds
     */
    public final FloatAveragedWindow dutyTime = new FloatAveragedWindow(windowLength, 0.5f, false);
    public final FloatAveragedWindow cycleTime =
            new FloatAveragedWindow(windowLength, 1f/ (float) windowLength /* == non-exponential mean? */, false);

    /** the current cycle time delta (nanoseconds) */
    public long cycleTimeNS = 0L;

    /** the current cycle time delta (seconds) */
    public double cycleTimeS = (double) 0;

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
        cycleTimeS = (double) cycleTimeNS / 1.0E9;
        this.cycleTime.next((float) cycleTimeS);

        long dutyTimeNS = afterIteration - beforeIteration;
        double dutyTimeS = (double) dutyTimeNS / 1.0E9;
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

}
