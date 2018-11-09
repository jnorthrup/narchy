package nars.truth;

import nars.$;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.and;
import static nars.$.t;
import static nars.truth.TruthFunctions.confCompose;
import static nars.truth.TruthFunctions.w2cSafe;

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

    @Nullable
    public static Truth analogyNew(/*@NotNull*/ Truth a, float bf, float bc, float minConf) {
        float c = and(bf,
                TruthFunctions.confCompose(a.conf(), bc)
                //a.conf() * bc
        );
        return c >= minConf ? t(a.freq(), c) : null;
    }
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

    /**
     * goal deduction
     */
    @Nullable
    public static Truth desire(/*@NotNull*/ Truth goal, /*@NotNull*/ Truth belief, float minConf, boolean strong) {

        float cc = confCompose(belief, goal);
        if (!strong)
            cc = weak(cc);

        float c = and(belief.freq(),
                cc)
        ;


//        if (!strong)
//            c = weak(c);
//

        if (c >= minConf) {


            //float f = Util.lerp(belief.freq(), 0.5f, goal.freq());
            //float f = Util.lerp(belief.freq(), 1-goal.freq(), goal.freq());
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



    /**
     * strong frequency and confidence the closer in frequency they are
     */
    public static Truth comparisonSymmetric(Truth t, Truth b, float minConf) {
        float c = confCompose(t, b);
        if (c < minConf) return null;
        float dF = Math.abs(t.freq() - b.freq());
        float sim = 1f - dF;
        float cc = /*weak*/(c * sim);
        return cc >= minConf ? $.t(sim, cc) : null;
    }
    public static float weak(float c) {
        //return w2cSafe(c);
        return c * w2cSafe(1.0f);
    }

    @Deprecated public static Truth weak(Truth t) {
        return t!=null ? $.t(t.freq(), weak(t.conf())) : null;
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


    /** output polarity matches Y's polarity. X determines pre and post negations */
    public static Truth decomposeDiff(Truth X, Truth Y, float minConf) {
        boolean xPos = X.isPositive();
        boolean yPos = Y.isPositive();
        Truth t = TruthFunctions.decompose(X, Y, xPos, yPos, yPos, minConf);
        return t!=null ? t.negIf(xPos) : null;
    }

    /**  X, (  X ==> Y) |- Y
     * --X, (--X ==> Y) |- Y
     * frequency determined by the impl
     * */
    public static Truth pre(Truth X, Truth XimplY, boolean weak, float minConf) {
        float c = confCompose(X, XimplY);
        if(c < minConf) return null;
        float cc = c * X.freq(); //match amplitude
        if (weak)
            cc = w2cSafe(cc);
        if(cc < minConf) return null;
        return $.t(XimplY.freq(), cc);
    }

    /**    Y, (X ==>   Y) |- X
     *   --Y, (X ==> --Y) |- X
     * */
    public static Truth post(Truth Y, Truth XimplY, boolean strong, float minConf) {

        float c = confCompose(Y, XimplY);
        if(c < minConf) return null;

        //frequency alignment
        float yF = Y.freq();
        float impF = XimplY.freq();
        float f;

        //polarized: -1..+1
        float yFp = 2 * (yF - 0.5f);
        float impFp = 2 * (impF - 0.5f);
        float alignment = (((yFp * impFp) /*-1..+1*/)+1)/2;
        c *= alignment;
        f = alignment;

        float cc = strong ? c : w2cSafe(c );
        if (cc < minConf) return null;
        return $.t(f, cc);
    }

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
}
