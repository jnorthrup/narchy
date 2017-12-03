package spacegraph;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.math.v2;

import java.io.PrintStream;

/**
 * planar subspace.
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
abstract public class Surface {

    /** smallest recognizable dimension change */
    public static final float EPSILON = 0.0001f;
    private boolean visible = true;

    public float x() {
        return bounds.min.x;
    }

    public float y() {
        return bounds.min.y;
    }

    public float cx() {
        return 0.5f * (bounds.min.x + bounds.max.x);
    }

    public float cy() {
        return 0.5f * (bounds.min.y + bounds.max.y);
    }

//    @Override
//    public String toString() {
//        return super.toString() + "{" +
//                ", bounds=" + bounds +
//                "scale=" + scale +
//                '}';
//    }

    public Surface pos(RectFloat2D r) {
        RectFloat2D b = this.bounds;
        if (b == null || !b.equals(r, Surface.EPSILON))
            this.bounds = r;
        return this;
    }
    public Surface pos(float x1, float y1, float x2, float y2) {
        RectFloat2D b = this.bounds;
        if (b ==null || !b.equals(x1, y1, x2, y2, Surface.EPSILON)) {
            pos(new RectFloat2D(x1, y1, x2, y2));
        }
        return this;
    }


    public AspectAlign align(AspectAlign.Align align, float aspectRatio) {
        return new AspectAlign(this, aspectRatio, align, 1f);
    }


    /**
     * scale can remain the unit 1 vector, normally
     */
//    public v2 scale = new v2(1, 1); //v2.ONE;
    public RectFloat2D bounds;
    public Surface parent;


    public Surface() {
        bounds = RectFloat2D.Unit;
    }

    public SurfaceRoot root() {
        Surface parent = this.parent;
        if (parent == null)
            return null;
        return parent.root();
    }


    /**
     * null parent means it is the root surface
     */
    public synchronized void start(@Nullable Surface parent) {
        this.parent = parent;
    }

    public synchronized void stop() {
        parent = null;
    }

    public void layout() {
        //nothing by default
    }


    public float w() {
        return bounds.max.x - bounds.min.x;
    }

    public float h() {
        return bounds.max.y - bounds.min.y;
    }

    /**
     * returns non-null if the event has been absorbed by a speciifc sub-surface
     * or null if nothing absorbed the gesture
     */
    public Surface onTouch(Finger finger, @Deprecated v2 hitPoint, @Deprecated short[] buttons) {
        //System.out.println(this + " " + hitPoint + " " + Arrays.toString(buttons));

        //1. test local reaction
        boolean b = onTouching(finger, hitPoint, buttons);
        if (b)
            return this;

        return null;
    }


    /**
     * may be overridden to trap events on this surface (returning true), otherwise they pass through to any children
     */
    @Deprecated
    protected boolean onTouching(Finger finger, v2 hitPoint, short[] buttons) {
        return false;
    }


    abstract protected void paint(GL2 gl);

    public Surface move(float dx, float dy) {
        pos(bounds.move(dx, dy));
        return this;
    }

    public void print(PrintStream out, int indent) {
        out.print(Texts.repeat("  ", indent));
        out.println(this.toString());
    }

    public final void render(GL2 gl) {

        if (!visible)
            return;

        paint(gl);

    }


    public static boolean leftButton(short[] buttons) {
        return buttons != null && buttons.length == 1 && buttons[0] == 1;
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
