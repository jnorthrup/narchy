package jcog.learn.ntm.learn;

import jcog.learn.ntm.control.UVector;
import jcog.learn.ntm.control.Unit;

import java.util.Random;

public class RandomWeightInitializer implements WeightUpdaterBase {
    private final Random rng;

    public RandomWeightInitializer(Random rand) {
        rng = rand;
    }


    @Override
    public void reset() {
    }

    @Override
    public void updateWeight(Unit data) {
        data.value = next();
    }

    private double next() {
        
        return rng.nextDouble() - 0.5;
    }

    @Override
    public void updateWeight(UVector data) {
        final double[] dd = data.value;
        for (int i = 0; i < data.size(); i++)
            dd[i] = next();
    }

}


