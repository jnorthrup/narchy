/*
 * TruthValue.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.truth;

import jcog.Texts;
import jcog.Util;
import nars.NAR;
import nars.Op;
import nars.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static jcog.Util.*;
import static nars.truth.TruthFunctions.w2cSafe;


/** scalar (1D) truth value "frequency", stored as a floating point value */
public interface Truth extends Truthed {
    /**
     * truth component resolution of a 16-bit encoding
     * this maximally uses the positive half of short (16-bit)'s range of precision
     */
    short hashDiscreteness16 = Short.MAX_VALUE - 1;

    /**
     * truth component resolution corresponding to Param.TRUTH_EPSILON
     */
    short hashDiscretenessEpsilon = (short)
            (Math.round(1f/(Param.TRUTH_EPSILON)));

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
    static int truthToInt(float freq, float conf, short discreteness) {

        if (!(Float.isFinite(freq) && (freq >= 0) && (freq <= 1)))
            throw new RuntimeException("invalid freq: " + freq);

        int freqHash = floatToInt(freq, discreteness);// & 0x0000ffff;

        if (!(Float.isFinite(conf) && conf >= 0 && conf <= Param.TRUTH_MAX_CONF))
            throw new RuntimeException("invalid conf: " + conf);

        int confHash = floatToInt(conf, discreteness);// & 0x0000ffff;

        return (freqHash << 16) | confHash;
    }

    static Truth intToTruth(int h) {
        return new DiscreteTruth(h);
    }

    static float freq(int h) {
        return intToFloat((h >> 16) /* & 0xffff*/, hashDiscretenessEpsilon);
    }

    static float conf(int h) {
        return intToFloat(h & 0xffff, hashDiscretenessEpsilon);
    }

//    float EVI_MIN = c2w(Param.TRUTH_EPSILON);

    @Override
    float freq();

    @Override
    float evi();


//    Term Truth_TRUE = $.the("TRUE");
//    Term Truth_FALSE = $.the("FALSE");
//    Term Truth_UNSURE = $.the("UNSURE"); //only really valid for describing expectation, not frequency by itself
//    Term Truth_MAYBE = $.the("MAYBE");
//    Term Truth_CERTAIN = $.the("CERTAIN");
//    Term Truth_UNCERTAIN = $.the("UNCERTAIN");
//
//    Comparator<Truthed> compareConfidence = (o1, o2) -> {
//        if (o1.equals(o2))
//            return 0;
//
//        float b = o2.conf();
//        float a = o1.conf();
//        if (b < a)
//            return -1;
//        else
//            return +1;
//    };



    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accurate to 1%
     */
    static StringBuilder appendString(StringBuilder sb, int decimals, float freq, float conf) {

        sb.ensureCapacity(3 + 2 * (2 + decimals) );

        return sb
                .append(Op.TRUTH_VALUE_MARK)
                .append(Texts.n(freq, decimals))
                .append(Op.VALUE_SEPARATOR)
                .append(Texts.n(conf, decimals))
                .append(Op.TRUTH_VALUE_MARK);

    }

    @Nullable
    @Override
    default Truth truth() { return this; }

    /**
     * Calculate the expectation value of the truth value
     *
     * @return The expectation value
     */
    @Override
    default float expectation() {
        return TruthFunctions.expectation(freq(), conf());
    }

    default float expectation(float factor) {
        return TruthFunctions.expectation(freq(), conf()*factor);
    }

    default float expectationNeg() {
        return TruthFunctions.expectation(1-freq(), conf());
    }


    @NotNull
    default StringBuilder appendString(StringBuilder sb) {
        return Truth.appendString(sb, 2, freq(), conf());
    }



    default String _toString() {
        //return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;
        //1 + 6 + 1 + 6 + 1
        return appendString(new StringBuilder(7)).toString();
    }

    static int compare(Truth a, Truth b) {
        if (a == b) return 0;

        //see how Truth hash() is calculated to know why this works
        return Integer.compare(b.hashCode(), a.hashCode());

//        tc = Float.compare(truth.getFrequency(), otruth.getFrequency());
//        if (tc!=0) return tc;
//        tc = Float.compare(truth.getConfidence(), otruth.getConfidence());
//        if (tc!=0) return tc;
//
//        return 0;
    }


//    @NotNull
//    default Truth interpolate(Truthed y) {
//        float xc = confWeight();
//        float yc = y.confWeight();
//
//        return new DefaultTruth(
//                //lerp by proportion of confidence contributed
//                lerp(freq(), y.freq(), xc / (xc+yc)),
//
//                //difference in freq means closer to the AND conf, otherwise if they are the same then closer to max
//                lerp(and(xc, yc), max(xc, yc), Math.abs(freq()-y.freq()))
//
//        );
//    }

    /** the negated (1 - freq) of this truth value */
    default Truth neg() {
        return new PreciseTruth(1f - freq(), conf());
    }

    default boolean equalsIn(@Nullable Truth x, float fTol, float cTol) {
        return x == this || (x!=null
                && Util.equals(conf(), x.conf(), cTol)
                && Util.equals(freq(), x.freq(), fTol)
        );
    }

    default boolean equalsIn(@Nullable Truth x, float tolerance) {
        return equalsIn(x, tolerance, tolerance);
    }

    default boolean equalsIn(@Nullable Truth x, NAR nar) {
        return equalsIn(x, nar.freqResolution.floatValue(), nar.confResolution.floatValue());
    }

    default Truth negIf(boolean negate) {
        return negate ? neg() : this;
    }

//    default float eternalizedConf() {
//        return TruthFunctions.eternalize(conf());
//    }
//    default float eternalizedConfWeight() {
//        //TODO does this simplify?
//        return c2w(TruthFunctions.eternalize(conf()));
//    }

    @Nullable
    static Truth stronger(@Nullable Truth a, @Nullable Truth b) {
        if (b == null)
            return a;
        if (a == null)
            return b;
        return a.evi() >= b.evi() ? a : b;
    }

    static float freq(float f, NAR n) {
        return freq(f, n.freqResolution.floatValue());
    }

    static float freq(float f, float epsilon) {
        assert(f==f): "invalid freq: " + f;
        return unitize(round(f, epsilon));
    }

    static float conf(float c, NAR n) {
        return freq(c, n.confResolution.floatValue());
    }

    static float conf(float c, float epsilon) {
        assert(c==c && c >= Param.TRUTH_EPSILON): "invalid conf: " + c;
        return confSafe(c, epsilon);
    }

    static float confSafe(float c, float epsilon) {
        if (epsilon == 0)
            return c; //unchanged

        return clamp(
                //ceil(c, epsilon), //optimistic
                round(c, epsilon), //semi-optimistic: adds evidence when rounding up, loses evidence when rounding down
                //floor(c, epsilon), //conservative
                0, 1f - epsilon);
    }


    @Nullable default PreciseTruth dither(NAR nar) {
        return theDithered(freq(), evi(), nar);
    }

    @Nullable
    static PreciseTruth theDithered(float f, float e, NAR nar) {
        return theDithered(f, nar.freqResolution.floatValue(), e, nar.confResolution.floatValue(), nar.confMin.floatValue());
    }

    @Nullable static PreciseTruth theDithered(float f, float fRes, float evi, float cRes, float confMin) {
        float c = w2cDithered(evi, cRes);
        return c >= confMin ? new PreciseTruth(freq(f, fRes), c) : null;
    }

    @Nullable default PreciseTruth dither(NAR nar, float eviGain) {
        return dither(nar.freqResolution.floatValue(), nar.confResolution.floatValue(), nar.confMin.floatValue(), eviGain);
    }

    @Deprecated @Nullable default PreciseTruth dither(float freqRes, float confRes, float confMin, float eviGain) {
        float c = w2cDithered(evi() * eviGain, confRes);
        return c < confMin ? null : new PreciseTruth(freq(freq(), freqRes), c);
    }

    @Nullable default PreciseTruth dither(float freqRes, float confRes, float confMin) {
        float c = w2cDithered(evi(), confRes);
        return c < confMin ? null : new PreciseTruth(freq(freq(), freqRes), c);
    }
    @Nullable default Truth ditherFreq(float freqRes) {
        return freqRes!=0 ? new PreciseTruth(freq(freq(), freqRes), evi(), false) : this;
    }





    static float w2cDithered(float evi, float confRes) {
        return confSafe(w2cSafe(evi), confRes);
    }


    default float freqTimesConf() {
        return freq() * conf();
    }

    default PreciseTruth withConf(float c) {
        return new PreciseTruth(freq(), c);
    }

    default PreciseTruth withEvi(float e) {
        if (e == 0)
            return null;
        return new PreciseTruth(freq(), e, false);
    }


    static void write(Truth t, DataOutput out) throws IOException {
        out.writeFloat(t.freq());
        out.writeFloat(t.conf());
    }

    static Truth read(DataInput in) throws IOException {
        float f = in.readFloat();
        float c = in.readFloat();
        return new PreciseTruth(f, c);
    }

    default Truth eternalized(float factor) {
        return new PreciseTruth(freq(), factor * eviEternalized(), false);
    }
    default Truth eternalized() {
        return eternalized(1f);
    }



//    default Truth eternalized() {
//        return $.t(freq(), eternalizedConf());
//    }


//    static <T extends Truthed> T minConf(T a, T b) {
//        return a.conf() <= b.conf() ? a : b;
//    }


//    enum TruthComponent {
//        Frequency, Confidence, Expectation
//    }
//
//    default float component(TruthComponent c) {
//        switch (c) {
//            case Frequency: return freq();
//            case Confidence: return conf();
//            case Expectation: return expectation();
//        }
//        return Float.NaN;
//    }
//
//    /** provides a statistics summary (mean, min, max, variance, etc..) of a particular TruthValue component across a given list of Truthables (sentences, TruthValue's, etc..).  null values in the iteration are ignored */
//    @NotNull
//    static DescriptiveStatistics statistics(Iterable<? extends Truthed> t, TruthComponent component) {
//        DescriptiveStatistics d = new DescriptiveStatistics();
//        for (Truthed x : t) {
//            Truth v = x.truth();
//            if (v!=null)
//                d.addValue(v.component(component));
//        }
//        return d;
//    }





}
