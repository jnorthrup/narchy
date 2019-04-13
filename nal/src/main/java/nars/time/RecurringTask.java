package nars.time;

import nars.NAR;
import nars.Param;

abstract public class RecurringTask extends ScheduledTask {

    private volatile long nextStart = Long.MIN_VALUE;

    @Override
    public long start() {
        return nextStart;
    }

    protected void scheduleNext(long durCycles, long started, NAR nar) {

        long idealNext = started + durCycles;
        long now = nar.time();
        if (idealNext <= now) {
            /** LAG - compute a correctional shift period, so that it attempts to maintain a steady rhythm and re-synch even if a frame is lagged*/
            long phaseLate = (now - idealNext) % durCycles;
            //idealNext = now + 1; //immediate
            idealNext = now + Math.max(1, durCycles - phaseLate);

            if (Param.DEBUG) {
                long earliest = started + durCycles;
                assert (nextStart >= earliest) : "starting too soon: " + nextStart + " < " + earliest;
                long latest = now + durCycles;
                assert (nextStart <= latest) : "starting too late: " + nextStart + " > " + earliest;
            }
        }

        runAt(idealNext, nar);
    }

    private void runAt(long idealNext, NAR nar) {
        nextStart = idealNext;

        nar.runAt(this);
    }
}
