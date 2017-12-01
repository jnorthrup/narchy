package jcog.learn;

import jcog.learn.lstm.Interaction;
import jcog.learn.lstm.test.LiveSTM;
import jcog.list.FasterList;
import jcog.math.FloatDelay;
import jcog.math.FloatSupplier;

import java.util.Collection;

/**
 * NOT TESTED YET
 * http://www.jakob-aungiers.com/articles/a/LSTM-Neural-Network-for-Time-Series-Prediction
 * https://medium.com/making-sense-of-data/time-series-next-value-prediction-using-regression-over-a-rolling-window-228f0acae363
 */
public class LivePredictor {


    public interface Framer {
        double[] inputs();

        double[] outputs();
        //double get(boolean inOrOut, int n);
    }

    /*public static class Autoregression implements Framer {

    }*/

    public static class HistoryFramer implements Framer {
        private final FloatSupplier[] ins;
        private final int history;
        public final FloatsDelay data;
        private final FloatSupplier[] outs;

        /**
         * temporary buffers, re-used
         */
        private double[] ii, oo;

        public HistoryFramer(FloatSupplier[] ins, int history, FloatSupplier[] outs) {
            this.ins = ins;
            this.history = history;
            this.data = FloatsDelay.delay(ins, history);
            this.outs = outs;
        }

        @Override
        public double[] inputs() {
            return ii = historyVector(data, history, ii);
        }

        @Override
        public double[] outputs() {
            return oo = vector(outs, oo);
        }
    }

    public interface Predictor {

        void learn(double[] ins, double[] outs);

        double[] predict();
    }

    public static class LSTMPredictor implements Predictor {
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
        public void learn(double[] ins, double[] outs) {
            synchronized (this) {
                if (net == null || net.inputs != ins.length || net.outputs != outs.length) {
                    net = new LiveSTM(ins.length, outs.length,
                            ins.length * outs.length * memoryScale) {
                        @Deprecated
                        @Override
                        protected Interaction observe() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
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


    public final Predictor model;
    public final Framer framer;

    public LivePredictor(Predictor model, Framer framer) {
        this.model = model;
        this.framer = framer;
    }

    /**
     * delay line of floats (plural, vector)
     */
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

        model.learn(framer.inputs(), framer.outputs());

        return model.predict();

    }


    static double[] vector(FloatSupplier[] x, double[] d) {
        if (d == null || d.length != x.length) {
            d = new double[x.length];
        }
        for (int k = 0; k < d.length; k++) {
            d[k] = x[k].asFloat();
        }
        return d;
    }

    static double[] historyVector(Collection<? extends FloatDelay> f, int history, double[] d) {
        if (d == null || d.length != f.size() * history) {
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

