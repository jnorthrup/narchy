package nars.gui;

import nars.NAR;
import nars.control.DurService;
import nars.util.TimeAware;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
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

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {

            starting();

            assert(on == null);
            on = DurService.on(nar, this::updateIfShowing);

            return true;
        }
        return false;
    }

    protected void starting() {

    }
    protected void stopping() {

    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            stopping();
            on.off();
            on = null;
            return true;
        }
        return false;
    }

    public static DurSurface get(Surface x, NAR n, Runnable eachDur) {
        return get(x, n, (nn)->eachDur.run());
    }

    public static DurSurface get(Surface x, NAR n, Consumer<TimeAware> eachDur) {
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

    public static DurSurface get(Surface narConsumer, NAR n) {
        return get(narConsumer, n, (Consumer<TimeAware>)narConsumer);
    }

}
