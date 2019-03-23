package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.ReSurface;


/** TODO use TriggeredMatrixView for async which should cause less updates */
public class PaintUpdateMatrixView extends BitmapMatrixView {
    public PaintUpdateMatrixView(float[] x) {
        super(x);
    }

    public PaintUpdateMatrixView(float[][] x) {
        super(x);
    }

    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        updateIfShowing();
        super.paint(gl, reSurface);
    }

}
