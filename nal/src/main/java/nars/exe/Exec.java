package nars.exe;

import jcog.Util;
import jcog.event.On;
import jcog.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.control.Activate;
import nars.control.Cause;
import nars.task.ITask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
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
        executeInline(t);
    }

    protected void executeInline(Object t) {
        try {
            if (t instanceof ITask) {
                ITask x = (ITask)t;
                while ((x = x.run(nar))!=null);
            } else if (t instanceof Consumer)
                ((Consumer) t).accept(nar);
            else if (t instanceof Runnable)
                ((Runnable)t).run();
            else
                throw new UnsupportedOperationException(t + " unexecutable");
        } catch (Throwable e) {
            logger.error("{} {}", t, e);
        }

    }

    @Override
    public void execute(Runnable async) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(async);
        } else {
            async.run();
        }
    }


    abstract public void fire(Predicate<Activate> each);

    /**
     * an estimate or exact number of parallel processes this runs
     */
    abstract public int concurrency();


    abstract public Stream<Activate> active();

    public synchronized void start(NAR nar) {
        if (this.nar != null) {
            this.onClear.off();
            this.onClear = null;
        }

        this.nar = nar;

        onClear = nar.eventClear.on((n) -> clear());
    }

    public abstract void cycle();

    public synchronized void stop() {
        if (onClear != null) {
            onClear.off();
            onClear = null;
        }
    }

    abstract void clear();

    /**
     * true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to be safe.
     */
    public boolean concurrent() {
        return concurrency() > 1;
    }


    public void execute(Consumer<NAR> r) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(() -> r.accept(nar));
        } else {
            r.accept(nar);
        }
    }

    public void print(PrintStream out) {
        out.println(this);
    }


    public float load() {
        if (nar.loop.isRunning()) {
            return Util.unitize(nar.loop.lag());
        } else {
            return 0;
        }
    }


    abstract public void activate(Concept c, float activationApplied);

    public interface Revaluator {
        /**
         * goal and goalSummary instances correspond to the possible MetaGoal's enum
         */
        void update(long time, int dur, FasterList<Cause> causes, float[] goal);

        default void update(NAR nar) {
            update(nar.time(), nar.dur(), nar.causes, nar.want);
        }
    }

}
