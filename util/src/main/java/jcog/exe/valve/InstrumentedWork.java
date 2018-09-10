package jcog.exe.valve;

import jcog.Texts;
import jcog.Util;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import static java.lang.System.nanoTime;

public class InstrumentedWork<Who, What> extends Share<Who, What> implements Work {

    final Work work;

    final static int WINDOW = 16;

//    /**
//     * total accumulated start/stop tme each cycle
//     */
//    public final SynchronizedDescriptiveStatistics startAndStopTimeNS = new SynchronizedDescriptiveStatistics(WINDOW);

    /**
     * total accumulated work time, each cycle
     */
    public final SynchronizedDescriptiveStatistics iterTimeNS = new SynchronizedDescriptiveStatistics(WINDOW);

    public final AtomicLong accumTimeNS = new AtomicLong(0);

    public final DescriptiveStatistics valuePerSecond = new SynchronizedDescriptiveStatistics(WINDOW);

    /**
     * total iterations, each cycle
     */
    public final SynchronizedDescriptiveStatistics iterations = new SynchronizedDescriptiveStatistics(WINDOW);


    transient public double valueNormalized;
    transient public double valuePerSecondNormalized;


    public InstrumentedWork(AbstractWork<Who, What> work) {
        this(work, work);
        work.demand.need(this);
    }

    public InstrumentedWork(Work work, Share<Who, What> wrapped) {
        super(wrapped.who, wrapped.what);
        this.work = work;
    }

    @Override
    public final boolean start() {
        return work.start();
    }

//    /**
//     * estimate, in NS
//     */
//    public double timePerIterationMean() {
//        double numer = iterTimeNS.getMean();
//        if (!Double.isFinite(numer) || numer < Double.MIN_NORMAL) {
//            return Double.POSITIVE_INFINITY;
//        } else {
//            double denom = iterations.getMean();
//            if (!Double.isFinite(denom) || denom < Double.MIN_NORMAL) {
//                return Double.POSITIVE_INFINITY;
//            }
//            return numer / denom;
//        }
//    }
    //TODO  timePerIterationPessimistic() {..

    @Override
    public boolean next() {
        return work.next();
    }

    @Override
    public final int next(int n) {
        return worked(nanoTime(), work.next(n));
    }


    public final int next(BooleanSupplier kontinue) {
        return worked(nanoTime(), work.next(kontinue));
    }


    private int worked(long a, int ran) {
        assert (ran >= 0);
        commit(ran, nanoTime() - a);

        return ran;
    }

    /** resets the accumulated time buffer */
    public long accumulatedTime(boolean clear) {
        return clear ? accumTimeNS.getAndSet(0) : accumTimeNS.getOpaque();
    }

    protected final void commit(int iterationsThisCycle, long workTimeThisCycleNS) {
        accumTimeNS.getAndAdd(workTimeThisCycleNS);
        iterTimeNS.addValue(workTimeThisCycleNS);
        iterations.addValue(iterationsThisCycle);




        //}
//
//        beforeEnd = nanoTime();
//        work.stop();
//        afterEnd = nanoTime();
//        startAndStopTimeNS.addValue((afterStart - beforeStart) + (afterEnd - beforeEnd));
    }

    public String summary() {
        return super.toString() +
                "{" + "valuePerSecond=" + Texts.n4(valuePerSecond.getMean()) +
                ", " + "iterTimeMeanNS=" + Texts.timeStr(iterTimeNS.getMean())
                + ", " + "itersMean=" + Texts.n4(iterations.getMean())
                + "}";
    }

    public void runUntil(long deadlineNS) {
        if (this.start()) {
            worked(nanoTime(), this.next(() -> System.nanoTime() < deadlineNS));
        }
    }

    public void runFor(long cycleNS) {


        if (this.start()) {


            long now = nanoTime();
            long deadlineNS = now + cycleNS;

            worked(now, this.next(() -> System.nanoTime() < deadlineNS));

//                do {
//                    double meanItertime = Math.min(runtimeNS, timePerIterationMean());
//                    double expectedNexts;
//                    if (meanItertime != meanItertime)
//                        expectedNexts = 1;
//                    else {
//                        expectedNexts = ((deadlineNS - now) / (meanItertime));
//                    }
//
//                    //System.out.println(x + " " + timeStr(deadlineNS - now) + " " + timeStr(deadlineNS - now) + "\t" + timeStr(meanItertime) + " \t= " + expectedNexts);
//                    int ran = this.next(Util.clamp((int) Math.round(Math.max(1, expectedNexts) + 1), 1, 1024));
//                    if (ran <= 0)
//                        break;
//
//                } while ((now = nanoTime()) < deadlineNS);


        }


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
