package nars.derive.rule;

import jcog.pri.ScalarValue;
import nars.control.Why;
import nars.derive.model.Derivation;
import nars.derive.op.Truthify;
import nars.term.control.AND;
import nars.term.control.PREDICATE;

/** branch in the derivation fork */
public final class DeriveAction  /*implements ThrottledAction<Derivation>*/ {

    public final Why why;
    private final Truthify truth;
    public final PREDICATE<Derivation> run;

    private DeriveAction(PREDICATE<Derivation> procedure, PremiseRuleProto.RuleWhy cause, Truthify t) {
        this.run = procedure;
        this.why = cause;
        this.truth = t;
    }


    public static DeriveAction action(PremiseRuleProto.RuleWhy cause, PREDICATE<Derivation> POST) {

        Truthify t = (Truthify) AND.first((AND<Derivation>)POST, x -> x instanceof Truthify);
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

        float puncFactor = d.preAmp(punc);
        if (puncFactor <= ScalarValue.EPSILON)
            return 0f; //entirely disabled by deriver

        float causeValue =
                why.amp();
                //why.value();

        return causeValue * puncFactor;
    }
}
