package com.ifesdjeen.timer;

import java.util.concurrent.Callable;

public class FixedRateTimedFuture<T> extends OneShotTimedFuture<T> {

    private final long resolution;
    private final int wheelSize;
    private volatile long period;

    public FixedRateTimedFuture(int rounds,
                                Callable<T> callable,
                                long recurringTimeout, long resolution, int wheelSize) {
        super(rounds, callable, 0);
        this.period = recurringTimeout;
        this.resolution = resolution;
        this.wheelSize = wheelSize;
        reset();
    }

    public void setPeriodMS(long periodMS) {
        setPeriodNS(periodMS * 1000 * 1000);
    }

    public void setPeriodNS(long periodNS) {
        this.period = periodNS;
    }

    public int getOffset() {
        return (int) (period / resolution);
    }

    public void reset() {
        this.status = Status.READY;
        this.rounds.set(getOffset() / wheelSize);
    }

    @Override
    public boolean runOnce() {
        return false;
    }

}
