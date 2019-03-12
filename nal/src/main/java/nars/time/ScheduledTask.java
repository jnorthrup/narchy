package nars.time;

import com.google.common.primitives.Longs;
import nars.$;
import nars.term.Term;
import nars.time.event.InternalEvent;

/** event schedled for a specific time in the past or present (run as soon as possible), or delayed until
 * the future */
public abstract class ScheduledTask extends InternalEvent implements Runnable,Comparable<ScheduledTask> {

//        public final long when;
//        public final Runnable what;
//
//
//        public SchedTask(long whenOrAfter, Runnable what) {
//            this.when = whenOrAfter;
//            this.what = what;
//        }


    @Override
    public Term term() {
        return $.p($.identity(this), $.the(start()));
    }

    /** when or after this task may be run next */
    abstract public long start();

    @Override
    public String toString() {
        return "@" + start() + ':' + super.toString();
    }


    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int compareTo(ScheduledTask that) {
        if (this == that) return 0;

        int t = Longs.compare(start(), that.start());
        if (t != 0) {
            return t;
        }

        return Integer.compare(System.identityHashCode(this), System.identityHashCode(that));
    }
}
