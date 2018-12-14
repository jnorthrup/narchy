package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import jcog.data.list.FasterList;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.hud.Ortho;

import java.util.function.BiConsumer;

/** surface rendering context */
public class SurfaceRender {


    private final FasterList<BiConsumer<GL2, SurfaceRender>>
            main = new FasterList<>();

    /** viewable pixel resolution */
    public float pw, ph;
    /** ms since last update */
    public int dtMS;
    public long renderStartNS;

    public float scaleX, scaleY;
    public float x1, x2, y1, y2;


    public SurfaceRender() {

    }



    /** encodes the rendering sequence */
    public void on(BiConsumer<GL2, SurfaceRender> renderable) {
        main.add(renderable);
    }


    public void clear() {
        main.clear();
    }

    public void render(GL2 gl) {
        main.forEach(rr -> rr.accept(gl, this));
    }

    public void render(Ortho.Camera cam, v2 scale, Surface root) {
        clear();
        set(cam, scale);
        root.recompile(this);
    }


    public SurfaceRender restart(float pw, float ph, int dtMS) {
        this.pw = pw;
        this.ph = ph;
        this.dtMS = dtMS;
        this.renderStartNS = System.nanoTime();
        return this;
    }

//    public void clone(float scale, v2 offset, Consumer<SurfaceRender> run, GL2 gl) {
//        SurfaceRender s = clone(scale, offset);
//        if (s!=this) {
//            gl.glPushMatrix();
//            {
//                gl.glTranslatef(offset.x, offset.y, 0);
//                gl.glScalef(scale, scale, 1);
//                run.accept(s);
//            }
//            gl.glPopMatrix();
//        } else {
//            run.accept(this);
//        }
//    }

//    public SurfaceRender clone(float scale, v2 offset) {
//        if (Util.equals(scale, 1f, ScalarValue.EPSILON) && offset.equalsZero())
//            return this; //unchanged
//        else
//            return new SurfaceRender(pw, ph, dtMS)
//                    .set(scaleX * scale, scaleY * scale,
//                        (x1 + x2)/2 + offset.x, (y1 + y2)/2 + offset.y);
//    }

    public SurfaceRender set(Ortho.Camera cam, v2 scale) {
        return set(scale.x, scale.y, cam.x, cam.y);
    }

    public SurfaceRender set(float scalex, float scaley, float cx, float cy) {
        this.scaleX = scalex;
        this.scaleY = scaley;
        float sxh = 0.5f * pw / scalex;
        float syh = 0.5f * ph / scaley;
        this.x1 = cx - sxh;
        this.x2 = cx + sxh;
        this.y1 = cy - syh;
        this.y2 = cy + syh;
        return this;
    }

    public RectFloat visible() {
        return RectFloat.XYXY(x1, y1, x2, y2);
    }
    public RectFloat pixelVisible() {
        return RectFloat.XYXY(0, 0, pw, ph);
    }

    public final boolean visible(RectFloat r) {
//        if (r.w < pixelScaleX)
//            return false;
//        if (r.h < pixelScaleY)
//            return false;
        return !(r.right() < x1) && !(r.left() > x2) && !(r.bottom() < y1) && !(r.top() > y2);
    }

    /** percentage of screen visible */
    public v2 visP(RectFloat bounds) {
        return new v2(bounds.w * scaleX, bounds.h * scaleY);
    }

    public float visPMin(RectFloat bounds) {
        return Math.min(bounds.w * scaleX, bounds.h * scaleY);
    }


    public final boolean visP(RectFloat bounds, int minPixelsToBeVisible) {

        return !(bounds.w * scaleX < minPixelsToBeVisible) && !(bounds.h * scaleY < minPixelsToBeVisible);
    }



//    /** adapts the world coordinates to a new virtual local coordinate system */
//    public SurfaceRender virtual(RectFloat xywh) {
//
//
//    }
}
