package jcog.exe;

import jcog.Log;
import jcog.exe.realtime.FixedRateTimedFuture;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static jcog.Texts.n2;


abstract public class Loop extends FixedRateTimedFuture {

    private static final Logger logger = Log.logger(Loop.class);



    /**
     * busy lock
     */
    protected final AtomicBoolean busy = new AtomicBoolean(false);


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

    @Override
    public String toString() {
        return getClass() + "@" + System.identityHashCode(this);
    }

    /**
     * create and auto-start
     */
    public Loop(AtomicInteger periodMS) {
        super();

        int p = periodMS.get();

        //HACK trigger change in period value to trigger start
        periodMS.set(-1);
        this.periodMS = periodMS;
        setPeriodMS(p);
    }

    public boolean isRunning() {
        return periodMS() >= 0;
    }

    @Override
    protected final boolean isReady() {
        return !busy.getOpaque();
    }

    public final Loop setFPS(float fps) {
        setPeriodMS(fpsToMS(fps));
        return this;
    }

    public final void ready() {
        busy.set(false);
    }



    static int fpsToMS(float fps) {
        return Math.max(1, Math.round(1000 / fps));
    }


    public final boolean setPeriodMS(int nextPeriodMS) {
        int prevPeriodMS;
        if ((prevPeriodMS = periodMS.getAndSet(nextPeriodMS)) != nextPeriodMS) {
            if (prevPeriodMS < 0 && nextPeriodMS >= 0) {

                _start(nextPeriodMS);

            } else if (/*prevPeriodMS >= 0 && */nextPeriodMS < 0) {

                _stop();

            } else /*if (prevPeriodMS >= 0)*/ {

                //logger.info("continue {}fps (each {}ms)", n2(1000f/nextPeriodMS), nextPeriodMS);

                super.setPeriodMS(nextPeriodMS);

            }
            return true;
        }
        return false;
    }

    public void _start(int nextPeriodMS) {
        logger.debug("start {} {} fps", this, n2(1000f/nextPeriodMS));

        synchronized (periodMS) {
            starting();
        }

        Exe.timer().scheduleAtFixedRate(this, nextPeriodMS, TimeUnit.MILLISECONDS);
    }

    public void _stop() {
        logger.debug("stop {}", this);

        cancel(false);

        synchronized (periodMS) {

            stopping();
        }
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
        //stop();
        logger.error("{} {}", this, e);
        e.printStackTrace(); //TEMPORARY
        //throw new RuntimeException(e);
    }


    @Override
    public final void run() {

        if (!busy.compareAndSet(false, true))
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
        int pms = periodMS();
        if (pms > 0 /* isRunning() */) {
            return 1000f / pms;
        } else {
            return pms == 0 ? Float.POSITIVE_INFINITY : 0;
        }
    }

    public long periodNS() {
        int m = periodMS();
        return m >= 0  ? m * 1_000_000L : -1;
    }

    public int periodMS() {
        return periodMS.getOpaque();
    }

}
