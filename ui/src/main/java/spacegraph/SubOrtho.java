package spacegraph;

import jcog.tree.rtree.rect.RectFloat2D;

/** ortho sized proportionally to its containing window */
public class SubOrtho extends Ortho {

    private RectFloat2D boundsWin;

    public SubOrtho(Surface content) {
        super(content);
        maximize();
    }

    public void maximize() {

        posWindow(0, 0, 1, 1);
    }


    /** position and size relative to the containing window (1=width, 1=height)*/
    public Ortho posWindow(float x, float y, float w, float h) {
        this.boundsWin = new RectFloat2D(x, y, w, h);
        layout();
        return this;
    }

    @Override
    protected void doLayout(int dtMS) {
        int ww = window.getWidth();
        int wh = window.getHeight();

        pos(new RectFloat2D(boundsWin.x * ww, boundsWin.y * wh, boundsWin.w * ww, boundsWin.h * wh ));

        super.doLayout(dtMS);
    }


}
