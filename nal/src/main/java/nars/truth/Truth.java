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

import com.google.common.io.ByteArrayDataOutput;
import jcog.Texts;
import jcog.Util;
import nars.NAL;
import nars.Op;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.IOException;

import static jcog.WTF.WTF;
import static nars.truth.func.TruthFunctions.*;


/**
 * scalar (1D) truth value "frequency", stored as a floating point value
 *
 * floating-point precision testing tool:
 *      http://herbie.uwplse.org/demo/
 */
public interface Truth extends Truthed {

    /**
     * truth component resolution corresponding to Param.TRUTH_EPSILON
     */
    short hashDiscretenessCoarse = (short)
            (Math.round(1.0 / (NAL.truth.TRUTH_EPSILON)));
    int hashDiscretenessFine = (int) Math.round(1.0 / (NAL.truth.TASK_REGION_CONF_EPSILON));

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
    static int truthToInt(float freq, short freqDisc, float conf, short confDisc) {

        if (!Float.isFinite(freq) || freq < 0 || freq > 1)
            throw new TruthException("invalid freq", freq);

        if (!Float.isFinite(conf) || conf < 0)
            throw new TruthException("invalid conf", conf);

        var freqHash = Util.toInt(freq, freqDisc);
        var confHash = Util.toInt(Math.min(NAL.truth.CONF_MAX, conf), confDisc);
        return (freqHash << 16) | confHash;
    }
    static int truthToInt(double freq, short freqDisc, double conf, short confDisc) {

        if (!Double.isFinite(freq) || freq < 0 || freq > 1)
            throw new TruthException("invalid freq", freq);

        if (!Double.isFinite(conf) || conf < 0)
            throw new TruthException("invalid conf", conf);

        var freqHash = (int) Util.toInt(freq, freqDisc);
        var confHash = (int) Util.toInt(Math.min(NAL.truth.CONF_MAX, conf), confDisc);
        return (freqHash << 16) | confHash;
    }

    static int truthToInt(float freq, float conf) {
        return truthToInt(freq, hashDiscretenessCoarse, conf, hashDiscretenessCoarse);
    }
    static int truthToInt(double freq, double conf) {
        return truthToInt(freq, hashDiscretenessCoarse, conf, hashDiscretenessCoarse);
    }


    static float polarity(float freq) {
        return Math.abs(freq - 0.5f) * 2f;
    }

    static void assertDithered(@Nullable Truth t, NAL n) {
        if (t != null) {
            Truth d = t.dither(n);
            if (!t.equals(d))
                throw WTF("not dithered");
        }
    }

    /** returns the un-negated, positive only form */
    default Truth pos() {
        return isNegative() ? neg() : this;
    }



    class TruthException extends RuntimeException {
        public TruthException(String reason, double value) {
            super(reason + ": " + value);
        }
    }

    static Truth intToTruth(int h) {
        return new DiscreteTruth(h);
    }

    static int freqI(int h) {
        return h >> 16;
    }

    static int confI(int h) {
        return h & 0xffff;
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



    static @Nullable <T extends Truthed> T stronger(@Nullable T a, @Nullable T b) {
        if (b == null)
            return a;
        else if (a == null)
            return b;
        else
            return a.evi() >= b.evi() ? a : b;
    }

//    /** TODO make int compare(a,b) */
//    @Nullable static Task stronger(@Nullable Task a, @Nullable Task b, long s, long e) {
//        if (a == null)
//            return b;
//        else if (b == null || a.equals(b))
//            return a;
//        else {
//            boolean ae = a.isEternal(), be = b.isEternal();
//            if (ae && be) {
//                return a.evi() >= b.evi() ? a : b;
//            } else if (ae || be) {
//                if (be) {
//                    @Nullable Task x = a;
//                    a = b;
//                    b = x; //swap so that 'b' is temporal
//                }
//                return TruthIntegration.evi(a, s, e, 0) >= TruthIntegration.evi(b) ? a : b;
//            } else if (s == TIMELESS || s == ETERNAL) {
//                //auto range
//                return TruthIntegration.evi(a) >= TruthIntegration.evi(b) ? a : b;
//            } else {
//                return TruthIntegration.evi(a, s, e, 0) >= TruthIntegration.evi(b, s, e, 0) ? a : b;
//            }
//        }
//    }


    static float freq(float f, float epsilon) {
        if (!Float.isFinite(f))
            throw new TruthException("non-finite freq", f);
        return freqSafe(f, epsilon);
    }

    static float freqSafe(float f, float epsilon) {
        return Util.unitizeSafe(Util.roundSafe(f, epsilon));
    }

    static double conf(double c, float epsilon) {
        if (!Double.isFinite(c))
            throw new TruthException("non-finite conf", c);
//        assert (c >= Param.TRUTH_EPSILON) : "invalid conf: " + c;
        return confSafe(c, epsilon);
    }

    static double confSafe(double c, float epsilon) {
		return epsilon <= Double.MIN_NORMAL ? c : Util.clampSafe(

			Util.roundSafe(c, epsilon),

			0, 1.0 - epsilon);
    }

    static @Nullable PreciseTruth theDithered(float f, double e, NAL nar) {
        if (e < nar.confMin.evi())
            return null;

        //keep evidence difference
        return PreciseTruth.byConfEvi(
                Truth.freq(f, nar.freqResolution.floatValue()),
                Truth.w2cDithered((float)e, nar.confResolution.floatValue()),
                e);

    }

    static double w2cDithered(double evi, float confRes) {
        return confSafe(w2cSafeDouble(evi), confRes);
    }

    static void write(Truth t, ByteArrayDataOutput out)  {
        out.writeFloat(t.freq());
        out.writeFloat(t.conf());
    }

    static Truth read(DataInput in) throws IOException {
        var f = in.readFloat();
        var c = in.readFloat();
        return PreciseTruth.byConf(f, c);
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
        return PreciseTruth.byEvi(1f - freq(), evi());
    }

    default boolean equalTruth(@Nullable Truth x, float fTol, float cTol) {
        return x == this || (x != null
                && Util.equals(conf(), x.conf(), cTol)
                && Util.equals(freq(), x.freq(), fTol)
        );
    }

    default boolean equalTruth(@Nullable Truth x, float tolerance) {
        return equalTruth(x, tolerance, tolerance);
    }

    default boolean equalTruth(@Nullable Truth x, NAL nar) {
        return equalTruth(x, nar.freqResolution.floatValue(), nar.confResolution.floatValue());
    }

    default Truth negIf(boolean negate) {
        return negate ? neg() : this;
    }

    static @Nullable Truth negIf(@Nullable Truth t, boolean n) {
        return n && t!=null ? t.neg() : t;
    }

    default @Nullable PreciseTruth dither(NAL nar) {
        return theDithered(freq(), evi(), nar);
    }


    default Truth dither(float freqRes, float confRes) {
        if (freqRes <= NAL.truth.TRUTH_EPSILON && confRes <= NAL.truth.TRUTH_EPSILON)
            return this;

        var f = freq();
        var ff = freqSafe(f, freqRes);
        var c = conf();
        var cc = confSafe(c, confRes);
		//return PreciseTruth.byConf(ff, cc);
		return Util.equals(f, ff, NAL.truth.TRUTH_EPSILON) && Util.equals(c, cc, NAL.truth.TRUTH_EPSILON) ? this : PreciseTruth.byConfEvi(ff, cc, evi() /* include extra precision */);
    }

    static Truth dither(Truth in, double eviMin, boolean negate, NAL nar) {
        var e = in.evi();
        if (e < eviMin)
            return null;

        var freqRes = nar.freqResolution.floatValue();
        var confRes = nar.confResolution.floatValue();

        if (!negate && freqRes < NAL.truth.TRUTH_EPSILON && confRes < NAL.truth.TRUTH_EPSILON)
            return in;

        var fBefore = in.freq();
        var fAfter = freq(negate ? 1-fBefore : fBefore, freqRes);
        var cBefore = w2cSafeDouble(e);
        var cAfter = conf(cBefore, confRes);
		return (!(in instanceof MutableTruth)) && (Util.equals(fBefore, fAfter, NAL.truth.TRUTH_EPSILON) && Util.equals(cBefore, cAfter, NAL.truth.EVI_MIN)) ? in : PreciseTruth.byConfEvi(fAfter, cAfter, e /* extra precision */);
    }




    default Truth eternalized(double confFactor, double eviMin, @Nullable NAL n) {
        var c = confFactor * w2cSafe(eviEternalized());
        var e = c2wSafe(c);
        if (e < eviMin)
            return null;

        var f = freq();
		return n != null ? Truth.theDithered(f, e, n) : PreciseTruth.byEvi(f, e);
    }


}
