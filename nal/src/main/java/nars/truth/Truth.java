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
 * along with Open-NARS.  If not, see <http:
 */
package nars.truth;

import jcog.Texts;
import jcog.Util;
import jcog.WTF;
import nars.NAR;
import nars.Op;
import nars.Param;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static jcog.Util.*;
import static nars.truth.TruthFunctions.w2cSafe;


/**
 * scalar (1D) truth value "frequency", stored as a floating point value
 */
public interface Truth extends Truthed {

    /**
     * truth component resolution corresponding to Param.TRUTH_EPSILON
     */
    short hashDiscretenessEpsilon = (short)
            (Math.round(1f / (Param.TRUTH_EPSILON)));

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

        int freqHash = floatToInt(freq, discreteness);

        if (!(Float.isFinite(conf) && conf >= 0))
            throw new RuntimeException("invalid conf: " + conf);

        int confHash = floatToInt(Math.min(Param.TRUTH_MAX_CONF, conf), discreteness);

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

    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accurate to 1%
     */
    static StringBuilder appendString(StringBuilder sb, int decimals, float freq, float conf) {

        sb.ensureCapacity(3 + 2 * (2 + decimals));

        return sb
                .append(Op.TRUTH_VALUE_MARK)
                .append(Texts.n(freq, decimals))
                .append(Op.VALUE_SEPARATOR)
                .append(Texts.n(conf, decimals))
                .append(Op.TRUTH_VALUE_MARK);

    }

    static int compare(Truth a, Truth b) {
        if (a == b) return 0;


        return Integer.compare(b.hashCode(), a.hashCode());


    }

    @Nullable
    static <T extends Truthed> T stronger(@Nullable T a, @Nullable T b) {
        if (b == null)
            return a;
        if (a == null)
            return b;
        return a.evi() >= b.evi() ? a : b;
    }

    static float freq(float f, float epsilon) {
        assert (f == f) : "invalid freq: " + f;

        Util.assertUnitized(f);
        return Util.unitize(round(f, epsilon));
    }

    static float conf(float c, float epsilon) {
        assert (c == c && c >= Param.TRUTH_EPSILON) : "invalid conf: " + c;
        return confSafe(c, epsilon);
    }

    static float confSafe(float c, float epsilon) {
        if (epsilon == 0)
            return c;

        return clamp(

                round(c, epsilon),

                0, 1f - epsilon);
    }

    @Nullable
    static PreciseTruth theDithered(float f, float e, NAR nar) {
        return theDithered(f, nar.freqResolution.floatValue(), e, nar.confResolution.floatValue(), nar.confMin.floatValue());
    }

    @Nullable
    static PreciseTruth theDithered(float f, float fRes, float evi, float cRes, float confMin) {
        float c = w2cDithered(evi, cRes);
        return c >= confMin ? new PreciseTruth(freq(f, fRes), c) : null;
    }

    static float w2cDithered(float evi, float confRes) {
        return confSafe(w2cSafe(evi), confRes);
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

    @Override
    float freq();

    @Override
    float evi();

    @Nullable
    @Override
    default Truth truth() {
        return this;
    }

    default StringBuilder appendString(StringBuilder sb) {
        return Truth.appendString(sb, 2, freq(), conf());
    }

    default String _toString() {


        return appendString(new StringBuilder(7)).toString();
    }

    /**
     * the negated (1 - freq) of this truth value
     */
    default Truth neg() {
        return new PreciseTruth(1f - freq(), conf());
    }

    default boolean equalsIn(@Nullable Truth x, float fTol, float cTol) {
        return x == this || (x != null
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

    @Nullable
    default PreciseTruth dither(NAR nar) {
        return theDithered(freq(), evi(), nar);
    }

    @Deprecated
    @Nullable
    default PreciseTruth dither(float freqRes, float confRes, float confMin, float eviGain) {
        float c = w2cDithered(evi() * eviGain, confRes);
        return c < confMin ? null : new PreciseTruth(freq(freq(), freqRes), c);
    }

    @Nullable
    default Truth ditherFreq(float freqRes) {
        return freqRes != 0 ? new PreciseTruth(freq(freq(), freqRes), evi(), false) : this;
    }

    default float freqTimesConf() {
        return freq() * conf();
    }

    default Truth eternalized(float factor, float eviMin, @Nullable NAR n) {
        float f = freq();
        float e = factor * eviEternalized();
        if (e < eviMin)
            return null;
        if (n!=null) {
            return Truth.theDithered(f, e, n);
        } else {
            return new PreciseTruth(f, e, false);
        }
    }

    default void ensureDithered(NAR n) {
        Truth d = dither(n);
        if (!equals(d))
            throw new WTF("not dithered");
    }

//    default Truth eternalized() {
//        return eternalized(1f);
//    }


}
