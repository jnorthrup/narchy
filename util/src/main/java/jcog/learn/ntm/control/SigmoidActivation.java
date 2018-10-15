



package jcog.learn.ntm.control;

public class SigmoidActivation implements IDifferentiableFunction
{
    public static final SigmoidActivation the = new SigmoidActivation();

    private final double _alpha;

    public SigmoidActivation() {
        this(1.0);
    }

    public SigmoidActivation(double alpha) {
        _alpha = alpha;
    }

    @Override
    public double value(double x) {
        return Sigmoid.getValue(x, _alpha);
    }

    @Override
    public double derivative(double y) {
        return (_alpha * y * (1.0 - y));
    }


    @Override
    public double derivative(double grad, double y) {
        return (grad * y * (1.0 - y));
    }

}


