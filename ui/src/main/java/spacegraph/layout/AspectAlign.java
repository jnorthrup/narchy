package spacegraph.layout;

import spacegraph.Surface;

import static spacegraph.layout.AspectAlign.Align.Center;

public class AspectAlign extends UnitContainer {

    /**
     * not used unless aspect ratio is set to non-NaN value
     */
    protected Align align = Align.Center;

    /**
     * height/width target aspect ratio; if aspect is NaN, no adjustment applied
     */
    protected float aspect;

    /**
     * relative size adjustment uniformly applied to x,y
     * after the 100% aspect size has been calculated
     */
    protected float scaleX, scaleY;

    protected AspectAlign() {
        this(1f);
    }

    protected AspectAlign(float scale) {
        this(null, 1f, Center, scale);
    }

    public AspectAlign(Surface the) {
        this(the, 1f, Center, 1f);
    }


    public AspectAlign(Surface the, Align a, float w, float h) {
        this(the, w / h, a, 1f);
    }

    public AspectAlign(Surface the, float aspect) {
        this(the, aspect, Align.Center);
    }

    public AspectAlign(Surface the, float aspect, Align a) {
        this(the, aspect, a, 1);
    }

    public AspectAlign(Surface the, float aspect, Align a, float scale) {
        this(the, aspect, a, scale, scale);
    }

    public AspectAlign(Surface the, float aspect, Align a, float scaleX, float scaleY) {
        super(the);
        this.aspect = aspect;
        this.align = a;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public AspectAlign scale(float sx, float sy) {
        this.scaleX = sx;
        this.scaleY = sy;
        layout(); //TODO only if changed
        return this;
    }

    @Override
    protected void doLayout(int dtMS) {

        //        v2 scale = this.scale;
//
//        float sx, sy;

        //local size
        final float w = w();
        final float h = h();

        //target's relative size being computed
        float tw = w * scaleX;
        float th = h * scaleY;

        float aspect = this.aspect;
        if (aspect == aspect /* not NaN */) {

            if (h > w/aspect) {
                //if (aspect >= 1) {
                    //taller than wide
                    //tw = w;
                    th = tw / aspect;
//                } else {
//                    //wider than tall
//                    tw = vw;
//                    th = vh*aspect;
//                }
            } else if (h * aspect > w) {
//                if (aspect >= 1) {
                    //th = h;
                    tw = th * aspect;
//                } else {
//                    tw = vw*aspect;
//                    th = vh;
//                }
            }
//            if (vh / vw > aspect) {
//                //wider, shrink y
//                tw = vw;
//                th = vw * aspect;
//            } else {
//                //taller, shrink x
//                tw = vh * aspect;
//                th = vh;
//            }
        }

        float tx, ty;
        switch (align) {

            //TODO others

            case Center:
                //HACK TODO figure this out
                tx = bounds.left() + (w - tw) / 2f;
                ty = bounds.top() + (h - th) / 2f;
                break;
            case LeftCenter:
                tx = bounds.left();
                ty = bounds.top() + (h - th) / 2f;
                break;

            case RightTop:
                tx = bounds.right() - tw;
                ty = bounds.bottom() - th;
                break;

            case RightTopOut:
                tx = bounds.right();
                ty = bounds.bottom();
                break;
            case LeftTopOut:
                tx = bounds.left();
                ty = bounds.bottom();
                break;

            case LeftTop:
                tx = bounds.left();
                ty = bounds.bottom() - th;
                break;

            case None:
            default:
                tx = bounds.left();
                ty = bounds.top();
                break;

        }

        doLayout(tx, ty, tw, th);
    }

    protected void doLayout(float tx, float ty, float tw, float th) {
        the.pos(tx, ty, tx+tw, ty+th);
    }

    @Override
    public AspectAlign align(Align align) {
        this.align = align;
        return this;
    }



//    public spacegraph.AspectAlign align(Align align, float width, float height) {
//        return align(align, height / width);
//    }

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
        RightTop, LeftTop,

        //TODO etc...

        RightTopOut, LeftTopOut
    }
}
