package jcog.exe.realtime;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** reschedules after each execution.  at that point it has the option
 * to use a fixed delay or to subtract from it the duty cycle
 * consumed since being invoked, to approximate fixed rate.
 *
 * TODO AbstracdtTimedRunnable
 */
public class FixedDelayTimedFuture<T> extends AbstractTimedCallable<T> {

    protected final Consumer<TimedFuture<?>> rescheduleCallback;
    protected long periodNS;
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


    @Override public int offset(long resolution) {
        return (int) Math.round(((double)Math.max(resolution, periodNS)) / resolution);
    }


    public void reset() {
        this.rounds = (int)
                Math.min(Integer.MAX_VALUE-1,
                        Math.round((((double)periodNS)/resolution) / wheels)
                );
    }

    @Override
    public void run() {
        super.run();
        if (status != Status.CANCELLED) {
            reset();
            rescheduleCallback.accept(this);
        }
    }



    public void setPeriodMS(int nextPeriodMS) {
        periodNS = nextPeriodMS * 1_000_000L;
    }
}
