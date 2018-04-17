package jcog.exe.realtime;

import java.util.concurrent.Callable;

public class FixedRateTimedFuture<T> extends AbstractTimedCallable<T> {

    /** adjustable while running */
    private volatile long period;

    public FixedRateTimedFuture(int rounds,
                                Callable<T> callable,
                                long recurringTimeout, long resolution, int wheelSize) {
        super(rounds, callable);
        this.period = recurringTimeout;
        reset(resolution, wheelSize);
    }

    @Override
    public void execute(HashedWheelTimer t) {
        super.execute(t);
        reset(t.resolution, t.wheels);
        t._schedule(this);
    }

    @Override
    public boolean isPeriodic() {
        return true;
    }

    public void setPeriodMS(long periodMS) {
        setPeriodNS(periodMS * 1000 * 1000);
    }

    public void setPeriodNS(long periodNS) {
        this.period = periodNS;
    }

    public int getOffset(long resolution) {
        return (int) (period / resolution);
    }

    public void reset(long resolution, int wheels) {
        this.rounds = (getOffset(resolution) / wheels);
    }

}
