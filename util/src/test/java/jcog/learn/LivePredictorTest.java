package jcog.learn;

import jcog.math.FloatSupplier;
import jcog.math.MutableInteger;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.junit.jupiter.api.Test;

import java.util.DoubleSummaryStatistics;

import static jcog.Texts.n4;
import static org.junit.jupiter.api.Assertions.*;

public class LivePredictorTest {

    @Test public void test1() {
        IntToFloatFunction ii = x -> (float)Math.sin(x/4f);
        IntToFloatFunction oo = x -> (float)Math.cos(x/4f);
        LivePredictor.LSTMPredictor model = new LivePredictor.LSTMPredictor(0.01f, 1);
        int iHistory = 8;
        int errorWindow = 16;
        int totalTime = 8192;
        float maxMeanError = 0.2f;


        assertCorrect(ii, oo, model, iHistory, errorWindow, totalTime, maxMeanError);
    }
    @Test public void test21() {
        IntToFloatFunction ii = x -> (float)Math.sin(x/4f);
        IntToFloatFunction oo = x -> (float)Math.cos(x/8f);
        LivePredictor.LSTMPredictor model = new LivePredictor.LSTMPredictor(0.5f, 1);
        int iHistory = 6;
        int errorWindow = 16;
        int totalTime = 8192*8;
        float maxMeanError = 0.2f;


        assertCorrect(ii, oo, model, iHistory, errorWindow, totalTime, maxMeanError);
    }

    static void assertCorrect(IntToFloatFunction ii, IntToFloatFunction oo, LivePredictor.LSTMPredictor model, int iHistory, int errorWindow, int totalTime, float maxMeanError) {
        MutableInteger m = new MutableInteger();


        FloatSupplier[] in =  { () -> ii.valueOf(m.intValue()) };
        FloatSupplier[] out = { () -> oo.valueOf(m.intValue()) };

        LivePredictor.HistoryFramer ih = new LivePredictor.HistoryFramer(in, iHistory, out);
        LivePredictor l = new LivePredictor(model, ih );

        DescriptiveStatistics error = new DescriptiveStatistics(errorWindow);

        for (int i = 0; i < totalTime; i++) {

            double[] prediction = l.next();

            float[] i0 = ih.data.get(0).data;
            assertEquals(i0[0], in[0].asFloat(), 0.001f);
            if (i > 1) {
                assertEquals(i0[1], ii.valueOf(m.intValue()-1), 0.001f);
            }

            double p0 = prediction[0];
            double a = oo.valueOf(m.intValue()+1);
            //System.out.println(n4(p0) + " , " + n4(a));

            double e = Math.abs(a - p0); //absolute error
            error.addValue(e);
            double eMean = error.getMean();
            //System.out.println(eMean);

            //System.out.print( n4(prediction) + "\t=?=\t");
            m.increment();
            //System.out.println(n4(d(in)) + "\t" + n4(d(out)) );
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