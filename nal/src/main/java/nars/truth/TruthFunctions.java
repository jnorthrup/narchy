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
package nars.truth;

import jcog.Util;
import nars.$;
import nars.Param;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.and;
import static jcog.Util.or;
import static nars.$.t;

/**
 * All truth-value (and desire-value) functions used in logic rules
 */
public final class TruthFunctions {

    /* ----- Single argument functions, called in MatchingRules ----- */

    /**
     * {<A ==> B>} |- <B ==> A>
     *
     * @param t Truth value of the premise
     * @return Truth value of the conclusion
     */
    public static Truth conversion(Truth t, float minConf) {
        float c = w2cSafe(t.freq() * t.conf());
        return c >= minConf ? t(1, c) : null;
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
//        return c >= minConf ? t(0, c) : null;
//    }


    /**
     * {M, <M ==> P>} |- P
     *
     * @param a        Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return AnalyticTruth value of the conclusion, because it is structural
     */
    @Nullable
    public static Truth deductionR(Truth a, float reliance, float minConf) {
        float f = a.freq();
        float c = and(f, a.conf(), reliance);
        return (c >= minConf) ? t(f, c) : null;
    }


    @Nullable
    public static Truth deduction(Truth a, float bF, float bC, float minConf) {

        float f = and(a.freq(), bF);
        float c = and(f,
                //and(a.conf(), bC)
                confCompose(a.conf(), bC)
        );

        return c >= minConf ? t(f, c) : null;
    }


    /**
     * {<S ==> M>, <M <=> P>} |- <S ==> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth analogy(Truth a, float bf, float bc, float minConf) {
        float c = and(and(a.conf(), bc), bf);
        return c >= minConf ? t(and(a.freq(), bf), c) : null;
    }


    /**
     * {<S ==> M>, <P ==> M>} |- <S ==> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion, or null if either truth is analytic already
     */
    public static Truth induction(Truth a, Truth b, float minConf) {
        float c = w2cSafe(and(a.conf(), b.conf()) * b.freq());
        return c >= minConf ? $.t(a.freq(), c) : null;
    }


    /**
     * {<M ==> S>, <P ==> M>} |- <S ==> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static Truth exemplification(Truth a, Truth b, float minConf) {
        float c = w2cSafe(a.freq() * b.freq() * confCompose(a, b));
        return c >= minConf ? t(1, c) : null;
    }


    @Nullable
    public static Truth comparison(Truth a, Truth b, float minConf) {
        return comparison(a, b, false, minConf);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S <=> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth comparison(Truth a, Truth b, boolean invertA, float minConf) {
        float f1 = a.freq();
        if (invertA) f1 = 1 - f1;

        float f2 = b.freq();


        float f0 = or(f1, f2);
        float c = w2cSafe(and(f0, TruthFunctions.confCompose(a, b)));
        if (c >= minConf) {
            float f = (Util.equals(f0, 0, Param.TRUTH_EPSILON)) ? 0 : (and(f1, f2) / f0);
            return t(f, c);
        }

        return null;
    }




    /*In the confidence functions, each case for the conclusion to reach its
    maximum is separately considered. The plus operator is used in place of an
    or operator, because the two cases involved are mutually exclusive, rather
    than independent of each other.

    Fint : Intersection
    f = and(f1, f2)
    c = or(and(not(f1), c1), and(not(f2), c2)) + and(f1, c1, f2, c2)
    Funi : Union
    f = or(f1, f2)
    c = or(and(f1, c1), and(f2, c2)) + and(not(f1), c1, not(f2), c2)
    Fdif : Difference
    f = and(f1, not(f2))
    c = or(and(not(f1), c1), and(f2, c2)) + and(f1, c1, not(f2), c2)
    */

    public static float confCompose(Truth a, Truth b) {
        return confCompose(a.conf(), b.conf());
    }

    public static float confCompose(float cx, float cy) {
        if (Param.STRONG_COMPOSITION) {
            //convinced
            return Math.min(cx, cy);
        } else {
            //classic
            return cx * cy;
        }
    }


    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth intersection(Truth v1, Truth v2, float minConf) {
        float f1 = v1.freq(), f2 = v2.freq();
        float c = confCompose(v1, v2);
        return (c < minConf) ? null : $.t(and(f1, f2), c);
    }


    /**
     * {(--, (&&, A, B)), B} |- (--, A)
     *
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth reduceConjunction(Truth v1, Truth v2, float minConf) {

        Truth i12 = intersection(v1.neg(), v2, minConf);
        if (i12 == null) return null;

        Truth v11 = deductionR(i12, 1.0f, minConf);
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
    public static Truth anonymousAnalogy(Truth a, Truth b, float minConf) {
        float v0c = w2cSafe(a.conf());

        return v0c < minConf ? null : TruthFunctions2.analogyNew(b, a.freq(), v0c, minConf);
    }

    /**
     * decompose positive / negative
     */
    @Nullable
    public static Truth decompose(Truth X, Truth Y, boolean x, boolean y, boolean z, float minConf) {
        float cxy = confCompose(X,Y);
        if (cxy < minConf) return null;
        float fx = X.freq(), fy = Y.freq();
        float fxy = and(x ? fx : 1 - fx, y ? fy : 1 - fy);
        float c = fxy * cxy;
        /*float c = (c12);*/
        return c < minConf ? null : t(z ? fxy : 1 - fxy, c);
    }


    public static float c2w(float c) {
        if (!Float.isFinite(c))
            throw new Truth.TruthException("non-finite confidence", c);

        if (c < Param.TRUTH_EPSILON)
            throw new Truth.TruthException("confidence underflow", c);

        if (c > Param.TRUTH_MAX_CONF) {
            //throw new Truth.TruthException("confidence overflow", c);
            c = Param.TRUTH_MAX_CONF;
        }

        return c2wSafe(c);
    }

    public static float c2wSafe(float c) {
        return c2wSafe(c, Param.HORIZON);
    }


    /**
     * A function to convert confidence to weight
     * http://www.wolframalpha.com/input/?i=x%2F(1-x)
     *
     * @param c confidence, in [0, 1)
     * @return The corresponding weight of evidence, a non-negative real number
     */
    public static float c2wSafe(float c, float horizon) {
        return horizon * c / (1 - c);
    }

    /** http://www.wolframalpha.com/input/?i=x%2F(x%2B1) */
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
        if (w < Param.TRUTH_MIN_EVI)
            throw new Truth.TruthException("insufficient evidence", w);
        if (!Float.isFinite(w))
            throw new Truth.TruthException("non-finite evidence", w);


        return w2cSafe(w);
    }

    public static float w2cSafe(float w) {
        return w2cSafe(w, Param.HORIZON);
    }


    public static float originality(int evidenceLength) {
        if (evidenceLength <= 1) {
            return 1f;
        } else {
            return 1f / (1f + (evidenceLength - 1) / (Param.STAMP_CAPACITY - 1f));
        }
    }

    public static float expectation(float frequency, float confidence) {
        return (confidence * (frequency - 0.5f) + 0.5f);
    }

    public static float eternalize(float evi) {
        return w2c(evi);
    }
}























