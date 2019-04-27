package nars.truth;

import nars.NAL;
import org.jetbrains.annotations.Nullable;

import static nars.truth.func.TruthFunctions.*;

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
public final class PreciseTruth extends DiscreteTruth {

    private final float f;
    private final double e;


    @Nullable
    public static PreciseTruth byConf(float freq, double /* double? */ conf) {
        double e = c2wSafe(conf);
        return byConfEvi(freq, conf, e);
    }

    public static PreciseTruth byEvi(double freq, double evi) {
        return byEvi((float)freq, (float)evi);
    }

    public static PreciseTruth byEvi(float freq, double evi) {
        return byConfEvi(freq, w2cSafeDouble(evi), evi);
    }

    /** use with caution, if you are calculating a precise evi and a dithered conf, they should correspond */
    static PreciseTruth byConfEvi(float freq, double conf, double evi) {

        if (evi < NAL.truth.TRUTH_EVI_MIN)
            return null;

        if (conf >= NAL.truth.TRUTH_CONF_MAX || evi>= NAL.truth.TRUTH_EVI_MAX) {
            //upper limit on truth
            conf = NAL.truth.TRUTH_CONF_MAX;
            evi = NAL.truth.TRUTH_EVI_MAX;
        }

        if (evi < NAL.truth.TRUTH_EVI_MIN || !Double.isFinite(evi))
            throw new TruthException("non-positive evi", evi);

        return new PreciseTruth(freq, conf, evi);
    }

    private PreciseTruth(float freq, double conf, double evi) {
        super(freq, conf);
        this.e = evi;
        this.f = freq;
    }

    @Override
    public final float freq() {
        return f;
    }

    @Override
    public final double evi() { return e; }

    @Override
    public final float conf() {
        return w2cSafe(e);
    }

    /** create a DiscreteTruth instance, shedding the freq,evi floats stored here */
    public DiscreteTruth raw() {
        return new DiscreteTruth(this);
    }
}
