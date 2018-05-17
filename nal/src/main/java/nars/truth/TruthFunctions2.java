package nars.truth;

import jcog.Util;
import nars.$;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.and;
import static nars.$.t;
import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2cSafe;

public enum TruthFunctions2 { ;

    /** freq symmetric intersection
     * to the degree the freq is the same, the evidence is additive
     * to the degree the freq is different, the evidence is multiplicative
     * resulting freq is weighted combination of inputs
     * */
    public static Truth intersectionX(Truth a, Truth b, float confMin) {
        float diff = Math.abs(a.freq() - b.freq());
        float ac = a.conf(), bc = b.conf();
        float conf = Util.lerp(diff, w2cSafe(c2wSafe(ac) + c2wSafe(bc)), (ac * bc));
        float freq = ((a.freq() * ac) + (b.freq() * bc)) / (ac + bc);
        return conf >= confMin ? $.t(freq, conf) : null;
    }

    /** freq symmetric difference
     * to the degree the freq differs or is similar, the evidence is additive
     * to the degree the freq is not different nor similar, the evidence is multiplicative
     * resulting freq is weighted difference of inputs
     * */
    public static Truth differenceX(Truth a, Truth b, float confMin) {
        float extreme = 2f * Math.abs(0.5f - Math.abs(a.freq() - b.freq()));
        float ac = a.conf(), bc = b.conf();
        float conf = Util.lerp(extreme, (ac * bc), w2cSafe(c2wSafe(ac) + c2wSafe(bc)));
        //float freq = ((((a.freq()) * ac) * ((1f-b.freq()) * bc)) / (ac + bc));
        float freq = a.freq() * (1f-b.freq());
        return conf >= confMin ? $.t(freq, conf) : null;
    }

    public static Truth unionX(Truth a, Truth b, float confMin) {
        Truth z = intersectionX(a.neg(), b.neg(), confMin);
        return z!=null ? z.neg() : null;
    }

    @Nullable
    public static Truth deduction(/*@NotNull*/ Truth a, float bF, float bC, float minConf) {

        //float f = and(a.freq()-0.5f, bF)+0.5f;
        float f = and(a.freq(), bF);
        //float c = w2cSafe(Util.lerp(bC, 0, a.evi())); //absolute conf
        //float c = w2cSafe(Util.lerp(bC/(bC + a.conf()), 0, a.evi())); //relative to a.conf
        float aC = a.conf();
        //float c = Util.lerp(bC/(bC + aC), aC * bC, aC); //relative to a.conf
        float c = Util.lerp(bC/(bC + aC), 0 ,aC);

        return c >= minConf ? t(f, c) : null;
    }

    @Nullable
    public static Truth analogyNew(/*@NotNull*/ Truth a, float bf, float bc, float minConf) {
        float c = and(a.conf(), bc, bf);
        return c >= minConf ? t(a.freq(), c) : null;
    }

    /**
     * frequency determined entirely by the desire component.
     */
    @Nullable public static Truth desireNew(/*@NotNull*/ Truth goal, /*@NotNull*/ Truth belief, float minConf, boolean strong) {

        float c = and(goal.conf(), belief.conf(), belief.freq());
        //float c = and(goal.conf(), belief.conf());
        if (!strong)
            c *= TruthFunctions.w2c(1.0f);

        if (c >= minConf) {

            //instead of the original's f = and(aFreq,bFreq),
            //this does not discriminate against negative goals but instead
            //pulls the frequency toward 0.5 in proportion to (1- bFreq)
            //float f = Util.lerp(b.freq(), 0.5f, a.freq());

            float f = goal.freq();

            return $.t(f, c);

        } else {
            return null;
        }
        //return c < minConf ? null : $.t(Util.lerp(b.freq(), 0.5f, a.freq()), c);


//        float c = a.conf() * b.freq();
//        return c < minConf ? null : $.t(a.freq(), c);

        //float c = and(a.conf(), b.conf(), b.freq());
//        return c < minConf ? null : $.t(a.freq(), c);
        //return $.t(a.expectation(b.freq()), b.conf());
        //return c < minConf ? null : $.t(a.expectation(b.freq()), c);
        //return c < minConf ? null : $.t(TruthFunctions.expectation(a.freq(), b.freq()) /* b.freq used in conf param */, c);


        //return c < minConf ? null : $.t(a.freq(), c);

    }
}
