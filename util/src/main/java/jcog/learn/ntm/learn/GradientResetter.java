



package jcog.learn.ntm.learn;

import jcog.learn.ntm.control.UVector;
import jcog.learn.ntm.control.Unit;

public class GradientResetter implements WeightUpdaterBase {

    @Override
    public void reset() {
    }

    @Override
    public void updateWeight(Unit data) {
        data.grad = 0.0;
    }

    @Override
    public void updateWeight(UVector data) {
        data.clearGrad();
    }

}


