package jcog.exe.realtime;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class FixedDelayTimedFuture<T> extends AbstractTimedCallable<T> {


    private final Consumer<TimedFuture<?>> rescheduleCallback;
    private long periodNS;
    @Deprecated private final int wheels;
    @Deprecated private final long resolution;

    public FixedDelayTimedFuture(int rounds,
                                 Callable<T> callable,
                                 long periodNS,
                                 long resolution,
                                 int wheels,
                                 Consumer<TimedFuture<?>> rescheduleCallback) {
        super(rounds, callable);
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

    public int getOffset(long resolution) {
        return (int) (periodNS / resolution);
    }

    void reset() {
        this.rounds = getOffset(resolution)/wheels;
    }

    @Override
    public void run() {
        super.run();
        reset();
        rescheduleCallback.accept(this);
    }

    public void setPeriodMS(int nextPeriodMS) {
        periodNS = nextPeriodMS * 1_000_000L;
    }
}
