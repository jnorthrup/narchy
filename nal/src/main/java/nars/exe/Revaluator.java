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

import java.util.Random;

import static nars.time.Tense.ETERNAL;

/** TODO refactor into an independent DurService that updates causes with wants */
public interface Revaluator {
    /**
     * goal and goalSummary instances correspond to the possible MetaGoal's enum
     */


    void update(NAR nar);

    final class NullRevaluator implements Revaluator {

        public static final Revaluator the = new NullRevaluator();

        private NullRevaluator() {

        }

        @Override
        public void update(NAR nar) {

        }
    }

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
        public float[] next;
        public Autoencoder ae;
        float NOISE = 0.01f;
        /**
         * hidden to visible neuron ratio
         */
        private final float hiddenMultipler = 0.25f;

        private float[] tmp;

        public AERevaluator(Random rng) {
            super();
            this.momentum.set(0.95f);
            this.rng = rng;
        }

        @Override
        protected void update(float[] val) {

            int numCauses = val.length;
            if (numCauses < 2)
                return;

            if (ae == null || ae.inputs() != numCauses) {
                int numHidden = Math.max(16, Math.round(hiddenMultipler * numCauses));

                ae = new Autoencoder(numCauses, numHidden, rng);
                tmp = new float[numHidden];
            }

            next = ae.reconstruct(val, tmp, true, false);

            float err = ae.put(val, learning_rate, NOISE, 0f, true, false);


        }
    }

    /** exponential decay memory */
    class DefaultRevaluator implements Revaluator {

        final static double minUpdateDurs = 1f;


        public final FloatRange momentum = FloatRange.unit(0.9f);

        volatile long lastUpdate = ETERNAL;
        /**
         * intermediate calculation buffer
         */
        float[] val = ArrayUtils.EMPTY_FLOAT_ARRAY;


        @Override
        public void update(NAR nar) {

            long time = nar.time();
            if (lastUpdate == ETERNAL)
                lastUpdate = time;
            int dur = nar.dur();
            double dt = (time - lastUpdate) / ((double) dur);
            if (dt < minUpdateDurs)
                return;

            FasterList<Cause> causes = nar.causes;
            float[] goal = nar.emotion.want;

            lastUpdate = time;


            int cc = causes.size();

            if (val.length != cc) {
                val = new float[cc];
            }

            for (Cause cause : causes) {
                cause

                        .commitFast();
            }


            int goals = goal.length;


            final float momentum = (float) Math.pow(this.momentum.floatValue(), dt);
            for (int i = 0; i < cc; i++) {
                Cause c = causes.get(i);

                Traffic[] cg = c.goal;


                float v = 0;
                for (int j = 0; j < goals; j++) {
                    v += goal[j] * cg[j].last;
                }


                float prev = val[i];
                float next = momentum * prev + (1f - momentum) * v;
                val[i] = next;
            }

            update(val);

            for (int i = 0; i < cc; i++)
                causes.get(i).setValue(val[i]);
        }

        /**
         * subclasses can implement their own filters and post-processing of the value vector
         */
        protected void update(float[] val) {

        }

    }
}
