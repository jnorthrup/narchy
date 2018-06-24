package jcog.exe.valve;

/**
 * micro-managed iterable with implementable pre-batch (start) and post-batch (stop) methods
 */
public interface Work {

    /**
     * called when a worker is selected to begin execution. it may refuse by returning false. in doing so it may be deferred further opportunity until the next cycle when it may have a chance to return true.
     */
    default boolean start() {
        return true;
    }

    /**
     * returns # actual iterations
     */
    default boolean next() {
        return next(1) >= 0;
    }

    /**
     * commands the next n iterations.  by returning false, the worker chooses to end this batch.  no guarantee it will be called again even if returning true.
     */
    default int next(int n) {
        int i;
        for (i = 0; i < n; i++) {
            if (!next())
                return -i;
        }
        return i;
    }

    /**
     * called after work iterations were performed. can be used to implement some clean-up or persist state.
     */
    default void stop() {

    }
}
