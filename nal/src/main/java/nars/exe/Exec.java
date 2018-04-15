package nars.exe;

import jcog.event.On;
import jcog.list.FasterList;
import nars.NAR;
import nars.concept.Concept;
import nars.control.Activate;
import nars.control.Cause;
import nars.task.ITask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * manages low level task scheduling and execution
 */
abstract public class Exec implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(Exec.class);

    protected NAR nar;

    private On onClear;


    public void execute(/*@NotNull*/ Iterator<? extends ITask> input) {
        input.forEachRemaining(this::execute);
    }

    public final void execute(/*@NotNull*/ Iterable<? extends ITask> input) {
        execute(input.iterator());
    }

    public void execute(/*@NotNull*/ Stream<? extends ITask> input) {
        input.forEach(this::execute);
    }

    public void execute(Object t) {
        executeNow(t);
    }


    /** inline, synchronous */
    final void executeNow(Object t) {
//        try {
            if (t instanceof ITask)
                ((ITask) t).run(nar);
            else if (t instanceof Runnable)
                ((Runnable) t).run();
            else //if (t instanceof Consumer)
                ((Consumer) t).accept(nar);
//            else {
//                throw new UnsupportedOperationException(t + " unexecutable");
//            }
//        } catch (Throwable e) {
//            logger.error("{} {}", t, Param.DEBUG ? e : e.getMessage());
//        }

    }

    @Override abstract public void execute(Runnable async);
    abstract public void execute(Consumer<NAR> r);


    abstract public void fire(Predicate<Activate> each);


    abstract public Stream<Activate> active();

    public void start(NAR nar) {
        synchronized (this) {
            this.nar = nar;
            onClear = nar.eventClear.on((n) -> clear());
        }
    }


    public void stop() {
        synchronized (this) {
            onClear.off();
            onClear = null;
            this.nar = null;
        }
    }

    abstract void clear();

    /**
     * true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to be safe.
     */
    public abstract boolean concurrent();




    public void print(PrintStream out) {
        out.println(this);
    }



    abstract public void activate(Concept c, float activationApplied);

    public interface Revaluator {
        /**
         * goal and goalSummary instances correspond to the possible MetaGoal's enum
         */
        void update(long time, int dur, FasterList<Cause> causes, float[] goal);

        default void update(NAR nar) {
            update(nar.time(), nar.dur(), nar.causes, nar.emotion.want);
        }
    }

}
