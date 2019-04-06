package spacegraph.space2d.hud;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;

/** manages a hover state triggered by Finger interaction */
public interface Hovered {

    /** return a surface to appear on the HUD layer of the screen. it will be added and removed by the display system */
    Surface hover(RectFloat screenBounds, Finger f);

}
