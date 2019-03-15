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

    public static final double minUpdateTimeSeconds = 1 / 30.0; /* 30fps */

    protected final NAR nar;
    DurService on;
    final long minUpdateTimeNS;

    @Deprecated protected DurSurface(S x, NAR nar) {
        this(x, nar, minUpdateTimeSeconds);
    }
    protected DurSurface(S x, NAR nar, double minUpdateTimeS) {
        super(x);
        this.nar = nar;
        this.minUpdateTimeNS = Math.round(minUpdateTimeS*1.0e9);
    }

    @Override
    public Off on() {
        return on = DurService.on(nar, this::updateIfShowing);
    }

    public DurSurface durs(float durs) {
        on.durs(durs);
        return this;
    }

    public static DurSurface get(Surface x, NAR n, Runnable eachDur) {
        return get(x, n, (nn)->eachDur.run());
    }
    public static DurSurface get(BitmapMatrixView x, NAR n) {
        return get(x, n, x::updateIfShowing);
    }

    long lastUpdate = Long.MIN_VALUE;
    public boolean showing() {
        if (super.showing()) {
            long now = System.nanoTime();
            if (lastUpdate < now - minUpdateTimeNS) {
                lastUpdate = now; //TODO throttle duration to match expected update speed if significantly different
                update();
            }
            return true;
        }
        return false;
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
//    public static DurSurface get(Surface x, NAR n, Consumer<NAR> start, Consumer<NAR> eachDur, Consumer<NAR> stop) {
//        return new DurSurface(x, n) {
//            @Override
//            protected void update() {
//                eachDur.accept(n);
//            }
//
//            @Override
//            protected void starting() {
//                super.starting();
//                start.accept(nar);
//            }
//
//            @Override
//            protected void stopping() {
//                stop.accept(nar);
//                super.stopping();
//            }
//
//            @Override
//            public String toString() {
//                return "DurSurface[" + x + "," + eachDur + "]";
//            }
//        };
//    }
    public static DurSurface get(Surface narConsumer, NAR n) {
        return get(narConsumer, n, (Consumer<NAR>)narConsumer);
    }

}
