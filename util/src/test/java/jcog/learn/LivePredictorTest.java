package jcog.learn;

import jcog.math.FloatSupplier;
import jcog.math.MutableInteger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LivePredictorTest {

    @Test public void test1() {
        IntToFloatFunction ii = x -> (float)Math.sin(x/4f);
        IntToFloatFunction oo = x -> (float)Math.cos(x/4f);
        LivePredictor.LSTMPredictor model = new LivePredictor.LSTMPredictor(0.1f, 1);
        int iHistory = 4;
        int errorWindow = 16;
        int totalTime = 8192;
        float maxMeanError = 0.2f;


        assertCorrect(ii, oo, model, iHistory, errorWindow, totalTime, maxMeanError);
    }
    @Test public void test21_LSTM() {
        IntToFloatFunction ii = x -> (float)Math.sin(x/4f);
        IntToFloatFunction oo = x -> (float)Math.cos(x/8f);
        LivePredictor.LSTMPredictor model = new LivePredictor.LSTMPredictor(0.5f, 1);
        int iHistory = 6;
        int errorWindow = 16;
        int totalTime = 8192*8;
        float maxMeanError = 0.1f;


        assertCorrect(ii, oo, model, iHistory, errorWindow, totalTime, maxMeanError);
    }

    @Test public void test12_MLP() {

        IntToFloatFunction ii = x -> (float)Math.sin(x/8f);
        IntToFloatFunction oo = x -> (float)Math.cos(x/8f);
        LivePredictor.MLPPredictor model =
                new LivePredictor.MLPPredictor(0.03f);
        int iHistory = 4;
        int errorWindow = 16;
        int totalTime = 1024;
        float maxMeanError = 0.15f;

        assertCorrect(ii, oo, model, iHistory, errorWindow, totalTime, maxMeanError);
    }

    static void assertCorrect(IntToFloatFunction ii, IntToFloatFunction oo, LivePredictor.Predictor model, int iHistory, int errorWindow, int totalTime, float maxMeanError) {
        MutableInteger m = new MutableInteger();


        FloatSupplier[] in =  { () -> ii.valueOf(m.intValue()),  () -> oo.valueOf(m.intValue()-1) };
        FloatSupplier[] out = { () -> oo.valueOf(m.intValue()) };

        LivePredictor.HistoryFramer ih = new LivePredictor.HistoryFramer(in, iHistory, out);
        LivePredictor l = new LivePredictor(model, ih );

        DescriptiveStatistics error = new DescriptiveStatistics(errorWindow);

        for (int i = 0; i < totalTime; i++, m.increment()) {

            double[] prediction = l.next();

            //test time shift preseves previous value;
            {
                float[] i0 = ih.data.get(0).data;
                assertEquals(i0[0], in[0].asFloat(), 0.001f);
                if (i > 1)
                    assertEquals(i0[1], ii.valueOf(m.intValue() - 1), 0.001f);
            }

            double predicted = prediction[0];
            double actual = oo.valueOf(m.intValue()+1);

            double e = Math.abs(actual - predicted); //absolute error
            error.addValue(e);

           //System.out.println( n4(predicted) + "\t" + n4(actual));
        }

        double eMean = error.getMean();
        assertTrue(eMean < maxMeanError, ()->"mean error: " + eMean);
    }

//    @Test public void testN() {
//        MutableFloat m = new MutableFloat();
//
//        FloatSupplier[] in = {
//                () -> 1f * (m.floatValue() % 2) > 0 ? 1 : -1,
//                () -> 1f * ((m.floatValue() % 3) > 0 ? 1 : -1)
//        };
//        FloatSupplier[] out = {
//                () -> 1f * (((m.floatValue() % 2) + (m.floatValue() % 3)) > 2 ? 1 : -1)
//        };
//        LivePredictor l = new LivePredictor(new LivePredictor.LSTMPredictor(),
//                in,
//                5, out
//        );
//
//        for (int i = 0; i < 1500; i++) {
//            double[] prediction = l.next();
//
//            //System.out.print( n4(prediction) + "\t=?=\t");
//            m.increment();
//            //System.out.println(n4(d(in)) + "\t" + n4(d(out)) );
//        }
//
//    }

    static double[] d(FloatSupplier[] f) {
        double[] d = new double[f.length];
        int i = 0;
        for (FloatSupplier g : f)
            d[i++] = g.asFloat();
        return d;
    }

}