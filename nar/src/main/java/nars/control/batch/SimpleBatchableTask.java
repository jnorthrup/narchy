package nars.control.batch;

import jcog.data.byt.DynBytes;

import java.util.function.BiConsumer;

/** a leaf procedure; doesnt fork any new tasks */
abstract public class SimpleBatchableTask extends BatchableTask {

    protected SimpleBatchableTask(float pri) {
        super(pri);
    }

    protected <A> SimpleBatchableTask(float pri, BiConsumer<A, DynBytes> argWriter, A... args) {
        super(pri, argWriter, args);
    }

    /** run the procedure, optionally using the 'pri' value */
    abstract protected void run();

    @Override
    public final void run(TaskBatch.TaskQueue next) {
        run();
    }
}
