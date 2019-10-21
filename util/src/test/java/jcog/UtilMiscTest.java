package jcog;

import jcog.io.SparkLine;
import jcog.random.XoRoShiRo128PlusRandom;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author me
 */
class UtilMiscTest {

    @Test void testBinDistribution() {
        int seed = 1;
        int samples = 5000;

        Random r = new XoRoShiRo128PlusRandom(seed);
        for (int bins = 2; bins < 15; bins++) {
            Frequency f = new Frequency();
            for (int s = 0; s < samples; s++)
                f.addValue( Util.bin(r.nextFloat(), bins) );
            assertEquals(bins, f.getUniqueCount());
            SummaryStatistics s = new SummaryStatistics();
            for (int b = 0; b < bins; b++)
                s.addValue(f.getPct(b));
            System.out.println(f);
            System.out.println(s);
            assertTrue(s.getVariance() < 0.01f);
        }
    }


    @Test
    void testCurveSawtooth() {
        int N = 32;
        Integer[] x = IntStream.range(0, N).mapToObj(new IntFunction<Integer>() {
            @Override
            public Integer apply(int i) {
                return Math.round(Util.sawtoothCurved((float) i / (N - 1)) * N);
            }
        }).toArray(Integer[]::new);
        System.out.println(SparkLine.render(x));
    }
    @Test
    void testMsgPackDecode(){
        testMsgPackTranscode("skjfldksf", Object.class);
    }

    private static void testMsgPackTranscode(Object x, Class cl) {
        try {
            Object result = Util.fromBytes(Util.toBytes(x, cl), cl);
            assertEquals(x, result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
