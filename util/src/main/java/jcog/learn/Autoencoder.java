package jcog.learn;

import jcog.Util;
import jcog.decide.Deciding;
import jcog.pri.Pri;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;

import java.util.Random;

import static java.util.Arrays.fill;

/**
 * Denoising Autoencoder (from DeepLearning.net)
 * 
 * TODO parameter for activation function (linear, sigmoid, etc..)
 */
public class Autoencoder {

	final static float NORMALIZATION_EPSILON = Pri.EPSILON*2;


	/** input vector after preprocessing (noise, corruption, etc..) */
	public final float[] xx;

	/** output vector */
	final public float[] y;

	public final float[][] W;

	private final float[] hbias;
	private final float[] vbias;
	private final Random rng;

	public final float[] z;
	final private float[] L_vbias;
	final private float[] L_hbias;

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

		xx = new float[ins];
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
		float a = 1f / W[0].length;
		for (int i = 0; i < W.length; i++) {
			float[] wi = this.W[i];
			randomize(a, wi);
		}
		fill(hbias, 0);
		fill(L_hbias, 0);
		fill(vbias, 0);
		fill(L_vbias, 0);
    }

	protected void randomize(float a, float[] wi) {
		for (int j = 0; j < W[0].length; j++) {
            wi[j] = uniform(-a, a);
        }
	}

	private float[] preprocess(float[] x, float noiseLevel, float corruptionRate) {



        Random r = this.rng;
		int ins = x.length;

		float[] xx = this.xx;
		for (int i = 0; i < ins; i++) {
			float v = x[i];
            if ((corruptionRate > 0) && (r.nextFloat() < corruptionRate)) {
				v = 0;
			}
			if (noiseLevel > 0) {
				v +=
					//r.nextGaussian() * noiseLevel;
					(r.nextFloat()-0.5f) * 2 * noiseLevel; //uniform

//				if (nx < 0)
//					nx = 0;
//				if (nx > 1)
//					nx = 1;
			}
			xx[i] = v;
		}

		for (int i = 0, inputLength = xx.length; i < inputLength; i++)
            xx[i] = Util.clamp(xx[i], 0, 1f); //safety

		return xx;
	}

	// Encode
	public float[] encode(float[] x, float[] y, boolean sigmoid, boolean normalize) {

		float[][] W = this.W;

		int ins = x.length;
		int outs = y.length;

//		if (y == null)
//			y = new float[outs];

		float[] hbias = this.hbias;

		float max = Float.NEGATIVE_INFINITY, min = Float.POSITIVE_INFINITY;
		for (int i = 0; i < outs; i++) {
			float yi = hbias[i];
			float[] wi = W[i];

			for (int j = 0; j < ins; j++) {
				yi += wi[j] * x[j];
			}

			if (sigmoid)
				yi = Util.sigmoid(yi);


			if (yi > max)
				max = yi;
			if (yi < min)
				min = yi;

			y[i] = yi;

		}



		if (normalize) {
			float lengthSq = 0;
			for (int i = 0; i < outs; i++) {
				lengthSq += Util.sqr(y[i]);
			}
			if (lengthSq > NORMALIZATION_EPSILON*NORMALIZATION_EPSILON) {
				float length = (float) Math.sqrt(lengthSq);
				assert(length > Pri.EPSILON);
				for (int i = 0; i < outs; i++) {
					y[i] /= length;
				}
			}

//			float maxMin = max - min;
//			if (maxMin > NORMALIZATION_EPSILON) {
//
//				for (int i = 0; i < outs; i++) {
//					y[i] = Util.clamp((y[i] - min) / maxMin, 0f, +1f);
//				}
//			 else {
//				//fill(y, 0);
//				randomize(1f / y.length, y);
//			}

//to check unit result:
//			float len = cartesianLength(y);
//			if (len > 0) {
//				for (int i = 0; i < nh; i++) {
//					y[i] = y[i] / len;
//				}
//				System.out.println(Arrays.toString(y) + " " + len + " " + cartesianLength(y));
//			}

//			for (int i = 0; i < nh; i++) {
//				y[i] = (y[i] - min) / (max-min);
//			}

		}


		return y;
	}

	private float cartesianLength(float[] y) {
		float d = 0;
		for (float z : y) {
			d += z*z;
		}
		return (float)Math.sqrt(d);
	}

	/** TODO some or all of the bias vectors may need modified too here */
	public void forget(float rate) {
		float mult = 1f - rate;
		float[][] w = this.W;
		int O = w.length;
		for (int o = 0; o < O; o++)
			Util.mul(mult, w[o]);
		Util.mul(mult, hbias);
		Util.mul(mult, this.L_hbias);
		Util.mul(mult, vbias);
		Util.mul(mult, this.L_vbias);
	}

	// Decode
	public float[] decode(float[] y, boolean sigmoid) {
		float[][] w = W;

		float[] vbias = this.vbias;
		int ins = vbias.length;
		int outs = y.length;
		float[] z = this.z;

		for (int i = 0; i < ins; ) {
			float zi = vbias[i];

			for (int j = 0; j < outs; ) {
				zi += w[j][i] * y[j++];
			}

			zi = sigmoid ?
					Util.sigmoid(zi)
					//(Util.tanhFast(zi) + 1) / 2f
					:
					zi;


			z[i++] = zi;
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
		return put(x, learningRate, noiseLevel, corruptionRate, sigmoid, sigmoid);
	}

	public float put(float[] x, float learningRate,
					 float noiseLevel, float corruptionRate,
					 boolean sigmoidEnc, boolean sigmoidDec) {
		return put(x, learningRate, noiseLevel, corruptionRate, sigmoidEnc, true, sigmoidDec);
	}

	/** returns the total error (not sqr(error) and not avg_error = error sum divided by # items) */
	public float put(float[] x, float learningRate,
					 float noiseLevel, float corruptionRate,
					 boolean sigmoidIn, boolean normalize, boolean sigmoidOut) {

		recode(preprocess(x, noiseLevel, corruptionRate), sigmoidIn, normalize, sigmoidOut);
		return put(x, y, learningRate);
	}

	/** returns the total error across all outputs */
	float put(float[] x, float[] y, float learningRate) {
		float[][] W = this.W;
		float[] L_hbias = this.L_hbias;
		float[] L_vbias = this.L_vbias;
		float[] vbias = this.vbias;

		int ins = x.length;

		int outs = y.length;

		float error = 0;

		float[] z = this.z;

		// vbias
		for (int i = 0; i < ins; i++) {

			float delta = x[i] - z[i];

			error += Math.abs(delta);
			vbias[i] += learningRate * (L_vbias[i] = delta);
		}

		float[] hbias = this.hbias;


		// hbias
		for (int i = 0; i < outs; i++) {
			L_hbias[i] = 0f;
			float[] wi = W[i];

			float lbi = 0f;
			for (int j = 0; j < ins; j++) {
				lbi += wi[j] * L_vbias[j];
			}
			L_hbias[i] += lbi;

			float yi = y[i];
			L_hbias[i] *= yi * (1f - yi);
			hbias[i] += learningRate * L_hbias[i];
		}

		// W
		float[] xx = this.xx;
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

	public float[] recode(float[] x, boolean sigmoidIn, boolean normalize, boolean sigmoidOut) {
		return decode(encode(x, y, sigmoidIn, normalize), sigmoidOut);
	}

	public float[] reconstruct(float[] x) {
		float[] y = new float[this.y.length];

		return reconstruct(x, y, true, true);
	}

	public float[] reconstruct(float[] x, float[] yTmp, boolean sigmoidEnc, boolean sigmoidDec) {
		return decode(encode(x, yTmp, sigmoidEnc, true), sigmoidDec);
	}

	public int decide(Deciding d) {
		return d.decide(y, -1);
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
		int start = rng.nextInt(outs); //random starting point to give a fair chance to all if the value is similar
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
				s.add((short)i);
			}
		}
		return (s == null) ? null : s.toArray();
	}


	public int hidden() {
		return y.length;
	}

	public int inputs() {
		return xx.length;
	}
}
