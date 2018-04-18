package jcog.decide;

import jcog.Util;
import jcog.pri.Pri;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;

import java.util.Random;

/**
 * roulette decision making
 */
public enum Roulette {
    ;

//    public final static FloatFunction<? super PriReference> linearPri = (p) -> {
//        return Math.max(p.priElseZero(), Prioritized.EPSILON);
//    };
//
//    final static FloatFunction<? super PriReference> softMaxPri = (p) -> {
//        return (float) exp(p.priElseZero() * 3 /* / temperature */);
//    };


    public static int selectRoulette(float[] x, Random rng) {
        return selectRoulette(x.length, (n) -> x[n], rng);
    }

    /**
     * https://en.wikipedia.org/wiki/Fitness_proportionate_selection
     * Returns the selected index based on the weights(probabilities)
     */
    public static int selectRoulette(int weightCount, IntToFloatFunction weight, Random rng) {
        // calculate the total weight
        assert (weightCount > 0);
        return selectRoulette(weightCount, weight, Util.sumIfPositive(weightCount, weight), rng);
    }


//    /**
//     * roulette selection on a softmax scale
//     */
//    public static int decideSoftmax(int count, IntToFloatFunction weight, float temperature, Random random) {
//        return decideRoulette(count, (i) ->
//                (float) exp(weight.valueOf(i) / temperature), random);
//    }

//  /** roulette selection on a softmax scale */
//    public static void decideSoftmax(int count, IntToFloatFunction weight, float temperature, Random random, IntFunction<RouletteControl> each) {
//        decideRoulette(count, (i) ->
//                (float) exp(weight.valueOf(i) / temperature), random, each);
//    }

    /**
     * faster if the sum is already known
     */
    public static int selectRoulette(final int count, IntToFloatFunction weight, float weight_sum, Random rng) {

        int i = rng.nextInt(count); //random start location
        assert (i >= 0);
        if (weight_sum < Pri.EPSILON) {
            return i; //flat, choose one at random
        }

        float distance = rng.nextFloat() * weight_sum;
        boolean dir = rng.nextBoolean(); //randomize the direction

        while ((distance = distance - weight.valueOf(i)) > 0) {
            if (dir) {
                if (++i == count) i = 0;
            } else {
                if (--i == -1) i = count - 1;
            }
        }

        return i;
    }


//    public static enum RouletteControl {
//        STOP, CONTINUE, WEIGHTS_CHANGED
//    }
}
