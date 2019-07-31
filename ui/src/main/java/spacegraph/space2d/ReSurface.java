package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import jcog.Paper;
import jcog.data.list.FasterList;
import jcog.math.FloatAveragedWindow;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.hud.Zoomed;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** surface rendering context */
public class ReSurface extends SurfaceCamera {

    static final float minVisibilityPixelPct =
            //0.5f;
            1f;

    /** time since last frame (seconds) */
    private float frameDT;

    /** can be used to calculate frame latency */
    public float frameDTideal;

    /** load metric as a temporal level-of-detail (QoS criteria)
     *    (0 = perfectly on-time, >= 1  stalled ) */
    @Paper
    public FloatAveragedWindow load = new FloatAveragedWindow(3, 0.5f);


    public long frameNS;
    @Deprecated public long frameUnixTime;

    private transient GL2 gl;


    @Deprecated public final void on(Consumer<GL2> renderable) {
        on((gl, rr)->renderable.accept(this.gl));
    }

    @Deprecated public final void on(BiConsumer<GL2, ReSurface> renderable) {
        renderable.accept(gl, this);
    }



    /** ortho restart */
    public ReSurface start(GL2 gl, float pw, float ph, float dtS, float fps) {
        this.frameDT = dtS;
        this.frameNS = System.nanoTime();
        this.frameUnixTime = System.currentTimeMillis();
        //this.frameDTms = Math.max(1, Math.round(1000 * frameDT));
        this.frameDTideal = (float) (1.0/Math.max(1.0E-9,fps));
        this.load.next( Math.max(0, dtS - frameDTideal) / frameDTideal );

        this.gl = gl;
        this.pw = pw;
        this.ph = ph;
        set(pw/2, ph/2, 1, 1);

        return this;
    }

    public ReSurface set(Zoomed.Camera cam, v2 scale) {
        return set(cam.x, cam.y, scale.x, scale.y);
    }

    public ReSurface set(float cx, float cy, float sx, float sy) {

        this.scaleX = sx;
        this.scaleY = sy;
        float sxw = 0.5f * pw / sx;
        float syh = 0.5f * ph / sy;
        this.x1 = cx - sxw;
        this.x2 = cx + sxw;
        this.y1 = cy - syh;
        this.y2 = cy + syh;
        w = x2-x1;
        h = y2-y1;
        return this;
    }

//    public RectFloat visible() {
//        return RectFloat.XYXY(x1, y1, x2, y2);
//    }
    public RectFloat pixelVisible() {
        return RectFloat.XYXY(0, 0, pw, ph);
    }

    public final boolean visibleByCamera(RectFloat r) {
//        if (r.w < 1/scaleX)
//            return false;
//        if (r.h < 1/scaleY)
//            return false;
//        return true;
        boolean v = r.intersectsX1Y1X2Y2(x1, y1, x2, y2);
        return v;
    }
    public final boolean visibleByPixels(RectFloat r) {
        boolean v = visP(r, minVisibilityPixelPct) > 0;
        return v;
    }

    public final float visP(RectFloat bounds, float minPixelsToBeVisible) {
        //return (bounds.w * scaleX >= minPixelsToBeVisible) && (bounds.h * scaleY >= minPixelsToBeVisible);

//        System.out.println(scaleX + " " + w + " " + pw);
        float p = (bounds.w / this.w) * pw * scaleX;
        if (p < minPixelsToBeVisible)
            return 0;
        float q = (bounds.h / this.h) * ph * scaleY;
        if (q < minPixelsToBeVisible)
            return 0;

        return Math.min(p, q);
    }


    /** seconds since last update */
    public float dtS() {
        return frameDT;
    }

    public float load() {
        return load.asFloat();
    }


    final FasterList<SurfaceCamera> stack = new FasterList();

    public void push(Zoomed.Camera cam, v2 scale) {
        SurfaceCamera prev = clone();
        stack.add(prev);
        set(cam, scale.scaleClone(prev.scaleX, prev.scaleY));
    }

    public void pop() {
        set(stack.removeLast());
    }

    public void end() {
        gl = null;
    }


    //    /** adapts the world coordinates to a new virtual local coordinate system */
//    public SurfaceRender virtual(RectFloat xywh) {
//
//
//    }
}
