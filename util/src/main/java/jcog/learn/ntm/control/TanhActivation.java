package jcog.learn.ntm.control;

import jcog.TODO;
import jcog.Util;

public class TanhActivation implements IDifferentiableFunction
{
    public static final TanhActivation the = new TanhActivation();

    @Override
    public double value(double x) {
        return Math.tanh(x);
    }

    @Override
    public double derivative(double y) {
        return 1 - Util.sqr(Math.tanh(y));
    }


    @Override
    public double derivative(double grad, double y) {
        throw new TODO();
    }

}
