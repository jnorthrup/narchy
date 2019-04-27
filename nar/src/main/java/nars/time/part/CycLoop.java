package nars.time.part;

import jcog.event.Off;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.control.NARPart;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * per-cycle invoked part
 * NOT AFFILIATED WITH CYCORP
 */
abstract public class CycLoop extends NARPart implements Consumer<NAL<NAL<NAR>>> {

    private final AtomicBoolean busy = new AtomicBoolean(false);

    private Off off = null;

    CycLoop(Term id) {
        super(id);
    }

    private static final Atom onCycle = Atomic.atom("onCycle");

    public static CycLoop the(Consumer<NAL<NAL<NAR>>> each) {
        return new LambdaCycLoop(each);
    }

    protected CycLoop() {

    }

    @Override
    protected void starting(NAR nar) {
        assert(off == null);
        off = nar.onCycle(this);
    }

    @Override
    protected void stopping(NAR nar) {
        @Nullable Off o = off;
        if (o!=null) {
            this.off = null;
            o.close();
        }
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

    abstract protected void run(NAL<NAL<NAR>> n);


    private static final class LambdaCycLoop extends CycLoop {
        private final Consumer<NAL<NAL<NAR>>> each;

        LambdaCycLoop(Consumer<NAL<NAL<NAR>>> each) {
            super($.func(CycLoop.onCycle, $.identity(each)));
            this.each = each;
        }

        @Override
        protected void run(NAL<NAL<NAR>> n) {
            each.accept(n);
        }
    }
}
