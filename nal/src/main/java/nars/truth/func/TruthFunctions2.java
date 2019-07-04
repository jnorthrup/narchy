package nars.truth.func;

import jcog.Skill;
import jcog.Util;
import nars.$;
import nars.NAL;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static nars.$.t;
import static nars.truth.func.TruthFunctions.confCompose;
import static nars.truth.func.TruthFunctions.w2cSafe;

public enum TruthFunctions2 {
    ;

//    /**
//     * freq symmetric intersection
//     * to the degree the freq is the same, the evidence is additive
//     * to the degree the freq is different, the evidence is multiplicative
//     * resulting freq is weighted combination of inputs
//     */
//    public static Truth intersectionX(Truth a, Truth b, float confMin) {
//        float diff = Math.abs(a.freq() - b.freq());
//        float ac = a.conf(), bc = b.conf();
//        float conf = Util.lerp(diff, w2cSafe(c2wSafe(ac) + c2wSafe(bc)), (ac * bc));
//        float freq = ((a.freq() * ac) + (b.freq() * bc)) / (ac + bc);
//        return conf >= confMin ? $.t(freq, conf) : null;
//    }

//    /**
//     * freq symmetric difference
//     * to the degree the freq differs or is similar, the evidence is additive
//     * to the degree the freq is not different nor similar, the evidence is multiplicative
//     * resulting freq is weighted difference of inputs
//     */
//    public static Truth differenceX(Truth a, Truth b, float confMin) {
//        float extreme = 2f * Math.abs(0.5f - Math.abs(a.freq() - b.freq()));
//        float ac = a.conf(), bc = b.conf();
//        float conf = Util.lerp(extreme, (ac * bc), w2cSafe(c2wSafe(ac) + c2wSafe(bc)));
//
//        float freq = a.freq() * (1f - b.freq());
//        return conf >= confMin ? $.t(freq, conf) : null;
//    }

//    public static Truth unionX(Truth a, Truth b, float confMin) {
//        Truth z = intersectionX(a.neg(), b.neg(), confMin);
//        return z != null ? z.neg() : null;
//    }

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

    //    @Nullable
//    public static Truth deduction(Truth a, float bF, float bC, float minConf) {
//
//        float f;
//        float aF = a.freq();
////        if (bF >= 0.5f) {
////            f = Util.lerp(bF, 0.5f, aF);
////        } else {
////            f = Util.lerp(bF, 0.5f, 1- aF);
////        }
//        f = Util.lerp(bF, 1-aF, aF);
//
//        float p = Math.abs(f - 0.5f)*2f; //polarization
//
//        float c = //and(/*f,*/ /*p,*/ a.conf(), bC);
//                    TruthFunctions.confCompose(a.conf(), bC);
//
//        return c >= minConf ? t(f, c) : null;
//    }
//
//    /**
//     * {<S ==> M>, <P ==> M>} |- <S ==> P>
//     *
//     * @param a Truth value of the first premise
//     * @param b Truth value of the second premise
//     * @return Truth value of the conclusion, or null if either truth is analytic already
//     */
//    public static Truth induction(Truth a, Truth b, float minConf) {
//        float c = w2cSafe(a.conf() * b.freqTimesConf());
//        return c >= minConf ? $.t(a.freq(), c) : null;
//    }

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

    @Nullable
    public static Truth desire(/*@NotNull*/ Truth goal, /*@NotNull*/ Truth belief, float minConf, boolean bipolar, boolean strong) {

        float cc = confCompose(belief, goal);
        if (cc >= minConf) {



            float bF =
                    belief.freq();
                    //(float) Math.sqrt(belief.freq());

            if (!bipolar)
                cc = Util.and(bF, cc); //in unipolar mode, attenuate confidence to zero as the low belief frequency pulls the output frequency to 0.5

            if (!strong)
                cc = weak(cc);

            if (cc >= minConf) {

                //float f =
                float gF = goal.freq();
                float f =
                        bipolar ?
                            Util.lerpSafe(bF, 1- gF, gF)
                            :
                            gF * bF
                            //gF
                            //Util.lerpSafe(bF, 0.5f, goal.freq())
                        ;

                return $.t(f, cc);

            }

        }
        return null;

    }


    /** full positive, half negative */
    @Nullable public static Truth desireSemiBipolar(/*@NotNull*/ Truth goal, /*@NotNull*/ Truth belief, float minConf, boolean strong) {

        float cc = confCompose(belief, goal);
        if (cc < minConf)
            return null;

        float bF = belief.freq();

        if (!strong) {
            cc = weak(cc);
            if (cc < minConf)
                return null;
        }

        float f = Util.lerp(bF, ((1-goal.freq()) + 0.5f)/2, goal.freq());
        return $.t(f, cc);
    }




    //    /**
//     * strong frequency and confidence the closer in frequency they are
//     */
//    public static Truth comparisonSymmetric(Truth t, Truth b, float minConf) {
//        float c = confCompose(t, b);
//        if (c < minConf) return null;
//        float dF = Math.abs(t.freq() - b.freq());
//        float sim = 1f - dF;
//        float cc = /*weak*/(c * sim);
//        return cc >= minConf ? $.t(sim, cc) : null;
//    }
    public static float weak(float c) {
        return w2cSafe(c);
        //return c * w2cSafe(1.0f);
    }

    @Deprecated
    @Nullable public static Truth weak(@Nullable Truth t, float minConf) {
        if (t == null)
            return null;
        float c = weak(t.conf());
        return c >= minConf ? $.t(t.freq(), c) : null;
    }

    /**
     * {<A ==> B>} |- <--B ==> --A>
     *
     * @param t Truth value of the premise
     * @return Truth value of the conclusion
     */
    public static Truth contraposition(Truth t, float minConf) {
        float f = t.freq();
        float fPolarization = t.polarity();
        float c = weak(fPolarization * t.conf());
        return c >= minConf ? t((1 - f), c) : null;
    }


    /**
     * output polarity matches Y's polarity. X determines pre and post negations
     */
    public static Truth decomposeDiff(Truth X, Truth Y, float minConf) {
        boolean xPos = X.isPositive();
        boolean yPos = Y.isPositive();
        Truth t = TruthFunctions.decompose(X, Y, xPos, yPos, yPos, minConf);
        return t != null ? t.negIf(xPos) : null;
    }

    /**
     *   X, (  X ==> Y) |- Y
     * --X, (--X ==> Y) |- Y
     * frequency determined by the XimplY
     */
    public static Truth pre(Truth X, Truth XimplY, boolean weak, float minConf) {
        float c = confCompose(X, XimplY);
        if (c < minConf) return null;
        float cc = c *
                (NAL.truth.preSoft ? X.freq() //soft polarity match (inverse bleed through)
                : Math.max(0, 2 * (X.freq()-0.5f))); //hard polarity match
        if (weak)
            cc = weak(cc);
        if (cc < minConf) return null;
        return $.t(XimplY.freq(), cc);
    }

    /**
     *   Y, (X ==>   Y) |- X
     * --Y, (X ==> --Y) |- X
     */
    public static Truth post(Truth Y, Truth XimplY, boolean strong, float minConf) {

        //test for matching frequency alignment
        float yF = Y.freq();
        float impF = XimplY.freq();
        boolean opposite = (yF >= 0.5f != impF >= 0.5f);


        float c = confCompose(Y, XimplY);
        c = strong ? c : weak(c);
        if (c < minConf) return null;



        //polarized: -1..+1
//        float yFp = 2 * (yF - 0.5f);
//        float impFp = 2 * (impF - 0.5f);
//        float preAlign = yFp * impFp;
//        float preAlign = 1-Math.abs(yF - impF);
        float dyf = Math.abs(yF - 0.5f);
        float dimpl = Math.abs(impF - 0.5f);
        //normalize to the maximum dynamic range
//        float range = Math.max(dyf, dimpl);
//
//        dimpl = dimpl / range;
//        dyf = dyf / range;

        float alignment = //dyf*dimpl * 4;
                dyf * dimpl * 2 + 0.5f;
        if(opposite)
            alignment = 1 - alignment;

        c *= alignment; if (c < minConf)  return null;
        float f =
                //Util.lerp(alignment, 0.5f, 1);  //vanish toward maybe
                1;

        return $.t(f, c);
    }

//    @Nullable public static Truth intersectionSym(Truth t, Truth b, float minConf) {
//        float c = confCompose(t, b);
//        if (c < minConf) return null;
//
//        float f;
//        if (t.isPositive()!=b.isPositive()) {
//            //f = 0.5f;
//            return null;
//        } else {
//            f = intersectionSym(t.freq(), b.freq());
//        }
//        return $.t(f, c);
//
////        if (T.isPositive() && B.isPositive()) {
////            return Intersection.apply(T, B, m, minConf);
////        } else if (T.isNegative() && B.isNegative()) {
////            Truth C = Intersection.apply(T.neg(), B.neg(), m, minConf);
////            return C != null ? C.neg() : null;
////        } else {
////            return null;
////        }
//    }
//    public static Truth unionSym(Truth t, Truth b, float minConf) {
//        float c = confCompose(t, b);
//        if (c < minConf) return null;
//
//        float f;
//        boolean pos = t.isPositive();
//        if (pos !=b.isPositive()) {
//            //f = 0.5f;
//            return null;
//        } else {
//            f = unionSym(t.freq(), b.freq());
//        }
//        return $.t(f, c);
//    }

    /**
     * freq: a,b; assumes they are of the same polarity
     */
    static float intersectionSym(float a, float b) {
        if (a >= 0.5f) {
            a = 2 * (a - 0.5f);
            b = 2 * (b - 0.5f);
            return 0.5f + (a * b) / 2;
        } else {
            a = 2 * (0.5f - a);
            b = 2 * (0.5f - b);
            return 0.5f - (a * b) / 2;
        }
    }

//    /** freq: a,b; assumes they are of the same polarity */
//    static float unionSym(float a, float b) {
//        if (a >= 0.5f) {
//            a = 2 * (a - 0.5f); b = 2 * (b - 0.5f);
//            return 0.5f + (1-((1-a) * (1-b)))/2;
//        } else {
//            a = 2 * (0.5f - a); b = 2 * (0.5f - b);
//            return 0.5f - (1-((1-a) * (1-b)))/2;
//        }
//    }


//    public static Truth maybeDuction(Truth a, float bC, float minConf) {
//
//        float freqDiff = a.freq();
//        float f = Util.lerp(freqDiff, 0.5f, 1f);
//            float c = and(freqDiff,
//                    //and(a.conf(), bC)
//                    confCompose(a.conf(), bC)
//            );
//
//            return c >= minConf ? t(f, c) : null;
//    }


    /** designed to be precise inverse of intersection (not a fuzzy-fuzzy-set result)
     *
     * XY = X * Y
     *  Y = XY / X
     *
     *  http://www.math.sk/fsta2014/presentations/VemuriHareeshSrinath.pdf
     * */
    @Skill("Fuzzy_set") @Nullable public static Truth divide(Truth XY, Truth X, float minConf) {
        //float c = confCompose(X, XY);
        float c = Math.min(XY.conf(), X.conf());
        if (c < minConf)
            return null;

        float fx = Math.max(Util.sqr(NAL.truth.TRUTH_EPSILON), X.freq()); //prevent division by zero
        float fxy = XY.freq();
        float fy = fxy / fx;

        if (fy > 1) {
            float doubt = 1/fy;
            c *= doubt;
            if (c < minConf)
                return null;
            fy = 1;
        }

        return t(fy, c);
    }
}
