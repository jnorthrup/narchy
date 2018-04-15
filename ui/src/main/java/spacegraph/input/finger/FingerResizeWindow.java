package spacegraph.input.finger;

import com.jogamp.newt.opengl.GLWindow;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.util.math.v2;
import spacegraph.video.JoglSpace;

public class FingerResizeWindow extends FingerResize {

    final static int MIN_WIDTH = 32;
    final static int MIN_HEIGHT = MIN_WIDTH;

    private final JoglSpace window;
    private final RectFloat2D originalSize;
    private float x1, y1, x2, y2;

    public FingerResizeWindow(JoglSpace window, int button, Windo.DragEdit mode) {
        super(button, mode);
        this.window = window;
        GLWindow w = this.window.window;
        int sh = w.getHeight();
        int sw = w.getWidth();
        int sy = w.getY();
        int sx = w.getX();
        originalSize = new RectFloat2D(sx, sy, sw + sx, sh + sy);
    }

    @Override
    protected v2 pos(Finger finger) {
        return new v2(finger.pointer.getX(), -finger.pointer.getY());
    }

    @Override
    protected RectFloat2D size() {
        return originalSize;
    }

    @Override
    protected void resize(float _x1, float _y1, float _x2, float _y2) {


        x1 = _x1;
        y1 = _y1;
        x2 = _x2;
        y2 = _y2;


        int w = Math.max(MIN_WIDTH, Math.round(x2 - x1));
        int h = Math.max(MIN_HEIGHT, Math.round(y2 - y1)); //y2-y1);
        window.setPosition(Math.round(x1), Math.round(y1));
        window.setSize(w, h);

    }

}
