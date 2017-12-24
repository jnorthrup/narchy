package nars;

import jcog.constraint.continuous.DoubleVar;
import jcog.exe.Loop;
import jcog.math.FloatParam;
import org.jetbrains.annotations.NotNull;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 */
public class NARLoop extends Loop {

    public final NAR nar;

    public final FloatParam throttle = new FloatParam(1f, 0f, 1f);

    /** scheduler temporal granularity -
     * fraction of cycle that a task is scheduled to run proportionally to */
    public final FloatParam jiffy = new FloatParam(0.15f, 0.001f, 0.5f);

    /**
     * starts paused; thread is not automatically created
     */
    public NARLoop(@NotNull NAR n) {
        super();
        nar = n;
    }

    /**
     * @param n
     * @param initialPeriod
     */
    public NARLoop(@NotNull NAR n, int initialPeriod) {
        this(n);
        setPeriodMS(initialPeriod);
    }

    @Override
    public final boolean next() {
        nar.run();
        return true;
    }

}
