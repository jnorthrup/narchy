package nars.exe;

import nars.NAR;
import nars.Param;
import nars.task.ITask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * manages low level task scheduling and execution
 */
abstract public class Exec implements Executor {

    protected static final Logger logger = LoggerFactory.getLogger(Exec.class);

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


    /** inline, synchronous */
    protected final void executeNow(Object t) {
        try {
            if (t instanceof ITask) {
//                ITask tt = (ITask) t;
//                if (Param.CAUSE_MULTIPLY_EVERY_TASK && t instanceof Task) {
//                    tt.priMult(nar.amp((Task) tt), Pri.EPSILON);
//                }
                ((ITask) t).run(nar);
            } else if (t instanceof Runnable)
                ((Runnable) t).run();
            else 
                ((Consumer) t).accept(nar);



        } catch (Throwable e) {
            logger.error("{} {}", t, Param.DEBUG ? e : e.getMessage());
        }

    }

    @Override abstract public void execute(Runnable async);
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




    public void print(PrintStream out) {
        out.println(this);
    }





    /** TODO refactor into an independent DurService that updates causes with wants */
    public interface Revaluator {
        /**
         * goal and goalSummary instances correspond to the possible MetaGoal's enum
         */
        

        void update(NAR nar);

    }

}
