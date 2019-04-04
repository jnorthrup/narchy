package jcog.learn;

import jcog.TODO;
import jcog.Util;
import jcog.learn.lstm.ExpectedVsActual;
import jcog.learn.lstm.test.LiveSTM;
import jcog.learn.ntm.control.SigmoidActivation;
import jcog.learn.ntm.control.TanhActivation;
import jcog.random.XoRoShiRo128PlusRandom;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;

import java.util.Random;

/**
 * NOT TESTED YET
 * http:
 * https:
 */
public class LivePredictor {



    /** TODO use or rewrite with Tensor api */
    @Deprecated public interface Framer {
        /** computes training vector from current observations */
        double[] inputs(long now);

        double[] outputs();


        double[] shift();
        
    }

    /*public static class Autoregression implements Framer {

    }*/

    public static class DenseShiftFramer implements Framer {
        /** time -> value function, (ex: one per concept) */
        private final LongToFloatFunction[] ins;
        private final LongToFloatFunction[] outs;

        private final int past;
        private final int dur;

        /**
         * temporary buffers, re-used
         */
        private double[] pastVector, present;

        public DenseShiftFramer(LongToFloatFunction[] ins, int past, int sampleDur, LongToFloatFunction[] outs) {
            this.ins = ins;
            this.outs = outs;
            this.past = past;
            this.dur = sampleDur;
        }

        @Override
        public double[] inputs(long now) {
            if (pastVector == null || pastVector.length!=(past * ins.length)) {
                pastVector = new double[past * ins.length];
                present = new double[1 * outs.length];
            }

            int i = 0;
            for (int t = past-1; t >=0; t--) {
                long w = now - (t + 1) * dur;
                for (LongToFloatFunction c : ins) {
                    pastVector[i++] = c.valueOf(w);
                }
            }
            int k = 0;
            for (LongToFloatFunction c : outs) {
                present[k++] = c.valueOf(now);
            }

            return pastVector;
        }

        @Override
        public double[] outputs() {
           return present;
        }

        @Override
        public double[] shift() {
            
            int stride = ins.length;
            int all = pastVector.length;
            System.arraycopy(pastVector, stride, pastVector, 0, all - stride);
            System.arraycopy(present, 0, pastVector, stride, present.length );

            return pastVector;
        }
    }

    public static class LSTMPredictor implements Predictor {
        private final int memoryScale;
        float learningRate;
        public LiveSTM lstm;


        public LSTMPredictor(float learningRate, int memoryScale) {
            this.learningRate = learningRate;
            this.memoryScale = memoryScale;
        }

        @Override
        public String toString() {
            return super.toString() + '[' + lstm + ']';
        }

        @Override
        public void learn(double[] x, double[] y) {
            synchronized (this) {
                ensureSize(x.length, y.length);
                lstm.agent.learn(x, y, learningRate);
            }
        }

        private void ensureSize(int xLen, int yLen) {
            if (lstm == null || lstm.inputs != xLen || lstm.outputs != yLen) {
                lstm = new LiveSTM(xLen, yLen,
                        Math.max(xLen, yLen) * memoryScale) {
                    @Deprecated
                    @Override
                    protected ExpectedVsActual observe() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }

        @Override
        public double[] predict(double[] x) {
            ensureSize(x.length, lstm.outputs);
            return lstm.agent.predict(x);
        }

    }

    public static class NTMPredictor implements Predictor {

        @Override
        public void learn(double[] x, double[] y) {
            throw new TODO();
        }

        @Override
        public double[] predict(double[] x) {
            throw new TODO();
        }
    }

    public static class MLPPredictor implements Predictor {

        private final Random rng;
        float learningRate;

        MLPMap mlp;

        public MLPPredictor(float learningRate) {
            this(learningRate, new XoRoShiRo128PlusRandom(1));
        }

        public MLPPredictor(float learningRate, Random rng) {
            this.learningRate = learningRate;
            this.rng = rng;
        }

        @Override
        public void learn(double[] x, double[] y) {
            if (mlp == null /*|| mlp.inputs()!=ins.length ...*/) {
                 mlp = new MLPMap(rng, x.length,
                         new MLPMap.Layer( (x.length + y.length), TanhActivation.the),
                         new MLPMap.Layer( 2 * (x.length + y.length), SigmoidActivation.the),
                         new MLPMap.Layer( y.length, null)
                 );
            }
            mlp.put(Util.toFloat(x), Util.toFloat(y), learningRate);
        }

        @Override
        public double[] predict(double[] x) {
            return Util.toDouble(mlp.get(Util.toFloat(x)));
        }
    }

    /* TODO public static class NTMPredictor implements Predictor {

    } */


    public final Predictor model;
    public final Framer framer;

    public LivePredictor(Predictor model, Framer framer) {
        this.model = model;
        this.framer = framer;
    }


    public double[] next(long when) {
        synchronized (model) {
            double[] x = framer.inputs(when);
            model.learn(x, framer.outputs());
            framer.shift();

            return model.predict(x);
        }
    }

    /** applies the vector as new hypothetical present inputs,
     * after shifting the existing data (destructively)
     * down one time slot.
     * then prediction can proceed again
     */
    public double[] project(double[] prevOut) {
        synchronized (model) {
            double[] x = ((DenseShiftFramer) framer).pastVector;
            model.learn(x, prevOut);
            double[] nextIn = framer.shift();

            return model.predict(x);
        }
    }


}









