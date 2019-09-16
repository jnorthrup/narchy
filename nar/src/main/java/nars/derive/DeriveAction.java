package nars.derive;

import nars.NAL;
import nars.derive.op.Truthify;
import nars.derive.rule.RuleWhy;
import nars.term.control.PREDICATE;

/** branch in the derivation fork.  first runs truth.test() before conclusion.test() */
public class DeriveAction  /*implements ThrottledAction<Derivation>*/ {

    public final RuleWhy why;

    /** 2nd stage filter and evaluator/ranker */
    public final Truthify truth;

    public final PREDICATE<Derivation> action;

    public DeriveAction(RuleWhy cause, Truthify pre, PREDICATE<Derivation> post) {
        this.why = cause;
        this.action = post;
        this.truth = pre;
    }

    @Override
    public boolean equals(Object obj) {
        return this==obj;// || why.rule.equals(((DeriveAction)obj).why.rule);
    }

    @Override
    public int hashCode() {
        return why.rule.hashCode();
    }

    /**
     * compute probabilistic throttle value, in consideration of the premise's task and the punctuation outcome
     * with respect to the deriver's punctuation equalization
     *
     * returning 0 invalidates this action for the provided derivation
     * 
     * TO BE REFINED
     */
    public final float pri(Derivation d) {

        float causeValue = why.amp();
        if (causeValue < Float.MIN_NORMAL)
            return 0f; //disabled

        byte punc = truth.preFilter(d);
        if (punc == 0)
            return 0f; //disabled or not applicable to the premise

        float puncFactor = d.preAmp(d.punc);
        if (puncFactor < Float.MIN_NORMAL)
            return 0f; //entirely disabled by deriver

        if (!truth.test(d))
            return 0;

        return causeValue * puncFactor * d.what.derivePri.prePri(d);
    }

    @Override
    public String toString() {
        return why.rule.toString();
    }

    public boolean run(PostDerivable p) {

        Derivation d = (Derivation) p.d;
        d.clear();
        d.retransform.clear();
        d.forEachMatch = null;

        p.apply(d);
        action.test(d);

        return d.use(NAL.derive.TTL_COST_BRANCH);
    }

}
