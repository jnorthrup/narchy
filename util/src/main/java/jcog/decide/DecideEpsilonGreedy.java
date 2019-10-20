package jcog.decide;

import jcog.random.XoRoShiRo128PlusRandom;
import jcog.util.ArrayUtil;

import java.util.Random;

/**
 * Created by me on 6/9/16.
 */
public class DecideEpsilonGreedy implements Deciding {

    /** argmax, with shuffling in case of a tie */
    public static final Deciding ArgMax = new DecideEpsilonGreedy(0, new XoRoShiRo128PlusRandom(1));

    private final Random random;

    /** TODO FloatRange */
    public float epsilonRandom;

    /*
    TODO - decaying epsilon:
            epsilonRandom *= epsilonRandomDecay;
            epsilonRandom = Math.max(epsilonRandom, epsilonRandomMin);
     */

    public DecideEpsilonGreedy(float epsilonRandom, Random random) {

        this.epsilonRandom = epsilonRandom;
        this.random = random;
    }

    int[] motivationOrder;

    @Override
    public int applyAsInt(float[] vector) {
        var actions = vector.length;

        if (motivationOrder == null || motivationOrder.length!=actions) {
            motivationOrder = new int[actions];
            for (var i = 0; i < actions; i++)
                motivationOrder[i] = i;
        }

        if (epsilonRandom > 0 && random.nextFloat() < epsilonRandom) {
            return random.nextInt(actions);
        }

        ArrayUtil.shuffle(motivationOrder, random);

        var nextMotivation = Float.NEGATIVE_INFINITY;
        var nextAction = -1;
        for (var j = 0; j < actions; j++) {
            var i = motivationOrder[j];
            var m = vector[i];

            if (m > nextMotivation) {
                nextAction = i;
                nextMotivation = m;
            }


        }

        var a = nextAction;
        if (a < 0)
            return random.nextInt(actions);
        return a;
    }
}
