package nars.task.util;

import jcog.data.list.FasterList;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import nars.NAR;
import nars.Task;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.term.Functor;

import java.util.concurrent.atomic.AtomicBoolean;

public class TaskBagDrainer extends AbstractTask {

    final AtomicBoolean busy = new AtomicBoolean(false);
    private final Bag<Task, Task> bag;
    private final boolean singleton;

    /**
     * (size, capacity) -> numToDrain
     */
    private final Functor.IntIntToIntFunction rateControl;


    public TaskBagDrainer(Bag<Task, Task> tasks, boolean singleton, Functor.IntIntToIntFunction rateControl) {
        this.bag = tasks;
        this.singleton = singleton;
        this.rateControl = rateControl;
    }

    static final ThreadLocal<FasterList<ITask>> batch = ThreadLocal.withInitial(FasterList::new);

    @Override
    public ITask next(NAR nar) {

        if (singleton && !busy.compareAndSet(false,true))
            return null; //an operation is in-progress

        try {

            int n = rateControl.apply(bag.size(), bag.capacity());
            if (n > 0) {

                if (bag instanceof ArrayBag) {
                    FasterList<ITask> batch = TaskBagDrainer.batch.get();
                    ((ArrayBag) bag).popBatch(n, batch);
                    if (!batch.isEmpty()) {
                        batch.forEachWith(ITask::run, nar);
                        batch.clear();
                    }

                } else {
                    bag.pop(null, n, nar::input); //per item.. may be slow
                }
            }

        } finally {
            busy.set(false);
        }

        return null;
    }

    @Override
    public String toString() {
        return "drainDerivations(" + bag.getClass()+ '@' + System.identityHashCode(bag) + ')';
    }
}
