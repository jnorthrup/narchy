package jcog.learn;

import jcog.learn.lstm.Interaction;
import jcog.learn.lstm.test.LiveSTM;
import jcog.list.FasterList;
import jcog.math.FloatDelay;
import jcog.math.FloatSupplier;

import java.util.Collection;

import static jcog.Texts.n4;

/**
 * NOT TESTED YET
 * http://www.jakob-aungiers.com/articles/a/LSTM-Neural-Network-for-Time-Series-Prediction
 * https://medium.com/making-sense-of-data/time-series-next-value-prediction-using-regression-over-a-rolling-window-228f0acae363
 *
 */
public class LivePredictor {


    public interface LivePredictorModel {
        /** may be called at any time to reinitialize the architecture */
        void init(int ins, int inHistory, int outs);

        void learn(double[] ins, double[] outs);

        double[] predict();
    }

    public static class LSTMPredictor implements LivePredictorModel {
        private final int memoryScale;
        float learningRate;
        private LiveSTM net;
        private double[] nextPredictions;

        public LSTMPredictor() {
            this(0.01f, 1);
        }

        public LSTMPredictor(float learningRate, int memoryScale) {
             this.learningRate = learningRate;
             this.memoryScale = memoryScale;
        }

        @Override
        public void init(int numInputs, int iHistory, int numOutputs) {
            synchronized (this) {
                net = new LiveSTM(numInputs * iHistory, numOutputs,
                        numInputs * numOutputs * iHistory * memoryScale) {
                    @Deprecated
                    @Override
                    protected Interaction observe() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }

        @Override
        public void learn(double[] ins, double[] outs) {
            synchronized (this) {
                nextPredictions = net.agent.learn(ins, outs, learningRate);
            }
        }

        @Override
        public double[] predict() {
            return nextPredictions;
        }

    }

    /* TODO public static class NTMPredictor implements LivePredictorModel {

    } */

    public final FloatsDelay Ihistory;

    /** temporary buffers, re-used */
    private double[] ii, oo;

    private final FloatSupplier[] INS;
    private final FloatSupplier[] OUTS;
    public final LivePredictorModel model;


   public LivePredictor(LivePredictorModel model, FloatSupplier[] INS, int iHistory, FloatSupplier[] OUTS) {

        this.INS = INS;
        this.OUTS = OUTS;
        this.Ihistory = FloatsDelay.delay(INS, iHistory);


        this.model = model;
        model.init(INS.length, iHistory, OUTS.length);
    }

    /** delay line of floats (plural, vector) */
    public static class FloatsDelay extends FasterList<FloatDelay> {

        public FloatsDelay(int size) {
            super(size);
        }

        public void next() {
            forEach(FloatDelay::next);
        }

        static FloatsDelay delay(FloatSupplier[] vector, int history) {
            FloatsDelay delayed = new FloatsDelay(vector.length);
            for (FloatSupplier f : vector)
                delayed.add(new FloatDelay(f, history));
            return delayed;
        }

        public void print() {
            forEach(System.out::println);
        }
    }


    public synchronized double[] next() {
        Ihistory.next();

        model.learn(
            ii = historyVector(Ihistory, Ihistory.get(0).data.length, ii),
            oo = vector(OUTS, oo)
        );

        return model.predict();
    }


    static double[] vector(FloatSupplier[] x, double[] d) {
        if (d==null || d.length != x.length) {
            d = new double[x.length];
        }
        for (int k = 0; k < d.length; k++) {
            d[k] = x[k].asFloat();
        }
        return d;
    }

    static double[] historyVector(Collection<? extends FloatDelay> f, int history, double[] d) {
        if (d==null || d.length != f.size() * history) {
            d = new double[f.size() * history];
        }
        int i = 0;
        for (FloatDelay g : f) {
            float[] gd = g.data;
            for (int k = 0; k < gd.length; k++)
                d[i++] = gd[k];
        }
        return d;
    }


}

//    public static double[] d(Collection<? extends FloatSupplier> f) {
//        double[] d = new double[f.size()];
//        int i = 0;
//        for (FloatSupplier g : f)
//            d[i++] = g.asFloat();
//        return d;
//    }

