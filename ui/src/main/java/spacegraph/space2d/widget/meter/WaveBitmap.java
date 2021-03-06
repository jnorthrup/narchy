package spacegraph.space2d.widget.meter;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.signal.Tensor;
import jcog.signal.buffer.CircularFloatBuffer;
import spacegraph.space2d.MenuSupplier;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.widget.button.PushButton;

import java.awt.*;
import java.awt.image.BufferedImage;

import static jcog.Util.unitizeSafe;

/** TODO refactor as a Container for the Bitmap matrix */
public class WaveBitmap extends Surface implements BitmapMatrixView.BitmapPainter, MenuSupplier, Timeline2D.TimeRangeAware {

    public final FloatRange height = new FloatRange(0.75f, 0.01f, 1f);
    public final FloatRange alpha = new FloatRange(0.75f, 0.01f, 1f);

    private final int w;
    private final int h;

    @FunctionalInterface
    public interface BitmapEvaluator {
        float amplitude(double start, double end);
    }

    protected final BitmapEvaluator buffer;


    private final transient float yMin;
    private final transient float yMax;
    private transient BitmapMatrixView bmp = null;
    private transient Graphics gfx;

    volatile boolean update = true;

    /**
     * visualization bounds
     */
    public long start;
    public long end;

    public WaveBitmap(int w, int h, BitmapEvaluator buffer) {
        this.w = w;
        this.h = h;
        this.yMin = -1.0F;
        this.yMax = (float) +1;
        this.buffer = buffer;
        this.start = 0L;
        this.end = 1L;
        update();
    }

    public WaveBitmap(Tensor wave, float sampleRate, int pixWidth, int pixHeight) {
        this(pixWidth, pixHeight, new BitmapEvaluator() {
            @Override
            public float amplitude(double s, double e) {
                double ss = s * (double) sampleRate, ee = e * (double) sampleRate;
                double sum = Util.interpSum(wave::getAt, wave.volume(),
                        ss, ee, false);
                return (float) (sum / (1.0 + (ee - ss)));
            }
        });
    }

    public WaveBitmap(CircularFloatBuffer wave, int pixWidth, int pixHeight) {
        this(pixWidth, pixHeight, new BitmapEvaluator() {
            @Override
            public float amplitude(double s, double e) {
                return (float) wave.mean(s, e);
            }
        });
    }

    @Override
    public void setTime(long tStart, long tEnd) {

        synchronized (this) {
            long end = (tEnd);
            long start = (tStart);
            if (update || !Util.equals((float) start, (float) this.start) || !Util.equals((float) end, (float) this.end)) {
                this.start = start;
                this.end = end;
                update = true;
            }
        }
    }

    @Override
    protected void stopping() {
        if (gfx != null) {
            gfx.dispose();
            gfx = null;
        }
        if (bmp != null) {
            bmp.stop();
            bmp = null;
        }
        super.stopping();
    }

    @Override
    protected void render(ReSurface r) {
        if (bmp == null) {
            bmp = new BitmapMatrixView(w, h, this) {
                @Override
                public boolean alpha() {
                    return true;
                }
            };
            bmp.start(this);
        }

        if (update) {
            update = !bmp.updateIfShowing(); //keep updating till updated
        }

        position(bmp);
        bmp.renderIfVisible(r);
    }

    private void position(BitmapMatrixView bmp) {
        if (bmp!=null) {
            float h = height.get();
            bmp.pos(bounds.scale(1.0F, h).move((float) 0, h/ 2.0F /* center vertical align */));
        }
    }

    public void update() {
        update = true;
    }

//    @Deprecated public void updateLive() {
//        updateLive(Integer.MAX_VALUE);
//    }

//    @Deprecated public void updateLive(int lastSamples) {
//        lastSamples = Math.min(buffer.capacity()-1, lastSamples);
//        setTime((this.end - lastSamples), buffer.bufEnd);
//    }

    private static final Color transparent = new Color(0, 0, 0, 0);

    @Override
    public void color(BufferedImage buf, int[] pix) {

        if (gfx == null) {
            gfx = buf.getGraphics();
            ((Graphics2D)gfx).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            ((Graphics2D)gfx).setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            ((Graphics2D)gfx).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            ((Graphics2D)gfx).setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            ((Graphics2D)gfx).setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        }

        ((Graphics2D)gfx).setBackground(transparent);
        gfx.clearRect(0, 0, w, h);

        int w = this.w;
        int h = this.h;
        float minValue = this.yMin, maxValue = this.yMax;

//        float yRange = ((maxValue) - (minValue));
        float absRange = Math.max(Math.abs(maxValue), Math.abs(minValue));
        if (absRange < Float.MIN_NORMAL) {
            //no signal
            //TODO display message
            return;
            //absRange = 1;
        }


//        float alpha = 0.9f; //1f / ns;

//        this.start = buffer._bufStart;
//        this.end = buffer._bufEnd;
//        System.out.println(start + ".." + end);

        double start = (double) this.start, end = (double) this.end;


//        int sn = buffer.capacity();

        //System.out.println(first + " "+ last);

        double range = end - start;
        float W = (float) w;

//        float[] rgba = new float[4];
//        float alpha = this.alpha.get();
        for (int x = 0; x < w; x++) {

            double sStart = start + range * (double) ((float) x / W);
            double sEnd = start + range * (double) ((float) (x + 1) / W);

            float amp = buffer.amplitude(sStart,sEnd);

            float intensity = unitizeSafe(Math.abs(amp) / absRange);

//            Draw.hsb(rgba, intensity, 0.9f, 0.1f*intensity + 0.9f, alpha);
            float ic = intensity * 0.9f + 0.1f;
            Color c = //new Color(unitizeSafe(rgba[0]), unitizeSafe(rgba[1]), unitizeSafe(rgba[2]), unitizeSafe(rgba[3]));
                    new Color(ic, 1.0F -ic, (float) 0);
            gfx.setColor(c);


            //float[] sc = s.color();
            //float iBase = Util.unitize(intensity / 2 + 0.5f);
            //gfx.setColor(new Color(sc[0] * iBase, sc[1] * iBase, sc[2] * iBase, alpha));

            float ampNormalized = (amp) / absRange;

            int ah = Math.round(ampNormalized * (float) h);
            gfx.drawLine(x, h / 2 - ah / 2, x, h / 2 + ah / 2);
        }
    }

//    private float sampleX(int x, int w, int first, int last) {
//        return ((float) x) / w * (last - first) + first;
//    }

    @Override
    public Surface menu() {
        return new Gridding(
                PushButton.awesome("play"),
                PushButton.awesome("microphone"),
                PushButton.awesome("save"), //remember
                PushButton.awesome("question-circle") //recognize

                //TODO trim, etc
        );
    }

}
