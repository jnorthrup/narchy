package nars.task;

import com.google.common.primitives.Longs;
import jcog.Util;
import jcog.pri.Priority;
import nars.NAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * for queued/scheduled native tasks
 * the reasoner remains oblivious of these.
 * but it holds a constant 1.0 priority.
 */
public abstract class NativeTask implements ITask {

    @Override
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

    @Override
    public @Nullable Priority clonePri() {
        throw new UnsupportedOperationException();
    }

    /**
     * wraps a Runnable
     */
    public static class RunTask extends NativeTask {

        final Runnable run;

        public RunTask(Runnable runnable) {
            run = runnable;
        }

        @Override
        public String toString() {
            return run.toString();
        }

        @Override
        public @Nullable Iterable<? extends ITask> run(NAR n) {
            run.run();
            return null;
        }

    }

    public static class SchedTask extends NativeTask implements Comparable<SchedTask>, Consumer<NAR> {

        public final long when;
        public final Object then;

        public SchedTask(long whenOrAfter, Consumer<NAR> then) {
            this.when = whenOrAfter;
            this.then = then;
        }

        public SchedTask(long whenOrAfter, Runnable then) {
            this.when = whenOrAfter;
            this.then = then;
        }

        @Override
        public String toString() {
            return "@" + when + ':' + then;
        }

        @Override
        public final void accept(NAR nar) {
            run(nar);
        }

        @Override
        public final @Nullable Iterable<? extends ITask> run(NAR n) {
            if (then instanceof Consumer)
                ((Consumer) then).accept(n);
            else if (then instanceof Runnable)
                ((Runnable) then).run();

            return null;
        }

        @Override
        public int compareTo(NativeTask.SchedTask b) {
            if (this == b) return 0;

            int t = Longs.compare(when, b.when);
            if (t != 0) {
                return t;
            }

            if (this.equals(b))
                return 0;

            Object aa = then;
            Object bb = b.then;
            if (aa == bb) return 0;

            //as a last resort, compare their system ID
            return Integer.compare(System.identityHashCode(aa), System.identityHashCode(bb)); //maintains uniqueness in case they occupy the same time
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
//    /**
//     * wraps a Runnable
//     */
//    public static class NARTask extends NativeTask {
//
//        final Consumer run;
//
//        public NARTask(@NotNull Consumer<NAR> runnable) {
//            run = runnable;
//        }
//
//        @Override
//        public String toString() {
//            return run.toString();
//        }
//
//        @Override
//        public @Nullable Iterable<? extends ITask> run(NAR x) {
//            run.accept(x);
//            return null;
//        }
//
//    }

}
