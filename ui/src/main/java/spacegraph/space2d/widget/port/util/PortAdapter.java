package spacegraph.space2d.widget.port.util;

import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.port.TypedPort;

import java.lang.ref.SoftReference;
import java.util.List;
import java.util.function.Function;

/** holds a pair of function lists */
public class PortAdapter<X,Y> extends Widget {

    private final List<Function/*<X,Y>*/> fxy;
    private final List<Function/*<Y,X>*/> fyx;

    public final Class<? super X> xClass;
    public final Class<? super Y> yClass;
    public final SoftReference<TypedPort<X>> x; //HACK
    public final SoftReference<TypedPort<Y>> y; //HACK

    /** current enabled strategy (selection index) */
    volatile int whichXY = -1, whichYX = -1;

    public PortAdapter( Class<? super X> xClass, List<Function/*<X,Y>*/> fxy, Class<? super Y> yClass, List<Function/*<Y,X>*/> fyx) {
        super();

        this.xClass = xClass; this.yClass = yClass;

        TypedPort<X> x = null;
        TypedPort<Y> y = null;
        Gridding g = new Gridding();
        if (!fxy.isEmpty()) {
            whichXY = 0;
            this.fxy = fxy;

            g.add(x = new TypedPort<>(xClass));
            x.on((xx)->PortAdapter.this.out(xx, true));

        } else { this.fxy = null; }
        if (!fyx.isEmpty()) {
            whichYX = 0;
            this.fyx = fyx;

            g.add(y = new TypedPort<>(yClass));
            y.on((yy)->PortAdapter.this.out(yy, false));

        } else this.fyx = null;

        this.x = new SoftReference(x); this.y = new SoftReference(y);
        set(g);
    }

    protected final boolean out(Object o, boolean sender) {
        TypedPort src = port(sender);
        if (src == null) {
            remove(); //done
            return false;
        }

        return src.out((sender? fxy : fyx).get(sender? whichXY : whichYX).apply(o));
    }

    public TypedPort<?> port(boolean xOrY) {
        return (xOrY ? this.x : this.y).get();
    }

//    @Override
//    protected Object transfer(Surface sender, Object x) {
//        if (sender == a && whichAB >= 0) {
//            x = (abAdapters.get(whichAB)).apply(x);
//        } else if (sender == b && whichBA >= 0) {
//            x = (baAdapters.get(whichBA)).apply(x);
//        }
//        return x;
//    }
}
