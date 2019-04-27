package nars.task;

import jcog.Util;
import jcog.pri.Prioritizable;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

/**
 * indestructible system/natively impl tasks
 *
 * the reasoner remains oblivious of these.
 * but it holds a constant 1.0 priority.
 */
public abstract class AbstractTask implements ITask, Prioritizable {

    @Nullable
    public static ITask of(@Nullable Collection<ITask> next) {
        if (next == null) return null;
        switch (next.size()) {
            case 0:
                return null;
            case 1:
                return Util.only(next);
            default:
                return new TasksArray(next.toArray(ITask[]::new), true);
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
        //return 1f;
        throw new UnsupportedOperationException();
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
////                    next.addAt(p);
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
                Arrays.sort(x, Comparators.byIntFunction((Prioritizable z)->z.getClass().hashCode()));
            }
        }

        @Override
        public ITask next(Object n) {
            for (ITask t: tasks)
                ITask.run(t, n);
            return null;
        }

    }
    /** execute the given tasks */
    public final static class TasksIterable extends AbstractTask {

        private final Iterable<? extends ITask> tasks;

        public TasksIterable(Iterable<? extends ITask> x) {
            this.tasks = x;
        }
        @Override
        public ITask next(Object n) {
            for (ITask t: tasks)
                ITask.run(t, n);
            return null;
        }

    }

}
