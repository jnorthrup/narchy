package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.atomic.AtomicCycle;
import spacegraph.space2d.ReSurface;
import spacegraph.video.Draw;

import java.util.function.Supplier;


/** TODO use TriggeredMatrixView for async which should cause less updates */
public class PaintUpdateMatrixView extends BitmapMatrixView {
    public PaintUpdateMatrixView(float[] x) {
        super(x);
    }
    public PaintUpdateMatrixView(double[] x) {
        super(x, 1, Draw::colorBipolar);
    }
    public PaintUpdateMatrixView(Supplier<double[]> x, int len) {
        super(x, len, Draw::colorBipolar);
    }
    public PaintUpdateMatrixView(Supplier<double[]> x, int len, int stride) {
        super(x, len, stride, Draw::colorBipolar);
    }

    public PaintUpdateMatrixView(float[][] x) {
        super(x);
    }

    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        updateIfShowing();
        super.paint(gl, reSurface);
    }

    public static PaintUpdateMatrixView scroll(double[] x, boolean normalize, int window, int step) {
        assert(window >= step && (window % step == 0));

        var max = x.length;
        assert(max >= window);

        var xx = new double[window];
        var i = new AtomicCycle.AtomicCycleN(max);
        return new PaintUpdateMatrixView(()->{
            var start = i.addAndGet(step);
            int over;
            var end = start + window;
            if (end >= max) {
                over = (end-max); end = max;
            } else
                over= 0;
            System.arraycopy(x, start, xx, 0, (end-start));
            if (over > 0)
                System.arraycopy(x, 0, xx, (end-start), over);

            if(normalize)
                Util.normalize(xx);
            return xx;
        }, window, step);
    }
}
