package nars.task;

import com.google.common.primitives.Longs;
import jcog.pri.Priority;
import nars.NAR;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * for queued/scheduled native tasks
 * the reasoner remains oblivious of these.
 * but it holds a constant 1.0 priority.
 */
public abstract class NativeTask implements ITask, Priority {

    public byte punc() {
        return 0;
    }

    @Override
    public float pri() {
        return 1;
    }

    @Override
    abstract public String toString();

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public float priSet(float p) {
        return 1f; //does nothing
    }

    public abstract ITask next(NAR n);

    public final boolean isInput() {
        return false;
    }

    /**
     * fluent form of setPri which returns this class
     */
    public ITask pri(float p) {
        priSet(p);
        return this;
    }

    /**
     * wraps a Runnable
     */
    public static final class RunTask extends NativeTask {

        public final Runnable run;

        public RunTask(Runnable runnable) {
            run = runnable;
        }

        @Override
        public String toString() {
            return run.toString();
        }

        @Override
        public ITask next(NAR n) {
            run.run();
            return null;
        }

    }


    public static final class SchedTask extends NativeTask implements Comparable<SchedTask> {

        public final long when;
        public final Object what;

        public SchedTask(long whenOrAfter, Consumer<NAR> what) {
            this.when = whenOrAfter;
            this.what = what;
        }

        public SchedTask(long whenOrAfter, Runnable what) {
            this.when = whenOrAfter;
            this.what = what;
        }

        @Override
        public String toString() {
            return "@" + when + ':' + what;
        }

        @Override
        public final ITask next(NAR n) {
            if (what instanceof Consumer)
                ((Consumer) what).accept(n);
            else if (what instanceof Runnable)
                ((Runnable) what).run();

            return null;
        }

        @Override
        public int compareTo(NativeTask.SchedTask that) {
            if (this == that) return 0;

            int t = Longs.compare(when, that.when);
            if (t != 0) {
                return t;
            }

//            Object aa = what;
//            Object bb = that.what;
//            if (aa == bb) return 0;

            //as a last resort, compare their system ID
            //return Integer.compare(System.identityHashCode(aa), System.identityHashCode(bb)); //maintains uniqueness in case they occupy the same time

            return Integer.compare(System.identityHashCode(this), System.identityHashCode(that)); //maintains uniqueness in case they occupy the same time
        }
    }

//    public static class SleepTask extends NativeTask {
//
//        private final AtomicInteger ms;
//        final int toSleep;
//
//        public SleepTask(int ms, int divisor) {
//            this.ms = new AtomicInteger(ms);
//            this.toSleep = Math.max(1,ms/divisor);
//        }
//
//        @Override
//        public String toString() {
//            return "sleep(" + ms + "ms)";
//        }
//
//        @Override
//        public @Nullable Iterable<? extends ITask> run(NAR n) {
//            /** min executor load to allow sleeping */
//
//            if (n.exe.load() >= 1f-1f/(1+n.exe.concurrency())) //<-TODO estimate this without seeing the sleep or maybe this is just a hack and a different throttle system will work better
//                return null;
//
//            if (ms.addAndGet(-toSleep) >= toSleep) {
//                n.exe.add(this); //re-input for another thread to continue sleeping
//            }
//
//            Util.pause(toSleep);
//
//            return null;
//        }
//    }
//

    public static class NARTask extends NativeTask {

        final Consumer run;

        public NARTask( Consumer<NAR> runnable) {
            run = runnable;
        }

        @Override
        public String toString() {
            return run.toString();
        }

        @Override
        public ITask next(NAR x) {
            run.accept(x);
            return null;
        }

    }

}
