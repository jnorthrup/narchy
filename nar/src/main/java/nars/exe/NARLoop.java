package nars.exe;

import jcog.exe.InstrumentedLoop;
import jcog.math.FloatRange;
import nars.NAR;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 */
public abstract class NARLoop extends InstrumentedLoop {

    public final NAR nar;

    public final FloatRange throttle = new FloatRange(1f, 0f, 1f);

    /**
     * starts paused; thread is not automatically created
     */
    NARLoop(NAR n) {
        super();
        nar = n;
    }

    public interface Pausing {
        void pause(boolean pause);
    }

    @Override
    protected void starting() {
        super.starting();
        nar.parts(Pausing.class).forEach(g->g.pause(false));
    }

    @Override
    protected void stopping() {
        nar.parts(Pausing.class).forEach(g->g.pause(true));
        super.stopping();
    }

    public static final class NARLoopSync extends NARLoop {

        public NARLoopSync(NAR n) { super(n); }

        @Override
        public final boolean next() {
            var n = nar;
            n.exe.next();
            n.eventCycle.emit(n);
            n.time.next();
            return true;
        }
    }

    public static final class NARLoopAsync extends NARLoop {

        public NARLoopAsync(NAR n) {
            super(n);
        }

        @Override
        protected boolean async() {
            return true;
        }

        @Override
        public final boolean next() {
            var n = nar;
            var exe = n.exe;
            exe.next();
            n.eventCycle.emitAsync(n, exe, this::ready);
            n.time.next();
            return true;
        }
    }

}
