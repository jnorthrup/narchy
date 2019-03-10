package jcog.learn;

import jcog.random.XoRoShiRo128PlusRandom;

import java.util.Random;

public class RandomAgent extends Agent {

    final Random rng;

    public RandomAgent(int inputs, int actions) {
        this(new XoRoShiRo128PlusRandom(1), inputs, actions);
    }

    public RandomAgent(Random rng, int inputs, int actions) {
        super(inputs, actions);
        this.rng = rng;
    }

    @Override
    public int decide(float[] actionFeedback, float reward, float[] input) {
        return rng.nextInt(actions);
    }
}
