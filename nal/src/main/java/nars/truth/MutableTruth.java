package nars.truth;

import static nars.truth.func.TruthFunctions.c2w;

public class MutableTruth implements Truth {

	float freq = Float.NaN;
	double evi;

	public MutableTruth() {

	}

	public MutableTruth(Truth t) {
		freq(t.freq());
		evi(t.evi());
	}

	@Override
	public float freq() {
		return freq;
	}

	@Override
	public double evi() {
		return evi;
	}

	public MutableTruth freq(float f) {
		this.freq = f;
		return this;
	}
	public MutableTruth evi(double e) {
		this.evi = e;
		return this;
	}

	public MutableTruth conf(float c) {
		return evi(c2w(c));
	}
	public MutableTruth conf(double c) {
		return evi(c2w(c));
	}

	/** modifies this instance */
	public final MutableTruth negateThis() {
		freq = 1.0f - freq;
		return this;
	}

	public final MutableTruth negateThisIf(boolean ifTrue) {
		if (ifTrue)
			negateThis();
		return this;
	}

	@Override
	public final int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean equals(Object obj) {
		return this == obj; //no other equality allowed
	}

	public PreciseTruth clone() {
		return PreciseTruth.byEvi(freq, evi);
	}

	public MutableTruth set(Truth x) {
		if (this!=x) {
			if (x!=null) {
				freq(x.freq());
				evi(x.evi());
			} else {
				clear();
			}
		}
		return this;
	}

	/** sets to an invalid state */
	public MutableTruth clear() {
		freq = Float.NaN;
		evi = 0;
		return this;
	}

}
