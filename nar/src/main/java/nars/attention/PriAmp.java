package nars.attention;

import jcog.math.FloatRange;

/** node with the addition of a multiplicative amplifier parameter */
public class PriAmp extends PriNode {

	public final FloatRange amp = new FloatRange(1, 0, 1f);

	public PriAmp(Object id) {
		super(id);
	}

	@Override
	protected float in(float p) {
		return amp.floatValue() * p;
	}
}
