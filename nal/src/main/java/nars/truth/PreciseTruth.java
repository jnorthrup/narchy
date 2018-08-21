package nars.truth;

import nars.Param;

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


    public static PreciseTruth byConf(float freq, float conf) {
        return byFreqConfEvi(freq, conf, c2wSafe(conf));
    }

    public static PreciseTruth byEvi(float freq, float evi) {
        return byFreqConfEvi(freq, w2cSafe(evi), evi);
    }

    /** use with caution, if you are calculating a precise evi and a dithered conf, they should correspond */
    protected static PreciseTruth byFreqConfEvi(float freq, float conf, float evi) {
        return new PreciseTruth(freq, conf, evi);
    }

    private PreciseTruth(float freq, float conf, float evi) {
        super(freq, conf);
        this.e = evi;
        this.f = freq;
    }

    static PreciseTruth theDithered(float f, float fRes, float evi, float cRes) {
        return PreciseTruth.byFreqConfEvi(
                Truth.freq(f, Math.max(Param.TRUTH_EPSILON, fRes)),
                Truth.w2cDithered(evi, Math.max(Param.TRUTH_EPSILON, cRes)),
                evi);
    }

    @Override
    public Truth neg() {
        return byEvi(1f - f, e);
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
