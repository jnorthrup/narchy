package nars.truth.util;

import jcog.math.FloatRange;
import nars.NAL;
import nars.truth.func.TruthFunctions;

import static nars.truth.func.TruthFunctions.c2wSafe;

public final class ConfRange extends FloatRange {

    private double _evi;

    public ConfRange() {
        this(NAL.truth.TRUTH_EPSILON);
    }

    public ConfRange(float initialValue) {
        super(initialValue, TruthFunctions.w2cSafe(NAL.truth.TRUTH_EVI_MIN), NAL.truth.TRUTH_CONF_MAX);
    }

    @Override
    public void set(float value) {
        super.set(value);
        _evi = c2wSafe((double)get());
    }

    public double asEvi() {
        return _evi;
    }
}
