package spacegraph.video;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.PaintSurface;

import java.awt.image.BufferedImage;

public class TexSurface extends PaintSurface {

    public final Tex tex;

    TexSurface() {
        this(new Tex());
    }

    public TexSurface(Tex tex) {
        this.tex = tex;
    }

    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        tex.paint(gl, bounds);
    }

    @Override
    protected void stopping() {
        tex.stop(this);
    }

    public TexSurface set(BufferedImage img) {
        tex.set(img);
        return this;
    }
}
