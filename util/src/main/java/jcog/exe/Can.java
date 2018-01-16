package jcog.exe;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * potentially executable procedure of some value N >=1 iterations per invocation
 */
public class Can {

    private final static AtomicInteger ids = new AtomicInteger();


    /**
     * in seconds
     */
    final DoubleAdder iterationTime = new DoubleAdder();

    final DoubleAdder supply = new DoubleAdder();

    final static int WINDOW = 8;

    public final DescriptiveStatistics supplyPerSecond = new DescriptiveStatistics(WINDOW);
    public double lastIterationTime;

    private final String id;


    public Can() {
        this(String.valueOf(ids.incrementAndGet()));
    }

    public Can(String id) {
        this.id = id;
    }


    public final void commit(int i, float[] supplyPerSecond) {
        double s = supply.sumThenReset();
        double t = Math.max(1E-9, iterationTime.sumThenReset()); //nanosecond resolution min granularity
        if (s != s) s = 0;
        if (t != t) t = 0;

        double r = s / t;
        this.supplyPerSecond.addValue(r);

        double rMean = this.supplyPerSecond.getMean();
        if (rMean != rMean) rMean = Float.MIN_NORMAL;


        this.lastIterationTime = (float) t;
        supplyPerSecond[i] = (float) rMean;
    }


    /**
     * totalTime in sec
     */
    public void done(int supplied, double totalTimeSec) {
        supply.add(supplied);
        iterationTime.add(totalTimeSec);
    }

    @Override
    public String toString() {
        return id;
    }

}
