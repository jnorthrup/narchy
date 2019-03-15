package nars.control.batch;

import jcog.pri.PriBuffer;
import jcog.pri.op.PriMerge;

/**
 *bounded-explosive semi-weighted task batching
 *     double buffer of task merge bags
 * recyclable; ideally thread-local */
public final class TaskBatch {

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

    public void add(BatchableTask x) {
        queue[run].put(x);
    }

    static class TaskQueue extends PriBuffer<BatchableTask> {

        TaskQueue() {
            super(PriMerge.plus);
        }

        /** execute and drain the queue */
        public void run(TaskQueue target) {
            /** TODO optional: sort the removed entries by class to group similar implementations in hopes of maximizing cpu code cache hits */
            update((x, pri)-> x.run(target));
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

        int out = (run + 1) & 1;

        running.run(queue[out]);

        run = out;
        return true;
    }

}
