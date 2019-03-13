package nars.gui;

import jcog.event.Off;
import nars.NAR;
import nars.time.event.DurService;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.meta.AbstractTriggeredSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;

import java.util.function.Consumer;

/**
 * updates approximately once per duration.
 * automatically attaches update handler on start (ex: added to graph) and
 * removes on stop (ex: removal from graph)
 */
abstract public class DurSurface<S extends Surface> extends AbstractTriggeredSurface<S> {
    protected final NAR nar;
    DurService on;

    protected DurSurface(S x, NAR nar) {
        super(x);
        this.nar = nar;
    }

    @Override
    public Off on() {
        return on = DurService.on(nar, this::updateIfShowing);
    }

    public DurSurface durs(float durs) {
        on.durs(durs);
        return this;
    }

    public DurService service() { return on; }

    public static DurSurface get(Surface x, NAR n, Runnable eachDur) {
        return get(x, n, (nn)->eachDur.run());
    }
    public static DurSurface get(BitmapMatrixView x, NAR n) {
        return get(x, n, x::updateIfShowing);
    }

    public static DurSurface get(Surface x, NAR n, Consumer<NAR> eachDur) {
        return new DurSurface(x, n) {
            @Override
            protected void update() {
                eachDur.accept(n);
            }

            @Override
            public String toString() {
                return "DurSurface[" + x + "," + eachDur + "]";
            }
        };
    }
    public static DurSurface get(Surface x, NAR n, Consumer<NAR> start, Consumer<NAR> eachDur, Consumer<NAR> stop) {
        return new DurSurface(x, n) {
            @Override
            protected void update() {
                eachDur.accept(n);
            }

            @Override
            protected void starting() {
                super.starting();
                start.accept(nar);
            }

            @Override
            protected void stopping() {
                stop.accept(nar);
                super.stopping();
            }

            @Override
            public String toString() {
                return "DurSurface[" + x + "," + eachDur + "]";
            }
        };
    }
    public static DurSurface get(Surface narConsumer, NAR n) {
        return get(narConsumer, n, (Consumer<NAR>)narConsumer);
    }

}
