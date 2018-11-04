package spacegraph.space2d.container.unit;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.video.Draw;

/** renders contents only within the bounds of the container */
public class Clipped extends UnitContainer {

    public Clipped(Surface the) {
        super(the);
    }

    @Override
    protected void paintBelow(GL2 gl, SurfaceRender r) {
        Draw.stencilStart(gl);
        Draw.rect(bounds, gl);
        Draw.stencilUse(gl, true);
    }

    @Override
    protected void paintAbove(GL2 gl, SurfaceRender r) {

        Draw.stencilEnd(gl);
    }
}
