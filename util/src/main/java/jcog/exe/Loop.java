package jcog.exe;

import com.ifesdjeen.timer.FixedRateTimedFuture;
import com.ifesdjeen.timer.HashedWheelTimer;
import com.ifesdjeen.timer.WaitStrategy;
import jcog.math.MutableInteger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;

import java.util.SortedMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by me on 10/20/16.
 */
abstract public class Loop {

    protected static final Logger logger = getLogger(Loop.class);

    //private volatile Thread thread = null;
    private volatile FixedRateTimedFuture task = null;
    private final AtomicBoolean executing = new AtomicBoolean(false);

    /** global timer */
    final static HashedWheelTimer timer =
        new HashedWheelTimer(Loop.class.getName(),
             TimeUnit.MILLISECONDS.toNanos(2),
             64,
            // new WaitStrategy.YieldingWait(),
                new WaitStrategy.SleepWait(),
            ForkJoinPool.commonPool());


//    private float lag, lagSum;

    protected final int windowLength = 4;

//    /**
//     * in seconds
//     */
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
    public final MutableInteger periodMS = new MutableInteger(-1);


    @Override
    public String toString() {
        return super.toString() + " ideal=" + periodMS + "ms";
                //Texts.n4(dutyTime.getMean()) + "+-" + Texts.n4(dutyTime.getStandardDeviation()) + "ms avg";
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
        //timer.scheduleWithFixedDelay()
    }

    public boolean isRunning() {
        return task != null;
    }

    public final Loop runFPS(float fps) {
        setPeriodMS(Math.round(1000f / fps));
        return this;
    }


    public final boolean setPeriodMS(int nextPeriodMS) {
        int prevPeriodMS;
        if ((prevPeriodMS = periodMS.getAndSet(nextPeriodMS)) != nextPeriodMS) {
            if (prevPeriodMS < 0 && nextPeriodMS >= 0) {
                synchronized (periodMS) {
//                    Thread myNewThread = newThread();
//                    myNewThread.start();
                    task = timer.scheduleAtFixedRate(this::_next, 0, nextPeriodMS, TimeUnit.MILLISECONDS);
                }
            } else if (prevPeriodMS >= 0 && nextPeriodMS < 0) {
                synchronized (periodMS) {
                    //Thread prevThread = this.thread;
                    FixedRateTimedFuture prevTask = this.task;
                    if (prevTask != null) {
                        //this.thread = null;
                        this.task = null;
                        try {
                            prevTask.cancel(false);
                            //prevThread.interrupt();
                            //prevThread.stop();
                        } catch (Throwable ii) {
                            ii.printStackTrace();
                        }
                        logger.info("stop {}", this);
                    }
                }
            } else if (prevPeriodMS >= 0) {
                //change speed
                FixedRateTimedFuture task = this.task;
                if (task!=null) {
                    task.setPeriodMS(nextPeriodMS);
                }
                logger.debug("{} period={}ms", this, nextPeriodMS);
            }
            return true;
        }
        return false;
    }

//    private synchronized Thread newThread() {
//        if (this.thread!=null)
//            throw new RuntimeException("thread already started: " + thread);
//
//        Thread t = new Thread(this::run);
//        this.thread = t;
//        return t;
//    }

    public final void stop() {
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

//    /**
//     * dont call this directly
//     */
//    private void run() {
//
//        onStart();
//
//        logger.info("start {} each {}ms", this, this.periodMS.intValue());
//
//        int periodMS;
//        while ((periodMS = this.periodMS.intValue()) >= 0) {
//
//            long beforeTimeNS = System.nanoTime();
//
//            try {
//                if (!next()) {
//                    stop(); //will exit after statistics at the end of this loop
//                }
//            } catch (Throwable e) {
//                thrown(e);
//            }
//
//            long dutyTimeNS = System.nanoTime() - beforeTimeNS;
//            double dutyTimeS = (dutyTimeNS) / 1.0E9;
//
//            double cycleTimeS;
//            long sleepTime = Math.round(periodMS - (dutyTimeNS / 1E6 /* to MS */));
//            if (sleepTime > 0) {
//                Util.sleep(sleepTime); //((long) this.dutyTime.getMean()) ));
//                cycleTimeS = (System.nanoTime() - beforeTimeNS) / 1.0E9;
//            } else {
//                cycleTimeS = dutyTimeS; //100% duty cycle
//            }
//
//            this.dutyTime.addValue(dutyTimeS);
//            this.cycleTime.addValue(cycleTimeS);
//        }
//
//        stop();
//
//        logger.info("stop {} each {}ms", this);
//
//        onStop();
//
////        lag = lagSum = 0;
//    }

    protected void thrown(Throwable e) {
        logger.error(" {}", e);
    }



    protected final void _next() {

        if (!executing.compareAndSet(false, true)) {
            //already in-progress
            return;
        }

        try {
            long beforeIteration = System.nanoTime();

            try {
                if (!next()) {
                    stop(); //will exit after statistics at the end of this loop
                }
            } catch (Throwable e) {
                thrown(e);
            }

            long lastIteration = this.last;
            long afterIteration = System.nanoTime();
            this.last = afterIteration;

            long dutyTimeNS = afterIteration - beforeIteration;
            double dutyTimeS = (dutyTimeNS) / 1.0E9;

            double cycleTimeS = (afterIteration - lastIteration) / 1.0E9;

            this.dutyTime.addValue(dutyTimeS);
            this.cycleTime.addValue(cycleTimeS);
        } finally {
            executing.set(false);
        }
    }

    volatile long last = System.nanoTime();

    abstract public boolean next();

//    public void join() {
//        try {
//            Thread t = thread;
//            if (t != null) {
//                t.join();
//            } else {
//                throw new RuntimeException("not started");
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }


//    /**
//     * lag in proportion to the current FPS, >= 0
//     */
//    public float lag() {
//        return lag;
//    }
//
//    public float lagSumThenClear() {
//        float l = lagSum;
//        this.lagSum = 0;
//        return l;
//    }

    public void stats(String prefix, SortedMap<String, Object> x) {
        x.put(prefix + " cycle time mean", cycleTime.getMean()); //in seconds
        x.put(prefix + " cycle time vary", cycleTime.getVariance()); //in seconds
        x.put(prefix + " duty time mean", dutyTime.getMean()); //in seconds
        x.put(prefix + " duty time vary", dutyTime.getVariance()); //in seconds
        //x.put(prefix + " lag", lag);
    }

    public float getFPS() {
        if (isRunning()) {
            return 1000f/periodMS.intValue();
        } else {
            return 0;
        }
    }
}
