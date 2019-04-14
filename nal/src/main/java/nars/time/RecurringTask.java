package nars.time;

import nars.NAR;

abstract public class RecurringTask extends ScheduledTask {

    public volatile long nextStart = Long.MIN_VALUE;

    @Override
    public long start() {
        return nextStart;
    }

    protected long scheduleNext(long durCycles, long started, long now) {

        final long idealNext = started + durCycles;
        if (now > idealNext) {
            /** LAG - compute a correctional shift period, so that it attempts to maintain a steady rhythm and re-synch even if a frame is lagged*/

//            if (Param.DEBUG) {
//                long earliest = started + durCycles;
//                assert (nextStart >= earliest) : "starting too soon: " + nextStart + " < " + earliest;
//                long latest = now + durCycles;
//                assert (nextStart <= latest) : "starting too late: " + nextStart + " > " + earliest;
//            }

            //async asap:
            //return now; //immediate

            //balanced
            long late = now - idealNext;
            long phaseLate = (now - idealNext) % durCycles;
            long delayToAlign = durCycles - phaseLate;
            if (delayToAlign < durCycles/2) {
                return now + delayToAlign;
            } else
                return now;

            //skip to keep aligned with phase:
            //long phaseLate = (now - idealNext) % durCycles;
            //return now + Math.max(1, durCycles - phaseLate);

        } else
            return idealNext;
    }

    private void runAt(long idealNext, NAR nar) {
        nextStart = idealNext;

        nar.runAt(this);
    }
}
