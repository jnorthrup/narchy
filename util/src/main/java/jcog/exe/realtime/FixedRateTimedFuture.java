package jcog.exe.realtime;

public class FixedRateTimedFuture extends AbstractTimedRunnable {

    /** adjustable while running */
    private volatile long period;

    public FixedRateTimedFuture(int rounds,
                                Runnable callable,
                                long recurringTimeout, long resolution, int wheelSize) {
        super(rounds, callable);
        this.period = recurringTimeout;
        reset(resolution, wheelSize);
    }

    @Override
    public boolean isCancelled() {
        return period < 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        period = -1;
        return true;
    }

    @Override
    public void execute(HashedWheelTimer t) {
        if (!isCancelled()) {
            super.execute(t);
            reset(t.resolution, t.wheels);
            t._schedule(this);
        }
    }

    @Override
    public boolean isPeriodic() {
        return true;
    }

    public void setPeriodMS(long periodMS) {
        setPeriodNS(periodMS * 1_000_000);
    }

    public void setPeriodNS(long periodNS) {
        this.period = periodNS;
    }

    public int getOffset(long resolution) {
        return (int) Math.round(((double)period)/resolution);
    }

    public void reset(long resolution, int wheels) {
        this.rounds = (int)
            Math.min(Integer.MAX_VALUE-1,
                Math.round((((double)period)/resolution) / wheels)
            );
    }


}



































