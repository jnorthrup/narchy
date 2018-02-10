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
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Param;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.*;
import static nars.truth.TruthFunctions.w2c;
import static nars.truth.TruthFunctions.w2cSafe;


/** scalar (1D) truth value "frequency", stored as a floating point value */
public interface Truth extends Truthed {

    @Override
    float freq();

    @Override
    float conf();

    static float eternalize(float conf) {
        return w2c(conf);
    }

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

    //DONT USE THIS IT IS BIASED AGAINST NEGATIVE FREQUENCY TRUTH
//    /**
//     * Calculate the absolute difference of the expectation value and that of a
//     * given truth value
//     *
//     * @param t The given value
//     * @return The absolute difference
//     */
//    default float getExpDifAbs(@NotNull Truth t) {
//        return Math.abs(expectation() - t.expectation());
//    }



    @NotNull
    default StringBuilder appendString(@NotNull StringBuilder sb) {
        return appendString(sb, 2);
    }


    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accruate to 1%
     */
    @NotNull
    default StringBuilder appendString(@NotNull StringBuilder sb, int decimals)  {

        sb.ensureCapacity(3 + 2 * (2 + decimals) );

        return sb
            .append(Op.TRUTH_VALUE_MARK)
            .append(Texts.n(freq(), decimals))
            .append(Op.VALUE_SEPARATOR)
            .append(Texts.n(conf(), decimals))
            .append(Op.TRUTH_VALUE_MARK);
    }


    default String _toString() {
        //return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;
        //1 + 6 + 1 + 6 + 1
        return appendString(new StringBuilder(7)).toString();
    }

    static int compare(@NotNull Truth a, @NotNull Truth b) {
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
//    default Truth interpolate(@NotNull Truthed y) {
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

    default boolean equals(@Nullable  Truthed x, float tolerance) {
        return x!=null
                && Util.equals(conf(), x.conf(), tolerance)
                && Util.equals(freq(), x.freq(), tolerance)
                ;
    }

    default boolean equals(@Nullable Truthed x, NAR nar) {
        return this == x ||
                (x!=null && (Util.equals(freq(), x.freq(), nar.freqResolution.floatValue()) &&
                Util.equals(conf(), x.conf(), nar.confResolution.floatValue())));
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
    static Truth maxConf(@Nullable Truth a, @Nullable Truth b) {
        if (b == null)
            return a;
        if (a == null)
            return b;
        return a.conf() >= b.conf() ? a : b;
    }

    static float freq(float f, float epsilon) {
        assert(f==f): "invalid freq: " + f;
        return unitize(round(f, epsilon));
    }

    static float conf(float c, float epsilon) {
        assert(c==c && c >= Param.TRUTH_EPSILON): "invalid conf: " + c;
        return confSafe(c, epsilon);
    }

    static float confSafe(float c, float epsilon) {
        return clamp(
                //ceil(c, epsilon), //optimistic
                round(c, epsilon), //semi-optimistic: adds evidence when rounding up, loses evidence when rounding down
                //floor(c, epsilon), //conservative
                0, 1f - epsilon);
    }


    @Nullable default PreciseTruth dither(NAR nar) {
        return dither(nar, 1f);
    }

    @Nullable default PreciseTruth dither(NAR nar, float eviGain) {
        return dither(nar.freqResolution.floatValue(), nar.confResolution.floatValue(), nar.confMin.floatValue(), eviGain);
    }

    @Deprecated @Nullable default PreciseTruth dither(float freqRes, float confRes, float confMin, float eviGain) {
        float c = w2cDithered(evi() * eviGain, confRes);
        return c < confMin ? null : new PreciseTruth(freq(freq(), freqRes), c);
    }

    @Nullable default DiscreteTruth ditherDiscrete(NAR nar) {
        return ditherDiscrete(nar.freqResolution.asFloat(), nar.confResolution.asFloat(), nar.confMin.asFloat(), evi());
    }


    @Nullable default DiscreteTruth ditherDiscrete(float freqRes, float confRes, float confMin, float newEvi) {
        float c = w2cDithered(newEvi, confRes);
        return c < confMin ? null : new DiscreteTruth(freq(freq(), freqRes), c);
    }


    /** warning: not dithered */
    @Nullable static PreciseTruth the(float freq, float evi, NAR nar) {
        float confMin = nar.confMin.floatValue();
        float c = w2cSafe(evi); //w2cDithered(evi, nar.confResolution.floatValue());
        if (c < confMin)
            return null;
        return new PreciseTruth(freq /*freq(freq, nar.freqResolution.floatValue())*/, c);
    }

    static float w2cDithered(float evi, float confRes) {
        return confSafe(w2cSafe(evi), confRes);
    }


    default float freqTimesConf() {
        return freq() * conf();
    }

    default float freqNegTimesConf() {
        return (1 - freq()) * conf();
    }

    default PreciseTruth withConf(float c) {
        return $.t(freq(), c);
    }

    default PreciseTruth withEvi(float e) {
        if (e == 0)
            return null;
        return new PreciseTruth(freq(), e, false);
    }

    /** dithers */
    @Nullable static DiscreteTruth theDiscrete(float freq, float e, NAR nar) {
        if (e < Float.MIN_NORMAL)
            return null; //wtf
        float c = conf(w2cSafe(e), nar.confResolution.asFloat());
        if (c < nar.confMin.asFloat())
            return null;
        else
            return new DiscreteTruth(freq(freq, nar.freqResolution.asFloat()), c);
    }


//    default Truth eternalized() {
//        return $.t(freq(), eternalizedConf());
//    }


//    static <T extends Truthed> T minConf(T a, T b) {
//        return a.conf() <= b.conf() ? a : b;
//    }


    enum TruthComponent {
        Frequency, Confidence, Expectation
    }
    
    default float component(@NotNull TruthComponent c) {
        switch (c) {
            case Frequency: return freq();
            case Confidence: return conf();
            case Expectation: return expectation();
        }
        return Float.NaN;
    }
    
    /** provides a statistics summary (mean, min, max, variance, etc..) of a particular TruthValue component across a given list of Truthables (sentences, TruthValue's, etc..).  null values in the iteration are ignored */
    @NotNull
    static DescriptiveStatistics statistics(@NotNull Iterable<? extends Truthed> t, @NotNull TruthComponent component) {
        DescriptiveStatistics d = new DescriptiveStatistics();
        for (Truthed x : t) {
            Truth v = x.truth();
            if (v!=null)
                d.addValue(v.component(component));
        }
        return d;
    }





}
