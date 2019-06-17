package nars.derive.rule;

import jcog.pri.ScalarValue;
import nars.NAL;
import nars.control.Why;
import nars.derive.model.Derivation;
import nars.derive.op.Truthify;
import nars.term.control.PREDICATE;

/** branch in the derivation fork.  first runs truth.test() before conclusion.test() */
public final class DeriveAction  /*implements ThrottledAction<Derivation>*/ {

    public final Why why;
    private final Truthify truth;
    private final PREDICATE<Derivation> conclusion;

    DeriveAction(PremiseRuleProto.RuleWhy cause, Truthify pre, PREDICATE<Derivation> post) {
        this.why = cause;
        this.conclusion = post;
        this.truth = pre;
    }


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

    public final boolean test(Derivation d) {
        if (truth.test(d))
            conclusion.test(d);

        return d.use(NAL.derive.TTL_COST_BRANCH);
    }

}
