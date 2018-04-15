package com.ifesdjeen.timer;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class FixedDelayTimedFuture<T> extends OneShotTimedFuture<T> {


    private final Consumer<TimedFuture<?>> rescheduleCallback;
    private final long periodNS;
    private final long resolution;
    private final int wheels;

    public FixedDelayTimedFuture(int rounds,
                                 Callable<T> callable,
                                 long periodNS,
                                 long resolution,
                                 int wheels,
                                 Consumer<TimedFuture<?>> rescheduleCallback) {
        super(rounds, callable, periodNS);
        this.periodNS = periodNS;
        this.resolution = resolution;
        this.wheels = wheels;
        this.rescheduleCallback = rescheduleCallback;
        reset();
    }

    @Override
    public boolean isPeriodic() {
        return true;
    }

    @Override
    public int rounds() {
        return getOffset() / wheels;
    }

    public int getOffset() {
        return (int) (periodNS / resolution);
    }

    public void reset() {
        this.rounds.set(rounds());
    }

    @Override
    public void run() {
        super.run();
        reset();
        rescheduleCallback.accept(this);
    }

}
