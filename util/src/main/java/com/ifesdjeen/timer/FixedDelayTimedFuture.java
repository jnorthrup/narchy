package com.ifesdjeen.timer;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

class FixedDelayTimedFuture<T> extends OneShotTimedFuture<T> {

    private final int rescheduleRounds;
    private final int scheduleOffset;
    private final Consumer<TimedFuture<?>> rescheduleCallback;

    public FixedDelayTimedFuture(int rounds,
                                 Callable<T> callable,
                                 long delay,
                                 int scheduleRounds,
                                 int scheduleOffset,
                                 Consumer<TimedFuture<?>> rescheduleCallback) {
        super(rounds, callable, delay);
        this.rescheduleRounds = scheduleRounds;
        this.scheduleOffset = scheduleOffset;
        this.rescheduleCallback = rescheduleCallback;
    }

    @Override
    public boolean isPeriodic() {
        return true;
    }

    public int getOffset() {
        return this.scheduleOffset;
    }

    public void reset() {
        this.rounds.set(rescheduleRounds);
    }

    @Override
    public void run() {
        super.run();
        reset();
        rescheduleCallback.accept(this);
    }

}
