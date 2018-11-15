package nars.video;

import jcog.Util;
import jcog.random.XorShift128PlusRandom;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.agent.NAgent;
import nars.concept.action.ActionConcept;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static jcog.Util.lerp;

/**
 * 2D flat Raytracing Retina
 */
public abstract class PixelBag implements Bitmap2D {

    private final int px;
    private final int py;

    final Random rng = new XorShift128PlusRandom(1);

    /**
     * Z = 0: zoomed in all the way
     * = 1: zoomed out all the way
     */
    public float X;
    public float Y;
    public float Z;






    public final float[][] pixels;

    /* > 0 */
    float minZoomOut = 0.05f;

    /**
     * increase >1 to allow zoom out beyond input size (ex: thumbnail size)
     */
    float maxZoomOut =
            1.0f;
            

    public boolean vflip;
    public List<ActionConcept> actions;
    private float fr = 1f;
    private float fg = 1f;
    private float fb = 1f;
    float minClarity = 1f, maxClarity = 1f;
    private final boolean inBoundsOnly = false; 


    public static PixelBag of(Supplier<BufferedImage> bb, int px, int py) {
        return new PixelBag(px, py) {

            public BufferedImage b;

            @Override
            public int sw() {
                return b.getWidth();
            }

            @Override
            public int sh() {
                return b.getHeight();
            }

            @Override
            public void update() {
                b = bb.get();
                if (b != null) {
                    synchronized (this) {
                        super.update();
                    }
                }

            }

            @Override
            public int rgb(int sx, int sy) {
                return b.getRGB(sx, sy);
            }
        };
    }

    public PixelBag(int px, int py) {
        this.px = px;
        this.py = py;
        this.pixels = new float[px][py];

        this.Z = 1f;
    }

    /**
     * source width, in pixels
     */
    abstract public int sw();

    /**
     * source height, in pixels
     */
    abstract public int sh();

    @Override
    public void update() {

        int sw = sw(), sh = sh();

        float ew, eh;


        float z = lerp(Z, maxZoomOut, minZoomOut);
        ew = z * sw;
        eh = z * sh;


        float minX, maxX, minY, maxY;
        if (inBoundsOnly) {
            
            
            float mw, mh;
            if (ew > sw) {
                mw = 0;
            } else {
                mw = sw - ew;
            }
            if (eh > sh) {
                mh = 0;
            } else {
                mh = sh - eh;
            }
            minX = (X * mw);
            maxX = minX + ew;
            minY = (Y * mh);
            maxY = minY + eh;
        } else {
            minX = (X * sw) - ew / 2f;
            maxX = (X * sw) + ew / 2f;
            minY = (Y * sh) - eh / 2f;
            maxY = (Y * sh) + eh / 2f;
        }

        

        float cx = px / 2f;
        float cy = py / 2f;



        float pxf = px - 1;
        float pyf = py - 1;

        float fr = this.fr, fg = this.fg, fb = this.fb;
        float fSum = fr + fg + fb;

        float xRange = maxX - minX;
        float yRange = maxY - minY;

        updateClip(sw, sh, minX, maxX, minY, maxY, cx, cy, pxf, pyf, fr, fg, fb, fSum, xRange, yRange);
    }

    private void updateClip(int sw, int sh, float minX, float maxX, float minY, float maxY, float cx, float cy, float pxf, float pyf, float fr, float fg, float fb, float fSum, float xRange, float yRange) {
        int supersampling = Math.min((int) Math.floor(xRange / px / 2f), (int) Math.floor(yRange / py / 2f));

        
        
        float maxCenterDistanceSq = Math.max(cx, cy) * Math.max(cx, cy) * 2;

        for (int ly = 0; ly < py; ly++) {
            float l = ly / pyf;
            int sy = Math.round(lerp(!vflip ? l : 1f - l, minY, maxY));

            float dy = Math.abs(ly - cy);
            float yDistFromCenterSq = dy * dy;

            for (int lx = 0; lx < px; lx++) {








                


                if (minClarity <1 ||maxClarity < 1) {
                    float dx = Math.abs(lx - cx);
                    float distFromCenterSq = dx * dx + yDistFromCenterSq; 

                    float clarity = (float) lerp(Math.sqrt(distFromCenterSq / maxCenterDistanceSq), maxClarity, minClarity);
                    if (rng.nextFloat() > clarity)
                        continue;
                }

                
                int sx = Math.round(lerp(lx / pxf, minX, maxX));

                int samples = 0;
                float R = 0, G = 0, B = 0;
                for (int esx = Math.max(0, sx - supersampling); esx <= Math.min(sw - 1, sx + supersampling); esx++) {

                    if (esx < 0 || esx >= sw)
                        continue;

                    for (int esy = Math.max(0, sy - supersampling); esy <= Math.min(sh - 1, sy + supersampling); esy++) {
                        if (esy < 0 || esy >= sh)
                            continue;

                        int RGB = rgb(esx, esy);
                        R += Bitmap2D.decode8bRed(RGB);
                        G += Bitmap2D.decode8bGreen(RGB);
                        B += Bitmap2D.decode8bBlue(RGB);
                        samples++;
                    }
                }
                float v = (samples == 0) ? 0.5f : (fr * R + fg * G + fb * B) / fSum / samples;
                pixels[lx][ly] = v;
            }
        }
    }

    public void setClarity(float minClarity, float maxClarity) {
        this.minClarity = minClarity;
        this.maxClarity = maxClarity;
    }

    abstract public int rgb(int sx, int sy);

    public void setMinZoomOut(float minZoomOut) {
        this.minZoomOut = minZoomOut;
    }

    public void setMaxZoomOut(float maxZoomOut) {
        this.maxZoomOut = maxZoomOut;
    }

    @Override
    public int width() {
        return px;
    }

    @Override
    public int height() {
        return py;
    }

    @Override
    public float brightness(int xx, int yy) {
        return pixels[xx][yy];
    }

    static float u(float f) {
        return f / 2f + 0.5f;
    }

    public float setZoom(float f) {
        Z = (float)Math.sqrt(Util.unitize(f)); 
        
        return f;
    }

    public float setYRelative(float f) {
        Y = f;
        return f;
    }

    public float setXRelative(float f) {
        X = f;
        return f;
    }

    public void setFilter(float r, float g, float b) {
        this.fr = r;
        this.fg = g;
        this.fb = b;
    }

    public boolean setRedFilter(float f) {
        this.fr = f;
        return true;
    }

    public boolean setGreenFilter(float f) {
        this.fr = f;
        return true;
    }

    public boolean setBlueFilter(float f) {
        this.fr = f;
        return true;
    }


    public PixelBag addActions(Term termRoot, NAgent a) {
        return addActions(termRoot, a, true, true, true);
    }

    public PixelBag addActions(Term termRoot, NAgent a, boolean horizontal, boolean vertical, boolean zoom) {
        actions = $.newArrayList(3);

        if (horizontal)
            actions.add(a.actionUnipolar($.p( termRoot, Atomic.the("panX")), this::setXRelative));
        else
            X = 0.5f;

        if (vertical)
            actions.add(a.actionUnipolar($.p( termRoot, Atomic.the("panY")), this::setYRelative));
        else
            Y = 0.5f;

        if (zoom)
            actions.add(a.actionUnipolar($.p( termRoot, Atomic.the("zoom")), this::setZoom));
        else
            Z = 0.5f;




        return this;
    }

}

