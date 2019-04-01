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
        //synchronized (this) {
            if (parent != null)
                the().start(this);
        //}
    }

    /** default behavior: inherit bounds directly
     * @param dtS*/
    @Override
    @Deprecated protected void doLayout(float dtS) {
        the().pos(innerBounds());
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
        //assert(t!=null);
        //if (t!=null) {
            o.accept(t);
        //}
    }
    @Override
    public boolean whileEach(Predicate<Surface> o) {
        S content = the();
        if (content instanceof Container)  return ((Container)content).whileEach(o);
        else return o.test(content);
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
//        S content = the();
//        if (content instanceof Container)  return ((Container)content).whileEachReverse(o);
//        else return o.test(content);
        return o.test(the());
    }


}
