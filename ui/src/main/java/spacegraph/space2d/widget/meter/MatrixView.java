package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.math.FloatSupplier;
import jcog.signal.tensor.ArrayTensor;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import spacegraph.space2d.widget.Widget;
import spacegraph.video.Draw;

import java.util.function.Supplier;

/**
 * Created by me on 7/29/16.
 */
@Deprecated public class MatrixView extends Widget {


    private final int w;
    private final int h;
    private final ViewFunction2D view;


    private static ViewFunction2D arrayRenderer(float[][] ww) {
        return (x, y, gl) -> {
            var v = ww[x][y];
            Draw.colorBipolar(gl, v);
            return 0;
        };
    }
    private static ViewFunction2D arrayRenderer(double[][] ww) {
        return (x, y, gl) -> {
            var v = (float) ww[x][y];
            Draw.colorBipolar(gl, v);
            return 0;
        };
    }

    public static ViewFunction2D arrayRenderer(float[] w) {
        return (x, y, gl) -> {
            var v = w[y];
            Draw.colorBipolar(gl, v);
            return 0;
        };
    }

    public static ViewFunction2D arrayRenderer(double[] w) {
        return (x, y, gl) -> {
            var v = (float) w[y];
            Draw.colorBipolar(gl, v);
            return 0;
        };
    }

    public interface ViewFunction1D {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        float update(float x, GL2 gl);
    }

    @FunctionalInterface
    public interface ViewFunction2D {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        float update(int x, int y, GL2 gl);
    }

    protected MatrixView(int w, int h) {
        this(w, h, null);
    }


    public MatrixView(float[] d, ViewFunction1D view) {
        this(d, 1, view);
    }


    public MatrixView(float[][] w) {
        this(w.length, w[0].length, MatrixView.arrayRenderer(w));
    }
    public MatrixView(double[][] w) {
        this(w.length, w[0].length, MatrixView.arrayRenderer(w));
    }

    public MatrixView(int w, int h, ViewFunction2D view) {
        this.w = w;
        this.h = h;
        //noinspection CastToIncompatibleInterface
        this.view = view != null ? view : ((ViewFunction2D) this);
    }

    private static final ViewFunction1D bipolar1 = (x, gl) -> {
        Draw.colorBipolar(gl, x);
        return 0;
    };
    private static final ViewFunction1D unipolar1 = (x, gl) -> {
        Draw.colorGrays(gl, x);
        return 0;
    };

    public MatrixView(float[] d, boolean bipolar) {
        this(d, 1, bipolar ? bipolar1 : unipolar1);
    }

    public MatrixView(float[] d, int stride, ViewFunction1D view) {
        this(stride, (int) Math.ceil(((float) d.length) / stride), (x, y, gl) -> {
            var i = y * stride + x;
            return i < d.length ? view.update(d[i], gl) : Float.NaN;
        });
    }

    public MatrixView(IntToFloatFunction d, int len, int stride, ViewFunction1D view) {
        this(stride, (int) Math.ceil(((float) len) / stride), (x, y, gl) -> {
            var i = y * stride + x;
            return i < len ? view.update(d.valueOf(i), gl) : Float.NaN;
        });
    }

    public MatrixView(double[] d, int stride, ViewFunction1D view) {
        this(stride, (int) Math.ceil(((float) d.length) / stride), (x, y, gl) -> {
            var i = y * stride + x;
            return i < d.length ? view.update((float) d[i], gl) : Float.NaN;
        });
    }

    public <P extends FloatSupplier> MatrixView(P[] d, int stride, ViewFunction1D view) {
        this(stride, (int) Math.ceil(((float) d.length) / stride), (x, y, gl) -> {
            var i = y * stride + x;
            return i < d.length ? view.update(d[i].asFloat(), gl) : Float.NaN;
        });
    }






    public static MatrixView get(ArrayTensor t, int stride, ViewFunction1D view) {
        var d = t.data;
        return new MatrixView(stride, (int) Math.ceil(((float) t.volume()) / stride), (x, y, gl) -> {
            var v = d[x * stride + y];
            return view.update(v, gl);
        });
    }

    public MatrixView(Supplier<double[]> e, int length, int stride, ViewFunction1D view) {
        this(stride, (int) Math.ceil(((float) length) / stride), (x, y, gl) -> {
            var d = e.get();
            if (d != null) {

                var i = y * stride + x;
                if (i < d.length)
                    return view.update((float) d[i], gl);
            }
            return Float.NaN;
        });
    }

    @Override
    protected void paintWidget(RectFloat bounds, GL2 gl) {

        float h = this.h;
        float w = this.w;

        if ((w == 0) || (h == 0))
            return;


        var dw = 1f / w * w();
        var dh = 1f / h * h();


        var tx = x();
        var ty = y();
        for (var y = 0; y < h; y++) {

            var yy = ty + 1f - (y + 1) * dh;

            for (var x = 0; x < w; x++) {


                var dz = view.update(x, y, gl);
                if (dz == dz) {
                    Draw.rect(gl, tx + x * dw, yy, dw, dh, dz);
                }
                /*} catch (Exception e) {
                    logger.error(" {}",e);
                    return;
                }*/

            }
        }





    }


}
