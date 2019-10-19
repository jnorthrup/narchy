package nars.derive.cond;

import nars.$;
import nars.derive.Derivation;
import nars.derive.PreDerivation;
import nars.derive.util.DerivationFunctors;
import nars.term.Term;
import nars.term.Variable;
import nars.unify.constraint.ConstraintAsPredicate;
import nars.unify.constraint.RelationConstraint;
import nars.unify.constraint.UnaryConstraint;
import nars.unify.constraint.UnifyConstraint;

import java.util.function.BiFunction;

public abstract class ConstraintAsPremisePredicate<C extends UnifyConstraint<Derivation.PremiseUnify>> extends ConstraintAsPredicate<PreDerivation,C> {

    private static final BiFunction<Term, Term, Term> TASK = (t, b) -> t;
    private static final BiFunction<Term, Term, Term> BELIEF = (t, b) -> b;

    private ConstraintAsPremisePredicate(Term p, C m, float v, BiFunction<Term, Term, Term> extractX, BiFunction<Term, Term, Term> extractY) {
        super(p, v, m, extractX, extractY);
    }

    /**
     * TODO generify a version of this allowing: U extends Unify
     * @return
     */
    public static ConstraintAsPredicate the(Variable x, Variable y, byte[] xInTask, byte[] xInBelief, byte[] yInTask, byte[] yInBelief, UnifyConstraint m) {

        int costPath = 0;

        BiFunction<Term, Term, Term> extractX;
        Term extractXterm;
        if (xInTask != null) { // && (xInBelief == null || xInTask.length < xInBelief.length)) {
            assert(xInBelief==null);
            extractX = xInTask.length == 0 ? TASK : (t, b) -> t.subPath(xInTask);
            extractXterm = $.func(DerivationFunctors.Task, $.p(xInTask));
            costPath += xInTask.length;
        } else {
            extractX = xInBelief.length == 0 ? BELIEF : (t, b) -> b.subPath(xInBelief);
            extractXterm = $.func(DerivationFunctors.Belief, $.p(xInBelief));
            costPath += xInBelief.length;
        }


        float cost = m.cost();
        if (m instanceof RelationConstraint) {
            BiFunction<Term, Term, Term> extractY;
            Term extractYterm;
            if (yInTask != null && (yInBelief == null || yInTask.length < yInBelief.length)) {
                extractY = yInTask.length == 0 ? TASK : (t, b) -> t.subPath(yInTask);
                extractYterm = $.func(DerivationFunctors.Task, $.p(yInTask));
                costPath += yInTask.length;
            } else {
                extractY = yInBelief.length == 0 ? BELIEF : (t, b) -> b.subPath(yInBelief);
                extractYterm = $.func(DerivationFunctors.Belief, $.p(yInBelief));
                costPath += yInBelief.length;
            }
            Term t = m.ref.replace(x, extractXterm).replace(y, extractYterm);
            return new RelationConstraintAsPremisePredicate(t, cost + costPath * 0.01f, (RelationConstraint) m, extractX, extractY);
        } else {
            Term t = m.ref.replace(x, extractXterm);
            return new UnaryConstraintAsPremisePredicate(t, cost + costPath * 0.01f, (UnaryConstraint) m, extractX);
        }
    }

    public static final class UnaryConstraintAsPremisePredicate extends ConstraintAsPredicate<PreDerivation, UnaryConstraint<Derivation.PremiseUnify>> {

        UnaryConstraintAsPremisePredicate(Term id, float cost, UnaryConstraint<Derivation.PremiseUnify> m, BiFunction<Term, Term, Term> extractX) {
            super(id, cost, m, extractX, null);
        }

        @Override
        public boolean test(PreDerivation p) {
            Term x = extractX.apply(p.taskTerm, p.beliefTerm);
            //<- does this happen?
            return x != null ? constraint.valid(x) : true;
        }
    }

    public static final class RelationConstraintAsPremisePredicate extends ConstraintAsPredicate<Derivation, RelationConstraint<Derivation.PremiseUnify>> {

        RelationConstraintAsPremisePredicate(Term id, float cost, RelationConstraint<Derivation.PremiseUnify> m, BiFunction<Term, Term, Term> extractX, BiFunction<Term, Term, Term> extractY) {
            super(id, cost, m, extractX, extractY);
        }

        @Override
        public boolean test(Derivation d) {

            Term T = d.taskTerm, B = d.beliefTerm;

            Term x = extractX.apply(T, B);
            if (x != null) {
                Term y = extractY.apply(T, B);
                if (y!=null)
                    return !constraint.invalid(x, y, d.unify);
            }

            return true; //<- does this happen?

        }

    }


}
