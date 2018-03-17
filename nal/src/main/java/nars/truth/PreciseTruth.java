package nars.truth;

import jcog.Util;
import nars.NAR;
import nars.Param;
import org.jetbrains.annotations.Nullable;

import static java.lang.Float.floatToIntBits;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2cSafe;

/**
 * represents a freq,evi pair precisely but does not
 * allow hashcode usage
 */
public class PreciseTruth implements Truth {

    final float f, e;

    public PreciseTruth(float freq, float conf) {
        this(freq, conf, true);
    }

    public PreciseTruth(float freq, float ce, boolean xIsConfOrEvidence) {
        assert ((Float.isFinite(freq ) ) && (freq >= 0) && (freq <= 1)):
                "invalid freq: " + freq;
        this.f = freq;

        if (xIsConfOrEvidence) {
            this.e = c2w(ce);
        } else {
            assert(Float.isFinite(ce) && ce > 0);
            this.e = ce;
        }
    }

    public PreciseTruth(@Nullable Truth truth) {
        this(truth.freq(), truth.evi(), false);
        assert(!(truth instanceof PreciseTruth)): "pointless";
    }

    @Override
    public Truth neg() {
        return new PreciseTruth(1f - f, e, false);
    }

    @Override
    public boolean equals(@Nullable Object that) {
        if (this == that) return true;
        if (!(that instanceof Truth)) return false;

        Truth y = (Truth)that;
        return Util.equals(e, y.evi(), Param.TRUTH_EPSILON)
                &&
               Util.equals(f, y.freq(), Param.TRUTH_EPSILON);
        
//        return
//            floatToIntBits(f) == floatToIntBits(y.freq())
//            &&
//            floatToIntBits(e) == floatToIntBits(y.evi());
    }

    /** equality test within a NAR's configured tolerances */
    public boolean equals(@Nullable Object that, NAR nar) {
        return this == that || (that!=null && equals((Truth)that, nar));
        //throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return floatToIntBits(f) & floatToIntBits(e);
    }

    @Override
    public String toString() {
        return _toString();
    }

    @Override
    public final float freq() {
        return f;
    }

    @Override
    public final float evi() { return e;    }

    @Override
    public final float conf() {
        return w2cSafe(e);
    }

}
