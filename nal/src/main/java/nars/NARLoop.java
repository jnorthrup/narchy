package nars;

import jcog.exe.Loop;
import jcog.math.FloatRange;
import org.jetbrains.annotations.NotNull;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 */
public class NARLoop extends Loop {

    public final NAR nar;

    public final FloatRange throttle = new FloatRange(1f, 0f, 1f);

    /** scheduler temporal granularity (in sec) */
    public final FloatRange jiffy = new FloatRange(0.05f, 0.001f, 0.5f);

    /**
     * starts paused; thread is not automatically created
     */
    public NARLoop(@NotNull NAR n) {
        super();
        nar = n;
    }


    @Override
    public final boolean next() {
        nar.run();
        return true;
    }

}
