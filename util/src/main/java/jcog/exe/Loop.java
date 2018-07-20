package jcog.exe;

import jcog.exe.realtime.FixedRateTimedFuture;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by me on 10/20/16.
 * <p>
 * the Runnable.run method is actually the iteration which will
 * be repeatedly called.
 * Do not call it externally.
 */
abstract public class Loop implements Runnable {

    protected final Logger logger;


    @Deprecated
    private volatile FixedRateTimedFuture task = null;

    /**
     * busy lock
     */
    protected final AtomicBoolean executing = new AtomicBoolean(false);


    public static Loop of(Runnable iteration) {
        return new LambdaLoop(iteration);
    }

    /**
     * < 0: paused
     * 0: loop at full speed
     * > 0: delay in milliseconds
     */
    public final AtomicInteger periodMS;


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

    public Loop(int periodMS) {
        this(new AtomicInteger(periodMS));
    }
    /**
     * create and auto-start
     */
    public Loop(AtomicInteger periodMS) {
        super();
        logger = getLogger(getClass());

        int p = periodMS.intValue();

        //HACK trigger change in period value to trigger start
        periodMS.set(-1);
        this.periodMS = periodMS;
        setPeriodMS(p);
    }

    public boolean isRunning() {
        return periodMS.intValue() >= 0;
    }

    public final Loop setFPS(float fps) {
        setPeriodMS(fpsToMS(fps));
        return this;
    }

    public final void ready() {
        executing.setRelease(false);
    }

    static int fpsToMS(float fps) {
        return Math.round(1000f / fps);
    }


    public final boolean setPeriodMS(int nextPeriodMS) {
        int prevPeriodMS;
        if ((prevPeriodMS = periodMS.getAndSet(nextPeriodMS)) != nextPeriodMS) {
            if (prevPeriodMS < 0 && nextPeriodMS >= 0) {
                logger.debug("start period={}ms", nextPeriodMS);

                synchronized (periodMS) {


                    assert (this.task == null);
                    starting();
                    this.task = Exe.timer()
                            .scheduleAtFixedRate(this, 0, nextPeriodMS, TimeUnit.MILLISECONDS);

                }
            } else if (/*prevPeriodMS >= 0 && */nextPeriodMS < 0) {

                logger.info("stop");

                synchronized (periodMS) {

                    FixedRateTimedFuture prevTask = this.task;
                    if (prevTask != null) {

                        this.task = null;

                        prevTask.cancel(false);


                    }

                    stopping();
                }
            } else if (prevPeriodMS >= 0) {


                logger.debug("period={}ms", nextPeriodMS);

                FixedRateTimedFuture task = this.task;
                if (task != null)
                    task.setPeriodMS(nextPeriodMS);

            }
            return true;
        }
        return false;
    }


    public final boolean stop() {
        return setPeriodMS(-1);
    }

    /**
     * for subclass overriding; called from the looping thread
     */
    protected void starting() {

    }

    /**
     * for subclass overriding; called from the looping thread
     */
    protected void stopping() {

    }

    protected void thrown(Throwable e) {
        stop();
        ready();
        logger.error(" {}", e);
        //throw new RuntimeException(e);
    }


    @Override
    public final void run() {

        if (!executing.weakCompareAndSetAcquire(false, true))
            return;

        beforeNext();
        try {
            if (!next()) {
                stop();
            }
        } catch (Throwable e) {
            thrown(e);
        } finally {
            afterNext();
            if (!async())
                ready();
        }

    }

    /**
     * if iterationAsync, then the executing flag will not be cleared automatically.  then it is the implementation's
     * responsibility to clear it so that the next iteration can proceed.
     */
    protected boolean async() {
        return false;
    }

    protected void beforeNext() {

    }

    protected void afterNext() {

    }

    abstract public boolean next();


    public float getFPS() {
        if (isRunning()) {
            return 1000f / periodMS.intValue();
        } else {
            return 0;
        }
    }

    public long periodNS() {
        return periodMS.getOpaque() * 1000000L;
    }

}
