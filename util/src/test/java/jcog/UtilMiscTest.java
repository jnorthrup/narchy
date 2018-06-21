package jcog;

import jcog.io.SparkLine;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author me
 */
class UtilMiscTest {

    @Test
    void test() {
        assertEquals(0, Util.bin(0, 10));
        assertEquals(1, Util.bin(0.1f, 10));
        assertEquals(9, Util.bin(0.9f, 10));
        assertEquals(9, Util.bin(0.925f, 10));
        assertEquals(9, Util.bin(0.975f, 10));
        assertEquals(9, Util.bin(1.0f, 10));
        
        
        assertEquals(0, Util.bin(0.0f, 9));
        assertEquals(1, Util.bin(0.1f, 9));
        assertEquals(8, Util.bin(0.9f, 9));
        assertEquals(8, Util.bin(1.0f, 9));
    }

    @Test
    void testCurveSawtooth() {
        int N = 32;
        Integer[] x = new Integer[N];
        for (int i = 0; i < N; i++) {
            x[i] = Math.round(Util.sawtoothCurved((float)i/(N-1)) * N);
        }
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
