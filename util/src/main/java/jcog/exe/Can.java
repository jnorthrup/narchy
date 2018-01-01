package jcog.exe;

import com.google.common.math.Stats;
import com.google.common.math.StatsAccumulator;
import jcog.Util;
import jcog.constraint.continuous.DoubleVar;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.DoubleAdder;

/** potentially executable procedure of some value N >=1 iterations per invocation */
public class Can {

    private final static AtomicInteger ids = new AtomicInteger();




    final DoubleAdder iterationTime = new DoubleAdder();
    final DoubleAdder supply = new DoubleAdder();

    final static int WINDOW = 4;

    public final DescriptiveStatistics iterPerSecond = new DescriptiveStatistics(WINDOW);
    public double lastSupply, lastIterationTime;

    private final String id;


    public Can() {
        this(String.valueOf(ids.incrementAndGet()));
    }

    public Can(String id) {
        this.id = id;
    }


    public final void commit(int i, float[] time, float[] supplied, float[] iterPerSecond) {
        double s = supply.sumThenReset();
        if (s != s) s = 0;

        double t = iterationTime.sumThenReset();
        if (t!=t) t = 0;


        double rMean;
        if (Math.abs(t) > 1.0E-9 /* nanosecond resolution */) {
            double r = s / t;
            this.iterPerSecond.addValue(r);
        }
        rMean = this.iterPerSecond.getMean();
        if (rMean!=rMean) rMean = Float.MIN_NORMAL;


        supplied[i] = (float) s;
        this.lastSupply = s;
        time[i] = (float) t;
        this.lastIterationTime = t;
        iterPerSecond[i] = (float)rMean;
    }



    /**
     * totalTime in sec
     */
    public void done(int supplied, double totalTimeSec) {
        supply.add(supplied);
        if (supplied > 0) {
            iterationTime.add(totalTimeSec);
        }
    }

    @Override
    public String toString() {
        return id;
    }

}
