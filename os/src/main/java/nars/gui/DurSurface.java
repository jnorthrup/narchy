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
abstract public class DurSurface<S extends Surface> extends UnitContainer<S> {

    public static final double minUpdateTimeSeconds = 1 / 30.0; /* 30fps */

    protected final NAR nar;
    DurLoop dur;
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
        nextUpdate = System.nanoTime();
    }

    @Override
    protected void starting() {
        if (dur ==null)
            dur = nar.onDur(this::updateIfShowing);
        else
            nar.start(dur);

        super.starting();
    }

    @Override
    protected void stopping() {
        if (dur !=null) {
            dur.delete();
            dur = null;
        }

        super.stopping();
    }

    /** sets the update period dur multiplier */
    public DurSurface durs(float durs) {
        throw new jcog.TODO();
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

    abstract protected void update();

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

    public DurSurface<S> live() {
        //if caching, during pre-render step if not invalid, then only call .layout()
        autolayout = true;
        return this;
    }

    @Override
    protected boolean preRender(ReSurface r) {
        if (super.preRender(r)) {
            if (autolayout)
                layout();
            return true;
        }
        return false;
    }
}
