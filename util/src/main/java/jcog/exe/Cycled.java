package jcog.exe;

import jcog.TODO;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * like an Iterator, but for unbounded iterations,
 * with both synchronous and asynchronous execution methods
 *
 * calls the Runnable.run() method each cycle/iteration.
 */
public interface Cycled extends Runnable {

    /**
     * run asynchronously with specified delay (milliseconds)
     */
    Loop startPeriodMS(int initialDelayMS);

    /**
     * runs until the returned AtomicBoolean is false
     */
    default AtomicBoolean runUntil() {
        var kontinue = new AtomicBoolean(true);
        while (kontinue.get())
            run();
        return kontinue;
    }

    /**
     * runs a specified number of cycles
     */
    default void run(int cycles) {
        for (; cycles > 0; cycles--)
            run();
    }

    default void run(int cycles, Runnable afterEachCycle) {
        for (; cycles > 0; cycles--) {
            run();
            afterEachCycle.run();
        }
    }

    /**
     * run while the supplied predicate returns true
     */
    default void runWhile(BooleanSupplier test) {
        while (test.getAsBoolean()) {
            run();
        }
    }

    /**
     * run asynchronously with no delay
     */
    @Deprecated default Loop start() {
        throw new TODO();
        
        
    }

    /**
     * run asynchronously at specified FPS
     */
    default Loop startFPS(float initialFPS) {
        assert (initialFPS >= 0);

        var millisecPerFrame = initialFPS > 0 ? 1000.0f / initialFPS : 0 /* infinite speed */;
        return startPeriodMS(Math.round(millisecPerFrame));
    }


}
