package spacegraph.input.finger;

import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.util.DragEdit;

/**
 * resizes a rectangular surface in one of the four cardinal or four diagonal directions
 */
public class FingerResizeSurface extends FingerResize {

    private final Surface target;
    @Nullable
    protected DragEdit mode;

    public FingerResizeSurface(Surface target, int button) {
        super(button);
        this.target = target;
    }

    /** move most of Windo.drag() logic here
     * @param finger*/
    @Nullable @Override @Deprecated public DragEdit mode(Finger finger) {
        return mode;
    }

    @Override
    protected v2 pos(Finger finger) {
        return finger.posGlobal().clone();
    }

    @Override
    protected RectFloat size() {
        return target.bounds;
    }

    @Override
    protected void resize(float x1, float y1, float x2, float y2) {
        target.pos(x1, y1, x2, y2);
    }

    @Override
    public Surface touchNext(Surface prev, Surface next) {
        return target;
    }
}
