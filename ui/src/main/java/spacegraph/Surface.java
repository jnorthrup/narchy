package spacegraph;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.NotNull;
import spacegraph.input.Finger;
import spacegraph.math.v2;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * planar subspace.
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
abstract public class Surface implements SurfaceBase {

    /**
     * smallest recognizable dimension change
     */
    public static final float EPSILON = 0.0001f;

    private final AtomicInteger serial = new AtomicInteger();

    /** serial id unique to each instanced surface */
    public final int id = serial.incrementAndGet();

    /**
     * scale can remain the unit 1 vector, normally
     */
//    public v2 scale = new v2(1, 1); //v2.ONE;
    public RectFloat2D bounds;
    public SurfaceBase parent;
    private boolean visible = true;

    public Surface() {
        bounds = RectFloat2D.Unit;
    }

    public static boolean leftButton(short[] buttons) {
        return buttons != null && buttons.length == 1 && buttons[0] == 1;
    }


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

    abstract protected void paint(GL2 gl, int dtMS);

    public Surface pos(RectFloat2D r) {
        posChanged(r);
        return this;
    }
    public final boolean posChanged(RectFloat2D r) {
        RectFloat2D b = this.bounds;
        if (!b.equals(r, Surface.EPSILON)) {
            this.bounds = r;
            return true;
        }
        return false;
    }

    public final Surface pos(float x1, float y1, float x2, float y2) {
        return pos(new RectFloat2D(x1, y1, x2, y2));
    }

    public AspectAlign align(AspectAlign.Align align) {
        return new AspectAlign(this, 1.0f, align, 1.0f);
    }

    public SurfaceRoot root() {
        SurfaceBase parent = this.parent;
        return parent == null ? null : parent.root();
    }

    /**
     * null parent means it is the root surface
     */
    public /*synchronized*/ void start(@NotNull SurfaceBase parent) {
        this.parent = parent;
    }

    public /*synchronized*/ void stop() {
        parent = null;
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

    /**
     * returns non-null if the event has been absorbed by a speciifc sub-surface
     * or null if nothing absorbed the gesture
     */
    public Surface onTouch(Finger finger, @Deprecated short[] buttons) {
        //System.out.println(this + " " + hitPoint + " " + Arrays.toString(buttons));

        return null;
    }

    public Surface move(float dx, float dy) {
        pos(bounds.move(dx, dy, EPSILON));
        return this;
    }

    public void print(PrintStream out, int indent) {
        out.print(Texts.repeat("  ", indent));
        out.println(this);
    }

    public final void render(GL2 gl, int dtMS) {

        if (!visible)
            return;

        //DEBUG
        if ((parent == null) && !(this instanceof Ortho)) {
            throw new RuntimeException(this + " being rendered with null parent");
        }

        paint(gl, dtMS);

    }

    public boolean onKey(KeyEvent e, boolean pressed) {
        return false;
    }

    /**
     * returns true if the event has been absorbed, false if it should continue propagating
     */
    public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
        return false;
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

    public float radius() {
        return Math.max(w(), h());
    }

}
