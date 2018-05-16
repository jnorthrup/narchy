package spacegraph.space2d.hud;

import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.Surface;

/** ortho sized proportionally to its containing window */
public class SubOrtho extends Ortho {

    private RectFloat2D boundsWin;

    public SubOrtho(Surface content) {
        super(content);
        //maximize();
    }

    @Override
    public boolean autoresize() {
        return true;
    }
    //    public void maximize() {
//
//        posWindow(0, 0, 1, 1);
//    }


    /** position and size relative to the containing window (1=width, 1=height)*/
    public Ortho posWindow(float x, float y, float w, float h) {
        this.boundsWin = RectFloat2D.XYXY(x, y, w, h);
        layout();
        return this;
    }

    @Override
    protected void doLayout(int dtMS) {
        int ww = window.getWidth();
        int wh = window.getHeight();

        pos(RectFloat2D.XYXY(boundsWin.x * ww, boundsWin.y * wh, boundsWin.w * ww, boundsWin.h * wh));

        super.doLayout(dtMS);
    }


}
