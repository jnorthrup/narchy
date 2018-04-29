package spacegraph.space2d.widget;

import com.google.common.collect.Iterables;
import jcog.math.Longerval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.MutableMapContainer;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

public class Timeline2D<E> extends MutableMapContainer<E, E> {

    /** viewable range */
    double tStart = 0, tEnd = 1;

    final TimelineModel<E> model;

    final Function<E, Surface> view;

    public Timeline2D(TimelineModel<E> model, Function<E, Surface> view) {
        this.model = model;
        this.view = view;
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
        if (t <= s) return X;
        float W = w();
        if (t >= e) return W + X;
        return (float)(((t - s)) / (e - s) * W + X);
    }

    public synchronized Timeline2D<E> update() {
        clear();
        model.events((long)Math.floor(tStart), (long)Math.ceil(tEnd-1)).forEach(e->{
           put(e, e, this::viewFunc);
        });
        return this;
    }

    @Override
    protected void doLayout(int dtMS) {
        final int[] lane = {0};
        int lanes = cache.size();
        forEachKeySurface((e,s)->{
            long[] r = model.range(e);
            float x1 = x(r[0]);
            float x2 = x(r[1]+1);
            float yy[] = y(e, lane[0]++, lanes);
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
