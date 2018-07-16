package nars.util.task;

import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Task;
import nars.task.AbstractTask;
import nars.task.ITask;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;

import java.util.concurrent.atomic.AtomicBoolean;

public class TaskBagDrainer extends AbstractTask {

    final AtomicBoolean busy = new AtomicBoolean(false);
    private final Bag<Task, Task> bag;
    private final boolean singleton;
    private final IntToIntFunction rate;

    public TaskBagDrainer(Bag<Task, Task> tasks, boolean singleton, IntToIntFunction rate) {
        this.bag = tasks;
        this.singleton = singleton;
        this.rate = rate;
    }

    @Override
    public ITask next(NAR nar) {

        if (singleton && !busy.weakCompareAndSetAcquire(false,true))
            return null; //an operation is in-progress

        try {

            int s = bag.size();
            int n = rate.applyAsInt(s);
            if (n > 0) {
                bag.pop(null, n, nar::input);
            }

        } finally {
            busy.setRelease(false);
        }

        return null;
    }

    @Override
    public String toString() {
        return "drainDerivations(" + bag.getClass()+ "@" + System.identityHashCode(bag) + ")";
    }
}
