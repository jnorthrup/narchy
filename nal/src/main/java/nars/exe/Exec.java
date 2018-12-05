package nars.exe;

import jcog.exe.Exe;
import nars.NAR;
import nars.task.ITask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * manages low level task scheduling and execution
 */
abstract public class Exec implements Executor {

    static final Logger logger = LoggerFactory.getLogger(Exec.class);

    protected NAR nar;


    public void execute(/*@NotNull*/ Iterator<? extends ITask> input) {
        input.forEachRemaining(this::execute);
    }

    public void execute(/*@NotNull*/ Stream<? extends ITask> input) {
        input.forEach(this::execute);
    }

    public final void execute(/*@NotNull*/ Iterable<? extends ITask> input) {
        execute(input.iterator());
    }


    public void execute(Object t) {
        executeNow(t);
    }


    /**
     * inline, synchronous
     */
    final void executeNow(Object t) {
        if (t instanceof ITask)
            executeNow((ITask) t);
        else {
            Exe.profiled(t, () -> {
                try {
                    if (t instanceof Runnable) {
                        ((Runnable) t).run();
                    } else {
                        ((Consumer) t).accept(nar);
                    }
                } catch (Throwable e) {
                    logger.error("{} {}", t, /*Param.DEBUG ?*/ e /*: e.getMessage()*/);
                }
            });
        }
    }

    final void executeNow(ITask t) {
        ITask.run(t, nar);
    }

    @Override
    abstract public void execute(Runnable async);

    abstract public void execute(Consumer<NAR> r);


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
