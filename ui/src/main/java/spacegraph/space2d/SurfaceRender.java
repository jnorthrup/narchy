package spacegraph.space2d;

import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.util.math.v2;
import spacegraph.util.math.v3;

/** surface rendering context */
public class SurfaceRender {



    /** viewable pixel resolution */
    private final float pw;
    private final float ph;

    /** ms since last update */
    public final int dtMS;
    private float scaleX, scaleY;
    private float x1, x2, y1, y2;

    public SurfaceRender(float pw, float ph, int dtMS) {
        this.pw = pw;
        this.ph = ph;
        this.dtMS = dtMS;
    }

    public SurfaceRender setScale(v3 cam, v2 scale) {
        return setScale(scale.x, scale.y, cam.x, cam.y);
    }

    public SurfaceRender setScale(float scalex, float scaley, float cx, float cy) {
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

    public boolean visible(RectFloat2D r) {
        if (r.right() < x1 || r.left() > x2)
            return false;
        return !(r.bottom() < y1) && !(r.top() > y2);
    }

    public v2 visP(RectFloat2D bounds) {
        float pctX = bounds.w / (x2-x1) * pw;
        float pctY = bounds.h / (y2-y1) * ph;
        return new v2(pctX, pctY);
    }






}
