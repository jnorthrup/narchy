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

    /** scheduler temporal granularity (in cycle fractions) */
    public final FloatRange jiffy = new FloatRange(0.25f, 0.001f, 1f);
    //private final boolean async;

    /**
     * starts paused; thread is not automatically created
     */
    public NARLoop(@NotNull NAR n) {
        super();
        nar = n;
        //async = n.exe.concurrent();
    }


    @Override
    public final boolean next() {

        nar.emotion.cycle();

        nar.time.cycle(nar);

//        if (async) {
//            nar.eventCycle.emitAsync(nar, nar.exe, () -> ready());
//        } else {
            nar.eventCycle.emit(nar);
//        }


        return true;
    }

    public NAR nar() {
        return nar;
    }


//    @Override
//    protected boolean async() {
//        return async;
//    }
}
