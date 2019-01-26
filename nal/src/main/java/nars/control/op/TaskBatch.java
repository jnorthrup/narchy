package nars.control.op;

import jcog.data.byt.DynBytes;
import jcog.pri.Prioritizable;
import jcog.pri.Weighted;
import jcog.pri.op.PriMerge;
import jcog.util.ArrayUtils;
import nars.attention.PriBuffer;
import nars.task.ITask;

import java.util.Arrays;
import java.util.function.BiConsumer;

import static jcog.data.byt.RecycledDynBytes.tmpKey;

/**
 *bounded-explosive semi-weighted task batching
 *     double buffer of task merge bags
 * recyclable; ideally thread-local */
public class TaskBatch {

    /** double buffered, page flipping strategy */
    final TaskQueue[] queue = new TaskQueue[2];

    /** oscillates between 0 and 1 */
    private int run = 0;

    public static final ThreadLocal<TaskBatch> these = ThreadLocal.withInitial(TaskBatch::new);

    public static TaskBatch get() {
        return these.get();
    }

    private TaskBatch() {
        queue[0] = new TaskQueue();
        queue[1] = new TaskQueue();
    }

    static class TaskQueue extends PriBuffer<BatchableTask> {

        TaskQueue() {
            super(PriMerge.plus, false);
        }

        /** execute and drain the queue */
        public void run(TaskQueue target) {
            /** TODO optional: sort the removed entries by class to group similar implementations in hopes of maximizing cpu code cache hits */
            update((x, pri)->{
                x.run(target);
            });
        }
    }

    public final synchronized void run() {
        while (next());
    }

    /** run tasks in the run queue */
    private boolean next() {
        TaskQueue running = queue[run];
        if (running.isEmpty())
            return false;

        int out = (run + 1) & 2;

        running.run(queue[out]);

        run = out;
        return true;
    }

    public static abstract class BatchableTask extends Weighted implements ITask, Prioritizable {

        /** key cache */
        public byte[] key = null;
        private int hash = 0;

        /** singleton */
        protected BatchableTask(float pri) {
            super(pri);
            key(null, ArrayUtils.EMPTY_OBJECT_ARRAY);
        }

        protected <A> BatchableTask(float pri, BiConsumer<A, DynBytes> argWriter, A... args) {
            super(pri);
            key(argWriter, args);
        }

        /** write a homoiconizing key describing the computation represented by this task */
        abstract public void key(DynBytes/*ByteArrayDataOutput*/ target);

//        abstract protected BatchableTask merge(BatchableTask b);

        /** run, adding any subsequent tasks to the provided task queue */
        abstract public void run(TaskQueue next);


        protected <A> byte[] key(BiConsumer<A, DynBytes> argWriter, A... args) {
            if (args.length == 0) {
                hash = 0;
                return key = ArrayUtils.EMPTY_BYTE_ARRAY;
            }

            DynBytes x = tmpKey();
            x.writeByte(args.length);
            for (A a : args) {
                argWriter.accept(a, x);
            }
            key = x.arrayCopy();
            hash = x.hashCode();
            x.clear();
            return key;
        }

        private byte[] key() {
            if (key==null) {
                DynBytes x = tmpKey();
                key(x);
                key = x.arrayCopy();
                hash = x.hashCode();
                x.clear();
            }
            return key;
        }

        @Override
        public final boolean equals(Object obj) {
            return this==obj || Arrays.equals(key(), ((BatchableTask)obj).key());
        }

        @Override
        public final int hashCode() {
            key();
            return hash;
        }
    }

    /** a leaf procedure; doesnt fork any new tasks */
    abstract public static class SimpleBatchableTask extends BatchableTask {

        protected SimpleBatchableTask(float pri) {
            super(pri);
        }

        protected <A> SimpleBatchableTask(float pri, BiConsumer<A, DynBytes> argWriter, A... args) {
            super(pri, argWriter, args);
        }

        /** run the procedure, optionally using the 'pri' value */
        abstract protected void run();

        @Override
        public final void run(TaskQueue next) {
            run();
        }
    }

}
