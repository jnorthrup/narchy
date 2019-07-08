package nars.truth.util;

import jcog.math.FloatRange;
import nars.NAL;

import static nars.truth.func.TruthFunctions.c2wSafe;
import static nars.truth.func.TruthFunctions.w2cSafe;

public final class ConfRange extends FloatRange {

    private double _evi;

    public ConfRange() {
        this(0);
    }

    public ConfRange(float initialValue) {
        super(initialValue, (float)w2cSafe(NAL.truth.EVI_MIN), NAL.truth.CONF_MAX);
    }

    @Override
    public void set(float value) {
        super.set(value);
        _evi = c2wSafe((double)get());
    }
    public final float conf() {
        return floatValue();
    }

    public final double evi() {
        return _evi;
    }

//    public Truth truth(float freq) {
//        return PreciseTruth.byEvi(freq, evi());
//    }
//
//    /** eternalized evidence */
//    public final double eviEte() {
//        return TruthFunctions.eternalize(evi());
//    }
//
//    /** eternalized conf */
//    public final double confEte() {
//        return w2cSafe(eviEte());
//    }

}
