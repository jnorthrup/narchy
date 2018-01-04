package jcog.decide;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;

import java.util.Random;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import static java.lang.Float.NaN;
import static java.lang.Math.exp;

/** roulette decision making */
public enum Roulette { ;

    public final static FloatFunction<? super PriReference> linearPri = (p) -> {
        return Math.max(p.priElseZero(), Prioritized.EPSILON);
    };

    final static FloatFunction<? super PriReference> softMaxPri = (p) -> {
        return (float) exp(p.priElseZero() * 3 /* / temperature */);
    };


    public static int decideRoulette(float[] x, Random rng) {
        return decideRoulette(x.length, (n) -> x[n], rng);
    }

    /**
     * https://en.wikipedia.org/wiki/Fitness_proportionate_selection
     * Returns the selected index based on the weights(probabilities)
     */
    public static int decideRoulette(int weightCount, IntToFloatFunction weight, Random rng) {
        // calculate the total weight
        assert (weightCount > 0);
        return decideRoulette(weightCount, weight, Util.sumIfPositive(weightCount, weight), rng);
    }

    public static void decideRouletteWhile(int choices, IntToFloatFunction choiceWeight, Random rng, IntFunction<RouletteControl> each) {
        float weightSum = NaN;
        while (true) {
            if (weightSum != weightSum) {
                weightSum = Util.sumIfPositive(choices, choiceWeight);
            }
            if (weightSum < Float.MIN_NORMAL * choices) {
                //flat
                float perChoice = 1f/choices;
                choiceWeight = (i) -> perChoice;
                weightSum = 1f;
            }
            switch (each.apply(decideRoulette(choices, choiceWeight, weightSum, rng))) {
                case STOP:
                    return;
                case CONTINUE:
                    break;
                case WEIGHTS_CHANGED:
                    weightSum = Float.NaN;
                    break;
            }
        }
    }

    /** roulette selection on a softmax scale */
    public static int decideSoftmax(int count, IntToFloatFunction weight, float temperature, Random random) {
        return decideRoulette(count, (i) ->
                (float) exp(weight.valueOf(i) / temperature), random);
    }

//  /** roulette selection on a softmax scale */
//    public static void decideSoftmax(int count, IntToFloatFunction weight, float temperature, Random random, IntFunction<RouletteControl> each) {
//        decideRoulette(count, (i) ->
//                (float) exp(weight.valueOf(i) / temperature), random, each);
//    }

    /**
     * faster if the sum is already known
     */
    public static int decideRoulette(final int count, IntToFloatFunction weight, float weight_sum, Random rng) {

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

    public static <X> void selectRouletteUnique(int choices, IntToFloatFunction choiceWeight, IntPredicate tgt, Random random) {
        assert(choices > 0);
        if (choices == 1) {
            tgt.test(0);
            return;
        }

        MetalBitSet selected = MetalBitSet.bits(choices);

        final int[] hardLimit = {choices*2};
        IntToFloatFunction cc = ii -> {
            if (selected.get(ii)) {
                return 0;
            } else {
                float w = choiceWeight.valueOf(ii);
                if (w <= 0)
                    selected.set(ii);
                return w;
            }
        };
        decideRouletteWhile(choices, cc, random, (int y) -> {
            selected.set(y);
//            int remain = choices - selected.cardinality();
            boolean kontinue = tgt.test(y) && (hardLimit[0]-- > 0);
//            if (kontinue && remain == 1) {
//                //"tail" roulette optimization
//                int x = selected.nextClearBit();
//                tgt.test(x);
//                return RouletteControl.STOP;
//            }
            return kontinue ?
                    RouletteControl.WEIGHTS_CHANGED : RouletteControl.STOP;
        });

//        if (sampled == 0) return Collections.emptyList();
//        if (sampled == choices) {
//            return List.of(x);
//        } else {
//            //TODO better selection method
//
//            List<X> l = new FasterList(sampled);
//            MetalBitSet b = new MetalBitSet(choices);
//            int limit = sampled * 4;
//            int c = 0;
//            for (int i = 0; c < sampled && i < limit; i++) {
//                int w = random.nextInt(choices); //<- TODO weighted roullette selection here
//                if (!b.getAndSet(w, true)) {
//                    l.add(x[w]);
//                    c++;
//                }
//            }
//            return l;
//        }
    }

//    @Nullable
//    public X decide(Random rng) {
//        int s = size();
//        if (s == 0)
//            return null;
//        return get(decideWhich(rng));
//    }
//
//    public int decideWhich(Random rng) {
//        int s = size();
//        if (s == 0)
//            return -1;
//        else if (s == 1)
//            return 0;
//
//        if (this.values == null || this.values.length!= s) {
//            this.values = new float[s];
//            float sum = 0;
//            for (int i = 0; i < s; i++) {
//                float f = eval.floatValueOf(get(i));
//                values[i] = f;
//                sum += f;
//            }
//            this.sum = sum;
//        }
//        return decideRoulette(s, (i)->values[i], sum, rng);
//    }

    public static enum RouletteControl {
        STOP, CONTINUE, WEIGHTS_CHANGED
    }
}
