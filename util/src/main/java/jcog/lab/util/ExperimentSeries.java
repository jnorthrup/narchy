package jcog.lab.util;

/** manages a thread that runs and collects the results from a series of experiments
 *  which it determines to conduct by changing a set of variables and observing
 *  effects.
 *  obeys specified stopping conditions, without which it can be allowed to run
 *  continuously untli it is stopped manually.
 */
abstract public class ExperimentSeries<E> implements Runnable {

    final Thread thread;
    private boolean shutdown = false;

    public ExperimentSeries() {
        this.thread = new Thread(this);
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        shutdown = true;
        thread.stop();
    }

    public final boolean isShutdown() {
        return shutdown;
    }

}
