package jcog.learn.ntm.control;

import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;

public interface IDifferentiableFunction extends DoubleToDoubleFunction  {

    double derivative(double y);

    double derivative(double grad, double value);

}


