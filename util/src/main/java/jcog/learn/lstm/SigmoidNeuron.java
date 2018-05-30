package jcog.learn.lstm;

final public class SigmoidNeuron implements Neuron
{
	@Override
	final public double activate(final double x) {

		
		return 1.0 / (1.0 + Math.exp(-x));
	}

	@Override
	final public double derivate(final double x) {
		double act = activate(x);
		return act * (1.0 - act);
	}

	
}
