package nars.derive.premise;

import jcog.pri.ScalarValue;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.op.Truthify;
import nars.term.control.AND;
import nars.term.control.PREDICATE;

final class DeriveAction  /*implements ThrottledAction<Derivation>*/ {

    public final Cause cause;
    private final Truthify truth;
    public final PREDICATE<Derivation> run;

    private DeriveAction(PREDICATE<Derivation> procedure, PremiseRuleProto.RuleCause cause, Truthify t) {
        this.run = procedure;
        this.cause = cause;
        this.truth = t;
    }


    static DeriveAction action(PremiseRuleProto.RuleCause cause, PREDICATE<Derivation> POST) {

        Truthify t = (Truthify) AND.first((AND)POST, x -> x instanceof Truthify);
        if (t == null)
            throw new NullPointerException();


        return new DeriveAction(POST, cause, t);
    }


//    @Override
//    public boolean test(Derivation d, float power) {
//        //d.use(power) //d's own powerToTTL function, temporarily subtract TTL for the fork
//        test(d);
//        //return the remaining TTL change or quit
//        return false;
//    }


    /**
     * compute probabilistic throttle value, in consideration of the premise's task and the punctuation outcome
     * with respect to the deriver's punctuation equalization
     */
    public final float value(Derivation d) {

        byte punc = truth.preFilter(d);
        if (punc == 0)
            return 0f; //disabled or not applicable to the premise

        float puncFactor = d.deriver.preAmp(punc);
        if (puncFactor <= ScalarValue.EPSILON)
            return 0f; //entirely disabled by deriver

        float causeValue = cause.amp();

        return causeValue * puncFactor;
    }
}
