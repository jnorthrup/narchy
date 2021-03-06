package nars.sensor;

import jcog.Util;
import jcog.math.v2;
import jcog.random.SplitMix64Random;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.game.Game;
import nars.game.action.ActionSignal;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.util.List;

import static java.lang.Math.round;
import static jcog.Util.lerp;
import static jcog.Util.lerpSafe;

/**
 * 2D flat Raytracing Retina
 */
public class PixelBag implements Bitmap2D {

    private final int px;
    private final int py;

    final SplitMix64Random rng = new SplitMix64Random(1L);

    /**
     * Z = 0: zoomed in all the way
     * = 1: zoomed out all the way
     */
    public final v2 pos = new v2(0.5f, 0.5f);
    public final v2 posNext = new v2(pos);
    private final Bitmap2D src;
    public float Z = 1f;
    public float Znext = Z;


    protected float panRate = 0.5f;
    protected float zoomRate = 0.75f;


    public final float[][] pixels;

    /* > 0 */
    float maxZoom;

    /**
     * increase >1 to allow zoom out beyond input size (ex: thumbnail size)
     */
    float minZoom =
            1f;


    public List<ActionSignal> actions;


    public PixelBag(Bitmap2D src, int px, int py) {
        this.src = src;
        this.px = px;
        this.py = py;
        this.pixels = new float[px][py];
        this.maxZoom = 1f / (float) (Math.min(px, py)); //one pixel observation

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
    public void updateBitmap() {

        src.updateBitmap();

        int sw = sw(), sh = sh();


        pos.move(posNext, panRate);


        //TODO zoom lerp
        Z = Util.lerpSafe(zoomRate, Z, Znext);


        float X = pos.x, Y = pos.y;
        float Z = this.Z;


        float visibleProportion = (float) lerp(Math.sqrt((double) (1 - Z)), (double) maxZoom, (double) minZoom);
        float ew = visibleProportion * (float) (sw);
        float eh = visibleProportion * (float) (sh);


        float minX, maxX, minY, maxY;
        boolean inBoundsOnly = false;
        if (inBoundsOnly) {


            float mw;
            if (ew > (float) sw) {
                mw = (float) 0;
            } else {
                mw = (float) sw - ew;
            }
            float mh;
            if (eh > (float) sh) {
                mh = (float) 0;
            } else {
                mh = (float) sh - eh;
            }
            minX = (X * mw);
            maxX = minX + ew;
            minY = (Y * mh);
            maxY = minY + eh;
        } else {
            minX = (X * (float) sw) - ew / 2f;
            maxX = (X * (float) sw) + ew / 2f;
            minY = (Y * (float) sh) - eh / 2f;
            maxY = (Y * (float) sh) + eh / 2f;
        }

        updateClip(sw, sh, minX, maxX, minY, maxY);
    }

    private void updateClip(int sw, int sh, float minX, float maxX, float minY, float maxY) {

        float px = (float) this.px, py = (float) this.py;
        //float cx = px / 2f, cy = py / 2f;

        float xRange = maxX - minX, yRange = maxY - minY;

        int supersamplingX = (int) Math.floor((double) (xRange / px / 2f)),
                supersamplingY = (int) Math.floor((double) (yRange / py / 2f));

        for (int oy = 0; (float) oy < py; oy++) {
            float sy = (lerpSafe(((float) oy / py), minY, maxY));

            for (int ox = 0; (float) ox < px; ox++) {

                //TODO optimize sources which are already gray (ex: 8-bit grayscale)

                float sx = (lerpSafe((float) (ox) / px, minX, maxX));


                /** sampled pixels in the original image (inclusive) */
                int x1 = Math.max(0, round(sx - (float) supersamplingX));
                int x2 = Math.min(sw - 1, round(sx + (float) supersamplingX + 1.0F));
                int y1 = Math.max(0, round(sy - (float) supersamplingY));
                int y2 = Math.min(sh - 1, round(sy + (float) supersamplingY + 1.0F));

                float v;
                if (x1 == x2 && y1 == y2) {
                    //simple case: the pixel exactly
                    v = src.brightness(x1, y2);
                } //else if (x2 - x1 == 2 && y2 - y1 == 2) {
                //TODO bicubic interpolation
                // }
                else {

                    //generic n-ary interpolation
                    float samples = (float) 0;
                    float brightSum = (float) 0;
                    //float R = 0, G = 0, B = 0;
                    for (int esx = x1; esx <= x2; esx++) {

                        float dpx = (float) esx - sx;

                        for (int esy = y1; esy <= y2; esy++) {

                            //TODO gaussian blur, not just flat average
                            float b = src.brightness(esx, esy);
                            if (b == b) {

                                float dpy = (float) esy - sy;

                                float a = kernelFade(dpx, dpy);
                                brightSum += b * a;
                                samples += a;
                            } //else: random?
                        }
                    }

                    v = (samples > (float) 0) ? brightSum / samples : Float.NaN;
                }


                if (v != v) {
                    v = missing();
                }
                pixels[ox][oy] = v;
            }

        }

    }


    /**
     * cheap sampling approximation
     */
    private static float kernelFade(float dpx, float dpy) {
        float manhattan = Math.abs(dpx) + Math.abs(dpy);
        return manhattan > Float.MIN_NORMAL ? 1f / (1.0F + 4.0F * manhattan) : 1.0F;
    }

    protected float missing() {
        return rng.nextFloat();
        //return Float.NaN;
    }

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

    public PixelBag addActions(Term termRoot, Game a) {
        return addActions(termRoot, a, true, true, true);
    }

    public PixelBag addActions(Term termRoot, Game a, boolean horizontal, boolean vertical, boolean zoom) {
        if (this.actions != null && !this.actions.isEmpty())
            throw new UnsupportedOperationException("actions already added");

        actions = $.INSTANCE.newArrayList(3);

        if (horizontal)
            actions.add(a.actionUnipolar($.INSTANCE.inh(termRoot, Atomic.the("panX")), this::setXRelative));
        else {
            pos.x = posNext.x = 0.5f;
        }

        if (vertical)
            actions.add(a.actionUnipolar($.INSTANCE.inh(termRoot, Atomic.the("panY")), this::setYRelative));
        else {
            pos.y = posNext.y = 0.5f;
        }

        if (zoom) {
            actions.add(a.actionUnipolar($.INSTANCE.inh(termRoot, Atomic.the("zoom")), this::setZoom));
            //minZoom = 1.5f; //expand to allow viewing the entire image as summary
        } else
            Z = Znext = 0.5f;

        return this;
    }

}

