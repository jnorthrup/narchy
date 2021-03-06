package nars.truth;

import org.jetbrains.annotations.Nullable;

import static nars.truth.func.TruthFunctions.c2wSafe;

public class MutableTruth implements Truth {

	float freq = Float.NaN;
	double evi;

	public MutableTruth() {

	}

	public MutableTruth(float f, double evi) {
		freq = f; this.evi = evi;
	}

	public MutableTruth(Truth t) {
		freq = t.freq();
		evi = t.evi();
	}

	@Override
	public final float freq() {
		return freq;
	}

	@Override
	public final double evi() {
		return evi;
	}

	public MutableTruth freq(double f) {
		return freq((float)f);
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
		evi = (double) c2wSafe(c);
		return this;
	}
	public MutableTruth conf(double c) {
		evi = c2wSafe(c);
		return this;
	}

	/** modifies this instance */
	public final MutableTruth negateThis() {
//		float f = freq();
//		if (f != f)
//			throw new NullPointerException();
		freq = 1.0f - freq;
		return this;
	}

	@Override
	public final MutableTruth neg() {
        MutableTruth x = new MutableTruth();
		x.freq = 1.0F - freq;
		x.evi = evi;
		return x;
	}

	public final MutableTruth negateThisIf(boolean ifTrue) {
		if (ifTrue)
			negateThis();
		return this;
	}

	@Override
	public final int hashCode() {
		//throw new UnsupportedOperationException();
		return System.identityHashCode(this);
	}

	@Override
	public final boolean equals(Object x) {
		//throw new UnsupportedOperationException();
		return this == x; //no other equality allowed
//		return this == x || (
//			Util.equals(freq, ((Truth)x).freq(), NAL.truth.TRUTH_EPSILON)
//			&&
//			Util.equals(conf(), ((Truth)x).conf(), NAL.truth.TRUTH_EPSILON)
//		);
	}

	public @Nullable PreciseTruth clone() {
		return is() ? PreciseTruth.byEvi(freq, evi) : null;
	}

	public MutableTruth set(@Nullable Truth x) {
		if (this!=x) {
			if (MutableTruth.is(x)) {
				freq = x.freq();
				evi = x.evi();
			} else {
				clear();
			}
		}
		return this;
	}

	/** sets to an invalid state */
	public final MutableTruth clear() {
		freq = Float.NaN;
		return this;
	}

	/** whether this instance's state is set to a specific value (true), or clear (false) */
	public final boolean is() {
		return freq==freq;
	}

	public static boolean is(@Nullable Truth x) {
		return x!=null && (!(x instanceof MutableTruth) || ((MutableTruth)x).is() );
	}

	@Override
	public String toString() {
		return _toString();
	}
}
