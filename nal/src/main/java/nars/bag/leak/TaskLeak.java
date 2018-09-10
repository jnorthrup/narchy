package nars.bag.leak;

import jcog.pri.PLink;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PLinkArrayBag;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.exe.Causable;

import java.util.Random;
import java.util.function.BooleanSupplier;


/**
 * interface for controlled draining of a bag
 * "leaky bucket" model
 */
public abstract class TaskLeak extends Causable {

    protected final DtLeak<Task, PLink<Task>> queue;

    protected TaskLeak(int capacity, NAR n) {
        
        this(
                
                new PLinkArrayBag(Param.taskMerge, capacity)





                , n
        );
    }


    TaskLeak(Bag<Task, PLink<Task>> bag, NAR n) {
        super();
        this.queue = new DtLeak<>(bag) {
            @Override
            protected Random random() {
                return n.random();
            }

            @Override
            protected float receive(PLink<Task> b) {
                Task t = b.id;
                return t.isDeleted() ? 0f : TaskLeak.this.leak(t);
            }

        };
        n.on(this);
    }


    @Override
    protected void starting(NAR nar) {
        ons.add(nar.onTask(this::accept));
    }


    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    protected void next(NAR nar, BooleanSupplier kontinue) {

        if (queue == null /* HACK */ || queue.isEmpty())
            return;

        queue.commit(nar, kontinue);
    }

    public final void accept(Task t) {
        if (preFilter(t)) {
            float p = pri(t);
            if (p == p)
                queue.put(new PLink<>(t, p));
        }
    }

    /** an adjusted priority of the task for its insertion to the leak bag */
    protected float pri(Task t) {
        return t.priElseZero();
    }

    protected boolean preFilter(Task next) {
        return true;
    }

    /** returns how much of the input was consumed; 0 means nothing, 1 means 100% */
    abstract protected float leak(Task next);
}
