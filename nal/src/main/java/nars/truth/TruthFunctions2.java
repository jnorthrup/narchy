package nars.truth;

import jcog.Util;
import nars.$;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.and;
import static nars.$.t;
import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2cSafe;

public enum TruthFunctions2 {
    ;

    /**
     * freq symmetric intersection
     * to the degree the freq is the same, the evidence is additive
     * to the degree the freq is different, the evidence is multiplicative
     * resulting freq is weighted combination of inputs
     */
    public static Truth intersectionX(Truth a, Truth b, float confMin) {
        float diff = Math.abs(a.freq() - b.freq());
        float ac = a.conf(), bc = b.conf();
        float conf = Util.lerp(diff, w2cSafe(c2wSafe(ac) + c2wSafe(bc)), (ac * bc));
        float freq = ((a.freq() * ac) + (b.freq() * bc)) / (ac + bc);
        return conf >= confMin ? $.t(freq, conf) : null;
    }

    /**
     * freq symmetric difference
     * to the degree the freq differs or is similar, the evidence is additive
     * to the degree the freq is not different nor similar, the evidence is multiplicative
     * resulting freq is weighted difference of inputs
     */
    public static Truth differenceX(Truth a, Truth b, float confMin) {
        float extreme = 2f * Math.abs(0.5f - Math.abs(a.freq() - b.freq()));
        float ac = a.conf(), bc = b.conf();
        float conf = Util.lerp(extreme, (ac * bc), w2cSafe(c2wSafe(ac) + c2wSafe(bc)));

        float freq = a.freq() * (1f - b.freq());
        return conf >= confMin ? $.t(freq, conf) : null;
    }

    public static Truth unionX(Truth a, Truth b, float confMin) {
        Truth z = intersectionX(a.neg(), b.neg(), confMin);
        return z != null ? z.neg() : null;
    }

//    @Nullable
//    public static Truth deduction(/*@NotNull*/ Truth a, float bF, float bC, float minConf) {
//
//
//        float f = and(a.freq(), bF);
//
//
//        float aC = a.conf();
//
//        float c = Util.lerp(bC/(bC + aC), 0 ,aC);
//
//        return c >= minConf ? t(f, c) : null;
//    }

    @Nullable
    public static Truth analogyNew(/*@NotNull*/ Truth a, float bf, float bc, float minConf) {
        float c = and(a.conf(), bc, bf);
        return c >= minConf ? t(a.freq(), c) : null;
    }
//
//    /**
//     * frequency determined entirely by the desire component.
//     */
//    @Nullable
//    public static Truth desireNew(/*@NotNull*/ Truth goal, /*@NotNull*/ Truth belief, float minConf, boolean strong) {
//
//        float c = and(goal.conf(), belief.conf(), belief.freq());
//
//        if (!strong) {
//            //c *= TruthFunctions.w2cSafe(1.0f);
//            c = w2cSafe(c);
//        }
//
//        if (c >= minConf) {
//
//
//            float f = goal.freq();
//
//            return $.t(f, c);
//
//        } else {
//            return null;
//        }
//    }

    /**
     * goal deduction
     */
    @Nullable
    public static Truth desire(/*@NotNull*/ Truth goal, /*@NotNull*/ Truth belief, float minConf, boolean strong) {

        float c = and(goal.conf(), belief.conf(), belief.freq());
        //float c = and(goal.conf(), belief.conf());

        if (!strong) {
            c = weak(c);
        }

        if (c >= minConf) {


            //float f = Util.lerp(belief.freq(), 0.5f, goal.freq());
            float f = goal.freq();

            return $.t(f, c);

        } else {
            return null;
        }
    }

    @Nullable
    public static Truth analogy(Truth a, Truth b, float minConf) {
        return analogyNew(a, b.freq(), b.conf(), minConf);
    }

    static Truth desire(float f1, float f2, float c) {
        return t(and(f1, f2), c);
    }

    /**
     * strong frequency and confidence the closer in frequency they are
     */
    public static Truth comparisonSymmetric(Truth t, Truth b, float minConf) {
        float c = t.conf() * b.conf();
        if (c < minConf) return null;
        float dF = Math.abs(t.freq() - b.freq());
        float sim = 1f - dF;
        float cc = weak(c * sim);
        return cc >= minConf ? $.t(sim, cc) : null;
    }
    static float weak(float c) {
        //return w2cSafe(c);
        return c * TruthFunctions.w2cSafe(1.0f);
    }

    /**
     * {<A ==> B>} |- <--B ==> --A>
     *
     * @param t Truth value of the premise
     * @return Truth value of the conclusion
     */
    public static Truth contraposition(Truth t, float minConf) {
        float f = t.freq();
        float fPolarization = 2 * Math.abs(f - 0.5f);
        float c = weak(fPolarization * t.conf());
        return c >= minConf ? t((1 - f), c) : null;
    }

    @Nullable
    public static Truth deduction(Truth a, float bF, float bC, float minConf) {

        float f = and(a.freq(), bF);
        float c = and(f,
                and //original
                        //or
                        //aveAri
                                (a.conf(), bC)
        );

        return c >= minConf ? t(f, c) : null;
    }


//        float f = 0.5f * ((f1-0.5f)*2*(f2-0.5f)*2 + 1);

}
