package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.SurfaceRender;

//TODO make this take a supplier of arrays, and re-create bitmap matrix view when the value changed
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
