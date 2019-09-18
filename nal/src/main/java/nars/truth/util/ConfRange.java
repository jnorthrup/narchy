package nars.truth.util;

import jcog.math.FloatRange;
import nars.NAL;
import nars.truth.func.TruthFunctions;

import static nars.truth.func.TruthFunctions.c2wSafe;
import static nars.truth.func.TruthFunctions.w2cSafe;

public final class ConfRange extends FloatRange {

    private double _evi;

    public ConfRange() {
        this(0);
    }

    public ConfRange(float initialValue) {
        super(initialValue, Math.max(Float.MIN_NORMAL, (float)w2cSafe(NAL.truth.EVI_MIN)), NAL.truth.CONF_MAX);
    }

    @Override
    public void set(float conf) {
        super.set(conf);
        _evi = c2wSafe(conf);
    }

    public final void conf(double conf) {
        super.set(conf);
        _evi = c2wSafe(conf);
    }
    public final void evi(double e) {
        super.set(TruthFunctions.w2cSafeDouble(e));
        _evi = e;
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
