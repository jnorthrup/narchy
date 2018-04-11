package jcog.exe;

import com.ifesdjeen.timer.FixedRateTimedFuture;
import com.ifesdjeen.timer.HashedWheelTimer;
import jcog.Util;
import org.slf4j.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by me on 10/20/16.
 */
abstract public class Loop {

    protected final Logger logger;

    //make Loop extend FixedRateFuture...
    @Deprecated private volatile FixedRateTimedFuture task = null;

    /** busy lock */
    private final AtomicBoolean executing = new AtomicBoolean(false);

    /** global timer */
    private static HashedWheelTimer timer = null;

    static synchronized HashedWheelTimer timer() {
        if (timer == null) {
            Executor exe = Util.executor();
            HashedWheelTimer.logger.info("global timer start: executor={}", exe);
            timer = new HashedWheelTimer(Loop.class.getName(),
                    TimeUnit.MILLISECONDS.toNanos(1),
                    32,
                    // HashedWheelTimer.WaitStrategy.YieldingWait,
                    HashedWheelTimer.WaitStrategy.SleepWait,
                    exe);
        }
        return timer;
    }

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
        return super.toString() + " ideal=" + periodMS + "ms";
                //Texts.n4(dutyTime.getMean()) + "+-" + Texts.n4(dutyTime.getStandardDeviation()) + "ms avg";
    }

    /**
     * create but do not start
     */
    public Loop() {
        this(-1);
    }

    /**
     * create and auto-start
     */
    public Loop(float fps) {
        this(fps >= 0 ? fpsToMS(fps) : -1);
    }

    /**
     * create and auto-start
     */
    public Loop(int periodMS) {
        super();
        logger = getLogger(getClass());
        setPeriodMS(periodMS);
        //timer.scheduleWithFixedDelay()
    }

    public boolean isRunning() {
        return periodMS.intValue() >= 0;
    }

    public final Loop runFPS(float fps) {
        setPeriodMS(fpsToMS(fps));
        return this;
    }

    static int fpsToMS(float fps) {
        return Math.round(1000f / fps);
    }


    public final boolean setPeriodMS(int nextPeriodMS) {
        int prevPeriodMS;
        if ((prevPeriodMS = periodMS.getAndSet(nextPeriodMS)) != nextPeriodMS) {
            if (prevPeriodMS < 0 && nextPeriodMS >= 0) {
                synchronized (periodMS) {
//                    Thread myNewThread = newThread();
//                    myNewThread.start();
                    onStart();
                    this.task = timer().scheduleAtFixedRate(this::loop, 0, nextPeriodMS, TimeUnit.MILLISECONDS);
                }
            } else if (prevPeriodMS >= 0 && nextPeriodMS < 0) {
                synchronized (periodMS) {
                    //Thread prevThread = this.thread;
                    FixedRateTimedFuture prevTask = this.task;
                    if (prevTask != null) {

                        this.task = null;
                        //try {
                            prevTask.cancel(false);
                            //prevThread.interrupt();
                            //prevThread.stop();
//                        } catch (Throwable ii) {
//                            ii.printStackTrace();
//                        }

                    }

                    logger.info("stop {}", this);
                    onStop();
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


    protected final void loop() {

        if (periodMS.intValue()<0)
            return; //stopped

        if (!executing.compareAndSet(false, true))
            return; //already in-progress

        try {

            beforeNext();
            try {
                if (!next()) {
                    stop(); //will exit after statistics at the end of this loop
                }
            } catch (Throwable e) {
                thrown(e);
            }

            afterNext();
        } finally {
            executing.set(false);
        }
    }

    protected void beforeNext() {

    }

    protected void afterNext() {

    }

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


    public float getFPS() {
        if (isRunning()) {
            return 1000f/periodMS.intValue();
        } else {
            return 0;
        }
    }

    public long periodNS() {
        return periodMS.longValue() * 1000000;
    }
}
