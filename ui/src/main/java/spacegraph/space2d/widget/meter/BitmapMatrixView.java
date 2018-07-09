package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.math.FloatSupplier;
import jcog.math.tensor.ArrayTensor;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.math.Point2i;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;
import spacegraph.video.Tex;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.function.Supplier;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;


/**
 * Created by me on 7/29/16.
 */
public class BitmapMatrixView extends Surface {

    public final int w;
    private final int h;
    private final ViewFunction2D view;
    private final Tex bmp;
    protected Tuple2f touchPos;
    protected Point2i touchPixel;
    private BufferedImage buf;
    private int[] pix;

    protected BitmapMatrixView(int w, int h) {
        this(w, h, null);
    }


    public BitmapMatrixView(float[] d, ViewFunction1D view) {
        this(d, 1, view);
    }

    public BitmapMatrixView(int w, int h, ViewFunction2D view) {
        this.w = w;
        this.h = h;
        this.view = view != null ? view : ((ViewFunction2D) this);
        this.bmp = new Tex();
    }


    BitmapMatrixView(float[] f) {
        this(f.length, 1, arrayRenderer(f));
    }

    public BitmapMatrixView(float[][] f) {
        this(f.length, f[0].length, arrayRenderer(f));
    }

    private BitmapMatrixView(float[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) d.length) / stride), stride, (x, y) -> {
            int i = y * stride + x;
            return i < d.length ? view.update(d[i]) : 0;
        });
    }

    public BitmapMatrixView(IntToFloatFunction d, int len, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) len) / stride), stride, (x, y) -> {
            int i = y * stride + x;
            return i < len ? view.update(d.valueOf(i)) : 0;
        });
    }


    public BitmapMatrixView(double[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) d.length) / stride), stride, (x, y) -> {
            int i = y * stride + x;
            return i < d.length ? view.update((float) d[i]) : 0;
        });
    }

    public <P extends FloatSupplier> BitmapMatrixView(P[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) d.length) / stride), stride, (x, y) -> {
            int i = y * stride + x;
            return i < d.length ? view.update(d[i].asFloat()) : 0;
        });
    }

    public BitmapMatrixView(Supplier<double[]> e, int length, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) length) / stride), stride, (x, y) -> {
            double[] d = e.get();
            if (d != null) {

                int i = y * stride + x;
                if (i < d.length)
                    return view.update((float) d[i]);
            }
            return 0;
        });
    }

    private static ViewFunction2D arrayRenderer(float[] ww) {
        return (x, y) -> {
            float v = ww[x];
            return Draw.colorBipolar(v);
        };
    }

    private static ViewFunction2D arrayRenderer(float[][] ww) {
        return (x, y) -> {
            float v = ww[x][y];
            return Draw.colorBipolar(v);
        };
    }

    public static BitmapMatrixView get(ArrayTensor t, int stride, ViewFunction1D view) {
        float[] d = t.data;
        return new BitmapMatrixView((int) Math.floor(((float) t.volume()) / stride), stride, (x, y) -> {
            float v = d[x * stride + y];
            return view.update(v);
        });
    }

    @Override
    public Surface tryTouch(Finger finger) {
        if (finger != null) {
            updateTouch(finger);
        } else {
            touchPos = null;
            touchPixel = null;
        }
        return null;

    }

    public void updateTouch(Finger finger) {

        touchPos = finger.relativePos(this).scaled(w, h);

        touchPixel = new Point2i((int) Math.floor(touchPos.x),
                (int) Math.floor(touchPos.y));


    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        bmp.paint(gl, bounds);
        if (touchPixel != null) {
            float w = w() / this.w;
            float h = h() / this.h;
            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.75f);
            gl.glLineWidth(2);
            float x = x();
            float y = y();
            Draw.rectStroke(gl, x + touchPixel.x * w, y + touchPixel.y * h, w, h);
        }
    }

    /**
     * the position of a cell's center
     */
    private v2 cell(float x, float y) {
        float W = w();
        float xx = ((x + 0.5f) / (w)) * W;
        float H = h();
        float yy = (1f - ((y + 0.5f) / (h))) * H;
        return new v2(xx, yy);
    }

    /**
     * the prw, prh represent a rectangular size proportional to the displayed cell size
     */
    public RectFloat2D cellRect(float x, float y, float prw, float prh) {
        v2 c = cell(x, y);
        float pw = prw / this.w;
        float ph = prh / this.h;
        return RectFloat2D.XYWH(c.x, c.y, pw * w(), ph * h());
    }

    /**
     * must call this to re-generate texture so it will display
     */
    public void update() {
        if (buf == null) {
            if (w == 0 || h == 0) return;

            buf = new BufferedImage(w, h, TYPE_INT_ARGB);
            this.pix = ((DataBufferInt) buf.getRaster().getDataBuffer()).getData();
        }

        int[] pix = this.pix;
        final int h = this.h;
        final int w = this.w;
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pix[i++] = view.update(x, y);
            }
        }

        bmp.update(buf);
    }

    @FunctionalInterface  public interface ViewFunction1D {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        int update(float x);
    }

    @FunctionalInterface
    public interface ViewFunction2D {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        int update(int x, int y);
    }


}
