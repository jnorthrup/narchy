package nars.truth;

import nars.Param;

import static nars.truth.TruthFunctions.c2wSafe;


/**
 * truth rounded to a fixed size precision
 * to support hashing and equality testing
 * internally stores representation as one 32-bit integer
 */
public class DiscreteTruth implements Truth {

    public final int hash;

    public DiscreteTruth(Truth t) {
        this(t.freq(), t.conf());
    }

    public DiscreteTruth(float f, float c) {
        this(Truth.truthToInt(f,
                //Math.min(c, Param.TRUTH_MAX_CONF),
                c,
                hashDiscretenessEpsilon));
    }

    public DiscreteTruth(float f, float c, float res) {
        this(f, c, res, res);
    }

    public DiscreteTruth(float f, float c, float freqRes, float confRes) {
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
        return new PreciseTruth(
                1f - freq(), conf());
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
    public float evi() {
        return c2wSafe(conf());
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
                    equalsIn((Truth) that, Param.TRUTH_EPSILON));
    }

    @Override
    public final int hashCode() {
        return hash;
    }

//    @NotNull
//    @Override
//    public final DefaultTruth negated() {
//        //float fPos = freq;
//
//        //if = 0.5, negating will produce same result
//        //return Util.equals(fPos, 0.5f, Global.TRUTH_EPSILON) ? this :
//
//        return new DefaultTruth(1.0f - freq, conf);
//    }

//    protected boolean equalsFrequency(@NotNull Truth t) {
//        return (Util.equals(freq, t.freq(), Param.TRUTH_EPSILON));
//    }
//
//    private static final int hashDiscreteness = (int)(1.0f / Param.TRUTH_EPSILON);



    /*    public float getEpsilon() {
        return DEFAULT_TRUTH_EPSILON;
    }*/

//    /** truth with 0.01 resolution */
//    public static class DefaultTruth01 extends DefaultTruth {
//
//        public DefaultTruth01(float f, float c) {
//            super(f, c);
//        }
//    }
//
//
//
//    /** truth with 0.1 resolution */
//    public static class DefaultTruth1 extends AbstractDefaultTruth {
//
//        @Override
//        public float getEpsilon() {
//            return 0.1f;
//        }
//    }
//
//
//    /** truth with 0.001 resolution */
//    public static class DefaultTruth001 extends AbstractDefaultTruth {
//
//        @Override
//        public float getEpsilon() {
//            return 0.001f;
//        }
//    }
//
//
//    /** truth with 0.05 resolution */
//    public static class DefaultTruth05 extends AbstractDefaultTruth {
//
//        @Override
//        public float getEpsilon() {
//            return 0.05f;
//        }
//    }

}
