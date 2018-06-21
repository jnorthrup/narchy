package nars.task;

import com.google.common.primitives.Longs;
import jcog.list.FasterList;
import jcog.pri.Priority;
import nars.NAR;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * for queued/scheduled native tasks
 * the reasoner remains oblivious of these.
 * but it holds a constant 1.0 priority.
 */
public abstract class NativeTask implements ITask, Priority {

    @Nullable
    public static ITask of(@Nullable FasterList<ITask> next) {
        if (next == null) return null;
        switch (next.size()) {
            case 0: return null;
            case 1: return next.get(0);
            default:
                return new NativeTaskSequence(next.toArrayRecycled(ITask[]::new));
        }
    }

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
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public float priSet(float p) {
        return 1f; 
    }

    public abstract ITask next(NAR n);


    /**
     * fluent form of setPri which returns this class
     */
    public ITask pri(float p) {
        priSet(p);
        return this;
    }

//    /**
//     * wraps a Runnable
//     */
//    public static final class RunTask extends NativeTask {
//
//        public final Runnable run;
//
//        public RunTask(Runnable runnable) {
//            run = runnable;
//        }
//
//        @Override
//        public String toString() {
//            return run.toString();
//        }
//
//        @Override
//        public ITask next(NAR n) {
//            run.run();
//            return null;
//        }
//
//    }


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





            
            

            return Integer.compare(System.identityHashCode(this), System.identityHashCode(that)); 
        }
    }


































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

    private final static class NativeTaskSequence implements ITask {
        private final ITask[] tasks;

        public NativeTaskSequence(ITask[] x) {
            this.tasks = x;
        }

        @Override
        public ITask next(NAR n) {
            FasterList<ITask> next = null;
            for (ITask t : tasks) {
                ITask p = t.next(n);
                if (p!=null) {
                    if (next == null) next = new FasterList(1);
                    next.add(p);
                }
            }
            return NativeTask.of(next);
        }

        @Override
        public byte punc() {
            return 0;
        }

        @Override
        public float priSet(float p) {
            return 0;
        }

        @Override
        public float pri() {
            return 0;
        }
    }
}
