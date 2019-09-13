package nars.truth;

import jcog.Util;
import nars.NAL;

import static nars.truth.func.TruthFunctions.c2wSafe;


/**
 * truth rounded to a fixed size precision
 * to support hashing and equality testing
 * internally stores representation as one 32-bit integer
 */
public class DiscreteTruth implements Truth {

    private final int hash;

    private static final DiscreteTruth[] shared = new DiscreteTruth[(int) Util.sqr(Math.ceil(1f/NAL.truth.TRUTH_EPSILON))];

    /** gets the shared instance */
    public static DiscreteTruth the(float f, float c) {
        int i = Truth.truthToInt(f, c);
        int index = i % shared.length;
        DiscreteTruth t = shared[index];
        if (t == null || t.hash != i)
            shared[index] = t = new DiscreteTruth(i);
        return t;
    }

    public DiscreteTruth(float f, float c) {
        this(Truth.truthToInt(f, c));
    }
    DiscreteTruth(double f, double c) {
        this(Truth.truthToInt(f, c));
    }

    DiscreteTruth(float f, float c, float res) {
        this(f, c, res, res);
    }

    private DiscreteTruth(float f, float c, float freqRes, float confRes) {
        this(
            Truth.freq(f, freqRes),
            Truth.conf(c, confRes)
        );
    }

    DiscreteTruth(int hash) {
        this.hash = hash;
    }

    @Override
    public Truth neg() {
        return PreciseTruth.byEvi(1 - freq(), evi());
    }

    @Override
    public float freq() {
        return Util.toFloat(Truth.freqI(hash) /* & 0xffff*/, hashDiscretenessCoarse);
    }

    @Override
    public float conf() {
        return Util.toFloat(Truth.confI(hash), hashDiscretenessCoarse);
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
                    equalTruth((Truth) that, NAL.truth.TRUTH_EPSILON));
    }

    @Override
    public final int hashCode() {
        return hash;
    }




















    /*    public float getEpsilon() {
        return DEFAULT_TRUTH_EPSILON;
    }*/








































}
