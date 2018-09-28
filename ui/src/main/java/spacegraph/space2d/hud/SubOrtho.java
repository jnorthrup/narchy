package spacegraph.space2d.hud;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.video.JoglSpace;

/** ortho sized proportionally to its containing window */
public class SubOrtho extends Ortho {

    private final Surface content;
    private RectFloat boundsWin;

    public SubOrtho(Surface content) {
        super();
        this.content = content;
    }

    @Override
    public void start(JoglSpace s) {
        super.start(s);
        setSurface(content);
    }


//    @Override
//    public boolean autoresize() {
//        return true;
//    }

    /** position and size relative to the containing window (1=width, 1=height)*/
    public Ortho posWindow(float x, float y, float w, float h) {
        this.boundsWin = RectFloat.XYWH(x, y, w, h);
        layout();
        return this;
    }

    @Override
    protected void doLayout(int dtMS) {
        int ww = window.getWidthNext();
        int wh = window.getHeightNext();

        super.doLayout(dtMS);


        surface.pos(RectFloat.XYXY(boundsWin.x * ww, boundsWin.y * wh, (boundsWin.x + boundsWin.w) * ww,
                (boundsWin.y + boundsWin.h) * wh));


        System.out.println(surface + " " + surface.bounds);

    }


}
