package spacegraph.video;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.SimpleSurface;

/**
 * from: http:
 */
public class GridTex extends SimpleSurface {

    private final Tex tex;

    private final static int[] rr128 = new int[128*128];
    static {
        int w = 128;
        int h = 128;
        for (int j = 0; j < h; ++j)
            for (int i = 0; i < w; ++i)
                rr128[j * w + i] = (i < w / 16 || j < h / 16 ? 255 : 0);
    }

    private final float repeatScale;

    boolean init = true;

    public GridTex(float repeatScale) {
        tex = new Tex();
        this.repeatScale = repeatScale;
    }

    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {

        if (tex.texture == null) {
            tex.set(rr128, 128, 128);
        }

        tex.paint(gl, bounds, repeatScale);
    }

}
