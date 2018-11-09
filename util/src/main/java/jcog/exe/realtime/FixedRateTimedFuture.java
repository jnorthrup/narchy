package jcog.exe.realtime;

import java.util.concurrent.atomic.AtomicBoolean;

public class FixedRateTimedFuture extends AbstractTimedRunnable {

    /**
     * adjustable while running
     */
    private volatile long periodNS;

    public FixedRateTimedFuture() {
        super();
    }

    public FixedRateTimedFuture(int rounds,
                                Runnable callable,
                                long recurringTimeout, long resolution, int wheelSize) {
        super(rounds, callable);
        init(recurringTimeout, resolution, wheelSize);
    }

    public void init(long recurringTimeout, long resolution, int wheelSize) {
        reset(this.periodNS = recurringTimeout, resolution, wheelSize);
    }

    @Override
    public boolean isCancelled() {
        return periodNS < 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        periodNS = -1;
        return true;
    }

    private final AtomicBoolean pending = new AtomicBoolean();

    @Override
    public void execute(HashedWheelTimer t) {
        if (pending.weakCompareAndSetAcquire(false, true)) {
            try {
                if (!isCancelled()) {
                    super.execute(t);
                    reset(periodNS, t.resolution, t.wheels); //TODO time since last
                    t._schedule(this);
                }
            } finally {
                pending.setRelease(false);
            }
        }

    }

//    @Override
//    public void run() {
//        if (pending.compareAndSet(true, false)) { //coalesce
//            //System.out.println(" run " + this);
//            super.run();
//        } else {
//            //elide
//            //System.out.println("skip " + this);
//        }
//    }

    @Override
    public boolean isPeriodic() {
        return true;
    }

    public void setPeriodMS(long periodMS) {
        setPeriodNS(periodMS * 1_000_000);
    }

    public void setPeriodNS(long periodNS) {
        this.periodNS = periodNS;
    }

    public int getOffset(long resolution) {
        return (int) Math.round(
                //Math.max(resolution, ((double) periodNS))
                ((double)periodNS)
                        / resolution);
    }

    public void reset(long period, long resolution, int wheels) {
        this.rounds = (int)
                Math.min(Integer.MAX_VALUE - 1,
                        Math.round((((double) period) / resolution) / wheels)
                );
    }


}



































