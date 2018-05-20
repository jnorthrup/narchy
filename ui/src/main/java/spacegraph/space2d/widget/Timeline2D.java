package spacegraph.space2d.widget;

import com.google.common.collect.Iterables;
import jcog.list.FasterList;
import jcog.math.Longerval;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.NotNull;
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

public class Timeline2D<E> extends Graph2D<E> {

    /** viewable range */
    double tStart = 0, tEnd = 1;

    final TimelineModel<E> model;


    public Timeline2D(TimelineModel<E> model, Consumer<NodeVis<E>> view) {
        super();
        this.model = model;
        nodeBuilder(view);

        //simple hack to prevent event nodes from overlapping.  it could be better
//        Graph2DLayout<E> l = new ForceDirected2D<E>() {
//            {
//                repelSpeed.set(0.25f);
//            }
//            @Override
//            protected void apply(NodeVis<E> n, RectFloat2D target) {
//
//                float h = Timeline2D.this.h()/8; //TODO parameterizable func
//                float y = Util.clamp(target.cy(), Timeline2D.this.top() + h/2, Timeline2D.this.bottom()-h/2);
//                long[] r = model.range(n.id);
//                float x1 = x(r[0]);
//                float x2 = x(r[1]+1);
//                n.pos(x1, y - h/2, x2, y + h/2);
//            }
//        };

//        Graph2DLayout<E> l = new Dyn2DLayout<>() {
//
//            @Override
//            public void initialize(Graph2D<E> g, NodeVis<E> n) {
//                long[] r = model.range(n.id);
//                float x1 = x(r[0]);
//                float x2 = x(r[1]+1);
//                float cy = (float) (g.y() + (Math.random() * g.h()));
//                float h= g.h()/8;
//                n.pos(x1, cy-h/2, x2, cy+h/2);
//            }
//
//            @Override
//            protected void apply(NodeVis<E> n, RectFloat2D target) {
//                float h = Timeline2D.this.h()/8;
//
//                //System.out.println(n + " " + target);
//
//                float y = Util.clamp(Util.lerp(0.75f,
//                        n.cy(),
//                        //target.cy()
//                        //Util.clamp(target.cy(), 0 + h/2, Timeline2D.this.h() - h/2)
//                        target.cy() ),
//
//                    //    Timeline2D.this.top(), Timeline2D.this.bottom())
//                    h/2, Timeline2D.this.h() - h/2)
//
//                        + 0//  (float)(Math.random()-0.5f)*h/10f
//                ;
//
//                long[] r = model.range(n.id);
//                float x1 = x(r[0]);
//                float x2 = x(r[1]+1);
//
//                n.pos(x1, Timeline2D.this.top() + y - h/2, x2, Timeline2D.this.top() + y + h/2);
//
//                //super.apply(n, target);
//            }
//        };
        layout(new DefaultTimelineLayout());
    }

    public Surface withControls() {
        return new Widget(new Splitting(new Clipped(this), controls(), 0.2f));
    }

    public Surface controls() {
        Bordering b = new Bordering();

        FloatSlider whenSlider = new FloatSlider(0.5f, 0, 1) {
            @Override
            public boolean prePaint(SurfaceRender r) {
                float p = (float) this.value();
                float v = (p - 0.5f) * 2;
                if (Math.abs(v) > 0.05f) {
                    viewShift(v * (tEnd - tStart) * 0.1f);
                }
                return super.prePaint(r);
            }
        }.type(SliderModel.KnobHoriz);

        b.center(whenSlider);

        FloatSlider zoomSlider = new FloatSlider(0.5f, 0, 1).type(SliderModel.KnobVert);
        b.edge(Bordering.E, 0.5f).east(zoomSlider);

        return b;
    }

    public Timeline2D<E> viewShift(double dt) {
        return view(tStart+dt, tEnd+dt);
    }

    public Timeline2D<E> view(double start, double end) {
        assert(start < end);
        tStart = start;
        tEnd = end;
        return update();
    }

    public float x(long t) {
        double s = tStart;
        double e = tEnd;
        float X = x();
        float W = w();
        return (float)(((t - s)) / (e - s) * W + X);
    }

    public synchronized Timeline2D<E> update() {
        set(model.events((long)Math.floor(tStart), (long)Math.ceil(tEnd-1)));
        return this;
    }

//    @Override
//    protected void doLayout(int dtMS) {
//        //final int[] lane = {0};
//        //int lanes = cellMap.cache.size();
//        float height = h()/8; //TODO
//        forEachKeySurface((e,s)->{
//            long[] r = model.range(e);
//            float x1 = x(r[0]);
//            float x2 = x(r[1]+1);
//            float y = s.cy();
//            float y1 = y -height/2;
//            float y2 = y +height/2;
//            //float yy[] = y(e, lane[0]++, lanes);
////            float h = h();
////            float y = y();
//            s.pos(x1, y1, x2, y2); //only affect X
//        });
//    }


    /** range of values between 0..1 */
    float[] y(E event, int lane, int lanes) {
        float thick = 1f/lanes;
        float margin = 0.02f * thick;
        return new float[] { lane * thick + margin, (lane+1)*thick - margin };
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
            return Long.compare(rx[1]-rx[0],ry[1]-ry[0]);
        }

        default long intersectLength(X x, X y) {
            long[] rx = range(x);
            long[] ry = range(y);
            return Longerval.intersectLength(rx[0], rx[1], ry[0], ry[1]);
        }

    }

    public static class SimpleEvent implements Comparable<SimpleEvent> {
        public final String name;
        public final long start, end;

        public SimpleEvent(String name, long start, long end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        @Override
        public int compareTo(@NotNull Timeline2D.SimpleEvent x) {
            if (this == x) return 0;
            int s = Long.compare(start, x.start);
            if (s!=0)
                return s;
            int e = Long.compare(end, x.end);
            if (e!=0)
                return e;
            return Integer.compare(System.identityHashCode(this), System.identityHashCode(x));
        }

    }

    public static class SimpleTimelineModel extends ConcurrentSkipListSet<SimpleEvent> implements TimelineModel<SimpleEvent> {

        @Override
        public Iterable<SimpleEvent> events(long start, long end) {
            //HACK TODO use NavigableMap iterators correctly
            return Iterables.filter(this, x-> intersects(x, start, end));
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
            FasterList<NodeVis<E>> byDuration = new FasterList<>();

            g.forEachValue(n -> {
                if (n.showing()) {
                    //set widths
                    byDuration.add(n);
                }
            });
            byDuration.sortThis((x,y)->{
                if (x == y) return 0;
                int wc = model.compareDur(x.id, y.id);
                if (wc!=0)
                    return wc;
                int xc = model.compareStart(x.id, y.id);
                if (xc!=0)
                    return xc;
                return ((Comparable)x.id).compareTo(y.id); //Integer.compare(x.hashCode(), y.hashCode());
            });
            if (byDuration.isEmpty())
                return;

            List<RoaringBitmap> lanes = new FasterList();
            RoaringBitmap l0 = new RoaringBitmap();
            l0.add(0); //largest event assigned to lane 0
            lanes.add(l0);

            for (int i = 1, byDurationSize = byDuration.size(); i < byDurationSize; i++) {
                NodeVis<E> in = byDuration.get(i);
                RectFloat2D ib = in.bounds;
                //find lowest lane that doesnt intersect
                int lane = -1;
                nextLane: for (int l = 0, lanesSize = lanes.size(); l < lanesSize; l++) {
                    RoaringBitmap r = lanes.get(l);
                    PeekableIntIterator rr = r.getIntIterator();
                    boolean collision = false;
                    while (rr.hasNext()) {
                        int j = rr.next();
                        if (model.intersectLength(byDuration.get(j).id,in.id)>0) {
                            collision = true;
                            break ; //next lane
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
                    NodeVis<E> jj = byDuration.get(j);
                    long[] w = model.range(jj.id);
                    jj.pos(x(w[0]), Y+laneHeight*i, x(w[1]), Y+laneHeight*(i+1));
                }
            }
        }
    }
}
