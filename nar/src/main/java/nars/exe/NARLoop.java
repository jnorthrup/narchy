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

    public final static class NARLoopSync extends NARLoop {

        public NARLoopSync(NAR n) { super(n); }

        @Override
        public final boolean next() {
            NAR n = nar;
            n.exe.next();
            n.eventCycle.emit(n);
            n.time.next();
            return true;
        }
    }

    public final static class NARLoopAsync extends NARLoop {

        public NARLoopAsync(NAR n) {
            super(n);
        }

        @Override
        protected boolean async() {
            return true;
        }

        @Override
        public final boolean next() {
            NAR n = nar;
            Exec exe = n.exe;
            exe.next();
            n.eventCycle.emitAsync(n, exe, this::ready);
            n.time.next();
            return true;
        }
    }

}
