package jcog.exe;

import jcog.Texts;
import jcog.Util;
import jcog.math.MutableInteger;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.SortedMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by me on 10/20/16.
 */
abstract public class Loop {

    protected static final Logger logger = getLogger(Loop.class);

    private Thread thread = null;

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
    public final MutableInteger periodMS = new MutableInteger(-1);


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
        return thread != null;
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
                synchronized (periodMS) {
                    Thread myNewThread = newThread();
                    myNewThread.start();
                }
            } else if (prevPeriodMS >= 0 && nextPeriodMS < 0) {
                synchronized (periodMS) {
                    Thread prevThread = this.thread;
                    if (prevThread != null) {
                        this.thread = null;
                        try {
                            prevThread.interrupt();
                            //prevThread.stop();
                        } catch (Throwable ii) {
                            ii.printStackTrace();
                        }
                        logger.info("stop {}", this);
                    }
                }
            } else if (prevPeriodMS >= 0) {
                //change speed
                logger.debug("{} period={}ms", this, nextPeriodMS);
            }
            return true;
        }
        return false;
    }

    private Thread newThread() {
        if (this.thread!=null)
            throw new RuntimeException("thread already started: " + thread);

        Thread t = new Thread(this::run);
        this.thread = t;
        return t;
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
    private final void run() {

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

    private int nextPeriodMS() {
        return this.periodMS.intValue();
    }

    abstract public boolean next();

    public void join() {
        try {
            Thread t = thread;
            if (t != null) {
                t.join();
            } else {
                throw new RuntimeException("not started");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
