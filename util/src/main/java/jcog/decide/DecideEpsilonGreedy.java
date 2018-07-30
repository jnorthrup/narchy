package jcog.decide;

import jcog.Util;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Random;

/**
 * Created by me on 6/9/16.
 */
public class DecideEpsilonGreedy implements Deciding {

    /** argmax, with shuffling in case of a tie */
    public static final Deciding ArgMax = new DecideEpsilonGreedy(0, new XorShift128PlusRandom());

    private final Random random;
    float epsilonRandom; 

    /*
    TODO - decaying epsilon:
            epsilonRandom *= epsilonRandomDecay;
            epsilonRandom = Math.max(epsilonRandom, epsilonRandomMin);
     */

    public DecideEpsilonGreedy(float epsilonRandom, Random random) {

        this.epsilonRandom = epsilonRandom;
        this.random = random;
    }

    int motivationOrder[];

    @Override
    public int decide(float[] vector, int lastAction) {
        int actions = vector.length;

        if (motivationOrder == null || motivationOrder.length!=actions) {
            motivationOrder = new int[actions];
            for (int i = 0; i < actions; i++)
                motivationOrder[i] = i;
        }

        if (epsilonRandom > 0 && random.nextFloat() < epsilonRandom) {
            return random.nextInt(actions);
        }

        int nextAction = -1;
        boolean equalToPreviousAction = true;
        float nextMotivation = Float.NEGATIVE_INFINITY;

        ArrayUtils.shuffle(motivationOrder, random);

        for (int j = 0; j < actions; j++) {
            int i = motivationOrder[j];
            float m = vector[i];

            if (m > nextMotivation) {
                nextAction = i;
                nextMotivation = m;
            }
            if (equalToPreviousAction && j > 0 && !Util.equals(m, vector[motivationOrder[j - 1]])) {
                equalToPreviousAction = false; 
            }

        }
        
        int a = equalToPreviousAction ? -1 : nextAction;
        if (a < 0)
            return random.nextInt(actions);
        return a;
    }
}
