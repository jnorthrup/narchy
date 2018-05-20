package spacegraph.space2d.widget;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.math.Longerval;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Clipped;
import spacegraph.space2d.container.ForceDirected2D;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.SliderModel;
import spacegraph.space2d.widget.windo.Widget;

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
        ForceDirected2D<E> l = new ForceDirected2D<>() {
            @Override
            protected void apply(NodeVis<E> n, RectFloat2D target) {

                float h = Timeline2D.this.h()/8; //TODO parameterizable func
                float y = Util.clamp(target.cy(), Timeline2D.this.top() + h/2, Timeline2D.this.bottom()-h/2);
                long[] r = model.range(n.id);
                float x1 = x(r[0]);
                float x2 = x(r[1]+1);
                n.pos(x1, y - h/2, x2, y + h/2);
            }
        };
        l.repelSpeed.set(0.25f);
        layout(l);
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
                viewShift(((p - 0.5f) * 2) * (tEnd-tStart)*0.1f);
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


}
