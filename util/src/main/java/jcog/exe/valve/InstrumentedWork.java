package jcog.exe.valve;

import jcog.Texts;
import jcog.Util;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import static java.lang.System.nanoTime;

public class InstrumentedWork<Who,What> extends Share<Who,What> implements Work {

    final Work work;

    

    static final int WINDOW = 4;

    /** total accumulated start/stop tme each cycle */
    public final DescriptiveStatistics startAndStopTimeNS = new DescriptiveStatistics(WINDOW);

    /** total accumulated work time, each cycle */
    public final DescriptiveStatistics iterTimeNS = new DescriptiveStatistics(WINDOW);

    /** total iterations, each cycle */
    public final DescriptiveStatistics iterations = new DescriptiveStatistics(WINDOW);


    transient public double valuePerSecond,valueNormalized;

    long beforeStart, afterStart, beforeEnd, afterEnd;
    long workTimeThisCycleNS;
    int iterationsThisCycle;

    public InstrumentedWork(AbstractWork<Who,What> work) {
        this(work, work);
        work.demand.need(this);
    }

    public InstrumentedWork(Work work, Share<Who,What> wrapped) {
        super(wrapped.who, wrapped.what);
        this.work = work;
    }

    @Override
    public final boolean start() {
        
        beforeStart = nanoTime();
        boolean starting = work.start();
        afterStart = nanoTime();

        return starting;
    }


    @Override
    public boolean next() {
        return work.next();
    }

    @Override
    public final int next(int n) {
        long a = nanoTime();

        int ran = work.next(n);
        int ii = Math.abs(ran);
        if (ii > 0) {
            workTimeThisCycleNS += (nanoTime() - a);
            iterationsThisCycle += ii;
        }
        return ran;
    }

    @Override
    public final void stop() {

        if (iterationsThisCycle > 0) {
            iterations.addValue(iterationsThisCycle);
            iterTimeNS.addValue(workTimeThisCycleNS);


            iterationsThisCycle = 0;
            workTimeThisCycleNS = 0;
        }

        beforeEnd = nanoTime();
        work.stop();
        afterEnd = nanoTime();
        startAndStopTimeNS.addValue((afterStart - beforeStart) + (afterEnd - beforeEnd));
    }

    public String summary() {
        return super.toString() +
          "{ " + "startStopTimeMeanNS=" + Texts.timeStr(startAndStopTimeNS.getMean())
        + ", " + "iterTimeMeanNS=" + Texts.timeStr(iterTimeNS.getMean())
        + ", " + "itersMean=" + Texts.n2(iterations.getMean())
        + "}";
    }

    public void runFor(long cycleNS) {

        if (this.start()) {

            float p = this.pri();
            if (p == p) {

                long runtimeNS = Math.round(cycleNS * p);

                long now = nanoTime();
                long deadlineNS = now + runtimeNS;

                do {
                    double meanItertime = Math.min(runtimeNS, this.iterTimeNS.getMean() / this.iterations.getMean());
                    double expectedNexts;
                    if (meanItertime!=meanItertime)
                        expectedNexts = 1;
                    else {
                        expectedNexts = ((deadlineNS - now) / ( meanItertime ));
                    }

                    //System.out.println(x + " " + timeStr(deadlineNS - now) + " " + timeStr(deadlineNS - now) + "\t" + timeStr(meanItertime) + " \t= " + expectedNexts);
                    int ran = this.next(Util.clamp((int) Math.round(Math.max(1, expectedNexts)+1), 1, 1024));
                    if (ran <= 0)
                        break;

                } while ((now = nanoTime()) < deadlineNS);

            }

        } else {

        }

        this.stop();

    }

    @Override
    public float pri(float p) {
        need(p);
        return super.pri(p);
    }


}
