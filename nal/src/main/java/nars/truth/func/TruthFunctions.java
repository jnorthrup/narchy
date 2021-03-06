/*
 * TruthFunctions.java
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
 * but WITHOUT ANY WARRANTY; without even the abduction warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http:
 */
package nars.truth.func;

import nars.$;
import nars.NAL;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.and;
import static jcog.Util.or;
import static nars.$.*;
import static nars.NAL.HORIZON;
import static nars.truth.func.TruthFunctions2.weak;

/**
 * All truth-value (and desire-value) functions used in logic rules
 */
public final class TruthFunctions{

    /* ----- Single argument functions, called in MatchingRules ----- */

    /**
     * {<A ==> B>} |- <B ==> A>
     *
     * @param t Truth value of the premise
     * @return Truth value of the conclusion
     */
    public static Truth conversion(Truth t, float minConf) {
        float c = (float) w2cSafe((double) t.freq() * t.confDouble());
        return c >= minConf ? INSTANCE.tt(1.0F, c) : null;
    }

    /* ----- Single argument functions, called in StructuralRules ----- */


//    /**
//     * {<A ==> B>} |- <--B ==> --A>
//     *
//     * @param t Truth value of the premise
//     * @return Truth value of the conclusion
//     */
//    public static Truth contraposition(Truth t, float minConf) {
//        float c = w2cSafe((1 - t.freq()) * t.conf());
//        return c >= minConf ? tt(0, c) : null;
//    }


    /**
     * {M, <M ==> P>} |- P
     *
     * @param a        Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return AnalyticTruth value of the conclusion, because it is structural
     */
    private static @Nullable Truth deductionR(Truth a, double reliance, float minConf) {
        float f = a.freq();
        float c = and(f, confCompose(a, reliance));
        return (c >= minConf) ? INSTANCE.tt(f, c) : null;
    }


    /**
     * commutative
     * {<M --> P>, <S --> M>} |- <S --> P>
     * {<S --> M>, <M --> P>} |- <P --> S>
     */
    public static @Nullable Truth deduction(Truth x, Truth y, boolean strong, float minConf) {
        float cxy = confCompose(x, y);
        if (cxy < minConf)
            return null;

        float fxy = and(x.freq(), y.freq());

        float c = and(fxy, cxy); if (c < minConf) return null;
        if (!strong)
            c = weak(c);

        return (c < minConf) ? null : INSTANCE.tt(fxy, c);
    }

    /**
     * {<S ==> M>, <M <=> P>} |- <S ==> P>
     *
     * stronger than deduction such that A's frequency does not reduce the output confidence
     */
    public static @Nullable Truth analogy(Truth a, float bf, double bc, float minConf) {
        float c = and(confCompose(a, bc), bf);
        return c >= minConf ? INSTANCE.tt(and(a.freq(), bf), c) : null;
    }


    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     *
     * stronger than analogy
     */
    static Truth resemblance(Truth  v1, Truth  v2, float minConf) {
        float f1 = v1.freq();
        float f2 = v2.freq();
        float c = and(confCompose(v1, v2), or(f1, f2));
        return c >= minConf ? INSTANCE.tt(and(f1, f2), c) : null;
    }

    /**
     * {<S ==> M>, <P ==> M>} |- <S ==> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion, or null if either truth is analytic already
     */
    public static Truth induction(Truth a, Truth b, float minConf) {
        float ab = confCompose(a, b);
        if (ab < minConf) return null;
        float c = w2cSafe(ab * b.freq());
        return c >= minConf ? $.INSTANCE.tt(a.freq(), c) : null;
    }


    /**
     * commutative
     * {<M ==> S>, <P ==> M>} |- <S ==> P>
     */
    static Truth exemplification(Truth a, Truth b, float minConf) {
        float c = w2cSafe(a.freq() * b.freq() * confCompose(a, b));
        return c >= minConf ? INSTANCE.tt(1.0F, c) : null;
    }


    public static @Nullable Truth comparison(Truth a, Truth b, float minConf) {
        float f1 = a.freq();
        float f2 = b.freq();
//        if (f1 < 0.5f) {
//            //negative polarity
//            f1 = 1 - f1;
//            f2 = 1 - f2;
//        }

        float f0 = or(f1, f2);
        double w = (double) and(f0, confCompose(a, b));
        double c = w2cSafe(w);
        if (c < (double) minConf)
            return null;

        float f = (f0 < NAL.truth.TRUTH_EPSILON) ? (float) 0 : (and(f1, f2) / f0);
        return $.INSTANCE.tt(f,(float)c);
    }

//    /**
//     * {<M ==> S>, <M ==> P>} |- <S <=> P>
//     *
//     * @param a Truth value of the first premise
//     * @param b Truth value of the second premise
//     * @return Truth value of the conclusion
//     */
//    @Nullable
//    private static Truth comparison(Truth a, boolean negA, Truth b, float minConf) {
//        float cc = TruthFunctions.confCompose(a, b);
//        if (cc < minConf) return null;
//
//        float f1 = a.freq();
//        if (negA) f1 = 1 - f1;
//
//        float f2 = b.freq();
//
//
//        float f0 =
//                //or(f1, f2);
//                Math.max(and(f1, f2), and(1 - f1, 1 - f2));
//        float c = w2cSafe(and(f0, cc));
//        if (!(c >= minConf))
//            return null;
//
//        //float f = (Util.equals(f0, 0, NAL.truth.TRUTH_EPSILON)) ? 0 : (and(f1, f2) / f0);
//        return tt(f0, c);
//    }


    /** TODO return double */
    public static float confCompose(Truth a, double b) {
        return confCompose(a.confDouble(), b);
    }

    /** TODO return double */
    public static float confCompose(Truth a, Truth b) {
        return confCompose(a.confDouble(), b.confDouble());
    }

    /** TODO return double */
    public static float confCompose(double cx, double cy) {
        //convinced
        //classic
        return NAL.nal_truth.STRONG_COMPOSITION ? (float) Math.min(cx, cy) : (float) (cx * cy);
    }


    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
     *
     * @param x Truth value of the first premise
     * @param y Truth value of the second premise
     * @return Truth value of the conclusion
     * <p>
     * In the confidence functions, each case for the conclusion to reach its
     * maximum is separately considered. The plus operator is used in place of an
     * or operator, because the two cases involved are mutually exclusive, rather
     * than independent of each other.
     * <p>
     * Fint : Intersection
     * f = and(f1, f2)
     * c = or(and(not(f1), c1), and(not(f2), c2)) + and(f1, c1, f2, c2)
     * Funi : Union
     * f = or(f1, f2)
     * c = or(and(f1, c1), and(f2, c2)) + and(not(f1), c1, not(f2), c2)
     * Fdif : Difference
     * f = and(f1, not(f2))
     * c = or(and(not(f1), c1), and(f2, c2)) + and(f1, c1, not(f2), c2)
     * <p>
     * https://en.wikipedia.org/wiki/Fuzzy_set_operations
     * https://en.wikipedia.org/wiki/Fuzzy_set#Fuzzy_set_operations
     * https://en.wikipedia.org/wiki/T-norm
     */
    public static @Nullable Truth intersection(Truth x, Truth y, float minConf) {


        //not commutive:
//        float c1 = v1.conf(), c2 = v2.conf();
//        float c = or(and((1-f1), c1), and((1-f2), c2)) + and(f1, c1, f2, c2);
//        c = Util.min(c, Util.max(c1, c2));

        return intersection(x, false, y, false, minConf);
    }
    public static @Nullable Truth intersection(Truth x, boolean negX, Truth y, boolean negY, float minConf) {
        float c = confCompose(x, y);
        return (c < minConf) ? null : $.INSTANCE.tt(and(negIf(x.freq(),negX), negIf(y.freq(),negY)), c);
    }

    private static float negIf(float f, boolean neg) {
        return neg ? (1.0F -f) : f;
    }

    /**
     * {(--, (&&, A, B)), B} |- (--, A)
     *
     * alternate names: "interdeduction" "deductersection"
     * return neg(deduct(intersect(neg(v1), v2), 1f));
     */
    @Deprecated
    static @Nullable Truth reduceConjunction(Truth a, Truth b, float minConf) {
        Truth i12 = intersection(a, true, b, false, minConf);
        if (i12 == null) return null;

        Truth v11 = deductionR(i12, 1.0, minConf);
        if (v11 == null) return null;

        return v11.neg();
    }


    /**
     * {(&&, <#x() ==> M>, <#x() ==> P>), S ==> M} |- <S ==> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth anonymousAnalogy(Truth a, Truth b, float minConf) {
        double v0c = w2cSafe(a.confDouble());
        return v0c < (double) minConf ? null : TruthFunctions.analogy(b, a.freq(), v0c, minConf);
    }


//    /** soft "sqrt" variation */
//    @Nullable public static Truth decomposeSoft(Truth X, Truth Y, boolean x, boolean y, boolean z, float minConf) {
//        float cxy = confCompose(X, Y);
//        if (cxy >= minConf) {
//            float fx = X.freq(), fy = Y.freq();
//            float fxy = and(
//                    x ? fx : 1 - fx,
//                    y ? fy : 1 - fy
//            );
//            //float fxySqrt = (float)Math.sqrt(fxy);
//            float c =
//                    cxy;
//                    //fxySqrt * cxy;
//
//            if (c >= minConf)
//                return tt(z ? fxy : 1 - fxy, c);
//
//        }
//        return null;
//    }

    /** original OpenNARS desire function */
    public static Truth desire(Truth a, Truth b, float minConf, boolean strong) {
        float f1 = a.freq();
        float f2 = b.freq();
        float f = and(f1, f2);
        float c12 = confCompose(a, b);
        float c = and(c12, f2) * (strong ? 1.0F : w2cSafe(1.0F));
        return c > minConf ? INSTANCE.tt(f, c) : null;
    }

    public static float c2w(float c) {
        return (float)c2w((double)c);
    }

    public static double c2w(double c) {
        if (c < (double) NAL.truth.TRUTH_EPSILON)
            throw new Truth.TruthException("confidence underflow", c);

        if (c > (double) NAL.truth.CONF_MAX) {
            throw new Truth.TruthException("confidence overflow", c);
            //c = Param.TRUTH_CONF_MAX;
        }

        if (!Double.isFinite(c))
            throw new Truth.TruthException("non-finite confidence", c);

        return c2wSafe(c);
    }

    public static float c2wSafe(float c) {
        return c2wSafe(c, HORIZON);
    }
    public static double c2wSafe(double c) {
        return c2wSafe(c, HORIZON);
    }


    /**
     * A function to convert confidence to weight
     * http://www.wolframalpha.com/input/?i=x%2F(1-x)
     *
     * @param c confidence, in [0, 1)
     * @return The corresponding weight of evidence, a non-negative real number
     */
    public static float c2wSafe(float c, float horizon) {
        return horizon * c / (1.0F - c);
    }
    public static double c2wSafe(double c, float horizon) {
        return (double) horizon * c / (1.0 - c);
    }

    /**
     * http://www.wolframalpha.com/input/?i=x%2F(x%2B1)
     */
    public static double w2cSafe(double w, float horizon) {
        return w / (w + (double) horizon);
    }
    public static float w2cSafe(float w, float horizon) {
        return w / (w + horizon);
    }

    /**
     * A function to convert weight to confidence
     *
     * @param w Weight of evidence, a non-negative real number
     * @return The corresponding confidence, in [0, 1)
     */
    public static float w2c(float w) {
        if ((double) w < NAL.truth.EVI_MIN)
            throw new Truth.TruthException("insufficient evidence", (double) w);
        if (!Float.isFinite(w))
            throw new Truth.TruthException("non-finite evidence", (double) w);
        return w2cSafe(w);
    }
    public static double w2c(double w) {
        if (w < NAL.truth.EVI_MIN)
            throw new Truth.TruthException("insufficient evidence", w);
        if (!Double.isFinite(w))
            throw new Truth.TruthException("non-finite evidence", w);
        return w2cSafe(w);
    }

    public static float w2cSafe(float w) {
        return w2cSafe(w, HORIZON);
    }

    public static double w2cSafe(double e) {
        return w2cSafeDouble(e);
    }
    /** high precision */
    public static double w2cSafeDouble(double e) {
        return w2cSafe(e, HORIZON);
    }


    public static float originality(int evidenceLength) {
        return evidenceLength <= 1 ? 1f : 1f / (1f + (float) (evidenceLength - 1) / ((float) NAL.STAMP_CAPACITY - 1f));
    }

    public static double expectation(float frequency, double confidence) {
        return (confidence * ((double) frequency - 0.5) + 0.5);
    }

    public static double eternalize(double evi) {
        return w2cSafeDouble(evi);
    }

    public static Truth union(Truth t, Truth b, float minConf) {
        @Nullable Truth z = TruthFunctions.intersection(t, true, b, true, minConf);
        return z != null ? z.neg() : null;
    }


}























