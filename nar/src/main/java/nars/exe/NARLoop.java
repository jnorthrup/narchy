package nars.exe;

import jcog.exe.InstrumentedLoop;
import jcog.math.FloatRange;
import nars.NAR;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 */
abstract public class NARLoop extends InstrumentedLoop {

    public final NAR nar;

    public final FloatRange throttle = new FloatRange(1f, 0f, 1f);

    /**
     * starts paused; thread is not automatically created
     */
    NARLoop(NAR n) {
        super();
        nar = n;
    }

    public static NARLoop the(NAR nar) {
        return nar.exe.concurrent() ? new NARLoopAsync(nar) : new NARLoopSync(nar);
    }


    private final static class NARLoopSync extends NARLoop {

        NARLoopSync(NAR n) { super(n); }

        @Override
        public final boolean next() {
            nar.exe.next(nar);
            nar.eventCycle.emit(nar);
            nar.time.next();
            return true;
        }
    }

    private final static class NARLoopAsync extends NARLoop {

        NARLoopAsync(NAR n) {
            super(n);
        }

        @Override
        protected boolean async() {
            return true;
        }

        @Override
        public final boolean next() {
            nar.exe.next(nar);
            nar.eventCycle.emitAsync(nar, nar.exe, this::ready);
            nar.time.next();
            return true;
        }
    }

}
