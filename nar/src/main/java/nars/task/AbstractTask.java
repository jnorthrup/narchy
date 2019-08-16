package nars.task;

import jcog.Log;
import jcog.Util;
import jcog.pri.Prioritizable;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Task;
import nars.attention.What;
import nars.exe.Exec;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;

/**
 * indestructible system/natively impl tasks
 *
 * the reasoner remains oblivious of these.
 * but it holds a constant 1.0 priority.
 */
public abstract class AbstractTask implements Task {

    public static final Logger logger = Log.logger(AbstractTask.class);

    public static Task multiTask(@Nullable Collection<Task> next) {
        if (next == null) return null;
        switch (next.size()) {
            case 0:
                return null;
            case 1:
                return Util.only(next);
            default:
                return new TasksArray(next.toArray(Task[]::new), true);
        }
    }


//    public static void error(Prioritizable t, Prioritizable x, Throwable ee) {
//        if (t == x)
//            Task.logger.error("{} {}", x, ee);
//        else
//            Task.logger.error("{}->{} {}", t, x, ee);
//    }

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

    /**
     * process the next stage; returns null if finished
     */
    @Deprecated
    public abstract Task next(Object n);


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

    @Override
    public short[] why() {
        return ArrayUtil.EMPTY_SHORT_ARRAY;
    }

    @Override
    public @Nullable Truth truth(long when, float dur) {
        return null;
    }

    @Override
    public @Nullable Truth truth(long targetStart, long targetEnd, float dur) {
        return null;
    }

    @Override
    public @Nullable Truth truth() {
        return null;
    }

    @Override
    public boolean isCyclic() {
        return false;
    }

    @Override
    public void setCyclic(boolean b) {

    }

    @Override
    public long creation() {
        return 0;
    }

    @Override
    public void setCreation(long creation) {

    }

    @Override
    public long start() {
        return ETERNAL;
    }

    @Override
    public long end() {
        return ETERNAL;
    }

    @Override
    public long[] stamp() {
        return ArrayUtil.EMPTY_LONG_ARRAY;
    }

    @Override
    public Term term() {
        return $.identity(this);
    }


    /** execute the given tasks */
    public final static class TasksArray extends AbstractTask {

        public final Task[] tasks;

        private TasksArray(Task[] x, boolean anyOrder) {
            this.tasks = x;

            //sort by type, emulating loop unrolling by batching the set of tasks by their type.
            if (anyOrder && x.length > 2) {
                Arrays.sort(x, Comparators.byIntFunction((Prioritizable z)->z.getClass().hashCode()));
            }
        }

        @Override
        public Task next(Object n) {
            What w = (What) n;
            for (Task t: tasks)
                Exec.run(t, w);
            return null;
        }

    }


}
