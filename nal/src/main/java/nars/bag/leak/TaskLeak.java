package nars.bag.leak;

import jcog.bag.Bag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.pri.PLink;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.exe.Causable;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.Random;


/**
 * interface for controlled draining of a bag
 * "leaky bucket" model
 */
public abstract class TaskLeak extends Causable {

    protected final DtLeak<Task, PLink<Task>> queue;

    protected TaskLeak(int capacity, float ratePerDuration, NAR n) {
        //noinspection Convert2Diamond
        this(
                //new ConcurrentArrayBag<Task, PLink<Task>>(PriMerge.max, capacity) {
                new PLinkArrayBag(Param.taskMerge, capacity)
//              {      @Nullable
//                    @Override
//                    public Task key(PLink<Task> t) {
//                        return t.get();
//                    } }
                , ratePerDuration, n
        );
    }

    protected TaskLeak(Bag<Task, PLink<Task>> bag, float ratePerDuration, NAR n) {
        this(bag, new MutableFloat(ratePerDuration), n);
    }


    TaskLeak(Bag<Task, PLink<Task>> bag, MutableFloat rate, NAR n) {
        super(n);
        this.queue = new DtLeak<>(bag, rate) {
            @Override
            protected Random random() {
                return n.random();
            }

            @Override
            protected float receive(PLink<Task> b) {
                Task t = b.get();
                if (t.isDeleted())
                    return 0f;
                else
                    return TaskLeak.this.leak(t);
            }
        };
    }

    @Override
    protected void starting(NAR nar) {
        ons.add(nar.onTask((t) -> accept(t)));
    }


    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    protected int next(NAR nar, int work) {
//        return in.commit(nar.time(), dt, nar.dur(),
//                work/((float)in.bag.capacity()));

        if (queue.isEmpty())
            return -1; //done for the cycle

        float done = queue.commit(nar, work);
        return (int) Math.floor(done);
    }

    public final void accept(Task t) {
        if (preFilter(t))
            queue.put(new PLink<>(t, pri(t)));
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
