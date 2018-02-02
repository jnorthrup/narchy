package nars.derive.constraint;

import nars.$;
import nars.derive.ProtoDerivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.pred.AbstractPred;
import nars.term.pred.PrediTerm;
import nars.term.subst.Unify;

public class SubsMin extends MatchConstraint {

    final int min;

    public SubsMin(Term target, int min) {
        super(target, "SubsMin", $.the(min));
        assert(min > 1);
        this.min = min;
    }

    @Override
    public float cost() {
        return 0.15f;
    }

    @Override
    public boolean invalid(Term y, Unify f) {
        return (!(y instanceof Compound)) || y.subs() < min;
    }

    public static PrediTerm<ProtoDerivation> proto(boolean taskOrBelief, int min) {
        return new SubsMinProto(taskOrBelief, min);
    }

    private static final class SubsMinProto extends AbstractPred<ProtoDerivation> {
        private final boolean taskOrBelief;
        private final int min;

        SubsMinProto(boolean taskOrBelief, int min) {
            super($.func("SubsMin", $.the(taskOrBelief ? "task" : "belief"), $.the(min)));
            this.taskOrBelief = taskOrBelief;
            this.min = min;
        }

        @Override
        public float cost() {
            return 0.15f;
        }

        @Override
        public boolean test(ProtoDerivation d) {
            Term t = taskOrBelief ? d.taskTerm : d.beliefTerm;
            return t.subs() >= min;
        }
    }
}
