package spacegraph.space2d.widget.meta;

import jcog.math.FloatRange;
import jcog.reflect.ExtendedCastGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.widget.port.FloatRangePort;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.List;
import java.util.function.Function;

public class ObjectSurface2 extends MutableUnitContainer {

    final ExtendedCastGraph builder;

    public ObjectSurface2(Object x) {
        this(x, DefaultBuilder);
    }

    public ObjectSurface2(Object x, ExtendedCastGraph builder) {
        this.builder = builder;
        set(x);
    }

    public synchronized void set(Object x) {
        List<Function<Object, Surface>> c = builder.convertors(x.getClass(), Surface.class);
        Surface y;
        switch (c.size()) {
            case 0:
                y = new VectorLabel(x.toString()); //TODO more info
                break;
            case 1:
                y = c.get(0).apply(x);
                break;
            default:
                //TODO selector frame
                y = c.get(0).apply(x);
                break;
        }
        set(y);
    }

    static public final ExtendedCastGraph DefaultBuilder = new ExtendedCastGraph();

    static private <X> void build(Class<? extends X> from, Function<X,Surface> builder) {
        DefaultBuilder.addEdge(from, builder, Surface.class);
    }
    {
        build(List.class, (List x) -> new Gridding(x));
        build(FloatRange.class, (FloatRange x) -> new FloatRangePort(x));
    }
}
