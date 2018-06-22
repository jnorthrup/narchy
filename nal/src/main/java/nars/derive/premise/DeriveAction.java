package nars.derive.premise;

import jcog.pri.Prioritized;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.step.Taskify;
import nars.derive.step.Truthify;
import nars.term.control.AND;
import nars.unify.op.UnifyTerm;

class DeriveAction extends AND<Derivation> /*implements ThrottledAction<Derivation>*/ {

    public final Cause cause;
    private final Truthify truth;

    private DeriveAction(AND<Derivation> procedure, PremiseDeriverProto.RuleCause cause, Truthify t) {
        super(procedure.cond);
        this.cause = cause;
        this.truth = t;
    }

    static DeriveAction action(AND<Derivation> POST) {

        PremiseDeriverProto.RuleCause cause = ((Taskify) AND.last(
                ((UnifyTerm.UnifySubtermThenConclude)
                AND.last(POST)
        ).eachMatch)).channel;

        Truthify t = (Truthify) AND.first(POST, x -> x instanceof Truthify);
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



    /** compute throttle value, in consideration of the premise's task and the punctuation outcome
     * with respect to the deriver's punctuation equalization */
    public float value(Derivation d) {

        byte punc = truth.preFilter(d);
        if (punc == 0)
            return 0f; //disabled or not applicable to the premise

        float puncFactor = d.deriver.puncFactor(punc);
        if (puncFactor <= Prioritized.EPSILON)
            return 0f; //entirely disabled by deriver

        float causeValue = cause.amp();

        return causeValue * puncFactor;
    }
}
