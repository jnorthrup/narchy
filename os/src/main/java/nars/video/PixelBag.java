package nars.video;

import jcog.Util;
import jcog.random.SplitMix64Random;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.agent.NAgent;
import nars.concept.action.ActionConcept;
import nars.term.Term;
import nars.term.atom.Atomic;
import spacegraph.util.math.v2;

import java.util.List;

import static jcog.Util.lerp;

/**
 * 2D flat Raytracing Retina
 */
public class PixelBag implements Bitmap2D {

    private final int px;
    private final int py;

    final SplitMix64Random rng = new SplitMix64Random(1);

    /**
     * Z = 0: zoomed in all the way
     * = 1: zoomed out all the way
     */
    public final v2 pos = new v2(0.5f, 0.5f), posNext = new v2(pos);
    private final Bitmap2D src;
    public float Z = 1f, Znext = Z;


    float panSpeed = 0.5f, zoomRate = 0.75f;


    public final float[][] pixels;

    /* > 0 */
    float maxZoom;

    /**
     * increase >1 to allow zoom out beyond input size (ex: thumbnail size)
     */
    float minZoom =
            1f;


    public List<ActionConcept> actions;
    float minClarity = 1f, maxClarity = 1f;
    private final boolean inBoundsOnly = false;


    public PixelBag(Bitmap2D src, int px, int py) {
        this.src = src;
        this.px = px;
        this.py = py;
        this.pixels = new float[px][py];
        this.maxZoom = 1f / (Math.min(px, py)); //one pixel observation

    }

    /**
     * source width, in pixels
     */
    public int sw() {
        return src.width();
    }

    /**
     * source height, in pixels
     */
    public int sh() {
        return src.height();
    }

    @Override
    public void update() {

        src.update();

        int sw = sw(), sh = sh();


        pos.move(posNext, panSpeed);


        //TODO zoom lerp
        Z = Util.lerp(zoomRate, Z, Znext);


        float X = pos.x, Y = pos.y;
        float Z = this.Z;



        float visibleProportion = (float) lerp(Math.sqrt(1 - Z), maxZoom, minZoom);
        float ew = visibleProportion * (sw);
        float eh = visibleProportion * (sh);


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

        updateClip(sw, sh, minX, maxX, minY, maxY);
    }

    private void updateClip(int sw, int sh, float minX, float maxX, float minY, float maxY) {

        float px = this.px, py = this.py;
        float cx = px / 2f, cy = py / 2f;

        float xRange = maxX - minX, yRange = maxY - minY;

        int supersamplingX = (int) Math.floor(xRange / px / 2f),
                supersamplingY = (int) Math.floor(yRange / py / 2f);

        float maxCenterDistanceSq = Math.max(cx, cy) * Math.max(cx, cy) * 2;

        for (int oy = 0; oy < py; oy++) {
            int sy = (int) Math.floor(lerp((oy / py), minY, maxY));

            float dy = Math.abs(oy - cy);
            float yDistFromCenterSq = dy * dy;

            for (int ox = 0; ox < px; ox++) {


                if (minClarity < 1 || maxClarity < 1) {
                    float dx = Math.abs(ox - cx);
                    float distFromCenterSq = dx * dx + yDistFromCenterSq;

                    float clarity = (float) lerp(Math.sqrt(distFromCenterSq / maxCenterDistanceSq), maxClarity, minClarity);
                    if (rng.nextFloat() > clarity)
                        continue;
                }


                //TODO optimize sources which are already gray (ex: 8-bit grayscale)

                int sx = (int) Math.floor(lerp((ox) / px, minX, maxX));

                float samples = 0;
                float brightSum = 0;
                //float R = 0, G = 0, B = 0;
                for (int esx = Math.max(0, sx - supersamplingX); esx <= Math.min(sw - 1, sx + 1 + supersamplingX); esx++) {

                    int dpx = esx - sx;

                    for (int esy = Math.max(0, sy - supersamplingY); esy <= Math.min(sh - 1, sy + 1 + supersamplingY); esy++) {

                        int dpy = esy - sy;

                        //TODO gaussian blur, not just flat average
                        float b = src.brightness(esx, esy);
                        if (b == b) {
                            float a = kernelFade(dpx, dpy);
                            brightSum += b * a;
                            samples += a;
                        } //else: random?
                    }
                }
                pixels[ox][oy] = (samples > 0) ? brightSum / samples : noise();
            }
        }
    }

    /** TODO refine */
    private float kernelFade(int dpx, int dpy) {
        int manhattan = Math.abs(dpx) + Math.abs(dpy);
        return manhattan > 0 ? 1f/(1+manhattan*manhattan) : 1;
    }

    protected float noise() {
        return rng.nextFloat();
        //return Float.NaN;
    }

    public void setClarity(float minClarity, float maxClarity) {
        this.minClarity = minClarity;
        this.maxClarity = maxClarity;
    }

//    abstract public int rgb(int sx, int sy);

    public void setMaxZoom(float maxZoom) {
        this.maxZoom = maxZoom;
    }

    public void setMinZoom(float minZoom) {
        this.minZoom = minZoom;
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


    public float setZoom(float f) {
        Znext = (Util.unitize(f));
        return f;
    }

    public float setYRelative(float f) {
        posNext.y = f;
        return f;
    }

    public float setXRelative(float f) {
        posNext.x = f;
        return f;
    }

    public PixelBag addActions(Term termRoot, NAgent a) {
        return addActions(termRoot, a, true, true, true);
    }

    public PixelBag addActions(Term termRoot, NAgent a, boolean horizontal, boolean vertical, boolean zoom) {
        if (this.actions != null && !this.actions.isEmpty())
            throw new UnsupportedOperationException("actions already added");

        actions = $.newArrayList(3);

        if (horizontal)
            actions.add(a.actionUnipolar($.p(termRoot, Atomic.the("panX")), this::setXRelative));
        else {
            pos.x = posNext.x = 0.5f;
        }

        if (vertical)
            actions.add(a.actionUnipolar($.p(termRoot, Atomic.the("panY")), this::setYRelative));
        else {
            pos.y = posNext.y = 0.5f;
        }

        if (zoom) {
            actions.add(a.actionUnipolar($.p(termRoot, Atomic.the("zoom")), this::setZoom));
            //minZoom = 1.5f; //expand to allow viewing the entire image as summary
        } else
            Z = Znext = 0.5f;


        return this;
    }

}

