package jcog.decide;

import jcog.Util;
import jcog.WTF;
import jcog.math.FloatSupplier;
import jcog.pri.ScalarValue;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;

import java.util.Random;

/**
 * roulette decision making
 */
public enum Roulette {
    ;


    public static int selectRoulette(float[] x, FloatSupplier rng) {
        return selectRoulette(x.length, (n) -> x[n], rng);
    }



    public static int selectRoulette(int weightCount, IntToFloatFunction weight, Random rng) {
        return selectRoulette(weightCount, weight, rng::nextFloat);
    }

    /**
     * https:
     * Returns the selected index based on the weights(probabilities)
     */
    public static int selectRoulette(int weightCount, IntToFloatFunction weight, FloatSupplier rng) {
        assert (weightCount > 0);

        if (weightCount == 1)
            return 0;

        return selectRoulette(weightCount, weight, Util.sumIfPositive(weightCount, weight), rng);
    }

    public static int selectRouletteCached(int weightCount, IntToFloatFunction weight, Random rng) {
        return selectRouletteCached(weightCount, weight, rng::nextFloat);
    }

    public static int selectRouletteCached(int weightCount, IntToFloatFunction weight, FloatSupplier rng) {

        if (weightCount == 1)
            return 0;
        else if (weightCount == 2) {

            float rx = weight.valueOf(0), ry = weight.valueOf(1);
            return rng.asFloat() <= (Util.equals(rx, ry, Float.MIN_NORMAL) ?
                    0.5f : (rx / (rx + ry))) ?
                        0 : 1;

        } else {

            float[] w = new float[weightCount];
            for (int i = 0; i < weightCount; i++) {
                float wi = weight.valueOf(i);
                if (wi < 0)
                    wi=0; //throw new WTF();
                w[i] = wi;
            }
            return selectRoulette(weightCount, i -> w[i], rng);
        }
    }

    /**
     * faster if the sum is already known
     * TODO use a generic FloatSupplier instead of Random
     */
    private static int selectRoulette(final int count, IntToFloatFunction weight, float weight_sum, FloatSupplier rng) {

        if (weight_sum < ScalarValue.EPSILON) {
            //flat
            return Util.bin(rng.asFloat() * count, count);//rng.nextInt(count)
        } else {

            return selectRouletteUnidirectionally
                    //return selectRouletteBidirectionally
                            (count, weight, weight_sum, rng);
        }
    }

    private static int selectRouletteUnidirectionally(int count, IntToFloatFunction weight, float weight_sum, FloatSupplier rng) {
        float distance = rng.asFloat() * weight_sum;
        int i = Util.bin(rng.asFloat() * count, count);

        int safetyLimit = count;
        while ((distance = distance - weight.valueOf(i)) > Float.MIN_NORMAL) {

            if (++i == count) i = 0; //wrap-around

            if (--safetyLimit==0)
                throw new WTF();
        }

        return i;
    }

    /** not sure if this offers any improvement over the simpler unidirectional iieration.
     * might also be biased to the edges or middle because it doesnt start at random index though this can be tried */
    private static int selectRouletteBidirectionally(int count, IntToFloatFunction weight, float weight_sum, FloatSupplier rng) {
        float x = rng.asFloat();
        int i;
        boolean dir;
        if (x <= 0.5f) {
            dir = true;
            i = 0; //count up
        } else {
            dir = false;
            i = count - 1; //count down
            x = 1 - x;
        }

        float distance = x * weight_sum;

        int limit = count;
        while ((distance = distance - weight.valueOf(i)) > Float.MIN_NORMAL) {
            if (dir) {
                if (++i == count) i = 0;
            } else {
                if (--i == -1) i = count - 1;
            }
            if (--limit==0)
                throw new WTF();
        }

        return i;
    }


}
