package nars.exe;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.learn.Autoencoder;
import jcog.learn.deep.RBM;
import jcog.math.FloatRange;
import nars.NAR;
import nars.control.Cause;
import nars.control.Traffic;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Random;

import static nars.time.Tense.ETERNAL;

/** TODO refactor into an independent DurService that updates causes with wants */
public interface Revaluator {
    /**
     * goal and goalSummary instances correspond to the possible MetaGoal's enum
     */


    void update(NAR nar);


    /**
     * uses an RBM as an adaptive associative memory to learn and reinforce the co-occurrences of the causes
     * the RBM is an unsupervised network to learn and propagate co-occurring value between coherent Causes
     */
    class RBMRevaluator extends DefaultRevaluator {

        private final Random rng;
        public double[] next;

        /**
         * learning iterations applied per NAR cycle
         */
        public int learning_iters = 1;

        public double learning_rate = 0.05f;

        public double[] cur;
        public RBM rbm;
        float rbmStrength = 0.25f;
        /**
         * hidden to visible neuron ratio
         */
        private final float hiddenMultipler = 1f;

        public RBMRevaluator(Random rng) {
            super();
            this.rng = rng;
        }

        @Override
        public void update(NAR nar) {
            super.update(nar);

            FasterList<Cause> causes = nar.causes;
            int numCauses = causes.size();
            if (numCauses < 2)
                return;

            if (rbm == null || rbm.n_visible != numCauses) {
                int numHidden = Math.round(hiddenMultipler * numCauses);

                rbm = new RBM(numCauses, numHidden, null, null, null, rng) {
                };
                cur = new double[numCauses];
                next = new double[numCauses];
            }


            for (int i = 0; i < numCauses; i++)
                cur[i] = Util.tanhFast(causes.get(i).value());

            rbm.reconstruct(cur, next);
            rbm.contrastive_divergence(cur, learning_rate, learning_iters);


            for (int i = 0; i < numCauses; i++) {

                float j = /*((rng.nextFloat()-0.5f)*2*noise)*/ +


                        (float) ((1f - rbmStrength) * cur[i] + rbmStrength * next[i]);
                causes.get(i).setValue(j);
            }
        }
    }

    /**
     * denoising autoencoder revaluator
     */
    class AERevaluator extends DefaultRevaluator {

        private final Random rng;


        public float learning_rate = 0.1f;
        public Autoencoder ae;
        float NOISE = 0.01f;
        /**
         * hidden to visible neuron ratio; determines amount of dimensionality reduction
         */
        private final float hiddenMultipler = 0.1f;

        private float[] tmp;

        public AERevaluator(Random rng) {
            super();
            this.momentum.set(0.5f);
            this.rng = rng;
        }

        @Override
        protected float[] update(float[] val) {

            int numCauses = val.length;
            if (numCauses < 2)
                return val;

            boolean sigmoidEnc = true;
            boolean sigmoidDec = false;
            float[] next = ae.reconstruct(val, tmp, sigmoidEnc, sigmoidDec);

            float err = ae.put(val, learning_rate, NOISE, 0f, sigmoidEnc, false, sigmoidDec);

            //System.out.println(this + "  " + err);

            return next;
        }

        @Override
        protected void resize(int causes) {
            super.resize(causes);
            if (ae == null || ae.inputs() != causes) {
                int numHidden = Math.max(2, Math.round(hiddenMultipler * causes));

                ae = new Autoencoder(causes, numHidden, rng);
                tmp = new float[numHidden];
            }
        }
    }

    /** exponential decay memory */
    class DefaultRevaluator implements Revaluator {



        public final FloatRange momentum = FloatRange.unit(0f);

        volatile long lastUpdate = ETERNAL;
        /**
         * intermediate calculation buffer
         */
        float[] val = ArrayUtils.EMPTY_FLOAT_ARRAY;

        public DefaultRevaluator() {
            this(0.5f);
        }

        public DefaultRevaluator(float momentum) {
            this.momentum.set(momentum);
        }

        @Override
        public void update(NAR nar) {

            long time = nar.time();
            if (lastUpdate == ETERNAL)
                lastUpdate = time;
            int dur = nar.dur();
            double dt = (time - lastUpdate) / ((double) dur);


            lastUpdate = time;

            FasterList<Cause> causes = nar.causes;


            int cc = causes.size();
            if (cc == 0)
                return;

            if (val.length != cc) {
                resize(cc);
            }

            Cause[] ccc = causes.array();

            float[] want = nar.emotion.want;
            if (Util.and(want, (float w) -> Util.equals(Math.abs(w), Float.MIN_NORMAL))) {
                Arrays.fill(val, 0);
                return; //no effect
            } else {

                final float momentum = (float) Math.pow(this.momentum.floatValue(), dt);
                for (int i = 0; i < cc; i++) {

                    Traffic[] cg = ccc[i].goal;

                    ccc[i].commitFast();

                    float v = 0;
                    for (int j = 0; j < want.length; j++) {
                        v += want[j] * cg[j].last;
                    }

                    float next = momentum > 0 ? momentum * val[i] + (1f - momentum) * v : v;
                    assert(next==next);
                    val[i] = next;
                }
            }

            float[] post = update(val);

            for (int i = 0; i < cc; i++)
                ccc[i].setValue(post[i]);
        }

        protected void resize(int causes) {
            val = new float[causes];
        }

        /**
         * subclasses can implement their own post-processing filter chain of the value vector
         */
        protected float[] update(float[] val) {
            return val;
        }

    }
}
