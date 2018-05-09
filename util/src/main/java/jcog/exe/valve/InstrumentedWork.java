package jcog.exe.valve;

import jcog.Texts;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import static java.lang.System.nanoTime;

public class InstrumentedWork implements Work {

    final Work work;

    //final org.HdrHistogram.Histogram startupTime = new org.HdrHistogram.Histogram();

    static final int WINDOW = 16;

    /** total accumulated start/stop tme each cycle */
    final DescriptiveStatistics startAndStopTimeNS = new DescriptiveStatistics(WINDOW);

    /** total accumulated work time, each cycle */
    final DescriptiveStatistics iterTimeNS = new DescriptiveStatistics(WINDOW);

    /** total iterations, each cycle */
    final DescriptiveStatistics iterations = new DescriptiveStatistics(WINDOW);

    /** owner */
    public final Share share;

    long beforeStart, afterStart, beforeEnd, afterEnd;
    long workTimeThisCycleNS;
    int iterationsThisCycle;

    public InstrumentedWork(Work work, Share s) {
        this.work = work;
        this.share = s;
    }

    @Override
    public boolean start() {
        //TODO measure start-up time
        beforeStart = nanoTime();
        boolean starting = work.start();
        afterStart = nanoTime();

        return starting;
    }

    @Override
    public final boolean next() {
        //TODO catch interrupted exception etc

        long a = nanoTime();
        boolean kontinue = work.next();
        workTimeThisCycleNS += (nanoTime() - a);
        iterationsThisCycle++;
        return kontinue;
    }

    @Override
    public void stop() {

        if (iterationsThisCycle > 0) {
            iterTimeNS.addValue(workTimeThisCycleNS);
            iterations.addValue(iterationsThisCycle);
            //iterMeanTimeNS.addValue(((double)iterationAccum)/iterations );

            iterationsThisCycle = 0;
            workTimeThisCycleNS = 0;
        }

        beforeEnd = nanoTime();
        work.stop();
        afterEnd = nanoTime();
        startAndStopTimeNS.addValue((afterStart - beforeStart) + (afterEnd - beforeEnd));
    }

    public String summary() {
        return share +
          "{ " + "startStopTimeMeanNS=" + Texts.timeStr(startAndStopTimeNS.getMean())
        + ", " + "iterTimeMeanNS=" + Texts.timeStr(iterTimeNS.getMean())
        + ", " + "itersMean=" + Texts.n2(iterations.getMean())
        + "}";
    }
}
