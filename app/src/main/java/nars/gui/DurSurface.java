package nars.gui;

import nars.NAR;
import nars.control.DurService;
import spacegraph.Scale;
import spacegraph.Surface;

import java.util.function.Consumer;

/**
 * updates approximately once per duration.
 * automatically attaches update handler on start (ex: added to graph) and
 * removes on stop (ex: removal from graph)
 */
abstract public class DurSurface extends Scale {
    final DurService on;

    public DurSurface(Surface x, NAR nar) {
        super(x, 1f);
        on = DurService.on(nar, this::update);
    }

    abstract protected void update();

    @Override
    public void stop() {
        super.stop();
        on.off();
    }

    public static DurSurface get(Surface x, NAR n, Consumer<NAR> eachDur) {
        return new DurSurface(x, n) {
            @Override
            protected void update() {
                eachDur.accept(n);
            }
        };
    }

    public static DurSurface get(Surface x, NAR n) {
        return new DurSurface(x, n) {
            @Override
            protected void update() {
                ((Consumer<NAR>) x).accept(n);
            }
        };
    }

}
