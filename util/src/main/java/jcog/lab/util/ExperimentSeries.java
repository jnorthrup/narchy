package jcog.lab.util;

/** manages a thread that runs and collects the results from a series of experiments
 *  which it determines to conduct by changing a set of variables and observing
 *  effects.
 *  obeys specified stopping conditions, without which it can be allowed to run
 *  continuously untli it is stopped manually.
 */
abstract public class ExperimentSeries<E> implements Runnable {
    final Thread thread;

    public ExperimentSeries() {
        this.thread = new Thread(this);
    }

    @Override
    public void run() {
        ExperimentRun<E> next;
        while ((next = next())!=null) {
            next.run();
            sense(next);
        }
    }

    /** collect results after an experiment has finished */
    protected void sense(ExperimentRun<E> next) {

    }

    /** return null to terminate before the next iteration */
    abstract protected ExperimentRun<E> next();



}
