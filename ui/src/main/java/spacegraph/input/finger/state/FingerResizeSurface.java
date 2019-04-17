package spacegraph.input.finger.state;

import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.util.DragEdit;

/**
 * resizes a rectangular surface in one of the four cardinal or four diagonal directions
 */
public class FingerResizeSurface extends FingerResize {

    private final Surface s;


    public FingerResizeSurface(Surface s, int button) {
        super(button);
        this.s = s;
    }

    /** move most of Windo.drag() logic here
     * @param finger*/
    @Nullable @Override @Deprecated public DragEdit mode(Finger finger) {
        return mode;
    }

    @Override
    protected boolean starting(Finger f) {
        return f.intersects(s.bounds) && super.starting(f);
    }

    @Override
    protected v2 pos(Finger finger) {
        return finger.posGlobal();
    }

    @Override
    protected RectFloat size() {
        return s.bounds;
    }

    @Override
    protected void resize(float x1, float y1, float x2, float y2) {
        RectFloat r = RectFloat.XYXY(x1, y1, x2, y2);
        s.pos(r);
    }

    @Override
    public Surface touchNext(Surface prev, Surface next) {
        return s;
    }
}
