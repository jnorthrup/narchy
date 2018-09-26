package nars.gui;

import nars.NAR;
import nars.control.DurService;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.UnitContainer;

import java.util.function.Consumer;

/**
 * updates approximately once per duration.
 * automatically attaches update handler on start (ex: added to graph) and
 * removes on stop (ex: removal from graph)
 */
abstract public class DurSurface<S extends Surface> extends UnitContainer<S> {
    protected final NAR nar;
    DurService on;

    protected DurSurface(S x, NAR nar) {
        super(x);
        this.nar = nar;
    }

    abstract protected void update();

    protected final void updateIfShowing() {
        if (showing()) {
            update();
        }
    }


    public DurSurface durs(float durs) {
        on.durs(durs);
        return this;
    }

    @Override
    protected void starting() {
        super.starting();

        assert(on == null);
        on = DurService.on(nar, this::updateIfShowing);
    }

    @Override
    protected void stopping() {
        on.off();
        on = null;
        super.stopping();
    }

    public static DurSurface get(Surface x, NAR n, Runnable eachDur) {
        return get(x, n, (nn)->eachDur.run());
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
