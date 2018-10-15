package jcog.learn;

import com.google.common.math.PairedStatsAccumulator;
import jcog.math.FloatSupplier;
import jcog.math.MutableInteger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LivePredictorTest {

    @Test
    void test1() {
        IntToFloatFunction ii = x -> (float)Math.sin(x/4f);
        IntToFloatFunction oo = x -> (float)Math.cos(x/4f);
        LivePredictor.LSTMPredictor model = new LivePredictor.LSTMPredictor(0.1f, 1);
        int iHistory = 4;
        int totalTime = 8192;
        float maxMeanError = 0.2f;


        assertCorrect(ii, oo, model, iHistory, totalTime, maxMeanError);
    }

    @Test
    void test21_LSTM() {
        IntToFloatFunction ii = x -> (float)Math.sin(x/4f);
        IntToFloatFunction oo = x -> (float)Math.cos(x/8f);
        LivePredictor.LSTMPredictor model = new LivePredictor.LSTMPredictor(0.2f, 1);
        int iHistory = 6;
        int totalTime = 8192*2;
        float maxMeanError = 0.1f;


        assertCorrect(ii, oo, model, iHistory, totalTime, maxMeanError);
    }

    @Test
    void test12_MLP() {

        IntToFloatFunction ii = x -> (float)Math.sin(x/8f);
        IntToFloatFunction oo = x -> (float)Math.cos(x/8f);
        LivePredictor.MLPPredictor model =
                new LivePredictor.MLPPredictor(0.05f);
        int iHistory = 4;
        int totalTime = 1024;
        float maxMeanError = 0.15f;

        assertCorrect(ii, oo, model, iHistory, totalTime, maxMeanError);
    }

    private static void assertCorrect(IntToFloatFunction ii, IntToFloatFunction oo, LivePredictor.Predictor model, int iHistory, int totalTime, float maxMeanError) {
        MutableInteger m = new MutableInteger();


        LongToFloatFunction[] in =  { (w) -> ii.valueOf((int)w),  (w) -> oo.valueOf((int)w-1) };
        LongToFloatFunction[] out = { (w) -> oo.valueOf((int)w) };

        LivePredictor.DenseShiftFramer ih = new LivePredictor.DenseShiftFramer(in, iHistory, 1, out);
        LivePredictor l = new LivePredictor(model, ih );

        int numSnapshots = 16;
        assert(totalTime > numSnapshots*2);
        int errorWindow = totalTime / numSnapshots;

        PairedStatsAccumulator errortime = new PairedStatsAccumulator();
        DescriptiveStatistics error = new DescriptiveStatistics(errorWindow);

        for (int t = 0; t < totalTime; t++, m.increment()) {

            double[] prediction = l.next(m.intValue());

            
            {




            }

            double predicted = prediction[0];
            double actual = oo.valueOf(m.intValue()+1);

            double e = Math.abs(actual - predicted); 
            error.addValue(e);

            if (t%errorWindow == errorWindow-1) {
                errortime.add(t, error.getMean());
            }
           
        }

        double eMean = error.getMean();

        System.out.println(model);
        System.out.println("\tmean error: " + eMean);
        System.out.println("\terror rate: " +
                errortime.leastSquaresFit().slope());

        assertTrue(errortime.leastSquaresFit().slope() < -1E-8);
        assertTrue(eMean < maxMeanError, ()->"mean error: " + eMean);
    }


























    static double[] d(FloatSupplier[] f) {
        double[] d = new double[f.length];
        int i = 0;
        for (FloatSupplier g : f)
            d[i++] = g.asFloat();
        return d;
    }

}