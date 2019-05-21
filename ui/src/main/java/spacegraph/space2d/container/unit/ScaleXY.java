package spacegraph.space2d.container.unit;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;

public class ScaleXY extends UnitContainer {

    private float sx, sy;

    public ScaleXY(Surface the, float sx, float sy) {
        super(the);
        scale(sx, sy);
    }

    private ScaleXY scale(float sx, float sy) {
        this.sx = sx;
        this.sy = sy;
        return this;
    }


    @Override
    protected RectFloat innerBounds() {
        return bounds.scale(sx, sy);
    }
}
