package spacegraph.space2d.container.time;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.map.CompactArrayMap;
import jcog.math.LongInterval;
import jcog.tree.rtree.Spatialization;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.unit.Clipped;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.video.Draw;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** view for one or more TimeRangeAware implementing surfaces that display aspects of a time-varying signal */
public class Timeline2D extends Stacking implements Finger.ScrollWheelConsumer {

    /**
     * viewable range
     */
    public long start, end;
    public long startNext, endNext;


    public interface TimeRangeAware {
        void setTime(long tStart, long tEnd);
    }

    public Timeline2D() {
        this(0,1);
    }

    public Timeline2D(long start, long end) {
        this.setTime(this.start = start, this.end = end);
    }


    @Override
    protected void _add(Surface x) {
        super._add(x);
        setLayerTime(x, start, end);
    }

    /** TODO move to Timeline2D */
    public void timeShiftPct(float pct) {
        if (Util.equals(pct, 0))
            return;


        synchronized (this) {
            long width = endNext - startNext;
//        int N = buffer.capacity();
//        if (width < N) {
            double mid = ((startNext + endNext) / 2);
            double nextMid = mid + (pct * width);

            double first = nextMid - width / 2;
            double last = nextMid + width / 2;
//            if (first < 0) {
//                first = 0;
//                last = first + width;
//            } else if (last > N) {
//                last = N;
//                first = last -width;
//            }

            setTime(first, last);
        }
//        }

    }

    /** TODO move to Timeline2D */
    public void scale(float pct) {
        if (Util.equals(pct, 1))
            return;

        synchronized (this) {
            double first = this.startNext, last = this.endNext;
            double width = last - first;
            double mid = (last + first) / 2;
            double viewNext = width * pct;

            first = mid - viewNext / 2;
            last = mid + viewNext / 2;
            if (last > 1) {
                last = 1;
                first = last - viewNext;
            }
            if (first < 0) {
                first = 0;
                last = first + viewNext;
            }

            setTime(first, last);
        }
    }
    public Surface withControls() {
        return new Splitting(new Clipped(this), 0.07f, controls());
    }

    public Bordering controls() {
        Bordering b = new Bordering();

        float sticking = 0; //0.05f;
        double tEpsilon = Spatialization.EPSILON;
        double speed = 0.1;

        FloatSlider whenSlider = new FloatSlider(0.5f, 0, 1) {


            @Override
            public boolean canRender(ReSurface r) {
                float v = this.get();
                float d = (v - 0.5f) * 2;
                double delta = d * (end - start) * speed;

                if (Math.abs(d) > tEpsilon) {
                    timeShift(delta);
                    set(Util.lerp(0.5f + sticking/2, v, 0.5f));
                }

                return super.canRender(r);
            }

            @Override
            public String text() {
                return "";
            }
        }.type(SliderModel.KnobHoriz);

        b.center(whenSlider);

        FloatSlider zoomSlider = new FloatSlider(0.5f, 0.48f, 0.52f) {
            @Override
            public boolean canRender(ReSurface r) {
                float v = this.get();
                timeScale((v + 0.5f));
                set(Util.lerp(0.5f + sticking/2, v, 0.5f));

                return super.canRender(r);
            }

            @Override
            public String text() {
                return "";
            }
        }.type(SliderModel.KnobVert);
        b.borderSize(Bordering.E, 0.2f).east(zoomSlider);

        return b;
    }

    public float x(long sample) {
        return (float) ((sample - start) / ((end - start) * (double)w()));
    }

    public Timeline2D timeShift(double dt) {
        if (Util.equals(dt, 0))
            return this;

        synchronized (this) {
            return setTime(startNext + dt, endNext + dt);
        }
    }

    public Timeline2D timeScale(double dPct) {
        if (Util.equals(dPct, 1))
            return this;

        synchronized (this) {
            double range = (endNext - startNext) * dPct;
            double tCenter = (endNext + startNext) / 2;
            return setTime(tCenter - range / 2, tCenter + range / 2);
        }

    }

    /**
     * keeps current range
     */
    public Timeline2D setTime(long when) {
        synchronized (this) {
            double range = endNext - startNext;
            assert (range > 0);
            return setTime(when - range / 2, when + range / 2);
        }
    }

    @Override
    protected void renderContent(ReSurface r) {
        _setTime(startNext, endNext);

        super.renderContent(r);
    }

    public Timeline2D setTime(double start, double end) {
        return setTime(Math.round(start), Math.round(end));
    }

    public Timeline2D setTime(long start, long end) {
        synchronized (this) {
            this.startNext = start;
            this.endNext = end;
            layout();
        }
        return this;
    }

    private void _setTime(long start, long end) {
        synchronized (this) {
            if (this.start!=start || this.end!=end) {
                this.start = start;
                this.end = end;
                forEach(x -> setLayerTime(x, this.start, this.end));
            }
        }
    }

    private void setLayerTime(Surface x, long s, long e) {
        if (x instanceof TimeRangeAware)
            ((TimeRangeAware) x).setTime(s, e);
    }

    /**
     * projects time to a position axis of given length
     */
    public static float x(long when, float X, float W, long s, long e) {
        return (float) (((when - s) / (double)(e - s)) * W + X);
    }

    public <X> Timeline2D addEvents(EventBuffer<X> e, Consumer<NodeVis<X>> r, Graph2D.Graph2DUpdater<X> u) {
        add(new Timeline2DEvents<>(e, r, u));
        return this;
    }

    /** model for discrete events to be materialized on the timeline */
    public interface EventBuffer<X> {
        /**
         * any events intersecting with the provided range
         */
        Iterable<X> events(long start, long end);

        long[] range(X event);
//        @Nullable X first();
//        @Nullable X last();

        default boolean intersects(X x, long start, long end) {
            long[] r = range(x);
            return LongInterval.intersects(r[0], r[1], start, end);
        }

        default int compareStart(X x, X y) {
            long rx = range(x)[0];
            long ry = range(y)[0];
            return Long.compare(rx, ry);
        }

        default int compareDur(X x, X y) {
            long[] rx = range(x);
            long[] ry = range(y);
            return compareDur(rx, ry);
        }

        static int compareDur(long[] rx, long[] ry) {
            return Long.compare(rx[1] - rx[0], ry[1] - ry[0]);
        }

        default long intersectLength(X x, X y) {
            long[] rx = range(x);
            long[] ry = range(y);
            return LongInterval.intersectLength(rx[0], rx[1], ry[0], ry[1]);
        }

        default int compareDurThenStart(X x, X y) {
            if (x.equals(y)) return 0;

            long[] rx = range(x);
            long[] ry = range(y);
            int wc = -compareDur(rx, ry);
            if (wc != 0)
                return wc;
            int xc = Long.compare(rx[0], ry[0]);
            if (xc != 0)
                return xc;

            return x instanceof Comparable ? ((Comparable) x).compareTo(y) : Integer.compare(System.identityHashCode(x), System.identityHashCode(y));
        }

    }

    public static class SimpleEvent implements Comparable<SimpleEvent> {
        public final Object name;
        final long start;
        public final long end;

        public SimpleEvent(Object name, long start, long end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return name + "[" + start + ((start != end) ? (end + "]") : "]");
        }

        @Override
        public int compareTo(Timeline2D.SimpleEvent x) {
            if (this == x) return 0;
            int s = Long.compare((start+end)/2, (x.start+x.end)/2);
            if (s != 0)
                return s;
            return Integer.compare(System.identityHashCode(this), System.identityHashCode(x));
        }

        public long range() {
            return end - start;
        }
    }

    public static class SimpleEventBuffer extends ConcurrentSkipListSet<SimpleEvent> implements EventBuffer<SimpleEvent> {

        @Override
        public Iterable<SimpleEvent> events(long start, long end) {

            return this.stream().filter(x -> intersects(x, start, end))::iterator;
        }

        @Override
        public boolean intersects(SimpleEvent simpleEvent, long start, long end) {
            return LongInterval.intersects(simpleEvent.start, simpleEvent.end, start, end);
        }

        @Override
        public long[] range(SimpleEvent event) {
            return new long[]{event.start, event.end};
        }
    }

    public static class FixedSizeEventBuffer<E extends SimpleEvent> extends ConcurrentSkipListSet<E> implements EventBuffer<E> {

        private final int cap;

        public FixedSizeEventBuffer(int cap) {
            this.cap = cap;
        }

        @Override
        public boolean add(E simpleEvent) {
            if (super.add(simpleEvent)) {
                while (size() > cap) {
                    pollFirst();
                }
                return true;
            }
            return false;
        }

        @Override
        public Iterable<E> events(long start, long end) {

            return this.stream().filter(x -> intersects(x, start, end)).collect(Collectors.toList());
        }

        @Override
        public boolean intersects(E simpleEvent, long start, long end) {
            return LongInterval.intersects(simpleEvent.start, simpleEvent.end, start, end);
        }

        @Override
        public long[] range(E event) {
            return new long[]{event.start, event.end};
        }
    }


    public static class TimelineGrid extends Surface implements TimeRangeAware {

        int THICKNESS = 2;
        //int DIVISIONS = 10; //TODO

        long start, end;
        private BiConsumer<GL2, ReSurface> paintGrid;

        @Override
        public void setTime(long tStart, long tEnd) {
            this.start = tStart; this.end = tEnd;
            paintGrid = null; //invalidate
        }

        @Override
        protected void render(ReSurface r) {
            if (paintGrid == null) {
                double range = end-start;
                double interval = interval(range);
                double phase = start % interval;
                double iMax = (range / interval) + 0.5f;
                paintGrid = (gl,sr)->{
                    float H = h(), W = w(), LEFT = x(), BOTTOM = y();
                    gl.glColor4f(0.3f,0.3f,0.3f,0.9f);

                    gl.glLineWidth(THICKNESS);
                    long x = Math.round(start - phase);
                    for (int i = 0; i <= iMax; i++) {
                        float xx = Timeline2D.x(x, LEFT, W, start, end);
                        Draw.line(xx, BOTTOM, xx, BOTTOM + H, gl);
                        x += interval;
                    }
                };
            }
            r.on(paintGrid);
        }

        /** TODO refine */
        static double interval(double range) {
            double x = Math.pow(10.0, Math.floor(Math.log10(range)));
            if (range / (x / 2.0) >= 10)
                return x / 2.0;
            else if (range / (x / 5.0) >= 10)
                return x / 5.0;
            else
                return x / 10.0;
        }
    }

    public static class AnalyzedEvent<X,Y> extends SimpleEvent {

        final CompactArrayMap<X,Y> map = new CompactArrayMap();

        public AnalyzedEvent(Object x, long start, long end) {
            super(x, start, end);
        }

        public Y get(X key) {
            return map.get(key);
        }
        public void put(X key, Y value) {
            map.put(key, value);
        }
        public void forEach(BiConsumer<X,Y> each) {
            map.forEach(each);
        }


    }
}
