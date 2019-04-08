package nars.exe;

import jcog.exe.InstrumentedLoop;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.control.NARPart;
import nars.term.Term;
import nars.term.atom.Atom;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 */
abstract public class NARLoop extends InstrumentedLoop {

    private static final Atom NAR_LOOP = $.the(NARLoop.class);

    public final NAR nar;

    public final FloatRange throttle = new FloatRange(1f, 0f, 1f);

    private final NARPart part;

    /**
     * starts paused; thread is not automatically created
     */
    NARLoop(NAR n) {
        super();
        nar = n;
        this.part = new NARPart((Term)$.inh(NAR_LOOP, n.self())) {

        };
        n.start(part);
    }

    public static NARLoop build(NAR nar) {
        return nar.exe.concurrent() ? new NARLoopAsync(nar) : new NARLoopSync(nar);
    }


    final static class NARLoopSync extends NARLoop {

        NARLoopSync(NAR n) {
            super(n);
        }

        @Override
        public final boolean next() {
            nar.time.next(nar);
            nar.eventCycle.emit(nar);
            return true;
        }
    }

    final static class NARLoopAsync extends NARLoop {

        NARLoopAsync(NAR n) {
            super(n);
        }

        @Override
        protected boolean async() {
            return true;
        }

        @Override
        public final boolean next() {

            nar.time.next(nar);

            nar.eventCycle.emitAsync(nar, nar.exe, this::ready);


            return true;
        }
    }

}
