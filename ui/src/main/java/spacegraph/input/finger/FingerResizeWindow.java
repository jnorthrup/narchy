package spacegraph.input.finger;

import com.jogamp.newt.opengl.GLWindow;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.util.DragEdit;
import spacegraph.video.JoglSpace;

public class FingerResizeWindow extends FingerResize {

    float margin = 0.15f;

    private final static int MIN_WIDTH = 32;
    private final static int MIN_HEIGHT = MIN_WIDTH;

    private final JoglSpace space;

    public FingerResizeWindow(JoglSpace space, int button) {
        super(button, true);
        this.space = space;
    }


    @Override
    public DragEdit mode(Finger finger) {
        DragEdit edge = DragEdit.edge(
                Finger.posRelative(finger.posPixel, space.display),
                margin);
        return edge;
    }

    @Override
    protected final v2 pos(Finger finger) {
        return finger.posScreen;
    }

    @Override
    protected RectFloat size() {

        GLWindow ww = this.space.display.window;
        return RectFloat.X0Y0WH(ww.getX(), ww.getY(), ww.getWidth(),ww.getHeight());
    }


    @Override
    protected void resize(float x1, float y1, float x2, float y2) {
//        System.out.println(x1 + " " + y1  + ".." + x2 + " " + y2);

        int w = Math.round(x2 - x1);
        if (w < MIN_WIDTH)
            return;
        int h = Math.round(y2 - y1);
        if (h < MIN_HEIGHT)
            return;

        space.display.setPositionAndSize(Math.round(x1), Math.round(y1), w, h);
    }

    @Override
    public Surface touchNext(Surface prev, Surface next) {
        return null;
    }
}
