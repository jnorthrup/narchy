package jcog.exe;

import jcog.Texts;
import jcog.Util;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;

import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by me on 10/20/16.
 */
abstract public class Loop {

    protected static final Logger logger = getLogger(Loop.class);

    public final AtomicReference<Thread> thread = new AtomicReference();

    protected final int windowLength = 8;

    private float lag, lagSum;

    /**
     * in seconds
     */
    public final DescriptiveStatistics dutyTime = new DescriptiveStatistics(windowLength); //in millisecond
    public final DescriptiveStatistics cycleTime = new DescriptiveStatistics(windowLength); //in millisecond


    public static Loop of(Runnable iteration) {
        return new Loop() {
            @Override
            public boolean next() {
                iteration.run();
                return true;
            }
        };
    }

    /**
     * < 0: paused
     * 0: loop at full speed
     * > 0: delay in milliseconds
     */
    public final AtomicInteger periodMS = new AtomicInteger(-1);


    @Override
    public String toString() {
        return super.toString() + " ideal=" + periodMS + "ms, " +
                Texts.n4(dutyTime.getMean()) + "+-" + Texts.n4(dutyTime.getStandardDeviation()) + "ms avg";
    }

    /**
     * create but do not start
     */
    public Loop() {

    }

    /**
     * create and auto-start
     */
    public Loop(float fps) {
        this();
        runFPS(fps);
    }

    /**
     * create and auto-start
     */
    public Loop(int periodMS) {
        this();
        setPeriodMS(periodMS);
    }

    public boolean isRunning() {
        return thread.get() != null;
    }

    public final Loop runFPS(float fps) {
        setPeriodMS(Math.round(1000f / fps));
        return this;
    }

    public final Loop runMS(int periodMS) {
        setPeriodMS(periodMS);
        return this;
    }

    public final boolean setPeriodMS(int nextPeriodMS) {
        int prevPeriodMS;
        if ((prevPeriodMS = periodMS.getAndSet(nextPeriodMS)) != nextPeriodMS) {
            if (prevPeriodMS < 0 && nextPeriodMS >= 0) {
                Thread myNewThread = new Thread(this::run);
                if (thread.compareAndSet(null, myNewThread)) {
                    myNewThread.start();
                }
            } else if (prevPeriodMS >= 0 && nextPeriodMS < 0) {
                Thread prevThread;
                if ((prevThread = thread.getAndSet(null)) != null) {
                    try {
                        prevThread.stop();
                    } catch (Throwable ignored) {
                    }
                    logger.info("stop {}", this);
                }
            } else if (prevPeriodMS >= 0) {
                //change speed
                logger.debug("{} period={}ms", this, nextPeriodMS);
            }
            return true;
        }
        return false;
    }

    public void stop() {
        setPeriodMS(-1);
    }

    /**
     * for subclass overriding; called from the looping thread
     */
    protected void onStart() {

    }

    /**
     * for subclass overriding; called from the looping thread
     */
    protected void onStop() {

    }

    /**
     * dont call this directly
     */
    protected final void run() {
        assert (thread.get() == Thread.currentThread());

        onStart();

        logger.info("start {} @ {}ms period", this, nextPeriodMS());

        int periodMS;
        long beforeTime = System.nanoTime();
        while ((periodMS = nextPeriodMS()) >= 0) {


            try {

                if (!next())
                    break;

            } catch (Throwable e) {
                logger.error(" {}", e);
                /*nar.eventError.emit(e);
                if (Param.DEBUG) {
                    stop();
                    break;
                }*/
            }


            long afterTime = System.nanoTime();


            long frameTime = afterTime - beforeTime;

            long frameTimeMS = frameTime / 1000000;
            lagSum += (this.lag = Math.max(0, (frameTimeMS /*nano to ms */ - periodMS) / ((float) periodMS)));

            //System.out.println(getClass() + " " + frameTime + " " + periodMS + " " + lag);

            double frameTimeS = (frameTime) / 1.0E9;
            this.dutyTime.addValue(frameTimeS);

            int sleepTime = (int) (periodMS - frameTimeMS);
            if (sleepTime > 0)
                Util.sleep(sleepTime); //((long) this.dutyTime.getMean()) ));

//            } else {
//                //Thread.yield();
//                //Thread.onSpinWait();
//            }

            long prevBeforeTime = beforeTime;
            beforeTime = System.nanoTime();
            this.cycleTime.addValue((beforeTime - prevBeforeTime) / 1.0E9);
        }

        stop();

        onStop();

        lag = lagSum = 0;
    }

    protected int nextPeriodMS() {
        return this.periodMS.get();
    }

    abstract public boolean next();

    public void join() {
        try {
            Thread t = thread.get();
            if (t != null) {
                t.join();
            } else {
                throw new RuntimeException("not started");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public Thread thread() {
        return thread.get();
    }

    /**
     * lag in proportion to the current FPS, >= 0
     */
    public float lag() {
        return lag;
    }

    public float lagSumThenClear() {
        float l = lagSum;
        this.lagSum = 0;
        return l;
    }

    public void stats(String prefix, SortedMap<String, Object> x) {
        x.put(prefix + " duty time mean", dutyTime.getMean()); //in seconds
        x.put(prefix + " duty time variance", dutyTime.getVariance()); //in seconds
        x.put(prefix + " lag", lag);
    }
}
