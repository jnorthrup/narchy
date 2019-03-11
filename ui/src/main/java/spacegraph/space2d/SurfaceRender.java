package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import jcog.data.list.FasterList;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.hud.Ortho;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

/** surface rendering context */
public class SurfaceRender {


    private final FasterList<BiConsumer<GL2, SurfaceRender>>
            main = new FasterList<>();

    /** viewable pixel resolution */
    public float pw, ph;
    /** ms since last update */
    public int dtMS;
    public long restartNS;

    public float scaleX, scaleY;
    public float x1, x2, y1, y2;
    transient float w, h;

    public SurfaceRender() {

    }

    public final void on(Consumer<GL2> renderable) {
        on((gl, rr)->renderable.accept(gl));
    }

    /** encodes the rendering sequence */
    public final void on(BiConsumer<GL2, SurfaceRender> renderable) {
        main.add(renderable);
    }

    public void clear() {
        main.clear();
    }

    public final void render(int w, int h, GL2 gl) {
//        float ss = (float) Math.pow(2, Math.random() + 1);
//        gl.glScalef(ss, ss, 1);
        //gl.glTranslatef((w()/2)/scale.x - cam.x, (h()/2)/scale.y - cam.y, 0);
        main.forEachWith((rr,ggl) -> {
            ggl.glViewport(0, 0, w, h);
            ggl.glMatrixMode(GL_PROJECTION);
            ggl.glLoadIdentity();

            ggl.glOrtho(0, w, 0, h, -1.5, 1.5);
            ggl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

            rr.accept(ggl, this);

        }, gl);
    }

    public SurfaceRender restart(float pw, float ph) {
        this.pw = pw;
        this.ph = ph;
        this.restartNS = System.nanoTime();
        return this;
    }

    public SurfaceRender restart(float pw, float ph, int dtMS) {
        this.dtMS = dtMS;
        return restart(pw, ph);
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
//                    .setAt(scaleX * scale, scaleY * scale,
//                        (x1 + x2)/2 + offset.x, (y1 + y2)/2 + offset.y);
//    }

    public SurfaceRender set(Ortho.Camera cam, v2 scale) {
        return set(cam.x, cam.y, scale.x, scale.y);
    }

    public SurfaceRender set(float cx, float cy, float sx, float sy) {
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
        boolean v = r.intersectsX1Y1X2Y2(x1, y1, x2, y2);
        return v;
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

    @Override
    public String toString() {
        return scaleX + "x" + scaleY + " " + main.size() + " renderables";
    }


    //    /** adapts the world coordinates to a new virtual local coordinate system */
//    public SurfaceRender virtual(RectFloat xywh) {
//
//
//    }
}
