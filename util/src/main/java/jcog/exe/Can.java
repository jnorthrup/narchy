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
//    final static int WINDOW = 8;
//    protected final DescriptiveStatistics value = new DescriptiveStatistics(WINDOW);

    /**
     * next iterations, to be solved
     */
    public final DoubleVar iterations;

    public Can() {
        this(String.valueOf(ids.incrementAndGet()));
    }

    public Can(String id) {
        iterations = new DoubleVar(id);
    }

    /**
     * in seconds
     */
    public float iterationTimeSum() {
        double s = iterationTime.sumThenReset();
        if (s!=s) return 0f;
        return (float) s;
    }

    /**
     * max iterations that can/should be requested
     */
    public double supply() {
        double s = supply.sumThenReset();
        if (s != s) return 0;
        return s;
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
    public void update(int supplied, double totalTimeSec) {
        supply.add(supplied);
        if (supplied > 0) {
//            value.addValue(totalValue / supplied);
            iterationTime.add(totalTimeSec);
        }
    }

    public final void commit(double iterations) {
        this.iterations.value(iterations);
//        commit();
    }

//    /** called after the iteration variable has been set */
//    public void commit() {
//
//    }

    @Override
    public String toString() {
        return iterations.name;
//        return iterations.name + "{" +
//                "iterations=" + iterations() +
//                ", value=" + n4(value()) +
//                ", iterationTime=" + Texts.strNS(Math.round(iterationTime.getMean()*1.0E9)) +
//                ", supply=" + n2(supply.getMax()) +
//                '}';
    }

    /** estimated iterations this should be run next, given the value and historically estimated cost */
    public int iterations() {
        return (int) Math.ceil(iterations.value());
    }
    public double iterationsRaw() {
        return iterations.value();
    }



}
