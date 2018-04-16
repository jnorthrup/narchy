package jcog.decide;

import jcog.Util;
import jcog.pri.Pri;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;

import java.util.Random;
import java.util.function.IntPredicate;

import static java.lang.Float.MIN_NORMAL;

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

    /** efficient embeddable roulette decision executor.
     * takes a weight-update function applied to the current
     * choice after it has been selected in order to influence
     * its probability of being selected again.
     *
     * if the update function returns zero, then the choice is
     * deactivated and can not be selected again.
     *
     * when no choices remain, it exits automatically.
     */
    public static class MutableRoulette {
        /** weights of each choice */
        private final float[] w;
        private final Random rng;

        private final FloatToFloatFunction weightUpdate;

        /** current weight sum */
        private float weightSum;
        private int remaining;

        int i;

        public static void run(float[] weights, FloatToFloatFunction weightUpdate, IntPredicate select, Random rng) {
            switch (weights.length) {
                case 0: throw new UnsupportedOperationException();
                case 1: select.test(0); return;
                //TODO simple 2 case, with % probability of the ordering then try the order in sequence
                default: {
                    MutableRoulette r = new MutableRoulette(weights, weightUpdate, rng);
                    while (r.next(select)) { }
                }
            }
        }


        MutableRoulette(float[] w, FloatToFloatFunction weightUpdate, Random rng) {
            this.w = w;
            this.remaining = w.length;
            this.weightUpdate = weightUpdate;

            float ws = 0;
            for (int i1 = 0, wLength = w.length; i1 < wLength; i1++) {
                float x = w[i1];
                if (x < MIN_NORMAL) {
                    w[i1] = 0; //hard set to zero
                    remaining--;
                }
                ws += x;
            }
            assert(remaining > 0);

            this.weightSum = ws;
            this.i = (this.rng = rng).nextInt(w.length); //random start position
        }

        public boolean next(IntPredicate select) {
            return select.test(next()) && --remaining > 0;
        }

        private int next() {

            assert(remaining > 0);

            float[] w = this.w;

            int count = w.length;

            if (remaining == 1) {
                //special case:
                //there is only one with non-zero weight remaining

                for (int x = 0; x < count; x++)
                    if (w[x] > 0)
                        return x;

                throw new RuntimeException();
            } else {

                float distance = rng.nextFloat() * weightSum;
                //boolean dir = rng.nextBoolean(); //randomize the direction

                int i = this.i;

                float wi;
                while ((distance = distance - (wi = w[i])) > MIN_NORMAL) {
                    //if (dir) {
                    if (++i == count) i = 0;
                    //} else {
                    //  if (--i == -1) i = count - 1;
                    //}
                }

                float nextWeight = weightUpdate.valueOf(wi);
                if (nextWeight!=wi) {
                    float delta = nextWeight - wi;
                    w[i] = nextWeight;
                    weightSum += delta;
                    if (nextWeight < Float.MIN_NORMAL)
                        remaining--;
                }

                this.i = i;

                return i;
            }

        }

    }


//    public static enum RouletteControl {
//        STOP, CONTINUE, WEIGHTS_CHANGED
//    }
}
