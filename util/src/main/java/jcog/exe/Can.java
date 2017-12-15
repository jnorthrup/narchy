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

    final static AtomicInteger ids = new AtomicInteger();




    final DoubleAdder iterationTime = new DoubleAdder();
    public final DoubleAdder supply = new DoubleAdder();

    final static int WINDOW = 8;
    protected final DescriptiveStatistics iterPerSecond = new DescriptiveStatistics(WINDOW);

    private final String id;
//    protected final DescriptiveStatistics value = new DescriptiveStatistics(WINDOW);
//
//    /**
//     * next iterations, to be solved
//     */
//    public final DoubleVar iterations;

    public Can() {
        this(String.valueOf(ids.incrementAndGet()));
    }

    public Can(String id) {
        this.id = id;
        //iterations = new DoubleVar(id);
    }


    public final void commit(int i, float[] time, float[] supplied, float[] iterPerSecond) {
        double s = supply.sumThenReset();
        if (s != s) s = 0;
        supplied[i] = (float) s;

        double t = iterationTime.sumThenReset();
        if (t!=t) t = 0;
        time[i] = (float) t;

        double r;
        if (Math.abs(s) < 0.01f || Math.abs(t) < 1E-9 /* nanosecond resolution */) {
            r = 0;
        } else {
            r = s / t;
            this.iterPerSecond.addValue(r);
        }
        double rMean = this.iterPerSecond.getMean();
        if (rMean!=rMean) rMean = 0;
        iterPerSecond[i] = (float)rMean;
    }


//    /**
//     * relative value of an iteration; ie. past value estimate divided by the actual supplied unit count
//     * >=0
//     */
//    public float value() {
//        double mean = value.getMean();
//
//        return mean != mean ? 0f : Math.max(0,Util.tanhFast((float) mean)+1);
//    }

    /**
     * totalTime in sec
     */
    public void done(int supplied, double totalTimeSec) {
        supply.add(supplied);
        if (supplied > 0) {
            iterationTime.add(totalTimeSec);
        }
    }


//    /** called after the iteration variable has been set */
//    public void commit() {
//
//    }

    @Override
    public String toString() {
        return id;
        //return iterations.name;
//        return iterations.name + "{" +
//                "iterations=" + iterations() +
//                ", value=" + n4(value()) +
//                ", iterationTime=" + Texts.strNS(Math.round(iterationTime.getMean()*1.0E9)) +
//                ", supply=" + n2(supply.getMax()) +
//                '}';
    }
//
//    /** estimated iterations this should be run next, given the value and historically estimated cost */
//    public int iterations() {
//        return (int) Math.ceil(iterations.value());
//    }
    //public double iterationsRaw() {
//        return iterations.value();
//    }



}
