package spacegraph.video;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.hud.Ortho;

import java.awt.image.BufferedImage;

public class TexSurface extends Surface {

    public final Tex tex;

    TexSurface() {
        this(new Tex());
    }

    public TexSurface(Tex tex) {
        this.tex = tex;
    }


    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        tex.paint(gl, bounds);
    }

    @Override
    protected void stopping() {
        Ortho r = (Ortho) root();
        if (r!=null && r.space!=null) {
            tex.stop(r.space.gl());
        }

    }

    public TexSurface set(BufferedImage img) {
        tex.set(img);
        return this;
    }
}
