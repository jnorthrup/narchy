package spacegraph.space2d.container.unit;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;

import static spacegraph.space2d.container.unit.AspectAlign.Align.Center;

public class AspectAlign extends UnitContainer {

    /**
     * not used unless aspect ratio is set to non-NaN value
     */
    private Align align;

    /**
     * height/width target aspect ratio; if aspect is NaN, no adjustment applied
     */
    private float aspect;

    /**
     * relative size adjustment uniformly applied to x,y
     * after the 100% aspect size has been calculated
     */
    private float scaleX;
    private float scaleY;

    protected AspectAlign() {
        this(1f);
    }

    private AspectAlign(float scale) {
        this(null, 1f, Center, scale);
    }

    public AspectAlign(Surface the) {
        this(the, 1f, Center, 1f);
    }


    public AspectAlign(Surface the, Align a, float w, float h) {
        this(the, h / w, a, 1f);
    }

    public AspectAlign(Surface the, float aspect) {
        this(the, aspect, Align.Center);
    }

    private AspectAlign(Surface the, float aspect, Align a) {
        this(the, aspect, a, 1);
    }

    public AspectAlign(Surface the, float aspect, Align a, float scale) {
        this(the, aspect, a, scale, scale);
    }

    public AspectAlign(Surface the, float aspect, Align a, float scaleX, float scaleY) {
        super(the);
        this.aspect = aspect;
        this.align = a;
        scale(scaleX, scaleY);
    }

    private AspectAlign scale(float sx, float sy) {
        this.scaleX = sx;
        this.scaleY = sy;
        layout();  //TODO only layout if sx,sy actually change
        return this;
    }


    @Override
    protected RectFloat innerBounds() {
        return innerBounds(bounds, scaleX, scaleY, aspect, align);
    }

    public static RectFloat innerBounds(RectFloat bounds, float aspect) {
        return innerBounds(bounds, aspect, Align.Center);
    }

    public static RectFloat innerBounds(RectFloat bounds, float aspect, Align align) {
        return innerBounds(bounds, 1, 1, aspect, align);
    }

    public static RectFloat innerBounds(RectFloat bounds, float scaleX, float scaleY, float aspect, Align align) {
        float w = bounds.w;
        float h = bounds.h;


        float tw = w * scaleX;
        float th = h * scaleY;
        float otw = tw, oth = th;

        if (aspect == aspect /* not NaN */) {

            if (otw * tw / aspect >= oth * th * aspect) {
                th = otw * aspect;
            } else {
                tw = oth / aspect;
            }

            if (tw > otw) {

                th = otw/(tw/th);
                tw = otw;
            }
            if (th > oth) {

                tw = oth/(th/tw);
                th = oth;
            }


        }

        float tx, ty;
        switch (align) {

            

            case Center:
                
                tx = bounds.left() + (w - tw) / 2f;
                ty = bounds.bottom() + (h - th) / 2f;
                break;
            case LeftCenter:
                tx = bounds.left();
                ty = bounds.bottom() + (h - th) / 2f;
                break;

            case TopRight:
                tx = bounds.right() - tw;
                ty = bounds.top() - th;
                break;

            case RightTopOut:
                tx = bounds.right();
                ty = bounds.top();
                break;
            case LeftTopOut:
                tx = bounds.left();
                ty = bounds.top();
                break;

            case LeftTop:
                tx = bounds.left();
                ty = bounds.top() - th;
                break;

            case None:
            default:
                tx = bounds.left();
                ty = bounds.bottom();
                break;

        }


        return RectFloat.X0Y0WH(tx, ty, tw, th);
    }

    public AspectAlign aspect(float aspect) {
        this.aspect = aspect;
        return this;
    }

    @Override
    public AspectAlign align(Align align) {
        this.align = align;
        return this;
    }







    public AspectAlign scale(float s) {
        return scale(s, s);
    }

    public enum Align {


        None,

        /**
         * 1:1, centered
         */
        Center,

        /**
         * 1:1, x=left, y=center
         */
        LeftCenter,

        /**
         * 1:1, x=right, y=center
         */
        TopRight, LeftTop,

        

        RightTopOut, LeftTopOut
    }
}
