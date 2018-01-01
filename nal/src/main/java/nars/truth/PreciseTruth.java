package nars.truth;

import jcog.Util;
import nars.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2c;
import static nars.truth.TruthFunctions.w2cSafe;

/**
 * represents a freq,evi pair precisely but does not
 * allow hashcode usage
 */
public class PreciseTruth implements Truth {

    final float f, e;

    public PreciseTruth(float freq, float conf) {
        this(freq, conf, true);
    }

    public PreciseTruth(float freq, float ce, boolean xIsConfOrEvidence) {
        assert ((freq == freq) && (freq >= 0) && (freq <= 1)):
                "invalid freq: " + freq;
        this.f = freq;
        assert ((ce == ce) && (ce > 0)):
                "invalid evidence/conf: " + ce;
        float e;
        if (xIsConfOrEvidence) {
//            if (ce > TruthFunctions.MAX_CONF)
//                throw new RuntimeException(ce + " is gte max (" + TruthFunctions.MAX_CONF + ')');
            ce = Util.min(ce, TruthFunctions.MAX_CONF);
            e = c2wSafe(ce, Param.HORIZON);
        } else {
            e = ce;
        }
        this.e = e;
    }

    @Override
    public Truth neg() {
        return new PreciseTruth(1f - f, e, false);
    }

    @Override
    public boolean equals(@Nullable Object that) {
        return this == that ||  (that!=null && equals((Truthed) that, Param.TRUTH_EPSILON ));
        //throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return _toString();
    }

    @Override
    public final float freq() {
        return f;
    }

    @Override
    public final float evi() { return e;    }

    @Override
    public final float conf() {
        return w2cSafe(e);
    }

//    public PreciseTruth eviMult(float v) {
//        return v == 1 ? this : new PreciseTruth(f, e * v, false);
//    }
}
