package spacegraph.space2d.container.unit;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;

import java.util.function.Consumer;
import java.util.function.Predicate;

abstract public class AbstractUnitContainer<S extends Surface> extends Container {

    public abstract S the();


    @Override
    protected void starting() {
        synchronized (this) {
            if (parent != null)
                the().start(this);
        }
    }

    /** default behavior: inherit bounds directly */
    @Override
    @Deprecated protected final void doLayout(int dtMS) {
        S t = the();
        t.pos(innerBounds());
        t.layout();
    }
    protected RectFloat innerBounds() {
        return bounds;
    }


    @Override
    public final int childrenCount() {
        return 1;
    }

    @Override
    public final void forEach(Consumer<Surface> o) {

        S t = the();
        assert(t!=null);
        //if (t!=null) {
            o.accept(t);
        //}
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return o.test(the());
    }

    @Override
    public final boolean whileEachReverse(Predicate<Surface> o) {
        return whileEach(o);
    }

}
