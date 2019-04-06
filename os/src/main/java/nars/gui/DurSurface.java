package nars.gui;

import jcog.event.Off;
import nars.NAR;
import nars.time.part.DurLoop;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.meta.AbstractCachedSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;

import java.util.function.Consumer;

/**
 * updates approximately once per duration.
 * automatically attaches update handler on start (ex: added to graph) and
 * removes on stop (ex: removal from graph)
 */
abstract public class DurSurface<S extends Surface> extends AbstractCachedSurface<S> {

    public static final double minUpdateTimeSeconds = 1 / 30.0; /* 30fps */

    protected final NAR nar;
    DurLoop on;
    final long minUpdateTimeNS;
    private boolean autolayout;

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
        return on = nar.onDur(this::updateIfShowing);
    }

    /** sets the update period dur multiplier */
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

    long lastUpdate = System.nanoTime();

    @Override
    protected void renderContent(ReSurface r) {
        long now = System.nanoTime();
        if (lastUpdate < now - minUpdateTimeNS) {
            lastUpdate = now; //TODO throttle duration to match expected update speed if significantly different
            update();
        }

        super.renderContent(r);
    }

    public static DurSurface get(Surface x, NAR n, Consumer<NAR> eachDur) {
        return new DurSurface(x, n) {
            @Override
            protected void update() {
                eachDur.accept(n);
            }

            @Override
            public String toString() {
                return "DurSurface[" + x + ',' + eachDur + ']';
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

    public DurSurface live() {
        //if caching, during pre-render step if not invalid, then only call .layout()
        autolayout = true;
        return this;
    }

    @Override
    protected boolean preRender(ReSurface r) {
        if (super.preRender(r)) {
            if (autolayout && cache)
                layout();
            return true;
        }
        return false;
    }
}
