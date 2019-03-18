package nars.exe;

import jcog.exe.InstrumentedLoop;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.control.NARService;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 */
public class NARLoop extends InstrumentedLoop {

    static final Atom NAR_LOOP = $.the(NARLoop.class);

    private final NAR nar;

    public final FloatRange throttle = new FloatRange(1f, 0f, 1f);
    private final NARService service;

    /**
     * starts paused; thread is not automatically created
     */
    public NARLoop(@NotNull NAR n) {
        super();
        nar = n;
        this.service = new NARService($.inh(NAR_LOOP, n.self()), n) {

        };
        n.on(service);
    }

//    @Override
//    protected boolean async() {
//        return true;
//    }

    @Override
    public final boolean next() {

        nar.time.cycle(nar);

//        if (nar.exe.concurrent()) {
//            nar.eventCycle.emitAsync(nar, nar.exe, this::ready);
//        } else {
            nar.eventCycle.emit(nar);
//            ready();
//        }

        return true;
    }

    public NAR nar() {
        return nar;
    }

}
