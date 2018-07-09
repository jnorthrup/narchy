package spacegraph.space2d.widget;

import jcog.Util;
import jcog.list.FasterList;
import jcog.math.Longerval;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Clipped;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.windo.Widget;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Timeline2D<E> extends Graph2D<E> {

    /** viewable range */
    protected double tStart = 0;
    protected double tEnd = 1;

    private final TimelineModel<E> model;


    public Timeline2D(TimelineModel<E> model, Consumer<NodeVis<E>> view) {
        super();
        this.model = model;
        nodeBuilder(view);

        























































        layout(new DefaultTimelineLayout());
    }

    public Surface withControls() {
        return new Widget(new Splitting(new Clipped(this), controls(), 0.2f));
    }

    public Bordering controls() {
        Bordering b = new Bordering();

        FloatSlider whenSlider = new FloatSlider(0.5f, 0, 1) {
            @Override public boolean prePaint(SurfaceRender r) {
                float v = ((float) this.value() - 0.5f) * 2;
                if (Math.abs(v) > 0.05f)
                    viewShift(v * (tEnd - tStart) * 0.1f);
                slider.value(Util.lerp(0.75f, slider.value(), 0.5f));
                return super.prePaint(r);
            }
            @Override
            public String text() {
                return "";
            }
        }.type(SliderModel.KnobHoriz);

        b.center(whenSlider);

        FloatSlider zoomSlider = new FloatSlider(0.5f, 0.48f, 0.52f) {
            @Override public boolean prePaint(SurfaceRender r) {
                float v = (float) this.value();
                viewScale((v+0.5f));
                slider.value(Util.lerp(0.75f, v, 0.5f));
                return super.prePaint(r);
            }

            @Override
            public String text() {
                return "";
            }
        }.type(SliderModel.KnobVert);
        b.edge(Bordering.E, 0.5f).east(zoomSlider);

        return b;
    }

    private Timeline2D<E> viewShift(double dt) {
        return view(tStart+dt, tEnd+dt);
    }
    private Timeline2D<E> viewScale(double dPct) {
        double range = (tEnd - tStart) * dPct;
        double tCenter = (tEnd + tStart)/2;
        return view(tCenter - range/2, tCenter + range/2);
    }

    /** keeps current range */
    public Timeline2D<E> view(double when) {
        double range = tEnd - tStart;
        assert(range>0);
        return view(when-range/2, when+range/2);
    }

    public Timeline2D<E> view(double start, double end) {
        assert(start < end);
        tStart = start;
        tEnd = end;
        return update();
    }

    private float x(double t) {
        double s = tStart;
        double e = tEnd;
        float X = x();
        float W = w();
        return (float)((t - s) / (e - s) * W + X);
    }

    private Timeline2D<E> update() {
        set(model.events((long)Math.floor(tStart), (long)Math.ceil(tEnd-1)));
        return this;
    }




























    interface TimelineModel<X> {
        /** any events intersecting with the provided range */
        Iterable<X> events(long start, long end);
        long[] range(X event);
        @Nullable X first();
        @Nullable X last();

        default boolean intersects(X x, long start, long end) {
            long[] r = range(x);
            return Longerval.intersectLength(r[0], r[1], start, end)>=0;
        }

        default int compareStart(X x, X y) {
            long rx = range(x)[0];
            long ry = range(y)[0];
            return Long.compare(rx,ry);
        }

        default int compareDur(X x, X y) {
            long[] rx = range(x);
            long[] ry = range(y);
            return compareDur(rx, ry);
        }

        static int compareDur(long[] rx, long[] ry) {
            return Long.compare(rx[1]-rx[0],ry[1]-ry[0]);
        }

        default long intersectLength(X x, X y) {
            long[] rx = range(x);
            long[] ry = range(y);
            return Longerval.intersectLength(rx[0], rx[1], ry[0], ry[1]);
        }

        default int compareDurThenStart(X x, X y) {
            if (x == y) return 0;
            long[] rx = range(x);
            long[] ry = range(y);
            int wc = -compareDur(rx, ry);
            if (wc!=0)
                return wc;
            int xc = Long.compare(rx[0], ry[0]);
            if (xc!=0)
                return xc;
            return ((Comparable)x).compareTo(y); 
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
            return name + "[" + start + ((start!=end) ? (end + "]") : "]");
        }

        @Override
        public int compareTo(Timeline2D.SimpleEvent x) {
            if (this == x) return 0;
            int s = Long.compare(start, x.start);
            if (s!=0)
                return s;
            int e = Long.compare(end, x.end);
            if (e!=0)
                return e;
            return Integer.compare(System.identityHashCode(this), System.identityHashCode(x));
        }

        public double range() {
            return end-start;
        }
    }

    public static class SimpleTimelineModel extends ConcurrentSkipListSet<SimpleEvent> implements TimelineModel<SimpleEvent> {

        @Override
        public Iterable<SimpleEvent> events(long start, long end) {
            
            return this.stream().filter(x -> intersects(x, start, end)).collect(Collectors.toList());
        }

        @Override
        public boolean intersects(SimpleEvent simpleEvent, long start, long end) {
            return Longerval.intersectLength(simpleEvent.start, simpleEvent.end, start, end) >= 0;
        }

        @Override
        public long[] range(SimpleEvent event) {
            return new long[] { event.start, event.end };
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
            return Longerval.intersectLength(simpleEvent.start, simpleEvent.end, start, end) >= 0;
        }

        @Override
        public long[] range(SimpleEvent event) {
            return new long[] { event.start, event.end };
        }
    }


    private class DefaultTimelineLayout implements Graph2DLayout<E> {



        @Override
        public void layout(Graph2D<E> g, int dtMS) {
            FasterList<NodeVis<E>> next = new FasterList<>();

            g.forEachValue(next::add);
            next.sortThis((x,y)->model.compareDurThenStart(x.id, y.id));
            if (next.isEmpty())
                return;




            List<RoaringBitmap> lanes = new FasterList();
            RoaringBitmap l0 = new RoaringBitmap();
            l0.add(0); 
            lanes.add(l0);

            for (int i = 1, byDurationSize = next.size(); i < byDurationSize; i++) {
                NodeVis<E> in = next.get(i);
                
                int lane = -1;
                nextLane: for (int l = 0, lanesSize = lanes.size(); l < lanesSize; l++) {
                    RoaringBitmap r = lanes.get(l);
                    PeekableIntIterator rr = r.getIntIterator();
                    boolean collision = false;
                    while (rr.hasNext()) {
                        int j = rr.next();
                        if (model.intersectLength(next.get(j).id,in.id)>0) {
                            collision = true;
                            break ; 
                        }
                    }
                    if (!collision) {
                        lane = l;
                        r.add(i);
                        break;
                    }
                }
                if (lane == -1) {
                    RoaringBitmap newLane = new RoaringBitmap();
                    newLane.add(i);
                    lanes.add(newLane);
                }
            }

            int nlanes = lanes.size();
            float laneHeight = g.h()/nlanes;
            float Y = g.y();
            for (int i = 0; i < nlanes; i++) {
                RoaringBitmap ri = lanes.get(i);
                PeekableIntIterator ii = ri.getIntIterator();
                while (ii.hasNext()) {
                    int j =  ii.next();
                    NodeVis<E> jj = next.get(j);
                    long[] w = model.range(jj.id);
                    jj.pos(x(w[0]), Y+laneHeight*i, x(w[1]), Y+laneHeight*(i+1));
                }
            }
        }
    }
}
