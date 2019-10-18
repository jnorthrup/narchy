package nars.control;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.learn.MLPMap;
import jcog.learn.ntm.control.SigmoidActivation;
import jcog.pri.ScalarValue;
import jcog.util.ArrayUtil;
import nars.NAR;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static jcog.Util.lerpSafe;

/** predictor for why's (what should be done)
 *  analogous to OS governor policy that decides priorities for the various cause's
 */
public enum Should { ;
	@Deprecated static float momentum = 0.5f;
	static float explorationRate = 0.01f;

	/** uses a small MLP for each cause to predict its value for the current metagoal vector */
	public static final BiConsumer<NAR,FasterList<Cause>> predictMLP = new BiConsumer<>() {

		float learningRate = 0.05f;

		float[] f = ArrayUtil.EMPTY_FLOAT_ARRAY;
		float[] fNorm = ArrayUtil.EMPTY_FLOAT_ARRAY;
		Predictor[] predictor = new Predictor[0];

		@Deprecated final int dims = MetaGoal.values().length;

		/** value vector -> priority for an individual cause, independently learned
		 *  1 hidden layer to be able to solve ~XOR problem
		 *  "A limitation of single layer perceptron is that it is only capable of separating data points with a single line."
		 *  http://www.mlopt.com/?p=160
		 * */
		final class Predictor extends MLPMap {
			Predictor() {
				super(dims,
					new MLPMap.Layer(
						dims, SigmoidActivation.the),
//						//Math.max(2, dims/2)
//						, /*TanhActivation.the*/   ),
					new MLPMap.Layer( 1,
						SigmoidActivation.the
						//null
					)
				);

			}
		}

		private void allocate(NAR n, int ww) {
			f = new float[ww];
			fNorm = new float[ww];
			predictor = new Predictor[ww];
			Random rng = n.random();
			for (int p = 0; p < ww; p++) {
				Predictor pp = new Predictor();
				pp.randomize(rng);
				predictor[p] = pp;
			}
		}

		@Override
		public void accept(NAR n, FasterList<Cause> cc) {



			Cause[] c = cc.array();
			int ww = Math.min(c.length, cc.size());
			if (f.length != ww) {
				allocate(n, ww);
			}

			float[] want = n.emotion.want.clone();
			Util.normalizeCartesian(want);


			//2. learn
			float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
			for (int i = 0; i < ww; i++) {
				float v = c[i].value;
				//v = Math.max(0, v); //clip at 0
				f[i] = v;
				min = Math.min(v, min);
				max = Math.max(v, max);
			}

			float range = max - min;
			if (range < ScalarValue.EPSILON) {
//                        float flat = 1f/ww;
//                        for (int i = 0; i < ww; i++)
//                            c[i].pri(flat);
			}

			{


				double errTotal = 0;
				for (int i = 0; i < ww; i++) {

					fNorm[i] = Util.normalizeSafe(f[i], min, max);

					float specificLearningRate =
						learningRate;
//						learningRate * Math.max(0.1f,
//							//Math.abs(0.5f-fNorm[i])*2f
//							Math.abs(f[i]) / Math.max(Math.abs(min), Math.abs(max)) //extremeness
//						);

					Predictor P = this.predictor[i];

					float[] out = P.put(want, new float[] { fNorm[i] }, specificLearningRate);

					float p = Util.unitize(out[0]);
					float pri = p * (1-explorationRate) + explorationRate;
					c[i].pri(pri);

					errTotal += P.errorAbs();
				}

				double errAvg = errTotal / ww;

				//if(PRINT_AVG_ERR && n.random().nextFloat() < 0.03f)
					//System.out.println(this + ":\t" + errAvg + " avg err");
			}
			//System.out.println(n4(min) + " " + n4(max) + "\t" + n4(nmin) + " " + n4(nmax));


		}



	};

	/** applies simple LERP memory to each computed priority */
	public static final Consumer<FasterList<Cause>> normalizePri = new Consumer<>() {

		float[] f = ArrayUtil.EMPTY_FLOAT_ARRAY;
		float[] fNorm = ArrayUtil.EMPTY_FLOAT_ARRAY;

		@Override
		public void accept(FasterList<Cause> cc) {

			Cause[] c = cc.array();
			int ww = Math.min(c.length, cc.size());
			if (f.length != ww) {
				f = new float[ww];
				fNorm = new float[ww];
			}


			float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
			for (int i = 0; i < ww; i++) {
				float v = c[i].value;
				v = Math.max(0, v); //clip at 0
				f[i] = v;
				min = Math.min(v, min);
				max = Math.max(v, max);
			}

			if (Util.equals(min, max)) {
//                        float flat = 1f/ww;
//                        for (int i = 0; i < ww; i++)
//                            c[i].setPri(flat);
			} else {
				for (int i = 0; i < ww; i++) {
					float v = f[i];
					float vNorm = Util.normalizeSafe(v, min, max);
					fNorm[i] = lerpSafe(momentum, vNorm, fNorm[i]);
				}
			}
			//System.out.println(n4(min) + " " + n4(max) + "\t" + n4(nmin) + " " + n4(nmax));

			float range = 1 - explorationRate;
			for (int i = 0; i < ww; i++)
				c[i].pri(explorationRate + range * fNorm[i]);

		}

	};


	/** applies simple LERP memory to each computed value */
	static final Consumer<FasterList<Cause>> normalizeValue = new Consumer<>() {

		float[] f = ArrayUtil.EMPTY_FLOAT_ARRAY;

		@Override
		public void accept(FasterList<Cause> cc) {

			Cause[] c = cc.array();
			int ww = Math.min(c.length, cc.size());
			if (f.length != ww)
				f = new float[ww];

			float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
			for (int i = 0; i < ww; i++) {
				float r = c[i].value;

				float v;
				if (r == r)
					f[i] = v = lerpSafe(momentum, r, f[i]);
				else
					v = f[i]; //unchanged, hold existing value

				min = Math.min(v, min);
				max = Math.max(v, max);
			}

//                    System.out.println(min + "\t" + max);

			if (Util.equals(min, max)) {
				for (int i = 0; i < ww; i++)
					c[i].pri(explorationRate); //flat
			} else {
				float range = 1 - explorationRate;
				for (int i = 0; i < ww; i++)
					c[i].pri(explorationRate + range * Util.normalizeSafe(f[i], min, max));
			}
		}

	};

}
