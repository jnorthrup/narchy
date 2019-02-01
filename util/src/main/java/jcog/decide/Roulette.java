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

        if (weightCount == 1)
            return 0;

        return selectRoulette(weightCount, weight, Util.sumIfPositive(weightCount, weight), rng);
    }

    public static int selectRouletteCached(int weightCount, IntToFloatFunction weight, Random rng) {
        if (weightCount == 1)
            return 0;

        float[] w = new float[weightCount];
        for (int i = 0; i < weightCount; i++)
            w[i] = weight.valueOf(i);
        return selectRoulette(weightCount, i->w[i], rng);
    }

    /**
     * faster if the sum is already known
     * TODO use a generic FloatSupplier instead of Random
     */
    public static int selectRoulette(final int count, IntToFloatFunction weight, float weight_sum, Random rng) {


        if (weight_sum < ScalarValue.EPSILON) {
            return rng.nextInt(count); //flat
        }

        float distanceFactor = rng.nextFloat();
        int i;
        boolean dir;
        if (distanceFactor <= 0.5f) {
            dir = true; i = 0; //count up
        } else {
            dir = false; i = count-1; //count down
            distanceFactor = 1 - distanceFactor;
        }

        float distance = distanceFactor * weight_sum;

        while ((distance = distance - weight.valueOf(i)) > Float.MIN_NORMAL) {
            if (dir) {
                if (++i == count) i = 0;
            } else {
                if (--i == -1) i = count - 1;
            }
        }

        return i;
    }


}
