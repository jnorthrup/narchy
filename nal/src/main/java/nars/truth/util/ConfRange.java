package nars.truth.util;

import jcog.math.FloatRange;
import nars.NAL;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import nars.truth.func.TruthFunctions;

import static nars.truth.func.TruthFunctions.c2wSafe;
import static nars.truth.func.TruthFunctions.w2cSafe;

public final class ConfRange extends FloatRange {

    private double _evi;

    public ConfRange() {
        this(NAL.truth.TRUTH_EPSILON);
    }

    public ConfRange(float initialValue) {
        super(initialValue, w2cSafe(NAL.truth.EVI_MIN), NAL.truth.CONF_MAX);
    }

    @Override
    public void set(float value) {
        super.set(value);
        _evi = c2wSafe((double)get());
    }

    public double asEvi() {
        return _evi;
    }

    public Truth truth(float freq) {
        return PreciseTruth.byEvi(freq, asEvi());
    }

    /** eternalized evidence */
    public final double eviEte() {
        return TruthFunctions.eternalize(asEvi());
    }

    /** eternalized conf */
    public final double confEte() {
        return w2cSafe(eviEte());
    }

}
