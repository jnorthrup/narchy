package jcog.exe.valve;

import jcog.Texts;
import jcog.Util;
import jcog.WTF;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;

import static java.lang.System.nanoTime;

public class InstrumentedWork<Who,What> extends Share<Who,What> implements Work {

    final Work work;

    final static int WINDOW = 16;

    /** total accumulated start/stop tme each cycle */
    public final SynchronizedDescriptiveStatistics startAndStopTimeNS = new SynchronizedDescriptiveStatistics(WINDOW);

    /** total accumulated work time, each cycle */
    public final SynchronizedDescriptiveStatistics iterTimeNS = new SynchronizedDescriptiveStatistics(WINDOW);

    /** total iterations, each cycle */
    public final SynchronizedDescriptiveStatistics iterations = new SynchronizedDescriptiveStatistics(WINDOW);


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

    /** estimate, in NS */
    public double timePerIterationMean() {
        double numer = iterTimeNS.getMean();
        if (!Double.isFinite(numer) || numer < Double.MIN_NORMAL) {
            return Double.POSITIVE_INFINITY;
        } else {
            double denom = iterations.getMean();
            if (!Double.isFinite(denom) || denom < Double.MIN_NORMAL) {
                return Double.POSITIVE_INFINITY;
            }
            return numer/denom;
        }
    }
    //TODO  timePerIterationPessimistic() {..

    @Override
    public boolean next() {
        return work.next();
    }

    @Override
    public final int next(int n) {
        long a = nanoTime();

        int ran = work.next(n);
        if (ran > 0) {
            workTimeThisCycleNS += (nanoTime() - a);
            iterationsThisCycle += ran;
        } else {
            if (ran < 0)
                throw new WTF();
        }
        return ran;
    }

    @Override
    public final void stop() {


        iterations.addValue(iterationsThisCycle);

        iterTimeNS.addValue(workTimeThisCycleNS);


        iterationsThisCycle = 0;
        workTimeThisCycleNS = 0;


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
                    double meanItertime = Math.min(runtimeNS, timePerIterationMean());
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

    public float pri(float pNext, float lerpRate) {
        float p = Util.lerp(lerpRate, this.need, pNext);
        need(p);
        return super.pri(p);
    }


}
