package jcog.exe.realtime;

import java.util.concurrent.atomic.AtomicBoolean;

public class FixedRateTimedFuture extends AbstractTimedRunnable {

    int offset;
    /**
     * adjustable while running
     */
    private /* volatile */ long periodNS;

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
        this.periodNS = recurringTimeout;
        reset(resolution, wheelSize);
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
        if (pending.compareAndSet(false, true)) {
            try {
                if (!isCancelled()) {
                    if (isReady()) {
                        super.execute(t);
                    }
                    reset(t.resolution, t.wheels); //TODO time since last
                    t._schedule(this);
                }
            } finally {
                pending.set(false);
            }
        }

    }

    /** override for returning false to pause automatic rescheduling */
    protected boolean isReady() {
        return true;
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

    public final int offset(long resolution) {
        return offset;
    }

    public final void reset(long resolution, int wheels) {
        int steps = (int) Math.round(((double) periodNS) / resolution);
        this.rounds = Math.min(Integer.MAX_VALUE - 1,
                        (steps / wheels)
                );
        this.offset = steps % wheels;
    }


}



































