package jcog.exe.realtime;

public class FixedRateTimedFuture extends AbstractTimedRunnable {

    private int offset;
    /**
     * adjustable while running
     */
    private /* volatile */ long periodNS;

    protected FixedRateTimedFuture() {
        super();
    }

    FixedRateTimedFuture(int rounds,
                         Runnable callable,
                         long periodNS, long resolution, int wheels) {
        super(rounds, callable);
        setPeriodNS(periodNS);
        reset(wheels, resolution);
    }

    @Override
    public boolean isCancelled() {
        return periodNS < 0L;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        periodNS = -1L;
        return true;
    }

    @Override
    public final void execute(HashedWheelTimer t) {

        if (!isCancelled()) {
            if (isReady())
                super.execute(t);

            reset(t.wheels, t.resolution);
            t.reschedule(this);
        }
    }

    /**
     * override for returning false to pause automatic rescheduling
     * TODO this interface method isnt needed; just use cancel and re-schedule to resume
     */
    @Deprecated protected boolean isReady() {
        return true;
    }

    @Override
    public final boolean isPeriodic() {
        return true;
    }

    public final void setPeriodMS(long periodMS) {
        setPeriodNS(periodMS * 1_000_000L);
    }

    public final void setPeriodNS(long periodNS) {
        this.periodNS = periodNS;
    }

    public final int offset(long resolution) {
        return offset;
    }

    /** TODO cache this
     * */
    protected void reset(int wheels, long resolution) {
        double epoch = (double) (resolution * (long) wheels);
        long periodNS = this.periodNS;
        int rounds = Math.min(Integer.MAX_VALUE, (int)((double) periodNS / epoch));
        this.rounds = rounds;
        this.offset = Math.max(1, (int)((((double) periodNS - (double) rounds * epoch) / (double) resolution)));
    }


}



































