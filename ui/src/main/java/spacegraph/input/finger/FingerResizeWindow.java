package spacegraph.input.finger;

import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.widget.windo.util.DragEdit;
import spacegraph.video.JoglDisplay;
import spacegraph.video.JoglWindow;

/** broke currently */
public class FingerResizeWindow extends FingerResize {

    float margin = 0.15f;

    private final static int MIN_WIDTH = 32;
    private final static int MIN_HEIGHT = MIN_WIDTH;

    private final JoglDisplay space;
    private RectFloat size = RectFloat.Zero;

    public FingerResizeWindow(JoglDisplay space, int button) {
        super(button);
        this.space = space;
    }


    @Override
    public DragEdit mode(Finger finger) {
        DragEdit edge = DragEdit.mode(
                Finger.normalize(finger.posScreen, space.video),
                //Finger.normalize(finger.posScreen, size()),
                margin);
        return edge;
    }

    @Override
    protected boolean ready(Finger f) {
        if (super.ready(f)) {
            JoglWindow ww = this.space.video;
            size = RectFloat.X0Y0WH(ww.getX(), ww.getY(), ww.getWidth(),ww.getHeight());
            return true;
        }
        return false;
    }

    @Override
    protected final v2 pos(Finger finger) {
        return finger.posScreen;
    }

    @Override
    protected RectFloat size() {
        return size;
    }


    @Override
    protected void resize(float x1, float y1, float x2, float y2) {
        //System.out.println(x1 + "," + y1  + ".." + x2 + "," + y2);

        int w = Math.round(x2 - x1);
        if (w < MIN_WIDTH)
            return;
        int h = Math.round(y2 - y1);
        if (h < MIN_HEIGHT)
            return;

        int xi = Math.round(x1);
        int yi = Math.round(y1);
        RectFloat nextSize = RectFloat.XYXY(x1, y1, x2, y2);
        if (!nextSize.equals(size, 1f /* 1 pixel */ )) {
            size = nextSize;
            //Exe.invokeLater(() -> {
                space.video.setPositionAndSize(xi, yi, w, h);
            //});
        }
    }


}
