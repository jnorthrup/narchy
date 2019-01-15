package nars.exe;

import jcog.exe.InstrumentedLoop;
import jcog.math.FloatRange;
import nars.NAR;
import org.jetbrains.annotations.NotNull;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 */
public class NARLoop extends InstrumentedLoop {

    private final NAR nar;

    public final FloatRange throttle = new FloatRange(1f, 0f, 1f);

    /**
     * starts paused; thread is not automatically created
     */
    public NARLoop(@NotNull NAR n) {
        super();
        nar = n;
    }

    @Override
    protected boolean async() {
        return true;
    }

    @Override
    public final boolean next() {

        try {
            nar.time.cycle(nar);

//        if (async) {
//            nar.eventCycle.emitAsync(nar, nar.exe, () -> ready());
//        } else {
            nar.eventCycle.emit(nar);
//        }
        } finally {
            ready();
        }

        return true;
    }

    public NAR nar() {
        return nar;
    }

}
