package jcog.learn.lstm;

public final class IdentityNeuron implements Neuron
{
	@Override
    public final double activate(double x)
	{
		return x;
	}

	@Override
    public final double derivate(double x) {
		return 1.0;
	}
}

