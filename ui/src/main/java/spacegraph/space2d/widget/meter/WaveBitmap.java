package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.signal.buffer.CircularFloatBuffer;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.container.unit.MutableUnitContainer;

import java.awt.*;
import java.awt.image.BufferedImage;

import static jcog.Util.unitizeSafe;

public class WaveBitmap extends MutableUnitContainer implements BitmapMatrixView.BitmapPainter, Timeline2D.TimelineRenderable {

    public final FloatRange height = new FloatRange(0.75f, 0.01f, 1f);
    public final FloatRange alpha = new FloatRange(0.75f, 0.01f, 1f);

    private final int w, h;
    private final CircularFloatBuffer buffer;


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

    public WaveBitmap(int w, int h, CircularFloatBuffer buffer) {
        this.w = w;
        this.h = h;
        this.yMin = -1;
        this.yMax = +1;
        this.buffer = buffer;
        this.start = 0;
        this.end = buffer.capacity();
        update();
    }
    @Override
    public void setTime(double tStart, double tEnd) {
        long start = Math.round(tStart);
        long end = Math.round(tEnd);
        if (start!=this.start || end!=this.end) {
            this.start = start; this.end = end;
            update = true;
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
    protected void paintIt(GL2 g, SurfaceRender r) {

    }

    @Override
    protected void compile(SurfaceRender r) {
        if (bmp == null) {
            bmp = new BitmapMatrixView(w, h, this) {
                @Override
                public boolean alpha() {
                    return true;
                }
            };
            position(bmp);
            set(bmp);
        }

        if (update) {
            update = !bmp.updateIfShowing(); //keep updating till updated
        }

        position(bmp);
        super.compile(r);
    }

    private void position(BitmapMatrixView bmp) {
        if (bmp!=null) {
            float h = height.get();
            bmp.pos(bounds.scale(1, h).move(0, h/2 /* center vertical align */));
        }
    }

    public void update() {
        update = true;
    }

    public void updateLive() {
        updateLive(Integer.MAX_VALUE);
    }

    public void updateLive(int lastSamples) {
        lastSamples = Math.min(buffer.capacity()-1, lastSamples);
        this.end = buffer.bufEnd;
        this.start = (this.end - lastSamples);
        update = true;
    }

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
        float minValue = this.yMin;
        float maxValue = this.yMax;

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

        long start = this.start, end = this.end;


//        int sn = buffer.capacity();

        //System.out.println(first + " "+ last);

        long range = end - start;
        float W = w;

//        float[] rgba = new float[4];
//        float alpha = this.alpha.get();
        for (int x = 0; x < w; x++) {

            float sStart = start + range * (x/ W);
            float sEnd = start + range * ((x+1)/ W);

            float amp = buffer.mean(sStart, sEnd);

            float intensity = unitizeSafe(Math.abs(amp) / absRange);

//            Draw.hsb(rgba, intensity, 0.9f, 0.1f*intensity + 0.9f, alpha);
            float ic = intensity * 0.9f + 0.1f;
            Color c = //new Color(unitizeSafe(rgba[0]), unitizeSafe(rgba[1]), unitizeSafe(rgba[2]), unitizeSafe(rgba[3]));
                    new Color(ic, 1-ic, 0);
            gfx.setColor(c);


            //float[] sc = s.color();
            //float iBase = Util.unitize(intensity / 2 + 0.5f);
            //gfx.setColor(new Color(sc[0] * iBase, sc[1] * iBase, sc[2] * iBase, alpha));

            float ampNormalized = (amp) / absRange;

            int ah = Math.round(ampNormalized * h);
            gfx.drawLine(x, h / 2 - ah / 2, x, h / 2 + ah / 2);
        }
    }

//    private float sampleX(int x, int w, int first, int last) {
//        return ((float) x) / w * (last - first) + first;
//    }

    public void pan(float pct) {
        long width = end - start;
        int N = buffer.capacity();
        if (width < N) {
            long mid = ((start + end)/2L);
            long nextMid = Math.round(mid + (pct * width));

            long first = nextMid - width/2;
            long last = nextMid + width/2;
            if (first < 0) {
                first = 0;
                last = first + width;
            } else if (last > N) {
                last = N;
                first = last -width;
            }

            this.start = first;
            this.end = last;
            update();
        }

    }

    public void scale(float pct) {

        long first = this.start, last = this.end;
        long width = last - first;
        long mid = (last + first) / 2;
        long viewNext = Util.clamp(Math.round(width * pct), 2, buffer.capacity());

        first = mid - viewNext / 2;
        last = mid + viewNext / 2;
        if (last > 1) {
            last = 1;
            first = last - viewNext;
        }
        if (first < 0) {
            first = 0;
            last = first + viewNext;
        }

        this.start = first;
        this.end = last;
        update();
    }

}
