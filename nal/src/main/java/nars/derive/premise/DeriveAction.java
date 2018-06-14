package nars.derive.premise;

import jcog.pri.Priority;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.step.Taskify;
import nars.derive.step.Truthify;
import nars.term.control.AndCondition;
import nars.term.control.PrediTerm;
import nars.unify.constraint.MatchConstraint;
import nars.unify.op.UnifyTerm;

class DeriveAction extends AndCondition<Derivation> /*implements ThrottledAction<Derivation>*/ {

    public final Cause cause;
    private final Truthify truth;

    private DeriveAction(AndCondition<Derivation> procedure, PremiseDeriverProto.RuleCause cause, Truthify t) {
        super(procedure.cond);
        this.cause = cause;
        this.truth = t;
    }

    static DeriveAction action(AndCondition<Derivation> procedure) {
        PrediTerm<Derivation> POST = procedure instanceof AndCondition ?
                ((AndCondition) procedure).transform(y -> y instanceof AndCondition ?
                                MatchConstraint.combineConstraints((AndCondition) y)
                                :
                                y
                        , null)
                :
                procedure;

        PremiseDeriverProto.RuleCause cause = ((Taskify) AndCondition.last(
                ((UnifyTerm.UnifySubtermThenConclude)
                AndCondition.last(POST)
        ).eachMatch)).channel;

        Truthify t = (Truthify) AndCondition.first((AndCondition)POST, x -> x instanceof Truthify);
        if (t == null)
            throw new NullPointerException();


        return new DeriveAction(procedure, cause, t);
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
        if (puncFactor <= Priority.EPSILON)
            return 0f; //entirely disabled by deriver

        float causeValue = cause.amp();

        return causeValue * puncFactor;
    }
}
