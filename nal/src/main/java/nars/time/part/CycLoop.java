package nars.time.part;

import nars.NAR;
import nars.control.NARPart;
import nars.time.event.WhenCycle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * per-cycle invoked part
 */
abstract public class CycLoop extends NARPart implements Consumer<NAR> {

    protected final AtomicBoolean busy = new AtomicBoolean(false);

    protected CycLoop(NAR nar) {
        super(nar);
    }

    @Override
    protected void starting(NAR nar) {
        whenOff(nar.onCycle(this));
    }

    @Override
    public void accept(NAR nar) {
        if (busy.compareAndSet(false, true)) {
            try {
                run(nar);
            } finally {
                busy.set(false);
            }
        }
    }

    abstract protected void run(NAR timeAware);

    public String toString(Object r) {
        return WhenCycle.class.getSimpleName() + "(" + r + ")";
    }
}
