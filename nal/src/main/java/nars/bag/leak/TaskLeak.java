package nars.bag.leak;

import jcog.bag.Bag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.pri.PLink;
import jcog.util.AtomicFloat;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.exe.Causable;

import java.util.Random;


/**
 * interface for controlled draining of a bag
 * "leaky bucket" model
 */
public abstract class TaskLeak extends Causable {

    protected final DtLeak<Task, PLink<Task>> queue;

    protected TaskLeak(int capacity, float ratePerDuration, NAR n) {
        
        this(
                
                new PLinkArrayBag(Param.taskMerge, capacity)





                , ratePerDuration, n
        );
    }

    protected TaskLeak(Bag<Task, PLink<Task>> bag, float ratePerDuration, NAR n) {
        this(bag, new AtomicFloat(ratePerDuration), n);
    }


    TaskLeak(Bag<Task, PLink<Task>> bag, Number rate, NAR n) {
        super(n);
        this.queue = new DtLeak<>(bag, rate) {
            @Override
            protected Random random() {
                return n.random();
            }

            @Override
            protected float receive(PLink<Task> b) {
                Task t = b.id;
                if (t.isDeleted())
                    return 0f;
                else
                    return TaskLeak.this.leak(t);
            }

            @Override
            protected boolean full() {
                return TaskLeak.this.full();
            }
        };
    }


    /** override to implement backpressure stop switch */
    protected boolean full() {
        return false;
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
    protected int next(NAR nar, int iterations) {

        if (queue.isEmpty())
            return -1; 

        float done = queue.commit(nar, iterations);
        return (int) Math.ceil(done);
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
