package nars.unify.constraint;

import nars.$;
import nars.derive.Derivation;
import nars.derive.premise.PreDerivation;
import nars.term.Term;
import nars.term.control.AbstractPred;
import nars.unify.Unify;

public class SubsMin extends MatchConstraint {

    final int min;

    public SubsMin(Term target, int min) {
        super(target, "subsMin", $.the(min));
        assert(min > 1);
        this.min = min;
    }

    @Override
    public float cost() {
        return 0.15f;
    }

    @Override
    public boolean invalid(Term y, Unify f) {
        return y.subs() < min;
    }



    public static final class SubsMinProto extends AbstractPred<PreDerivation> {
        private final boolean taskOrBelief;
        private final int min;

        public SubsMinProto(boolean taskOrBelief, int min) {
            super($.func("subsMin", taskOrBelief ? Derivation.Task: Derivation.Belief , $.the(min)));
            this.taskOrBelief = taskOrBelief;
            this.min = min;
        }

        @Override
        public float cost() {
            return 0.15f;
        }

        @Override
        public boolean test(PreDerivation d) {
            Term t = taskOrBelief ? d.taskTerm : d.beliefTerm;
            return t.subs() >= min;
        }
    }
}
