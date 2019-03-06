package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.WTF;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.collection.AbstractMutableContainer;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.widget.meta.WeakSurface;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Predicate;

/**
 * planar subspace.
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
abstract public class Surface implements SurfaceBase, spacegraph.input.finger.Fingered {

    private static final AtomicReferenceFieldUpdater<Surface, RectFloat> BOUNDS = AtomicReferenceFieldUpdater.newUpdater(Surface.class, RectFloat.class, "bounds");
    private final static AtomicReferenceFieldUpdater<Surface,SurfaceBase> PARENT = AtomicReferenceFieldUpdater.newUpdater(Surface.class, SurfaceBase.class, "parent");



    public static final Surface[] EmptySurfaceArray = new Surface[0];

    private final static AtomicInteger serial = new AtomicInteger();
    protected boolean clipBounds = true;
    /**
     * serial id unique to each instanced surface
     */
    public final int id = serial.incrementAndGet();

    /**
     * scale can remain the unit 1 vector, normally
     */

    public volatile RectFloat bounds = RectFloat.Zero;
    public volatile SurfaceBase parent;
    protected volatile boolean visible = true, showing = false;

//    public volatile int zIndex;

    public Surface() {

    }

    public final float cx() {
        return bounds.cx();
    }

    public final float cy() {
        return bounds.cy();
    }

    @Deprecated public final float x() {
        return left();
    }

    @Deprecated public final float y() {
        return top();
    }

    public final float left() {
        return bounds.left();
    }

    public final float top() {
        return bounds.top();
    }

    public final float right() {
        return bounds.right();
    }

    public final float bottom() {
        return bounds.bottom();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return id;
    }

    @Deprecated protected void paint(GL2 gl, SurfaceRender surfaceRender) {

    }

    public <S extends Surface> S pos(RectFloat next) {
        BOUNDS.lazySet(this, next);
//        if (bounds.area() < ScalarValue.EPSILON)
//            throw new WTF();
        return (S) this;
    }

    /** override and return false to prevent movement */
    protected boolean posChanged(RectFloat next) {
        RectFloat last = BOUNDS.getAndSet(this, next);
//        if (bounds.area() < ScalarValue.EPSILON)
//            throw new WTF();
        return !last.equals(next, Spatialization.EPSILONf);
    }

    public final Surface pos(float x1, float y1, float x2, float y2) {
        return pos(RectFloat.XYXY(x1, y1, x2, y2));
    }

    public final Surface posXYWH(float cx, float cy, float w, float h) {
        return pos(RectFloat.XYWH(cx, cy, w, h));
    }

    public AspectAlign align(AspectAlign.Align align) {
        return new AspectAlign(this, 1.0f, align, 1.0f);
    }

    public SurfaceRoot root() {
        return rootParent();
    }

    /** default root() impl */
    public final SurfaceRoot rootParent() {
        SurfaceBase parent = this.parent;
        return parent == null ? null : parent.root();
    }

    /**
     * finds the most immediate parent matching the class
     */
    public <S> S parent(Class<S> s) {
        return (S)(s.isInstance(this) ? this : parent(s::isInstance));
    }

    /**
     * finds the most immediate parent matching the predicate
     */
    private SurfaceBase parent(Predicate<SurfaceBase> test) {

        SurfaceBase p = this.parent;

        if (p instanceof Surface)
            return test.test(p) ? p : ((Surface) p).parent(test);
        else
            return null;
    }

    public boolean start(SurfaceBase parent) {
        assert(parent!=null);
        SurfaceBase p = PARENT.getAndSet(this, parent);

            if (p != parent) {
                if (p != null)
                    throw new WTF();

                //synchronized (this) {
                starting();
                //}
                return true;
            }

        return false;
    }


    protected void starting() {
        //for implementing in subclasses
    }

    protected void stopping() {
        //for implementing in subclasses
    }

    public boolean stop() {
        if (PARENT.getAndSet(this, null) != null) {
            //synchronized (this) {
                showing = false;
                stopping();
            //}
            return true;
        }
        return false;
    }

    public void layout() {
        
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

    /** prepares the rendering procedures in the rendering context */
    public final void recompile(SurfaceRender r) {
        if (!showing) {
            showing = (visible() && (!clipBounds || r.visible(bounds)));
        }

        if (showing) {
            compile(r);
        }
    }

    protected void compile(SurfaceRender r) {
        r.on(this::render);
    }

    @Deprecated public final void render(GL2 gl, SurfaceRender r) {
        if (showing = visible(r))
            paint(gl, r);
    }






    public Surface hide() {
        visible = false;
        showing = false;
        return this;
    }

    public Surface show() {
        visible = true;
        return this;
    }

    public Surface visible(boolean b) {
        return b ? show() : hide();
    }

    public boolean visible() {
        return parent!=null && visible;
    }
    public final boolean visible(SurfaceRender r) {
         return visible() && (!clipBounds || r.visible(bounds));
    }

    public float radius() {
        return bounds.radius();
    }


    public Surface size(float w, float h) {
        return pos(bounds.size(w, h));
    }

    public boolean showing() {
        return showing;
    }

    public void posxyWH(float x, float y, float w, float h) {
        pos(RectFloat.X0Y0WH(x,y,w,h));
    }


    /** detach from parent, if possible
     * TODO common remove(x) interface
     * */
    public boolean remove() {
        SurfaceBase p = this.parent;

        if (p instanceof MutableUnitContainer) {
            ((MutableUnitContainer)p).set(new EmptySurface());
            return true;
        }
        if (p instanceof AbstractMutableContainer) {
            return ((AbstractMutableContainer) p).remove(this);
        }
        if(p instanceof WeakSurface) {
            return ((WeakSurface) p).remove();
        }
        if (p instanceof Container) {
            return ((Container)p).remove();
        }
        return false;
    }

    public boolean reattach(Surface nextParent) {
        if (this == nextParent)
            return true;

        SurfaceBase prevParent = this.parent;
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



}
