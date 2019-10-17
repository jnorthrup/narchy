package jcog.learn.lstm;

public final class SigmoidNeuron implements Neuron
{
	@Override
	public final double activate(final double x) {

		
		return 1.0 / (1.0 + Math.exp(-x));
	}

	@Override
	public final double derivate(final double x) {
		double act = activate(x);
		return act * (1.0 - act);
	}

	
}
