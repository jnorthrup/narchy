package nars.truth;

import nars.Param;

import static nars.truth.func.TruthFunctions.c2wSafe;


/**
 * truth rounded to a fixed size precision
 * to support hashing and equality testing
 * internally stores representation as one 32-bit integer
 */
public class DiscreteTruth implements Truth {

    private final int hash;

    DiscreteTruth(Truth t) {
        this(t.freq(), t.conf());
    }

    public DiscreteTruth(float f, float c) {
        this(Truth.truthToInt(f, c));
    }
    public DiscreteTruth(double f, double c) {
        this(Truth.truthToInt(f, c));
    }

    public DiscreteTruth(float f, float c, float res) {
        this(f, c, res, res);
    }

    private DiscreteTruth(float f, float c, float freqRes, float confRes) {
        this(
            Truth.freq(f, freqRes),
            Truth.conf(c, confRes)
        );
    }

    protected DiscreteTruth(int hash) {
        this.hash = hash;
    }

    @Override
    public Truth neg() {
        return PreciseTruth.byEvi(1 - freq(), evi());
    }

    @Override
    public float freq() {
        return Truth.freq(hash);
    }

    @Override
    public float conf() {
        return Truth.conf(hash);
    }

    @Override
    public double evi() {
        return c2wSafe((double)conf());
    }

    @Override
    public String toString() {
        return _toString();
    }

    @Override
    public boolean equals(Object that) {
        return
            (this == that)
                    ||
            ((that instanceof DiscreteTruth) ?
                    (hash == ((DiscreteTruth)that).hash) :
                    equalTruth((Truth) that, Param.truth.TRUTH_EPSILON));
    }

    @Override
    public final int hashCode() {
        return hash;
    }




















    /*    public float getEpsilon() {
        return DEFAULT_TRUTH_EPSILON;
    }*/








































}
