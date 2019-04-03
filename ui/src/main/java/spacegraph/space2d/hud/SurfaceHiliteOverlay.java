package spacegraph.space2d.hud;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.v2;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.SimpleSurface;
import spacegraph.util.math.Color4f;
import spacegraph.video.Draw;
import spacegraph.video.font.HersheyFont;

public abstract class SurfaceHiliteOverlay extends SimpleSurface {

    protected final Ortho.Camera cam;

    protected float thick = 16;
    protected final Color4f color = new Color4f(0.5f, 0.5f, 0.5f, 0.5f);

    public SurfaceHiliteOverlay(Ortho.Camera cam) {
        this.cam = cam;
        clipBounds = false;
    }

    Surface last = null;

    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        if (!enabled())
            return;

        Surface t = target();

        if (t!=null) {
            if (!t.showing())
                t = null;
            else {

                paintFrame(t, gl);

                paintCaption(t, reSurface, gl);

            }
        }

        last = t;
    }

    String caption = null;
    float captionFlashtimeS = 0.5f;
    float captionFlashRemain = 0;
    private void paintCaption(Surface t, ReSurface s, GL2 gl) {
        if (t != last) {
            //update
            caption = t.toString();
            captionFlashRemain = captionFlashtimeS;
        } else {
            captionFlashRemain = Math.max(0, captionFlashRemain - s.dtS());
        }

        //if (captionFlashRemain > 0) {
//        gl.glEnable(GL_COLOR_LOGIC_OP);
        //            gl.glLogicOp(
//                    GL_XOR
//                    //GL_INVERT
//                    //GL_OR_INVERTED
//                    //GL_EQUIV
//            );
        gl.glLineWidth(1f);
        float i = Util.lerp((captionFlashRemain / ((float) captionFlashtimeS)), 0.25f, 0.9f);
        gl.glColor3f(i,i,i);

        float w = s.pw, h = s.ph;
        float scale = Math.min(w, h) / 80f;
        HersheyFont.hersheyText(gl, caption, scale, w / 2, scale, 0, Draw.TextAlignment.Center);
        //        gl.glDisable(GL_COLOR_LOGIC_OP);
        //}
    }

    private void paintFrame(Surface t, GL2 gl) {
        float tx = t.x(), ty = t.y();
        v2 p = cam.worldToScreen(tx, ty);
        v2 q = cam.worldToScreen(tx + t.w(), ty + t.h());

        color.apply(gl);

        //TODO margin
        //                float px = p.x - thick;
        //                float py = p.y - thick;
        //gl.glLineWidth(thick);
        //Draw.rectStroke(gl, px, py, q.x+thick-px, q.y+thick-py);
        Draw.rectFrame((p.x + q.x) / 2, (p.y + q.y) / 2, q.x - p.x, q.y - p.y, thick, gl);
    }


    protected boolean enabled() {
        return true;
    }

    abstract protected Surface target();
}
