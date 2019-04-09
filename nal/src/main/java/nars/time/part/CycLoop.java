package nars.time.part;

import jcog.event.Off;
import nars.$;
import nars.NAR;
import nars.control.NARPart;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.time.event.WhenCycle;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * per-cycle invoked part
 * NOT AFFILIATED WITH CYCORP
 */
abstract public class CycLoop extends NARPart implements Consumer<NAR> {

    protected final AtomicBoolean busy = new AtomicBoolean(false);

    private Off off = null;

    protected CycLoop(Term id) {
        super(id);
    }

    static final Atom CYC = Atomic.atom("cyc");

    public static CycLoop the(Consumer<NAR> each) {
        return new LambdaCycLoop(each);
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
            o.close();
            this.off = null;
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

    abstract protected void run(NAR n);

    public String toString(Object r) {
        return WhenCycle.class.getSimpleName() + "(" + r + ")";
    }

    private static final class LambdaCycLoop extends CycLoop {
        private final Consumer<NAR> each;

        public LambdaCycLoop(Consumer<NAR> each) {
            super($.p(CycLoop.CYC, $.the(each.hashCode()) /* $.identity(each) */));
            this.each = each;
        }

        @Override
        protected void run(NAR n) {
            each.accept(n);
        }
    }
}
