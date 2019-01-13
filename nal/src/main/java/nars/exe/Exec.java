package nars.exe;

import nars.NAR;
import nars.control.channel.ConsumerX;
import nars.task.ITask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * manages low level task scheduling and execution
 */
abstract public class Exec extends ConsumerX<ITask> implements Executor {

    static final Logger logger = LoggerFactory.getLogger(Exec.class);

    protected NAR nar;

    public void input(Object t) {
        executeNow(t);
    }

    /** immediately execute a Task */
    @Override public final void input(ITask t) {
        ITask.run(t, nar);
    }

    abstract public void input(Consumer<NAR> r);

    @Override
    abstract public void execute(Runnable async);


    /**
     * inline, synchronous
     */
    final void executeNow(Object t) {
        if (t instanceof ITask)
            input((ITask) t);
        else {
//            Exe.run(t, () -> {
                try {
                    if (t instanceof Runnable) {
                        ((Runnable) t).run();
                    } else {
                        ((Consumer) t).accept(nar);
                    }
                } catch (Throwable e) {
                    logger.error("{} {}", t, /*Param.DEBUG ?*/ e /*: e.getMessage()*/);
                }
//            });
        }
    }


    public void start(NAR nar) {
        this.nar = nar;
    }


    public void stop() {

        //this.nar = null;
    }


    /**
     * true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to be safe.
     */
    public abstract boolean concurrent();

    /**
     * current concurrency level; may change
     */
    public abstract int concurrency();

    /**
     * maximum possible concurrency; should remain constant
     */
    abstract public int concurrencyMax();


    public void print(Appendable out) {
        try {
            out.append(this.toString());
            out.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
