package spacegraph.render;

import com.jogamp.opengl.GL2;
import spacegraph.Surface;

/**
 * from: http://www.howtobuildsoftware.com/index.php/how-do/bQ9/opengl-glsl-how-can-i-render-an-infinite-2d-grid-in-glsl
 */
public class GridTex extends Surface {

    private final Tex tex;

    final static int rr128[] = new int[128*128];
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
    protected void paint(GL2 gl, int dtMS) {

        if (tex.texture == null) {
            tex.update(rr128, 128, 128);
        }

        tex.paint(gl, bounds, repeatScale);
    }

}
