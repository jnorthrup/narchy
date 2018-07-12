package jcog.decide;

import jcog.Util;
import jcog.pri.ScalarValue;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;

import java.util.Random;

/**
 * roulette decision making
 */
public enum Roulette {
    ;










    public static int selectRoulette(float[] x, Random rng) {
        return selectRoulette(x.length, (n) -> x[n], rng);
    }

    /**
     * https:
     * Returns the selected index based on the weights(probabilities)
     */
    public static int selectRoulette(int weightCount, IntToFloatFunction weight, Random rng) {
        
        assert (weightCount > 0);
        return selectRoulette(weightCount, weight, Util.sumIfPositive(weightCount, weight), rng);
    }
















    /**
     * faster if the sum is already known
     */
    public static int selectRoulette(final int count, IntToFloatFunction weight, float weight_sum, Random rng) {

        int i = rng.nextInt(count); 
        assert (i >= 0);
        if (weight_sum < ScalarValue.EPSILON) {
            return i; 
        }

        float distance = rng.nextFloat() * weight_sum;
        boolean dir = rng.nextBoolean(); 

        while ((distance = distance - weight.valueOf(i)) > 0) {
            if (dir) {
                if (++i == count) i = 0;
            } else {
                if (--i == -1) i = count - 1;
            }
        }

        return i;
    }





}
