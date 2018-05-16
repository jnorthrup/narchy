package spacegraph.space2d;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.container.AspectAlign;
import spacegraph.util.math.v2;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Predicate;

/**
 * planar subspace.
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
abstract public class Surface implements SurfaceBase {

    public static final Surface[] EmptySurfaceArray = new Surface[0];

    private final static AtomicInteger serial = new AtomicInteger();

    /**
     * serial id unique to each instanced surface
     */
    public final int id = serial.incrementAndGet();

    /**
     * scale can remain the unit 1 vector, normally
     */
//    public v2 scale = new v2(1, 1); //v2.ONE;
    public volatile RectFloat2D bounds;
    public volatile SurfaceBase parent;
    protected volatile boolean visible = true;

    public Surface() {
        bounds = RectFloat2D.Unit;
    }

//    public static boolean leftButton(short[] buttons) {
//        return buttons != null && buttons.length == 1 && buttons[0] == 1;
//    }


    public float x() {
        return bounds.x;
    }

    public float y() {
        return bounds.y;
    }

    public float left() {
        return bounds.left();
    }

    public float top() {
        return bounds.top();
    }

    public float right() {
        return bounds.right();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return id;
    }

    public float bottom() {
        return bounds.bottom();
    }

    public float cx() {
        return bounds.x + 0.5f * bounds.w;
    }

    public float cy() {
        return bounds.y + 0.5f * bounds.h;
    }

    abstract protected void paint(GL2 gl, SurfaceRender surfaceRender);

    public Surface pos(RectFloat2D r) {
        posChanged(r);
        return this;
    }

    protected final boolean posChanged(RectFloat2D r) {
        RectFloat2D b = this.bounds;
        if (!b.equals(r, Spatialization.EPSILONf)) {
            this.bounds = r;
            return true;
        }
        return false;
    }

    public final Surface pos(float x1, float y1, float x2, float y2) {
        return pos(RectFloat2D.XYXY(x1, y1, x2, y2));
    }

    public AspectAlign align(AspectAlign.Align align) {
        return new AspectAlign(this, 1.0f, align, 1.0f);
    }

    public SurfaceRoot root() {
        SurfaceBase parent = this.parent;
        return parent == null ? null : parent.root();
    }

    /**
     * finds the most immediate parent matching the class
     */
    public <S extends Surface> S parent(Class<S> s) {
        return (S) parent(s::isInstance);
    }

    /**
     * finds the most immediate parent matching the predicate
     */
    public SurfaceBase parent(Predicate<SurfaceBase> test) {

        SurfaceBase p = this.parent;

        if (p instanceof Surface) {
            if (test.test(p))
                return p;
            else
                return ((Surface) p).parent(test);
        }

        return null;
    }

    public boolean start(SurfaceBase parent) {
        assert(parent!=null);
        if (_parent.getAndSet(this, parent)==null) { //if this atomic update changed from non-null to null, the callee has got it
            return true;
        }
        return false;
    }


    final static AtomicReferenceFieldUpdater<Surface,SurfaceBase> _parent =
            AtomicReferenceFieldUpdater.newUpdater(Surface.class, SurfaceBase.class, "parent");

    public boolean stop() {
        return _parent.getAndSet(this, null) != null;
        //already stopped. ok
    }

    public void layout() {
        //nothing by default
    }

    public float w() {
        return bounds.w;
    }

    public float h() {
        return bounds.h;
    }

    public Surface move(float dx, float dy) {
        pos(bounds.move(dx, dy, Spatialization.EPSILONf));
        return this;
    }

    public void print(PrintStream out, int indent) {
        out.print(Texts.repeat("  ", indent));
        out.println(this);
    }

    @Deprecated public final void render(GL2 gl, int pw, int ph, int dtMS) {
        if (!visible) return;
        paint(gl, new SurfaceRender(pw, ph, dtMS));
    }

    @Deprecated public final void render(GL2 gl, SurfaceRender r) {

        paint(gl, r);

    }

    public boolean tryKey(KeyEvent e, boolean pressed) {
        return false;
    }

    /**
     * returns true if the event has been absorbed, false if it should continue propagating
     */
    public boolean tryKey(v2 hitPoint, char charCode, boolean pressed) {
        return false;
    }


    /**
     * returns non-null if the event has been absorbed by a speciifc sub-surface
     * or null if nothing absorbed the gesture
     */
    public Surface tryTouch(Finger finger) {
        return null;
    }

    public Surface hide() {
        visible = false;
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

    public float radius() {
        return bounds.radius();
    }


    public void size(float w, float h) {
        pos(bounds.size(w, h));
    }
}
