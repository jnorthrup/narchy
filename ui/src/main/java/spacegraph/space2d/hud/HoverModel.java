package spacegraph.space2d.hud;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;

public abstract class HoverModel {
    Surface s;
    Finger f;

    public void set(Surface root, Finger finger) {
        this.s = root;
        this.f = finger;
    }

    abstract RectFloat pos(float hoverTimeS);

    //TODO abstract float alpha(float hoverTimeS);

    /** attached relative to cursor center and sized relative to element */
    public static class Cursor extends HoverModel {

        @Override
        RectFloat pos(float hoverTimeS) {
            //RectFloat ss = f.globalToPixel(s.bounds);
            float scale = Math.max(f.boundsScreen.w, f.boundsScreen.h);
            double pulse = Math.cos(hoverTimeS * 6f) * 0.05f;
            float ss = (float) (
                        pulse +
                        Math.min(Math.exp(hoverTimeS/4f) * 0.2f, 0.3f)) *
                               scale;
            return RectFloat.XYWH(f.posPixel, ss, ss);
        }
    }

    /** smaller and to the side so as not to cover any of the visible extents of the source.
     *  uses global screen pos as a heuristic of what direction to shift towards to prevent
     *  clipping beyond the screen edge
     * */
    public static class ToolTip extends HoverModel {

        @Override
        RectFloat pos(float hoverTimeS) {
            RectFloat ss = f.globalToPixel(s.bounds);
            return ss.scale(0.25f).move(ss.w/2, ss.h / 2);
        }
    }

    /** maximized to screen extents */
    public static class Maximum extends HoverModel {

        @Override
        RectFloat pos(float hoverTimeS) {
            return f.boundsScreen;
        }
    }

    public static class Exact extends HoverModel {

        @Override
        RectFloat pos(float hoverTimeS) {
            return s.bounds;
        }
    }
}
