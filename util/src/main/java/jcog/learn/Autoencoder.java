package jcog.learn;

import jcog.Util;
import jcog.pri.ScalarValue;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;

import java.util.Random;

import static java.util.Arrays.fill;

/**
 * Denoising Autoencoder (from DeepLearning.net)
 * <p>
 * TODO parameter for activation function (linear, sigmoid, etc..)
 */
public class Autoencoder {

    static final float NORMALIZATION_EPSILON = (float) Math.sqrt((double) ScalarValue.Companion.getEPSILON());


    /**
     * input vector after preprocessing (noise, corruption, etc..)
     */
    public float[] x;

    /**
     * output vector
     */
    public final float[] y;

    public final float[][] W;

    public final float[] hbias;
    public final float[] vbias;
    public final Random rng;

    public final float[] z;
    private final float[] L_vbias;
    private final float[] L_hbias;

    private float uniform(float min, float max) {
        return rng.nextFloat() * (max - min) + min;
    }

    /*
     * public float binomial(final int n, final float p) { if (p < 0 || p > 1)
     * { return 0; }
     *
     * int c = 0; float r;
     *
     * for (int i = 0; i < n; i++) { r = rng.nextfloat(); if (r < p) { c++; } }
     *
     * return c; }
     */


    public Autoencoder(int ins, int outs, Random rng) {

        x = new float[ins];
        z = new float[ins];
        L_vbias = new float[ins];
        y = new float[outs];
        L_hbias = new float[outs];

        this.rng = rng;

        this.W = new float[outs][ins];
        this.hbias = new float[outs];
        this.vbias = new float[ins];

        randomize();
    }

    public void randomize() {
        float a = 1f / (float) W[0].length;
        for (float[] wi : W) {
            randomize(a, wi);
        }
        fill(hbias, (float) 0);
        fill(L_hbias, (float) 0);
        fill(vbias, (float) 0);
        fill(L_vbias, (float) 0);
    }

    protected void randomize(float a, float[] wi) {
        for (int j = 0; j < W[0].length; j++) {
            wi[j] = uniform(-a, a);
        }
    }

    /** preprocessing filter, applied to each x[]'s value */
    public static float pre(float x) {
        return !Float.isFinite(x) ? (float) 0 : x;
    }

    public static float post(float x) {
        return pre(x);
    }

    private float[] preprocess(float[] x, float noiseLevel, float corruptionRate) {


        Random r = this.rng;
        int ins = x.length;

        float[] xx = this.x;
        for (int i = 0; i < ins; i++) {
            float v = pre(x[i]);
            if ((corruptionRate > (float) 0) && (r.nextFloat() < corruptionRate)) {
                v = (float) 0;
            }
            if (noiseLevel > (float) 0) {
                v +=

                        (r.nextFloat() - 0.5f) * 2.0F * noiseLevel;


            }
            xx[i] = v;
        }

//        for (int i = 0, inputLength = xx.length; i < inputLength; i++)
//            xx[i] = Util.clamp(xx[i], 0, 1f);

        return xx;
    }


    public float[] encode(float[] _x, float[] y, float noise, float corruption, boolean sigmoid, boolean normalize) {

        this.x = _x;

        float[] x = preprocess(_x, noise, corruption);

        float[][] W = this.W;

        int ins = x.length;
        int outs = y.length;


        float[] hbias = this.hbias;

        //float max = Float.NEGATIVE_INFINITY, min = Float.POSITIVE_INFINITY;
        for (int i = 0; i < outs; i++) {
            double yi = (double) hbias[i];
            if (yi!=yi) {
                hbias[i] = (float) 0; //corrupted hbias
                y[i] = (float) 0;
                continue;
            }

            float[] wi = W[i];

            for (int j = 0; j < ins; j++) {
                float wij = wi[j];
                if (wij!=wij)
                    wi[j] = (float) 0; //corrupted weight
                else
                    yi = yi + (double) wij * pre(x[j]);
            }

            //TODO tanH as modular activation functions

//            if (yi > max)
//                max = yi;
//            if (yi < min)
//                min = yi;

            y[i] = post((float)(sigmoid ? Util.sigmoid(yi) : yi));

        }


        if (normalize) {
            float lengthSq = (float) 0;
            for (float v : y) lengthSq += Util.sqr(v);

            if (lengthSq > NORMALIZATION_EPSILON) {
                float length = (float) Math.sqrt((double) lengthSq);
                for (int i = 0; i < outs; i++)
                    y[i] /= length;
            } else {
                for (int i = 0; i < outs; i++)
                    y[i] = (float) 0;
            }


        }


        return y;
    }
//
//    private float cartesianLength(float[] y) {
//        float d = 0;
//        for (float z : y) {
//            d += z * z;
//        }
//        return (float) Math.sqrt(d);
//    }

    /**
     * TODO some or all of the bias vectors may need modified too here
     */
    public void forget(float rate) {
        float mult = 1f - rate;
        float[][] w = this.W;
        for (float[] floats : w)
            Util.mul(mult, floats);
        Util.mul(mult, hbias);
        Util.mul(mult, L_hbias);
        Util.mul(mult, vbias);
        Util.mul(mult, L_vbias);
    }


    public float[] decode(float[] y, boolean sigmoid) {
        float[][] w = W;

        float[] vbias = this.vbias;
        int ins = vbias.length;
        int outs = y.length;
        float[] z = this.z;

        for (int i = 0; i < ins; ) {
            double zi = (double) vbias[i];

            for (int j = 0; j < outs; ) {
                zi = zi + (double) w[j][i] * y[j++];
            }

            zi = sigmoid ?
                    Util.sigmoid(zi)

                    :
                    zi;


            z[i++] = pre((float)zi);
        }

        return z;
    }


    public int outputs() {
        return y.length;
    }

    public float[] output() {
        return y;
    }

    public float put(float[] x, float learningRate,
                     float noiseLevel, float corruptionRate,
                     boolean sigmoid) {
        return put(x, learningRate, noiseLevel, corruptionRate, sigmoid, false, true);
    }

    /**
     * returns the total error (not sqr(error) and not avg_error = error sum divided by # items)
     */
    public float put(float[] x, float learningRate,
                     float noiseLevel, float corruptionRate,
                     boolean sigmoidIn, boolean normalize, boolean sigmoidOut) {

        float[] z = recode(x, noiseLevel, corruptionRate, sigmoidIn, normalize, sigmoidOut);
        //float[] y = encode(x, y, noiseLevel, corruptionRate, sigmoidIn, normalize);
        return put(x, y, learningRate);
    }

    /**
     * returns the total error across all outputs
     */
    float put(float[] x, float[] y, float learningRate) {
        float[][] W = this.W;
        float[] L_hbias = this.L_hbias;
        float[] L_vbias = this.L_vbias;
        float[] vbias = this.vbias;

        int ins = x.length;

        int outs = y.length;

        float error = (float) 0;

        float[] z = this.z;


        for (int i = 0; i < ins; i++) {

            float delta = x[i] - z[i];

            error += Math.abs(delta);
            vbias[i] += learningRate * (L_vbias[i] = delta);
        }

        float[] hbias = this.hbias;


        for (int i = 0; i < outs; i++) {
            L_hbias[i] = 0f;
            float[] wi = W[i];

            double lbi = 0;
            for (int j = 0; j < ins; j++)
                lbi = lbi + (double) wi[j] * L_vbias[j];

            L_hbias[i] += (float)lbi;
            float yi = y[i];
            L_hbias[i] *= yi * (1f - yi);
            hbias[i] += learningRate * L_hbias[i];
        }


        float[] xx = this.x;
        for (int i = 0; i < outs; i++) {
            float yi = y[i];
            float lhb = L_hbias[i];
            float[] wi = W[i];
            for (int j = 0; j < ins; j++) {
                wi[j] += learningRate * (lhb * xx[j] + L_vbias[j] * yi);
            }
        }

        return error;
    }

    public float[] recode(float[] x, boolean sigmoidIn, boolean sigmoidOut) {
        return recode(x, (float) 0, (float) 0, sigmoidIn, false, sigmoidOut);
    }

    public float[] recode(float[] x, float noise, float corruption, boolean sigmoidIn, boolean normalizeIn, boolean sigmoidOut) {
        return decode(encode(x, y, noise, corruption, sigmoidIn, normalizeIn), sigmoidOut);
    }

    public float[] reconstruct(float[] x) {
        return reconstruct(x, new float[this.y.length], true, true);
    }

    public float[] reconstruct(float[] x, float[] yTmp, boolean sigmoidEnc, boolean sigmoidDec) {
        return decode(encode(x,yTmp, (float) 0, (float) 0, sigmoidEnc, false), sigmoidDec);
    }

    /**
     * finds the index of the highest output value, or returns a random one if
     * none are
     */
    public int max() {

        float m = Float.NEGATIVE_INFINITY;
        int best = -1;
        float[] y = this.y;
        int outs = y.length;
        int start = rng.nextInt(outs);
        for (int i = 0; i < outs; i++) {
            int ii = (i + start) % outs;
            float Y = y[ii];
            if (Y > m) {
                m = Y;
                best = ii;
            }
        }
        return best;
    }

    public short[] max(float thresh) {
        float[] y = this.y;
        ShortArrayList s = null;
        int outs = y.length;
        for (int i = 0; i < outs; i++) {
            float Y = y[i];
            if (Y >= thresh) {
                if (s == null)
                    s = new ShortArrayList(3 /* est */);
                s.add((short) i);
            }
        }
        return (s == null) ? null : s.toArray();
    }


    public int hidden() {
        return y.length;
    }

    public int inputs() {
        return x.length;
    }
}
