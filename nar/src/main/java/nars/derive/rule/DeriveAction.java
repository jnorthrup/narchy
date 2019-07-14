package nars.derive.rule;

import jcog.pri.ScalarValue;
import nars.NAL;
import nars.derive.model.Derivation;
import nars.derive.op.Truthify;
import nars.term.control.PREDICATE;

/** branch in the derivation fork.  first runs truth.test() before conclusion.test() */
public class DeriveAction  /*implements ThrottledAction<Derivation>*/ {

    public final PremiseRuleProto.RuleWhy why;
    public final Truthify truth;
    public final PREDICATE<Derivation> conclusion;

    DeriveAction(PremiseRuleProto.RuleWhy cause, Truthify pre, PREDICATE<Derivation> post) {
        this.why = cause;
        this.conclusion = post;
        this.truth = pre;
    }

    @Override
    public boolean equals(Object obj) {
        return this==obj || why.rule.equals(((DeriveAction)obj).why.rule);
    }

    @Override
    public int hashCode() {
        return why.rule.hashCode();
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

    @Override
    public String toString() {
        return why.rule.toString();
    }

//    @Deprecated public boolean test(Derivation d) {
//        if (truth.test(d)) {
//
//            d.clear();
//            d.retransform.clear();
//            d.forEachMatch = null;
//
//            conclusion.test(d);
//        }
//
//        return d.use(NAL.derive.TTL_COST_BRANCH);
//    }

    public boolean run(PostDerivable p) {

        Derivation d = (Derivation) p.d;
        d.clear();
        d.retransform.clear();
        d.forEachMatch = null;

        p.apply(d);
        conclusion.test(d);

        return d.use(NAL.derive.TTL_COST_BRANCH);
    }

}
