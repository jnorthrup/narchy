package nars.truth;

import jcog.Util;
import nars.Param;
import org.jetbrains.annotations.Nullable;

import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2cSafe;

/**
 * extends DiscreteTruth's raw hash representation with
 * a freq and evidence float pairs.
 *
 * this allows it to store, internally, more precision than the
 * discrete representation, yet is comparable with DiscreteTruth
 * (according to system TRUTH_EPSILON tolerance).
 *
 * the additional precision is useful for intermediate calculations
 * where premature rounding could snowball into significant error.
 *
 */
public class PreciseTruth extends DiscreteTruth {

    final float f, e;

    public PreciseTruth(float freq, float conf) {
        this(freq, conf, true);
    }

    public PreciseTruth(float freq, float ce, boolean xIsConfOrEvidence) {
        super(freq, xIsConfOrEvidence ? ce : w2cSafe(ce));

        float e;
        if (xIsConfOrEvidence)
            e = c2wSafe(ce); //CONF -> EVI
        else
            e = ce; //EVI -> EVI

        this.e = Util.clamp(e, Param.TRUTH_MIN_EVI, Param.TRUTH_MAX_EVI);
        this.f = Util.clamp(freq, 0, 1f);
    }

    public PreciseTruth(Truth truth) {
        this(truth.freq(), truth.evi(), false);
        assert(!(truth instanceof PreciseTruth)): "pointless";
    }

    @Override
    public Truth neg() {
        return new PreciseTruth(1f - f, e, false);
    }

    @Override
    public final float freq() {
        return f;
    }

    @Override
    public final float evi() { return e; }

    @Override
    public float conf() {
        return w2cSafe(e);
    }

    /** create a DiscreteTruth instance, shedding the freq,evi floats stored here */
    public DiscreteTruth raw() {
        return new DiscreteTruth(this);
    }
}
