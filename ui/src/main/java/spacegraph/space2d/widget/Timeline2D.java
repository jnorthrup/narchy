package spacegraph.space2d.widget;

import com.google.common.collect.Iterables;
import com.jogamp.opengl.GL2;
import jcog.math.Longerval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.MutableMapContainer;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

public class Timeline2D<E> extends MutableMapContainer<E, E> {

    /** viewable range */
    double tStart = 0, tEnd = 1;

    final TimelineModel<E> model;

    final Function<E,Surface> view;

    public Timeline2D(TimelineModel<E> model, Function<E, Surface> view) {
        this.model = model;
        this.view = view;
    }

    public Timeline2D view(double start, double end) {
        assert(start < end);
        tStart = start;
        tEnd = end;
        update();
        return this;
    }

    public float x(long t) {
        double s = tStart;
        double e = tEnd;
        float X = x();
        if (t <= s) return X;
        float W = w();
        if (t >= e) return W + X;
        return (float)(((t - s)) / (e - s) * W + X);
    }

    public synchronized void update() {
        clear();
        model.events((long)Math.floor(tStart), (long)Math.ceil(tEnd-1)).forEach(e->{
           put(e, e, this::viewFunc);
        });
    }

    @Override
    protected void doLayout(int dtMS) {
        forEachKeySurface((e,s)->{
            long[] r = model.range(e);
            float x1 = x(r[0]);
            float x2 = x(r[1]+1);
            float yy[] = y(e);
            float h = h();
            float y = y();
            s.pos(x1, y + yy[0] * h, x2, y + yy[1] * h);
        });
    }

    protected Surface viewFunc(E e, E ee) {
        Surface s = view.apply(e);
        return s;
    }

    /** range of values between 0..1 */
    float[] y(E event) {
        //TODO layout
        return new float[] { 0.25f, 0.75f };
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

    static class SimpleEvent implements Comparable<SimpleEvent> {
        public final String name;
        public final long start, end;

        SimpleEvent(String name, long start, long end) {
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

    static class SimpleTimelineModel extends ConcurrentSkipListSet<SimpleEvent> implements TimelineModel<SimpleEvent> {

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

    public static void main(String[] args) {

        SimpleTimelineModel dummyModel = new SimpleTimelineModel();
        dummyModel.add(new SimpleEvent("x", 0, 1));
        dummyModel.add(new SimpleEvent("y", 1, 3));
        dummyModel.add(new SimpleEvent("z", 2, 5));
        dummyModel.add(new SimpleEvent("w", 3, 3)); //point

        SpaceGraph.window(new Timeline2D<>(dummyModel, e->new PushButton(e.name)){
            @Override
            protected void paintBelow(GL2 gl) {
                gl.glColor3f(0, 0, 0.25f);
                Draw.rect(gl, bounds);
            }
        }.view(1, 4), 800, 600);
    }
}
