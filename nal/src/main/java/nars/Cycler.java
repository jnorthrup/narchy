package nars;

import jcog.exe.Loop;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** like an Iterator, but for unbounded iterations,
  * with both synchronous and asynchronous execution methods */
public interface Cycler {

    void cycle();


    /** runs until the returned AtomicBoolean is false */
    default AtomicBoolean run() {
        AtomicBoolean kontinue = new AtomicBoolean(true);
        while (kontinue.get())
            cycle();
        return kontinue;
    }

    /**
     * runs a specified number of cycles
     */
    default void run(int cycles) {
        for (; cycles > 0; cycles--)
            cycle();
    }

    /** run while the supplied predicate returns true */
    default void runWhile(BooleanSupplier test) {
        while (test.getAsBoolean()) {
            cycle();
        }
    }

    /** run asynchronously with no delay */
    default Loop start() {
        return startPeriodMS(0);
    }

    /** run asynchronously at specified FPS */
    default Loop startFPS(float initialFPS) {
        assert (initialFPS >= 0);

        float millisecPerFrame = initialFPS > 0 ? 1000.0f / initialFPS : 0 /* infinite speed */;
        return startPeriodMS(Math.round(millisecPerFrame));
    }

    /** run asynchronously with specified delay (milliseconds) */
    Loop startPeriodMS(int initialDelayMS);

}
