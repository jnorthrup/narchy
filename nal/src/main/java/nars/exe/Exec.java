package nars.exe;

import jcog.data.list.FasterList;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.BufferedBag;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.channel.ConsumerX;
import nars.task.ITask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * manages low level task scheduling and execution
 */
abstract public class Exec extends ConsumerX<ITask> implements Executor {

    protected static final Logger logger = LoggerFactory.getLogger(Exec.class);

    protected NAR nar;
    protected int concurrencyMax;

    public Exec(int concurrencyMax) {
        this.concurrencyMax = concurrencyMax; //TODO this will be a value like Runtime.getRuntime().availableProcessors() when concurrency can be adjusted dynamically
    }

    public void input(Object t) {
        executeNow(t);
    }

    /**
     * immediately execute a Task
     */
    @Override
    public final void input(ITask x) {
        ITask t = x;
        try {
            ITask.run(t, nar);
        } catch (Throwable ee) {
            taskError(t, x, ee, nar);
        }
    }
    static void taskError(ITask t, ITask x, Throwable ee, NAR nar) {
        if (t == x)
            nar.logger.error("{} {}", x, ee);
        else
            nar.logger.error("{}->{} {}", t, x, ee);

        if (Param.DEBUG)
            throw new RuntimeException(ee);
    }

    @Override
    public void execute(Runnable async) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(async);
        } else {
            async.run();
        }
    }

    public void input(Consumer<NAR> r) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(() -> r.accept(nar));
        } else {
            r.accept(nar);
        }
    }

    /**
     * true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to be safe.
     */
    public boolean concurrent() {
        return concurrencyMax > 1;
    }

    /**
     * current concurrency level; may change
     */
    @Override
    abstract public int concurrency();

    /**
     * maximum possible concurrency; should remain constant
     */
    public final int concurrencyMax() {
        return concurrencyMax;
    }

    /**
     * inline, synchronous
     */
    protected final void executeNow(Object t) {
        if (t instanceof ITask)
            input((ITask) t);
        else {
            try {
                if (t instanceof Runnable) {
                    ((Runnable) t).run();
                } else {
                    ((Consumer) t).accept(nar);
                }
            } catch (Throwable e) {
                logger.error("{} {}", t, /*Param.DEBUG ?*/ e /*: e.getMessage()*/);
            }
        }
    }



    public void start(NAR nar) {
        this.nar = nar;
    }


    public void stop() {

        //this.nar = null;
    }


    public void print(Appendable out) {
        try {
            out.append(this.toString());
            out.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static final ThreadLocal<FasterList<ITask>> tmpTasks = ThreadLocal.withInitial(FasterList::new);


    /** asynchronously drain N elements from a bag as input */
    public void input(Bag<?, ITask> taskBag, int max) {
        Bag b;
        if  (taskBag instanceof BufferedBag)
            b = ((BufferedBag)taskBag).bag;
        else
            b = taskBag;

        input(nn -> {

            FasterList batch = Exec.tmpTasks.get();

            if (b instanceof ArrayBag) {
                boolean blocking = true;
                ((ArrayBag) b).popBatch(max, blocking, batch::add);
            } else {
                b.pop(null, max, batch::add); //per item.. may be slow
            }

            if (!batch.isEmpty()) {
                try {
                    if (batch.size() > 2)
                        batch.sortThis(Task.sloppySorter);

                    ITask.run(batch, nn);
                } finally {
                    batch.clear();
                }
            }
        });

    }


}
