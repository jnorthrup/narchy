package nars.derive.premise;

import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.step.Taskify;
import nars.term.control.AndCondition;
import nars.term.control.PrediTerm;
import nars.unify.constraint.MatchConstraint;
import nars.unify.op.UnifyTerm;

class DeriveAction extends AndCondition<Derivation> implements ThrottledAction<Derivation> {

    public final Cause causes;

    private DeriveAction(AndCondition<Derivation> procedure, PremiseDeriverProto.RuleCause cause) {
        super(procedure.cond);
        this.causes = cause;
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

        PremiseDeriverProto.RuleCause cause = ((Taskify) AndCondition.last(((UnifyTerm.UnifySubtermThenConclude)
                AndCondition.last(POST)
        ).eachMatch)).channel;

        return new DeriveAction(procedure, cause);
    }


    @Override
    public boolean test(Derivation d, float power) {
        //d.use(power) //d's own powerToTTL function, temporarily subtract TTL for the fork
        test(d);
        //return the remaining TTL change or quit
        return false;
    }
}
