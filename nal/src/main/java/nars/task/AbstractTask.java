package nars.task;

import com.google.common.primitives.Longs;
import jcog.Util;
import jcog.pri.Priority;
import nars.NAR;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * indestructible system/natively impl tasks
 *
 * the reasoner remains oblivious of these.
 * but it holds a constant 1.0 priority.
 */
public abstract class AbstractTask implements ITask, Priority {

    @Nullable
    public static ITask of(@Nullable Collection<ITask> next) {
        if (next == null) return null;
        switch (next.size()) {
            case 0:
                return null;
            case 1: {
                return Util.only(next);
            }
            default: {
                return new TasksArray(next.toArray(ITask[]::new), true);
            }
        }
    }

    public final byte punc() {
        return 0;
    }

    @Override
    public final float pri() {
        return 1;
    }

//    @Override
//    public String toString() {
//        return getClass().toStri
//    }

    @Override
    public final boolean delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean isDeleted() {
        return false;
    }

    @Override
    public final float pri(float p) {
        return 1f;
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


    public static final class SchedTask extends AbstractTask implements Comparable<SchedTask> {

        public final long when;
        public final Consumer<NAR> what;

        public SchedTask(long whenOrAfter, Consumer<NAR> what) {
            this.when = whenOrAfter;
            this.what = what;
        }

        public SchedTask(long whenOrAfter, Runnable what) {
            this(whenOrAfter, (n) -> what.run());
        }

        @Override
        public String toString() {
            return "@" + when + ':' + what;
        }

        @Override
        public final ITask next(NAR n) {
            what.accept(n);
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int compareTo(AbstractTask.SchedTask that) {
            if (this == that) return 0;

            int t = Longs.compare(when, that.when);
            if (t != 0) {
                return t;
            }

            return Integer.compare(System.identityHashCode(what), System.identityHashCode(that.what));
        }
    }


//    public static class NARTask extends AbstractTask {
//
//        final Consumer run;
//
//        public NARTask(Consumer<NAR> runnable) {
//            run = runnable;
//        }
//
//        @Override
//        public String toString() {
//            return run.toString();
//        }
//
//        @Override
//        public ITask next(NAR x) {
//            run.accept(x);
//            return null;
//        }
//
//    }
//    public final static class TasksCollection extends AbstractTask {
//        private final Collection<ITask> tasks;
//
//        public TasksCollection(Collection<ITask> x) {
//            this.tasks = x;
//            if (x.size() > 3) {
//                //sort by the task type
//            }
//        }
//
//        @Override
//        public ITask next(NAR n) {
//            //FasterList<ITask> next = null;
//            for (ITask t: tasks) {
//                t.run(n);
////                ITask p = t.next(n);
////                if (p!=null) {
////                    if (next == null) next = new FasterList(1);
////                    next.add(p);
////                }
//            }
//            return null;
//        }
//
//    }

    /** execute the given tasks */
    public final static class TasksArray extends AbstractTask {
        private final ITask[] tasks;

        private TasksArray(ITask[] x, boolean anyOrder) {
            this.tasks = x;

            //sort by type, emulating loop unrolling by batching the set of tasks by their type.
            if (anyOrder && x.length > 2) {
                Arrays.sort(x, Comparators.byIntFunction((ITask z)->z.getClass().hashCode()));
            }
        }

        @Override
        public ITask next(NAR n) {
            //FasterList<ITask> next = null;
            for (ITask t: tasks) {
                t.run(n);
//                ITask p = t.next(n);
//                if (p!=null) {
//                    if (next == null) next = new FasterList(1);
//                    next.add(p);
//                }
            }
            return null;
        }

    }
}
