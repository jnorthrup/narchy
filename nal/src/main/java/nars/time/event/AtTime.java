package nars.time.event;

import nars.$;
import nars.term.Term;
import nars.time.ScheduledTask;

public final class AtTime extends ScheduledTask {
    private final long whenOrAfter;
    private final Runnable then;

    public AtTime(long whenOrAfter, Runnable then) {
        this.whenOrAfter = whenOrAfter;
        this.then = then;
    }

    @Override
    public long start() {
        return whenOrAfter;
    }

    @Override
    public void run() {
        then.run();
    }

    @Override
    public Term term() {
        return $.p($.identity(then), $.the(whenOrAfter));
    }
}
