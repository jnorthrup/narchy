package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.SurfaceRender;


public class AutoUpdateMatrixView extends BitmapMatrixView {
    public AutoUpdateMatrixView(float[] x) {
        super(x);
    }

    public AutoUpdateMatrixView(float[][] x) {
        super(x);
    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        update();
        super.paint(gl, surfaceRender);
    }
}
