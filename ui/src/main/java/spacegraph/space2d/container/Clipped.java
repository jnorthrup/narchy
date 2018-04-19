package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.Surface;
import spacegraph.video.Draw;

/** renders contents only within the bounds of the container */
public class Clipped extends UnitContainer {

    public Clipped(Surface the) {
        super(the);
    }

    @Override
    protected void paintBelow(GL2 gl) {
        Draw.stencilStart(gl);
        Draw.rect(gl, bounds);
        Draw.stencilUse(gl, true);
    }

    @Override
    protected void paintAbove(GL2 gl, int dtMS) {

        Draw.stencilEnd(gl);
    }
}
