package spacegraph.widget.sketch;

import com.jogamp.opengl.GL2;
import jcog.signal.Bitmap2D;
import jcog.tree.rtree.rect.RectFloat2D;
import org.apache.commons.math3.random.MersenneTwister;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.render.Tex;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.abs;
import static java.lang.Math.min;

public class Sketch2DBitmap extends Sketch2D {

    final Tex bmp = new Tex();
    private final int[] pix;
    private BufferedImage buf;
    private final int pw, ph;

    public float brushWidth = 0.2f, brushAlpha = 0.5f;

    final MersenneTwister rng = new MersenneTwister();
    private GL2 gl;

    public Sketch2DBitmap(int w, int h) {
        this.pw = w;
        this.ph = h;
        this.buf = new BufferedImage(w, h, TYPE_INT_ARGB);
        this.pix = ((DataBufferInt) buf.getRaster().getDataBuffer()).getData();
    }


    /**
     * must call this to re-generate texture so it will display
     */
    public void update() {
        bmp.update(buf);
    }



//    FastBlur fb;

    @Override
    public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {


        if (hitPoint != null && buttons != null && buttons.length > 0 && buttons[0] == 1) {

//            if (fb == null)
//                fb = new FastBlur(pw, ph);

            int ax = Math.round(hitPoint.x * pw);

            int ay = Math.round((1f - hitPoint.y) * ph);

//            int R = Math.round(paintR * 255f);
//            int G = Math.round(paintG * 255f);
//            int B = Math.round(paintB * 255f);
//            int RGB = R << 16 | G << 8 | B;
            float w = this.brushWidth*this.brushWidth;
            float a = brushAlpha * brushAlpha * 10;
            for (int i = 0; i < a; i++) {
                int px = (int) (ax + rng.nextGaussian() * w);
                if (px >= 0 && px < pw) {
                    int py = (int) (ay + rng.nextGaussian() * w);
                    if (py >= 0 && py < ph) {
                        //pix[py * pw + px] = RGB;
                        mix(pix, py * pw + px);
                    }
                }
            }

//gfx.setColor(Color.ORANGE);
//            //gfx.fillOval(ax, ay, 5, 5);

//            if (rng.nextInt(16)==0)
//                fb.blur(pix, pw, ph, 1);

            update();
            return this;
        }

        return super.onTouch(finger, hitPoint, buttons);
    }

    private void mix(int[] pix, int i) {
        int e = pix[i];
        float r = Bitmap2D.decodeRed(e) * 0.5f + paintR * 0.5f;
        float g = Bitmap2D.decodeGreen(e) * 0.5f + paintG * 0.5f;
        float b = Bitmap2D.decodeBlue(e) * 0.5f + paintB * 0.5f;
        int f = Bitmap2D.encodeRGB(r, g, b);
        pix[i] = f;
//            int R = Math.round(paintR * 255f);
//            int G = Math.round(paintG * 255f);
//            int B = Math.round(paintB * 255f);
//            int RGB = R << 16 | G << 8 | B;
    }

    @Override
    protected void paintWidget(GL2 gl, RectFloat2D bounds) {
        if (gl == null) {
            this.gl = gl;
            bmp.profile = gl.getGLProfile();
            update();
        }

        bmp.paint(gl, bounds);
    }

    private float paintR = 0.75f, paintG = 0.75f, paintB = 0.75f;

    /**
     * set paint (foreground) color
     */
    public void color(float r, float g, float b) {
        this.paintR = r;
        this.paintG = g;
        this.paintB = b;
    }



    static class FastBlur {

        private int[][] stack;
        private int[] dv;
        int wm, hm, wh, div, r[], g[], b[], vmin[];

        public FastBlur(int w, int h) {
            wm = w - 1;
            hm = h - 1;
            wh = w * h;

            r = new int[wh];
            g = new int[wh];
            b = new int[wh];
            vmin = new int[Math.max(w, h)];


        }

        /**
         * http://incubator.quasimondo.com/processing/fast_blur_deluxe.php
         */
        void blur(int[] pix, int w, int h, int radius) {
            if (radius < 1) {
                return;
            }

            int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
            yw = yi = 0;

            int stackpointer;
            int stackstart;
            int[] sir;
            int rbs;
            int r1 = radius + 1;
            int routsum, goutsum, boutsum;
            int rinsum, ginsum, binsum;
            div = radius + radius + 1;
            if (stack == null || stack.length != div) {
                stack = new int[div][3];

                int divsum = (div + 1) >> 1;
                divsum *= divsum;
                dv = new int[256 * divsum];
                for (int m = 0; m < 256 * divsum; m++) {
                    dv[m] = (m / divsum);
                }
            }

            for (y = 0; y < h; y++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                for (i = -radius; i <= radius; i++) {
                    p = pix[yi + min(wm, Math.max(i, 0))];
                    sir = stack[i + radius];
                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);
                    rbs = r1 - abs(i);
                    rsum += sir[0] * rbs;
                    gsum += sir[1] * rbs;
                    bsum += sir[2] * rbs;
                    int ds = sir[0] + sir[1] + sir[2];
                    if (i > 0) rinsum += ds;
                    else routsum += ds;
                }
                stackpointer = radius;

                for (x = 0; x < w; x++) {

                    r[yi] = dv[rsum];
                    g[yi] = dv[gsum];
                    b[yi] = dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (y == 0) {
                        vmin[x] = min(x + radius + 1, wm);
                    }
                    p = pix[yw + vmin[x]];

                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[(stackpointer) % div];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];

                    yi++;
                }
                yw += w;
            }
            for (x = 0; x < w; x++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                yp = -radius * w;
                for (i = -radius; i <= radius; i++) {
                    yi = Math.max(0, yp) + x;

                    sir = stack[i + radius];

                    sir[0] = r[yi];
                    sir[1] = g[yi];
                    sir[2] = b[yi];

                    rbs = r1 - abs(i);

                    rsum += r[yi] * rbs;
                    gsum += g[yi] * rbs;
                    bsum += b[yi] * rbs;

                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }

                    if (i < hm) {
                        yp += w;
                    }
                }
                yi = x;
                stackpointer = radius;
                for (y = 0; y < h; y++) {
                    pix[yi] = 0xff000000 | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (x == 0) {
                        vmin[y] = min(y + r1, hm) * w;
                    }
                    p = x + vmin[y];

                    sir[0] = r[p];
                    sir[1] = g[p];
                    sir[2] = b[p];

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[stackpointer];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];

                    yi += w;
                }
            }
        }
    }
}
