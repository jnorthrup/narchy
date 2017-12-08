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
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * manages low level task scheduling and execution
 */
abstract public class Exec implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(Exec.class);

    protected NAR nar;

    private On onClear;


    /**
     * schedules the task for execution but makes no guarantee it will ever actually execute
     */
    abstract public void add(/*@NotNull*/ ITask input);

    public void add(/*@NotNull*/ Iterator<? extends ITask> input) {
        input.forEachRemaining(this::add);
    }

    public final void add(/*@NotNull*/ Iterable<? extends ITask> input) {
        add(input.iterator());
    }

    public void add(/*@NotNull*/ Stream<? extends ITask> input) {
        add(input.iterator());
    }

    protected void execute(ITask x) {

        try {

            Iterable<? extends ITask> y = x.run(nar);
            if (y != null)
                add(y.iterator());

        } catch (Throwable e) {
            if (Param.DEBUG) {
                throw e;
            } else {
                logger.error("{} {}", x, e); //(Param.DEBUG) ? e : e.getMessage());
                x.delete();
            }
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


    @Override
    public void execute(Runnable async) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(async);
        } else {
            async.run();
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
    }

}
