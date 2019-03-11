package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.math.FloatSupplier;
import jcog.math.v2;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.wave2d.Bitmap2D;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.math.Point2i;
import spacegraph.video.Draw;
import spacegraph.video.Tex;
import spacegraph.video.TexSurface;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.function.Supplier;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.sqrt;
import static jcog.Util.clampSafe;


/**
 * 1D/2D bitmap viewer with parametric coloring.
 * updated and displayed as a bitmap texture
 */
public class BitmapMatrixView extends TexSurface {

    public final int w;
    public final int h;
    private volatile BitmapPainter view;
    protected final v2 touchPos = new v2();
    protected final Point2i touchPixel = new Point2i();
    private BufferedImage buf;
    private int[] pix;
    private boolean cellTouch = true;

    /** the implementation must implement BitmapPainter */
    protected BitmapMatrixView(int w, int h) {
        this(w, h, null);
    }


    public BitmapMatrixView(float[] d, ViewFunction1D view) {
        this(d, 1, view);
    }

    public BitmapMatrixView(int w, int h, ViewFunction2D view) {
        this(w, h, (BitmapPainter)view);
    }

    public BitmapMatrixView(int w, int h, BitmapPainter view) {
        super(new Tex());
        this.w = w;
        this.h = h;
        this.view = view != null ? view : ((BitmapPainter) this);
    }

    public BitmapMatrixView(Bitmap2D tex) {
        this(tex.width(), tex.height(), (x, y)->{
            float a = tex.brightness(x, y);
            return Draw.rgbInt(a,a,a);
        });
    }


    public BitmapMatrixView(float[] f) {
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

    public BitmapMatrixView(IntToFloatFunction d, int len, ViewFunction1D view) {
        this(d, len, Math.max(1, (int) Math.ceil(sqrt(len))), view);
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
    public Surface finger(Finger finger) {
        if (cellTouch && updateTouch(finger))
            return this;
        return null;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            return true;
        }
        return false;
    }

    public boolean updateTouch(Finger finger) {

        finger.posRel(this).scaled(w, h, touchPos);

        touchPixel.set(
                clampSafe((int) Math.floor(touchPos.x),0,w-1),
                clampSafe((int) Math.floor(touchPos.y), 0, h-1));

        return true;

    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        super.paint(gl, surfaceRender);
        /* paint cursor hilited cell */
        if (cellTouch) {
            float w = w() / this.w, h = h() / this.h;
            float x = x(), y = y();
            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.75f);
            gl.glLineWidth(2);
            Draw.rectStroke(x + touchPixel.x * w, y + touchPixel.y * h, w, h, gl);
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
    public RectFloat cellRect(float x, float y, float prw, float prh) {
        float pw = prw / this.w;
        float ph = prh / this.h;
        v2 c = cell(x, y);
        return RectFloat.XYWH(c.x, c.y, pw * w(), ph * h());
    }

    /**
     * must call this to re-generate texture so it will display
     */
    public final boolean updateIfNotShowing() {

        if (!showing())
            return false;

        return update();
    }

    public boolean update() {
        if (buf == null) {
            if (w == 0 || h == 0) return false;

            this.buf = new BufferedImage(w, h, alpha() ? TYPE_INT_ARGB : TYPE_INT_RGB);
            this.pix = ((DataBufferInt) buf.getRaster().getDataBuffer()).getData();
        }


        view.color(buf, pix);

        return tex.set(buf);
    }

    public boolean alpha() {
        return false;
    }

//    public boolean equalShape(Tensor x) {
//        int[] shape = x.shape();
//        if (shape.length == 1)
//            return h == 1 && w == shape[0];
//        else if (shape.length == 2) {
//            return w == shape[0] && h == shape[1];
//        } else
//            return false;
//    }

    @FunctionalInterface  public interface ViewFunction1D {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        int update(float x);
    }

    @FunctionalInterface
    public interface BitmapPainter {
        /** provides access to the bitmap in either BufferedImage or the raw int[] raster */
        void color(BufferedImage buf, int[] pix);
    }

    @FunctionalInterface
    public interface ViewFunction2D extends BitmapPainter {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        int color(int x, int y);

        default void color(BufferedImage buf, int[] pix) {
            final int w = buf.getWidth(), h = buf.getHeight();
            int i = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pix[i++] = color(x, y);
                }
            }
        }
    }

    public BitmapMatrixView cellTouch(boolean cellTouch) {
        this.cellTouch = cellTouch;
        return this;
    }
}
