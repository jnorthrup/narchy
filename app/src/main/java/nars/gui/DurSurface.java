package nars.gui;

import nars.NAR;
import nars.control.DurService;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.widget.windo.Widget;

import java.util.function.Consumer;

/**
 * updates approximately once per duration.
 * automatically attaches update handler on start (ex: added to graph) and
 * removes on stop (ex: removal from graph)
 */
abstract public class DurSurface extends Widget {
    protected final NAR nar;
    DurService on;

    protected DurSurface(NAR nar) {
        super();
        this.nar = nar;
    }

    private DurSurface(Surface x, NAR nar) {
        super(x);
        this.nar = nar;
    }

    abstract protected void update();

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            assert(on == null);
            on = DurService.on(nar, this::update);
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            on.off();
            on = null;
            return true;
        }
        return false;
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
        };
    }

    public static DurSurface get(Surface narConsumer, NAR n) {
        return get(narConsumer, n, (Consumer<NAR>)narConsumer);
    }

}
