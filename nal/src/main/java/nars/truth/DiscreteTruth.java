package nars.truth;

import jcog.Util;
import nars.Param;
import org.jetbrains.annotations.NotNull;

import static nars.truth.TruthFunctions.c2wSafe;


/**
 * truth rounded to a fixed size precision
 * to support hashing and equality testing
 */
public final class DiscreteTruth implements Truth {

    /**
     * truth component resolution of a 16-bit encoding
     */
    static final int hashDiscreteness16 = Short.MAX_VALUE - 1;

    public static DiscreteTruth Null = new DiscreteTruth(0xffffffff);

    public final float freq, conf;
    public final int hash;

    public DiscreteTruth(float f, float c) {
        this(f, c, Param.TRUTH_EPSILON);
    }

    public DiscreteTruth(float f, float c, float epsilon) {
        this.hash = truthToInt(
                this.freq = Truth.freq(f, epsilon),
                this.conf = Truth.conf(c, epsilon)
        );
    }
    private DiscreteTruth(int hash) {
        this.freq = Float.NaN;
        this.conf = Float.NaN;
        this.hash = hash;
    }

    /**
     * The hash code of a TruthValue, perfectly condensed,
     * into the two 16-bit words of a 32-bit integer.
     * <p>
     * Since the same epsilon used in other truth
     * resolution here (Truth components do not necessarily utilize the full
     * resolution of a floating point value, and also do not necessarily
     * need the full resolution of a 16bit number when discretized)
     * the hash value can be used for equality comparisons
     * as well as non-naturally ordered / non-lexicographic
     * but deterministic compareTo() ordering.
     * correct behavior of this requires epsilon
     * large enough such that: 0 <= h < 2^15:
     */
    public static int truthToInt(float freq, float conf) {

        int freqHash = Util.hashFloat(freq, hashDiscreteness16) & 0x0000ffff;
        int confHash = Util.hashFloat(conf, hashDiscreteness16) & 0x0000ffff;

        return (freqHash << 16) | confHash;
    }

    @Override
    public DiscreteTruth neg() {
        return new DiscreteTruth(1f - freq(), conf());
    }

    @NotNull
    public static Truth intToTruth(int h) {
        return new DiscreteTruth(
                Util.unhashFloat((h >> 16) /* & 0xffff*/, hashDiscreteness16),
                Util.unhashFloat(h & 0xffff, hashDiscreteness16)
        );
    }

    @Override
    public final float freq() {
        return freq;
    }

    @Override
    public final float conf() {
        return conf;
    }

    @Override
    public float evi() {
        return c2wSafe(conf);
    }

    @NotNull
    @Override
    public String toString() {
        //return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;

        //1 + 6 + 1 + 6 + 1
        return appendString(new StringBuilder(7)).toString();
    }

    @Override
    public final boolean equals(Object that) {
        return
            (this == that)
                    ||
            ((that instanceof DiscreteTruth) ? (hash == ((DiscreteTruth)that).hash) :
                    equals((Truthed) that, Param.TRUTH_EPSILON));
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
