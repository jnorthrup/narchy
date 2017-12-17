package jcog.bag.impl.hijack;

import jcog.memoize.HijackMemoize;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class LambdaMemoizerTest {

    @Test
    public void test1() throws NoSuchMethodException {

        Function<Object[],Integer> cachingSlowFunction =
                LambdaMemoizer.memoize(LambdaMemoizerTest.class, "slowFunction", int.class,
                        (f) -> new HijackMemoize<>(f, 16, 3));

        /* warmup */
        cachingSlowFunction.apply(new Object[] { 3 });

        long startSlow = System.currentTimeMillis();
        int result = cachingSlowFunction.apply(new Object[] { 2 });
        long endSlow = System.currentTimeMillis();

        // Do it again!
        long startFast = System.currentTimeMillis();
        int result2 = cachingSlowFunction.apply(new Object[] { 2 });
        long endFast = System.currentTimeMillis();

        assertEquals(4, result);
        assertEquals(4, result2);

        long first = endSlow - startSlow;
        System.out.printf("The first time took %dms%n", first);
        long second = endFast - startFast;
        System.out.printf("The second time took %dms%n", second);
        assertTrue(first - second > 25 /* ms speedup */);
    }

    public static int slowFunction(int input) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return input * 2;
    }

}