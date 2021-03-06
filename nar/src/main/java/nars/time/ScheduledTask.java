package nars.time;

import com.google.common.primitives.Longs;
import nars.NAR;
import nars.term.Term;
import nars.time.event.WhenInternal;

import java.util.function.Consumer;

/** event schedled for a specific time in the past or present (run as soon as possible), or delayed until
 * the future */
public abstract class ScheduledTask extends WhenInternal implements Consumer<NAR>, Comparable<ScheduledTask> {

    public volatile boolean scheduled = false;

    /** when or after this task may be run next */
    public abstract long start();

    @Override
    public abstract Term term();

    @Override
    public final String toString() {
        return "@" + start() + ':' + super.toString();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int compareTo(ScheduledTask that) {
        if (this == that) return 0;

        int t = Longs.compare(start(), that.start());
		return t != 0 ? t : Integer.compare(System.identityHashCode(this), System.identityHashCode(that));
    }

}
