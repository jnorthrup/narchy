package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.pri.ScalarValue;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.util.math.v2;
import spacegraph.util.math.v3;

import java.util.function.Consumer;

/** surface rendering context */
public class SurfaceRender {



    /** viewable pixel resolution */
    public final float pw, ph;

    /** ms since last update */
    public final int dtMS;
    public final long renderStartNS;
    private float scaleX, scaleY;
    private float x1, x2, y1, y2;

    public SurfaceRender(float pw, float ph, int dtMS) {
        this.pw = pw;
        this.ph = ph;
        this.dtMS = dtMS;
        this.renderStartNS = System.nanoTime();
    }

    public void clone(float scale, v2 offset, Consumer<SurfaceRender> run, GL2 gl) {
        SurfaceRender s = clone(scale, offset);
        if (s!=this) {
            gl.glPushMatrix();
            {
                gl.glTranslatef(offset.x, offset.y, 0);
                gl.glScalef(scale, scale, 1);
                run.accept(s);
            }
            gl.glPopMatrix();
        } else {
            run.accept(this);
        }
    }

    public SurfaceRender clone(float scale, v2 offset) {
        if (Util.equals(scale, 1f, ScalarValue.EPSILON) && offset.equalsZero())
            return this; //unchanged
        else
            return new SurfaceRender(pw, ph, dtMS)
                    .set(scaleX * scale, scaleY * scale,
                        (x1 + x2)/2 + offset.x, (y1 + y2)/2 + offset.y);
    }

    public SurfaceRender set(v3 cam, v2 scale) {
        return set(scale.x, scale.y, cam.x, cam.y);
    }

    public SurfaceRender set(float scalex, float scaley, float cx, float cy) {
        this.scaleX = scalex;
        this.scaleY = scaley;
        float sxh = 0.5f * pw / scaleX;
        float syh = 0.5f * ph / scaleY;
        this.x1 = cx - sxh;
        this.x2 = cx + sxh;
        this.y1 = cy - syh;
        this.y2 = cy + syh;
        return this;
    }


    public final boolean visible(RectFloat r) {
//        if (r.w < pixelScaleX)
//            return false;
//        if (r.h < pixelScaleY)
//            return false;
        if (r.right() < x1 || r.left() > x2)
            return false;
        if (r.bottom() < y1 || r.top() > y2)
            return false;
        return true;
    }

    public v2 visP(RectFloat bounds) {
        float pctX = bounds.w * scaleX;
        float pctY = bounds.h * scaleY;
        return new v2(pctX, pctY);
    }

    public float visPMin(RectFloat bounds) {
        return Math.min(bounds.w * scaleX, bounds.h * scaleY);
    }


    public final boolean visP(RectFloat bounds, int minPixelsToBeVisible) {

        if (bounds.w * scaleX < minPixelsToBeVisible)
            return false;
        if (bounds.h * scaleY < minPixelsToBeVisible)
            return false;

        return true;
    }

//    /** adapts the world coordinates to a new virtual local coordinate system */
//    public SurfaceRender virtual(RectFloat xywh) {
//
//
//    }
}
