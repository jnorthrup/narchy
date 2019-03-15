package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.signal.buffer.CircularFloatBuffer;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Stacking;

import java.awt.*;
import java.awt.image.BufferedImage;

public class WaveBitmap extends Stacking implements BitmapMatrixView.BitmapPainter {

    private final int w, h;
    private final CircularFloatBuffer buffer;


    private final transient float yMin;
    private final transient float yMax;
    private transient BitmapMatrixView bmp = null;
    private transient Graphics gfx;

    volatile boolean update = false;

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
    public boolean stop() {
        if (super.stop()) {
            if (gfx != null) {
                gfx.dispose();
                gfx = null;
            }
            if (bmp != null) {
                bmp.stop();
                bmp = null;
            }
            return true;
        }
        return false;
    }

//    @Override
//    protected void paintIt(GL2 gl) {
//        super.paintIt(gl);
//    }

    @Override
    //protected void paintWidget(GL2 g, RectFloat2D bounds) {
    protected void paintIt(GL2 g, SurfaceRender r) {
        if (bmp == null) {
            bmp = new BitmapMatrixView(w, h, this) {
                @Override
                public boolean alpha() {
                    return true;
                }
            };
            set(bmp);
        }

        if (update) {
            update = !bmp.updateIfShowing(); //keep updating till updated
        }

        //bmp.paintMatrix(g);
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

    @Override
    public synchronized void color(BufferedImage buf, int[] pix) {


        if (gfx == null) {
            gfx = buf.getGraphics();
        }

        gfx.clearRect(0, 0, w, h);

        int w = this.w;
        int h = this.h;
        float minValue = this.yMin;
        float maxValue = this.yMax;


//        float yRange = ((maxValue) - (minValue));
        float absRange = Math.max(Math.abs(maxValue), Math.abs(minValue));
        if (absRange < Float.MIN_NORMAL) absRange = 1;


//        float alpha = 0.9f; //1f / ns;

//        this.start = buffer._bufStart;
//        this.end = buffer._bufEnd;
//        System.out.println(start + ".." + end);

        long start = this.start;
        long end = this.end;

//        int sn = buffer.capacity();

        //System.out.println(first + " "+ last);

        for (int x = 0; x < w; x++) {

            float sStart = start + (end - start) * (x/((float)w));
            float sEnd = start + (end - start) * ((x+1)/((float)w));

            float amp = buffer.mean(sStart, sEnd);

            float ampNormalized = (amp) / absRange;

            float intensity = Math.abs(amp) / absRange;

            gfx.setColor(Color.getHSBColor(intensity, 0.7f, 0.7f));

            //float[] sc = s.color();
            //float iBase = Util.unitize(intensity / 2 + 0.5f);
            //gfx.setColor(new Color(sc[0] * iBase, sc[1] * iBase, sc[2] * iBase, alpha));

            int ah = Math.round(ampNormalized * h);
            gfx.drawLine(x, h / 2 - ah / 2, x, h / 2 + ah / 2);
        }
    }

//    private float sampleX(int x, int w, int first, int last) {
//        return ((float) x) / w * (last - first) + first;
//    }

    public synchronized void pan(float pct) {
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

    public synchronized void scale(float pct) {

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
