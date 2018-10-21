package spacegraph.space2d.hud;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.math.Color4f;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;

public abstract class SurfaceHiliteOverlay extends Surface {

    protected final Ortho.Camera cam;

    protected float thick = 6;
    protected final Color4f color = new Color4f(0.5f, 0.5f, 0.5f, 0.75f);

    public SurfaceHiliteOverlay(Ortho.Camera cam) {
        this.cam = cam;
        clipBounds = false;
    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        if (enabled()) {

            Surface t = target();

            if (t!=null && t.showing()) {


                float sw = surfaceRender.pw, sh = surfaceRender.ph;
                v2 p = cam.worldToScreen(sw, sh, t.x(), t.y());
                v2 q = cam.worldToScreen(sw, sh,t.x()+t.w(), t.y() + t.h());

                //TODO margin
                float px = p.x - thick;
                float py = p.y - thick;

                gl.glLineWidth(thick);
                color.apply(gl);
                Draw.rectStroke(gl, px, py, q.x+thick-px, q.y+thick-py);
            }
        }
    }


    protected boolean enabled() {
        return true;
    }

    abstract protected Surface target();
}
