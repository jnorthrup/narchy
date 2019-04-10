package spacegraph.space2d;

import jcog.Texts;
import jcog.WTF;
import jcog.math.v2;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.ContainerSurface;
import spacegraph.space2d.container.collection.AbstractMutableContainer;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.widget.meta.WeakSurface;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.MutableRectFloat;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * planar subspace.
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
abstract public class Surface implements Surfacelike, spacegraph.input.finger.Fingered {


    public static final Surface[] EmptySurfaceArray = new Surface[0];
    public static final Supplier<Surface> TODO = () -> new VectorLabel("TODO");
    private static final AtomicReferenceFieldUpdater<Surface, RectFloat> BOUNDS = AtomicReferenceFieldUpdater.newUpdater(Surface.class, RectFloat.class, "bounds");
    private final static AtomicReferenceFieldUpdater<Surface, Surfacelike> PARENT = AtomicReferenceFieldUpdater.newUpdater(Surface.class, Surfacelike.class, "parent");
    private final static AtomicInteger serial = new AtomicInteger();
    /**
     * serial id unique to each instanced surface
     */
    public final int id = serial.incrementAndGet();
    /**
     * whether content can be expected outside of the bounds, ex: in order to react to events
     */
    public boolean clipBounds = true;
    /**
     * scale can remain the unit 1 vector, normally
     */

    public volatile RectFloat bounds = RectFloat.Unit;
    public volatile Surfacelike parent;
    protected volatile boolean visible = true;
    private volatile boolean showing = false;

//    public volatile int zIndex;

    public Surface() {

    }

    public final float cx() {
        return bounds.cx();
    }

    public final float cy() {
        return bounds.cy();
    }

    @Deprecated
    public final float x() {
        return left();
    }

    @Deprecated
    public final float y() {
        return top();
    }

    public final float left() {
        return bounds.left();
    }

    public final float top() {
        return bounds.bottom();
    }

    public final float right() {
        return bounds.right();
    }

    public final float bottom() {
        return bounds.top();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return id;
    }


    public <S extends Surface> S pos(MutableRectFloat next) {
        //TODO equality test?
        return pos(next.immutable());
    }

    public <S extends Surface> S pos(RectFloat next) {
        // if (next.area() < ScalarValue.EPSILON)
        //      throw new WTF();

        //BOUNDS.set(this, next);

        BOUNDS.accumulateAndGet(this, next,(prev,n)->prev.equals(n) ? prev : n );

        return (S) this;
    }

    /**
     * override and return false to prevent movement
     */
    protected boolean posChanged(RectFloat next) {
        RectFloat last = BOUNDS.getAndSet(this, next);
//        if (bounds.area() < ScalarValue.EPSILON)
//            throw new WTF();
        return last!=next && !last.equals(next, Spatialization.EPSILONf);
    }

    public final Surface pos(float x1, float y1, float x2, float y2) {
        RectFloat r = RectFloat.XYXY(x1, y1, x2, y2);
        return pos(r);
    }

    public final Surface posXYWH(float cx, float cy, float w, float h) {
        return pos(RectFloat.XYWH(cx, cy, w, h));
    }

    public AspectAlign align(AspectAlign.Align align) {
        return new AspectAlign(this, 1.0f, align, 1.0f);
    }

    public SurfaceGraph root() {
        return rootParent();
    }

    /**
     * default root() impl
     */
    public final SurfaceGraph rootParent() {
        Surfacelike parent = this.parent;
        return parent == null ? null : parent.root();
    }

    /**
     * finds the most immediate parent matching the class
     */
    public <S> S parentOrSelf(Class<S> s) {
        return (S) (s.isInstance(this) ? this : parent(s::isInstance, false));
    }

    /**
     * finds the most immediate parent matching the predicate
     */
    public Surfacelike parent(Predicate<Surfacelike> test, boolean includeSelf) {

        if (includeSelf && test.test(this))
            return this;

        Surfacelike p = this.parent;

        if (p instanceof Surface)
            return test.test(p) ? p : ((Surface) p).parent(test, true);
        else
            return null;
    }

    public boolean start(Surfacelike parent) {
        assert (parent != null);
        Surfacelike p = PARENT.getAndSet(this, parent);

        if (p != parent) {
            if (p != null) {  //throw new WTF();
                delete();
                parent = PARENT.getAndSet(this, parent);
                if (parent!=null) throw new WTF();
            }

            //synchronized (this) {
            starting();
            //}
            return true;
        }

        return false;
    }

    /**
     * TODO
     */
    public String term() {
        return toString();
    }

    protected void starting() {
        //for implementing in subclasses
    }

    protected void stopping() {
        //for implementing in subclasses
    }

    public final boolean stop() {
        if (PARENT.getAndSet(this, null) != null) {
            hide();
            stopping();
            return true;
        }
        return false;
    }


    public float w() {
        return bounds.w;
    }

    public float h() {
        return bounds.h;
    }

    public Surface pos(float x, float y) {
        pos(bounds.pos(x, y, Spatialization.EPSILONf));
        return this;
    }

    public Surface move(float dx, float dy) {
        pos(bounds.move(dx, dy, Spatialization.EPSILONf));
        return this;
    }

    public void print(PrintStream out, int indent) {
        out.print(Texts.repeat("  ", indent));
        out.println(this);
    }

    /**
     * prepares the rendering procedures in the rendering context
     */
    public final void tryRender(ReSurface r) {
        if (this.showing = visible(r))
            render(r);
    }

    /** test visibility in the current rendering context */
    public final boolean visible(ReSurface r) {
        return visible() && (!clipBounds || r.isVisible(bounds));
    }

    abstract protected void render(ReSurface r);

    public final Surface hide() {
        visible = false;
        showing = false;
        return this;
    }

    public final Surface show() {
        visible = true;
        return this;
    }

    public final Surface visible(boolean b) {
        return b ? show() : hide();
    }

    public final boolean visible() {
        return visible && parent != null;
    }

    public final boolean showing() {
        return showing;
    }

    public float radius() {
        return bounds.radius();
    }



    /**
     * detach from parent, if possible
     * TODO common remove(x) interface
     */
    public boolean delete() {

        Surfacelike p = PARENT.getAndSet(this, null);
        if (p == null)
            return false;

        if (p instanceof MutableUnitContainer) {
            ((MutableUnitContainer) p).set(null);
            return true;
        }
        if (p instanceof AbstractMutableContainer) {
            return ((AbstractMutableContainer) p).remove(this);
        }
        if (p instanceof WeakSurface) {
            return ((WeakSurface) p).delete();
        }
        if (p instanceof ContainerSurface) {
            return ((ContainerSurface) p).delete();
        }

        stop();

        if (this instanceof ContainerSurface) {
            ((ContainerSurface)this).forEach(Surface::delete);
        }
        return true;
    }

    public boolean reattach(Surface nextParent) {
        if (this == nextParent)
            return true;

        Surfacelike prevParent = this.parent;
        if (prevParent instanceof AbstractMutableContainer && nextParent instanceof AbstractMutableContainer) {
            AbstractMutableContainer prevMutableParent = (AbstractMutableContainer) prevParent;
            AbstractMutableContainer nextMutableParent = (AbstractMutableContainer) nextParent;
            if (PARENT.compareAndSet(this, prevParent, nextParent)) {
                if (prevMutableParent.detachChild(this)) {

                    //now it is at risk of being lost

                    if (nextMutableParent.attachChild(this)) {
                        return true;
                    } else {
                        //could not attach to nextParent, reattach to prev
                        if (!prevMutableParent.attachChild(this)) {
                            //recovered
                        }
                    }

                }
                System.err.println("lost: " + this + " while reattaching from " + prevParent + " to " + nextParent);
                //TODO logger.warn(...
                stop();
            }
        }

        return false;
    }


    public boolean exist() {
        //TODO optimize with boolean flag
        return rootParent() != null;
    }

    public v2 pos() {
        return new v2(x(), y());
    }


    public final Surface resize(float w, float h) {
        return pos(bounds.size(w, h));
    }

    public final boolean resizeIfChanged(float w, float h) {
        return posChanged(bounds.size(w, h));
    }
}
