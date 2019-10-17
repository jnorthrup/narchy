package nars.time;

public abstract class RecurringTask extends ScheduledTask {

    public volatile long nextStart = Long.MIN_VALUE;

    @Override
    public long start() {
        return nextStart;
    }


//    private void runAt(long idealNext, NAR nar) {
//        nextStart = idealNext;
//
//        nar.runAt(this);
//    }
}
