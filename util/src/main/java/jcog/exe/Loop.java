package jcog.exe;

import jcog.exe.realtime.FixedRateTimedFuture;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by me on 10/20/16.
 *
 * the Runnable.run method is actually the iteration which will
 * be repeatedly called.
 * Do not call it externally.
 */
abstract public class Loop implements Runnable {

    protected final Logger logger;

    
    @Deprecated private volatile FixedRateTimedFuture task = null;

    /** busy lock */
    private final AtomicBoolean executing = new AtomicBoolean(false);


    public static Loop of(Runnable iteration) {
        return new LambdaLoop(iteration);
    }

    /**
     * < 0: paused
     * 0: loop at full speed
     * > 0: delay in milliseconds
     */
    public final AtomicInteger periodMS = new AtomicInteger(-1);








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
                logger.debug("start period={}ms", nextPeriodMS);

                synchronized (periodMS) {


                    assert(this.task == null);
                    onStart();
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

                    onStop();
                }
            } else if (prevPeriodMS >= 0) {
                

                logger.debug("period={}ms", nextPeriodMS);

                FixedRateTimedFuture task = this.task;
                if (task!=null)
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
    protected void onStart() {

    }

    /**
     * for subclass overriding; called from the looping thread
     */
    protected void onStop() {

    }

    protected void thrown(Throwable e) {
        logger.error(" {}", e);
    }


    @Override public final void run() {


        if (!executing.compareAndSet(false, true))
            return; 

        try {
//            if (periodMS.intValue()<0) {
//                return;
//            }

            beforeNext();
            try {
                if (!next()) {
                    stop(); 
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
