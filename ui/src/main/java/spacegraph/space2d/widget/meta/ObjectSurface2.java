package spacegraph.space2d.widget.meta;

import com.google.common.base.Joiner;
import jcog.TODO;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.reflect.CastGraph;
import jcog.sort.FloatRank;
import jcog.sort.RankedN;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.widget.port.FloatRangePort;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectSurface2 extends MutableUnitContainer {


    public static void main(String[] args) {
        Object x = List.of(new FloatRange(0.6f, 0.0f, 1f), new FasterList().with("x", "y"));
        SpaceGraph.window(new ObjectSurface2(x), 500, 500);
    }

    public static final CastGraph DefaultBuilder = new CastGraph();

    private static <X> void build(Class<? extends X> from, Function<? extends X,Surface> builder) {
        DefaultBuilder.addEdge(from, builder, Surface.class);
    }
    {
        build(List.class, new Function<List<Object>, Surface>() {
            @Override
            public Surface apply(List<Object> x) {
                List<Surface> list = new ArrayList<>();
                for (Object o : x) {
                    Surface build = ObjectSurface2.this.build(o);
                    list.add(build);
                }
                return new Gridding(list.toArray(new Surface[0]));
            }
        });
        build(String.class, VectorLabel::new);
        build(FloatRange.class, FloatRangePort::new);
        build(FloatRange.class, new Function<FloatRange, Surface>() {
            @Override
            public Surface apply(FloatRange x) {
                return new VectorLabel(x.toString());
            }
        });
        build(AnyOf.class, new Function<AnyOf, Surface>() {
            @Override
            public Surface apply(AnyOf x) {
                return new TextEdit(x.toString());
            }
        });
    }


    final CastGraph the;

    public ObjectSurface2(Object x) {
        this(x, DefaultBuilder);
    }

    public ObjectSurface2(Object x, CastGraph builder) {
        this.the = builder;
        set(x);
    }

    public void set(Object x) {
        Surface y = build(x);
        set(y);
    }

    protected Surface build(Object x) {
        List<Function<Object, Surface>> xy = the.applicable(x.getClass(), Surface.class);
        Surface y;
        switch (xy.size()) {
            case 0:
                y = new VectorLabel(x.toString()); //TODO more info
                break;
            case 1:
                y = xy.get(0).apply(x);
                break;
            default:
                List<Function<AnyOf, Surface>> xyz = the.applicable(AnyOf.class, Surface.class); //warning, could recurse
                assert(xyz.size()==1): "multiple materializations of " + AnyOf.class;
                y = xyz.get(0).apply(new AnyOf(x, xy));
                break;
        }
        return y;
    }

    //interface FunctionOftheObject..
    //interface FunctionOftheSurface..


    public static class AnyOf<X,Y>  {

        final List<Function<Object, Y>> f;
        final X x;

        public AnyOf(X x, List<Function<Object, Y>> f) {
            this.f = f;
            this.x = x;
        }

        @Override
        public String toString() {
            return Joiner.on("\n").join(f) + "\n\t x " + x;
        }
    }

    public abstract static class Way<X> implements Supplier<X> {
        public String name;
    }

    /** supplies zero or more chocies from a set */
    public static class Some<X> implements Supplier<X[]> {
        final Way<X>[] way;
        final AtomicMetalBitSet enable = new AtomicMetalBitSet();

        public Some(Way<X>[] way) {
            this.way = way;
            assert(way.length > 1 && way.length <= 31 /* AtomicMetalBitSet limit */);
        }

        public Some<X> set(int which, boolean enable) {
            this.enable.set(which, enable);
            return this;
        }

        @Override
        public X[] get() {
            throw new TODO();
        }

        public int size() {
            return way.length;
        }
    }

    public static class Best<X> extends RankedN implements Supplier<X> {
        final Some<X> how;
        final FloatRank<X> rank;

        public Best(Some<X> how, FloatRank<X> rank) {
            super(new Object[how.size()], rank);
            this.how = how;
            this.rank = rank;
        }

        @Override
        public X get() {
            clear();
            X[] xx = how.get();
            if (xx.length == 0)
                return null;
            for (X x : xx)
                add(x);
            return (X) top();
        }
    }

}
