package nars.gui;

import nars.NAR;
import nars.time.part.DurLoop;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.UnitContainer;
import spacegraph.space2d.widget.meter.BitmapMatrixView;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * updates approximately once per duration.
 * automatically attaches update handler on start (ex: added to graph) and
 * removes on stop (ex: removal from graph)
 */
public abstract class DurSurface<S extends Surface> extends UnitContainer<S> {

    public static final double minUpdateTimeSeconds = 1.0 / 30.0; /* 30fps */

    protected final NAR nar;
    final DurLoop dur;
    final long minUpdateTimeNS;
    private boolean autolayout;
    volatile long nextUpdate = System.nanoTime();

    @Deprecated protected DurSurface(S x, NAR nar) {
        this(x, nar, minUpdateTimeSeconds);
    }
    protected DurSurface(S x, NAR nar, double minUpdateTimeS) {
        super(x);
        this.nar = nar;
        this.minUpdateTimeNS = Math.round(minUpdateTimeS*1.0e9);
        dur = nar.onDur(this::updateIfShowing, false);
        nextUpdate = System.nanoTime();
    }

    @Override
    protected void starting() {

        nar.add(dur);

        super.starting();
    }

    @Override
    protected void stopping() {

        nar.stop(dur);

        super.stopping();
    }

    /** sets the update period dur multiplier */
    public DurSurface durs(float durs) {
        dur.durs(durs);
        return this;
    }

    public static DurSurface get(Surface x, NAR n, Runnable eachDur) {
        return get(x, n, (nn)->eachDur.run());
    }
    public static DurSurface get(BitmapMatrixView x, NAR n) {
        return get(x, n, x::updateIfShowing);
    }


    final AtomicBoolean busy = new AtomicBoolean();

    @Override
    protected void renderContent(ReSurface r) {

//            S x = the();
//            if (x instanceof ContainerSurface && (((ContainerSurface) x).layoutPending())) {
//                invalidate();
//            }

            if (busy.compareAndSet(false,true)) {
                long start = System.nanoTime();
                try {
                    if (nextUpdate >= start) {
                        update();
                        nextUpdate = start + minUpdateTimeNS; //TODO throttle duration to match expected update speed if significantly different
                    }
                } finally {
                    busy.lazySet(false);
                }
            }


        super.renderContent(r);
    }

    protected final void updateIfShowing() {
        if (showing())
            update();
    }

    protected abstract void update();

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
    public static  <S extends Surface> DurSurface<S> get(S narConsumer, NAR n) {
        return get(narConsumer, n, (Consumer<NAR>)narConsumer);
    }

    public DurSurface<S> every(float durs) {
        this.durs(durs);
        return every();
    }

    public DurSurface<S> every() {
        //if caching, during pre-render step if not invalid, then only call .layout()
        autolayout = true;
        return this;
    }

    @Override
    protected boolean canRender(ReSurface r) {
        if (super.canRender(r)) {
            if (autolayout)
                layout();
            return true;
        }
        return false;
    }
}
