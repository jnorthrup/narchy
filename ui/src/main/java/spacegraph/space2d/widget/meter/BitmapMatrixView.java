package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.math.FloatSupplier;
import jcog.math.v2;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.wave2d.Bitmap2D;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.util.math.Point2i;
import spacegraph.video.Draw;
import spacegraph.video.Tex;
import spacegraph.video.TexSurface;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.atomic.AtomicBoolean;
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
    protected BitmapPainter view;
    protected final v2 touchPos = new v2();
    protected final Point2i touchPixel = new Point2i();
    private BufferedImage buf;
    private int[] pix;
    public boolean cellTouch = true;

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
        this(tex.width(), tex.height(), new ViewFunction2D() {
            @Override
            public int color(int x, int y) {
                float a = tex.brightness(x, y);
                return Draw.rgbInt(a, a, a);
            }
        });
    }


    public BitmapMatrixView(float[] f) {
        this(f.length, 1, arrayRenderer(f));
    }

    public BitmapMatrixView(float[][] f) {
        this(f.length, f[0].length, arrayRenderer(f));
    }

    private BitmapMatrixView(float[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor((double) (((float) d.length) / (float) stride)), stride, new ViewFunction2D() {
            @Override
            public int color(int x, int y) {
                int i = y * stride + x;
                return i < d.length ? view.update(d[i]) : 0;
            }
        });
    }

    public BitmapMatrixView(IntToFloatFunction d, int len, ViewFunction1D view) {
        this(d, len, Math.max(1, (int) Math.ceil(sqrt((double) len))), view);
    }

    public BitmapMatrixView(IntToFloatFunction d, int len, int stride, ViewFunction1D view) {
        this((int) Math.floor((double) (((float) len) / (float) stride)), stride, new ViewFunction2D() {
            @Override
            public int color(int x, int y) {
                int i = y * stride + x;
                return i < len ? view.update(d.valueOf(i)) : 0;
            }
        });
    }

    public BitmapMatrixView(double[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor((double) (((float) d.length) / (float) stride)), stride, new ViewFunction2D() {
            @Override
            public int color(int x, int y) {
                int i = y * stride + x;
                return i < d.length ? view.update((float) d[i]) : 0;
            }
        });
    }

    public <P extends FloatSupplier> BitmapMatrixView(P[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor((double) (((float) d.length) / (float) stride)), stride, new ViewFunction2D() {
            @Override
            public int color(int x, int y) {
                int i = y * stride + x;
                return i < d.length ? view.update(d[i].asFloat()) : 0;
            }
        });
    }


    public BitmapMatrixView(Supplier<double[]> e, int len, ViewFunction1D view) {
        this(e, len, Math.max(1, (int) Math.ceil(sqrt((double) len))), view);
    }

    public BitmapMatrixView(Supplier<double[]> e, int len, int stride, ViewFunction1D view) {
        this((int) Math.floor((double) (((float) len) / (float) stride)), stride, new ViewFunction2D() {
            @Override
            public int color(int x, int y) {
                double[] d = e.get();
                if (d != null) {

                    int i = y * stride + x;
                    if (i < d.length)
                        return view.update((float) d[i]);
                }
                return 0;
            }
        });
    }

    public static ViewFunction2D arrayRenderer(float[] ww) {
        return arrayRendererX(ww);
    }


    public static BitmapMatrixView.ViewFunction2D arrayRendererX(float[] ww) {
        return new ViewFunction2D() {
            @Override
            public int color(int x, int y) {
                return Draw.colorBipolar(ww[x]);
            }
        };
    }
    public static BitmapMatrixView.ViewFunction2D arrayRendererY(float[] ww) {
        return new ViewFunction2D() {
            @Override
            public int color(int x, int y) {
                return Draw.colorBipolar(ww[y]);
            }
        };
    }

    public static ViewFunction2D arrayRenderer(float[][] ww) {
        return new ViewFunction2D() {
            @Override
            public int color(int x, int y) {
                float v = ww[x][y];
                return Draw.colorBipolar(v);
            }
        };
    }

    public static BitmapMatrixView get(ArrayTensor t, int stride, ViewFunction1D view) {
        float[] d = t.data;
        return new BitmapMatrixView((int) Math.floor((double) (((float) t.volume()) / (float) stride)), stride, new ViewFunction2D() {
            @Override
            public int color(int x, int y) {
                float v = d[x * stride + y];
                return view.update(v);
            }
        });
    }

    @Override
    public Surface finger(Finger finger) {
        if (cellTouch && updateTouch(finger))
            return this;
        return null;
    }

    public boolean updateTouch(Finger finger) {

        v2 r = finger.posRelative(bounds);
        if (r.inUnit()) {
            r.scaleInto((float) w, (float) h, touchPos);

            touchPixel.set(
                    clampSafe((int) Math.floor((double) touchPos.x), 0, w - 1),
                    clampSafe((int) Math.floor((double) touchPos.y), 0, h - 1));
            return true;
        }
        return false;
    }

    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        super.paint(gl, reSurface);
        /* paint cursor hilited cell */
        if (cellTouch) {
            float w = w() / (float) this.w, h = h() / (float) this.h;
            float x = x(), y = y();
            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.75f);
            gl.glLineWidth(2.0F);
            Draw.rectStroke(x + (float) touchPixel.x * w, y + (float) touchPixel.y * h, w, h, gl);
        }
    }


    /**
     * the position of a cell's center
     */
    private v2 cell(float x, float y) {
        float W = w();
        float xx = ((x + 0.5f) / (float) (w)) * W;
        float H = h();
        float yy = (1f - ((y + 0.5f) / (float) (h))) * H;
        return new v2(xx, yy);
    }

    /**
     * the prw, prh represent a rectangular size proportional to the displayed cell size
     */
    public RectFloat cellRect(float x, float y, float prw, float prh) {
        float pw = prw / (float) this.w;
        float ph = prh / (float) this.h;
        v2 c = cell(x, y);
        return RectFloat.XYWH(c.x, c.y, pw * w(), ph * h());
    }

    /**
     * must call this to re-generate texture so it will display
     */
    public final boolean updateIfShowing() {
        return showing() && update();
    }

    private final AtomicBoolean busy = new AtomicBoolean();

    protected final boolean update() {

        if (!busy.compareAndSet(false, true))
            return false;

        try {
            if (buf == null) {
                if (w == 0 || h == 0) return false;

                this.buf = new BufferedImage(w, h, alpha() ? TYPE_INT_ARGB : TYPE_INT_RGB);
                this.pix = ((DataBufferInt) buf.getRaster().getDataBuffer()).getData();
            }

            renderView();

            return tex.set(buf);

        } catch (NullPointerException e) {
            //HACK, try again
            return false;
        } finally {
            busy.set(false);
        }

    }

    private void renderView() {
        view.color(buf, pix);
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
            int w = buf.getWidth(), h = buf.getHeight();
            int i = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pix[i++] = color(x, y);
                }
            }
        }
    }

    BitmapMatrixView cellTouch(boolean cellTouch) {
        this.cellTouch = cellTouch;
        return this;
    }
}
