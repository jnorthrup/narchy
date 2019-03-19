package spacegraph.space2d.container.time;

import jcog.Util;
import jcog.math.Longerval;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.unit.Clipped;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Timeline2D extends Stacking {

    /**
     * viewable range
     */
    public double start;
    public double end;
    private double startNext;
    private double endNext;

    public <X> Timeline2D addEvents(TimelineModel<X> e, Consumer<Graph2D.NodeVis<X>> r) {
        add(new Timeline2DEvents<>(e, r));
        return this;
    }


    public interface TimelineRenderable {
        void setTime(double tStart, double tEnd);
    }

    @Deprecated
    public Timeline2D(double start, double end) {
        this.setTime(start, end);
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
            double width = endNext - startNext;
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
        return new Splitting(new Clipped(this), controls(), 0.07f);
    }

    public Bordering controls() {
        Bordering b = new Bordering();

        FloatSlider whenSlider = new FloatSlider(0.5f, 0, 1) {
            @Override
            public boolean prePaint(SurfaceRender r) {
                float v = this.get();
                float d = (v - 0.5f) * 2;
                if (Math.abs(d) > 0.05f)
                    timeShift(d * (end - start) * 0.1);

                set(Util.lerp(0.6f, v, 0.5f));

                return super.prePaint(r);
            }

            @Override
            public String text() {
                return "";
            }
        }.type(SliderModel.KnobHoriz);

        b.center(whenSlider);

        FloatSlider zoomSlider = new FloatSlider(0.5f, 0.48f, 0.52f) {
            @Override
            public boolean prePaint(SurfaceRender r) {
                float v = this.get();
                timeScale((v + 0.5f));
                set(Util.lerp(0.75f, v, 0.5f));
                return super.prePaint(r);
            }

            @Override
            public String text() {
                return "";
            }
        }.type(SliderModel.KnobVert);
        b.borderSize(Bordering.E, 0.25f).east(zoomSlider);

        return b;
    }

    public float x(float sample) {
        double f = start;
        return (float) ((sample - f) / (end - f) * w());
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
    public Timeline2D setTime(double when) {
        synchronized (this) {
            double range = endNext - startNext;
            assert (range > 0);
            return setTime(when - range / 2, when + range / 2);
        }
    }

    @Override
    protected void compileChildren(SurfaceRender r) {
        _setTime(startNext, endNext);

        super.compileChildren(r);
    }

    public Timeline2D setTime(double start, double end) {
        synchronized (this) {
            this.startNext = start;
            this.endNext = end;
        }
        return this;
    }

    private void _setTime(double start, double end) {
        synchronized (this) {
            if (!Util.equals(this.start, start) || !Util.equals(this.end, end)) {
                this.start = start;
                this.end = end;
            } else
                return;
        }

        forEach(x -> setLayerTime(x, start, end));
    }

    private void setLayerTime(Surface x, double s, double e) {
        if (x instanceof TimelineRenderable)
            ((TimelineRenderable) x).setTime(s, e);
    }

    /**
     * proportionizes time to an axis of given length
     */
    public static float x(double s, double e, float X, float W, double t) {
        return (float) ((t - s) / (e - s) * W + X);
    }


    public interface TimelineModel<X> {
        /**
         * any events intersecting with the provided range
         */
        Iterable<X> events(long start, long end);

        long[] range(X event);
//        @Nullable X first();
//        @Nullable X last();

        default boolean intersects(X x, long start, long end) {
            long[] r = range(x);
            return Longerval.intersects(r[0], r[1], start, end);
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
            return Longerval.intersectLength(rx[0], rx[1], ry[0], ry[1]);
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
        public final String name;
        final long start;
        public final long end;

        public SimpleEvent(String name, long start, long end) {
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
            int s = Long.compare(start, x.start);
            if (s != 0)
                return s;
            int e = Long.compare(end, x.end);
            if (e != 0)
                return e;
            return Integer.compare(System.identityHashCode(this), System.identityHashCode(x));
        }

        public double range() {
            return end - start;
        }
    }

    public static class SimpleTimelineModel extends ConcurrentSkipListSet<SimpleEvent> implements TimelineModel<SimpleEvent> {

        @Override
        public Iterable<SimpleEvent> events(long start, long end) {

            return this.stream().filter(x -> intersects(x, start, end)).collect(Collectors.toList());
        }

        @Override
        public boolean intersects(SimpleEvent simpleEvent, long start, long end) {
            return Longerval.intersects(simpleEvent.start, simpleEvent.end, start, end);
        }

        @Override
        public long[] range(SimpleEvent event) {
            return new long[]{event.start, event.end};
        }
    }

    public static class FixedSizeTimelineModel extends ConcurrentSkipListSet<SimpleEvent> implements TimelineModel<SimpleEvent> {

        private final int cap;

        public FixedSizeTimelineModel(int cap) {
            this.cap = cap;
        }

        @Override
        public boolean add(SimpleEvent simpleEvent) {
            if (super.add(simpleEvent)) {
                while (size() > cap) {
                    pollLast();
                }
                return true;
            }
            return false;
        }

        @Override
        public Iterable<SimpleEvent> events(long start, long end) {

            return this.stream().filter(x -> intersects(x, start, end)).collect(Collectors.toList());
        }

        @Override
        public boolean intersects(SimpleEvent simpleEvent, long start, long end) {
            return Longerval.intersects(simpleEvent.start, simpleEvent.end, start, end);
        }

        @Override
        public long[] range(SimpleEvent event) {
            return new long[]{event.start, event.end};
        }
    }


}
