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

        float weight_sum = Util.sumIfPositive(weightCount, weight);

        if (weight_sum < ScalarValue.EPSILON) {
            //flat
            return Util.bin(rng.asFloat() * weightCount, weightCount);//rng.nextInt(count)
        } else {

            return selectRouletteUnidirectionally
                    //return selectRouletteBidirectionally
                            (weightCount, weight, weight_sum, rng);
        }
    }

    public static int selectRouletteCached(int weightCount, IntToFloatFunction weight, Random rng) {
        return selectRouletteCached(weightCount, weight, rng::nextFloat);
    }

    /** returns -1 if no option (not any weight==NaN, or non-positive) */
    public static int selectRouletteCached(int weightCount, IntToFloatFunction weight, FloatSupplier rng) {

        if (weightCount == 1) {
            float w = weight.valueOf(0);
            return valid(w) ? 0 : -1;
        } else if (weightCount == 2) {

            float rx = weight.valueOf(0), ry = weight.valueOf(1);
            boolean bx = valid(rx), by = valid(ry);
            if (bx && by)
                return rng.asFloat() <= (Util.equals(rx, ry, Float.MIN_NORMAL) ?
                        0.5f : (rx / (rx + ry))) ?
                        0 : 1;
            else if (!bx && !by) return -1;
            else if (bx && !by) return 0;
            else return 1;

        } else {

            float[] w = new float[weightCount];
            int lastValid = -1;
            for (int i = 0; i < weightCount; i++) {
                float wi = weight.valueOf(i);
                if (valid(wi)) {
                    w[i] = wi;
                    if (lastValid == -1)
                        lastValid = i;//first
                    else
                        lastValid = -2;//more than one
                }
            }

            if (lastValid == -1)
                return -1;
            else if (lastValid!= -2) {
                return lastValid;
//                for (int i = 0; i < weightCount; i++)
//                    if (w[i] > 0)
//                        return i; //choose the only
//
//                throw new WTF(); //return -1; //WTF
            } else {
                return selectRoulette(weightCount, i -> w[i], rng);
            }
        }
    }

    private static boolean valid(float w) {
        return w==w && w > 0;
    }

    private static int selectRouletteUnidirectionally(int count, IntToFloatFunction weight, float weight_sum, FloatSupplier rng) {
        float distance = rng.asFloat() * weight_sum;
        int i = Util.bin(rng.asFloat() * count, count);

        int safetyLimit = count;
        while ((distance = distance - Math.max(0, weight.valueOf(i))) > Float.MIN_NORMAL && --safetyLimit > 0) {

            if (++i == count) i = 0; //wrap-around

        }
        if (safetyLimit<0) throw new WTF();

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
