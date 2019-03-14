package nars.exe;

import nars.NAR;
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
    protected final int concurrency, concurrencyMax;

    public Exec(int concurrency, int concurrencyMax) {
        this.concurrency = concurrency;
        this.concurrencyMax = concurrencyMax; //TODO this will be a value like Runtime.getRuntime().availableProcessors() when concurrency can be adjusted dynamically
    }

    public void input(Object t) {
        executeNow(t);
    }

    /**
     * immediately execute a Task
     */
    @Override
    public final void input(ITask t) {
        ITask.run(t, nar);
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
    public final int concurrency() {
        return concurrency;
    }

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
        try {
            if (t instanceof ITask)
                input((ITask) t);
            else {
                if (t instanceof Runnable) {
                    ((Runnable) t).run();
                } else {
                    ((Consumer) t).accept(nar);
                }
            }
        } catch (Throwable e) {
            logger.error("{} {}", t, /*Param.DEBUG ?*/ e /*: e.getMessage()*/);
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


}
